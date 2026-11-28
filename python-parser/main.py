import os
import re
import tempfile
from typing import Any, Dict, List

import pdfplumber
from casparser import read_cas_pdf
from fastapi import FastAPI, File, Form, HTTPException, UploadFile

app = FastAPI(title="CAS PDF Parser")

# Matches Indian ISINs: INE... (Equities/Bonds), INF... (Mutual Funds), IN0... (G-Sec)
ISIN_REGEX = re.compile(r"\b(IN[EF0][A-Z0-9]{9})\b")


def _clean_number(val: Any) -> str:
    """Strip commas/currency symbols and return a clean decimal string for Java BigDecimal."""
    if val is None:
        return "0"
    s = re.sub(r"[,₹Rs\s]", "", str(val)).strip()
    if not s or s in {"-", "--", "NA", "N/A"}:
        return "0"
    try:
        float(s)
        return s
    except ValueError:
        return "0"


def _normalize_casparser_output(raw: Dict[str, Any]) -> Dict[str, Any]:
    """Flatten casparser's folios -> schemes hierarchy into CasService's expected 'holdings' list."""
    holdings: List[Dict[str, Any]] = []
    for folio in raw.get("folios", []):
        folio_num = folio.get("folio") or "UNKNOWN"
        for scheme in folio.get("schemes", []):
            valuation = scheme.get("valuation") or {}
            units = _clean_number(scheme.get("close") or scheme.get("units"))
            nav = _clean_number(valuation.get("nav"))
            value = _clean_number(valuation.get("value"))

            # Skip zero-unit closed folios
            if float(units) <= 0 and float(value) <= 0:
                continue

            isin = scheme.get("isin") or "UNKNOWN"
            asset_type = "EQUITY" if isin.startswith("INE") else "MUTUAL_FUND"

            holdings.append({
                "isin": isin,
                "scheme_name": (scheme.get("scheme") or "Unknown Scheme").strip(),
                "folio_number": str(folio_num).strip(),
                "units": units,
                "nav": nav,
                "value": value,
                "asset_type": asset_type,
            })

    return {"source": "CAMS_KFINTECH", "holdings": holdings}


def _parse_nsdl_ecas(pdf_path: str, password: str) -> Dict[str, Any]:
    """
    Unlock an NSDL / CDSL e-CAS PDF using pdfplumber and extract all rows containing
    an ISIN (demat equities, ETFs, and mutual fund units) into CasService's schema.
    """
    holdings_by_key: Dict[str, Dict[str, Any]] = {}
    current_folio = "NSDL-DEMAT"

    with pdfplumber.open(pdf_path, password=password) as pdf:
        for page in pdf.pages:
            page_text = page.extract_text() or ""

            # Capture actual DP ID or numeric Folio Number (avoids matching the word "No")
            dp_match = re.search(r"DP\s*ID\s*[:\-]?\s*(IN\d+)", page_text, re.I)
            folio_match = re.search(r"Folio\s*No\.?\s*[:\-]?\s*([0-9][A-Z0-9/\-]{3,})", page_text, re.I)
            if dp_match:
                current_folio = dp_match.group(1).strip()
            elif folio_match:
                current_folio = folio_match.group(1).strip()

            tables = page.extract_tables() or []
            for table in tables:
                for row in table:
                    if not row:
                        continue
                    cells = [c.replace("\n", " ").strip() if c else "" for c in row]
                    row_joined = " ".join(cells)

                    isin_match = ISIN_REGEX.search(row_joined)
                    if not isin_match:
                        continue

                    isin = isin_match.group(1)

                    nums: List[str] = []
                    text_parts: List[str] = []
                    for cell in cells:
                        if not cell or isin in cell:
                            cleaned_text = ISIN_REGEX.sub("", cell).strip(" -:")
                            if cleaned_text and not re.fullmatch(r"[\d,.\-]+", cleaned_text):
                                text_parts.append(cleaned_text)
                            continue

                        num_str = _clean_number(cell)
                        if num_str != "0" or re.fullmatch(r"0(?:\.0+)?", cell.replace(",", "").strip()):
                            nums.append(num_str)
                        elif len(cell) > 2:
                            text_parts.append(cell)

                    if not nums:
                        continue

                    scheme_name = max(text_parts, key=len) if text_parts else isin
                    is_demat = current_folio.startswith("IN")

                    if is_demat and len(nums) >= 3:
                        # Demat table ends with: [Units, Market Price, Value]
                        units, nav, value = nums[-3], nums[-2], nums[-1]
                    elif not is_demat and len(nums) >= 4:
                        # NSDL MF SoA table ends with: [Units, Cost, Current Value, Gain/Loss]
                        units = nums[-4]
                        value = nums[-2]
                        nav = str(round(float(value) / float(units), 4)) if float(units) > 0 else "0"
                    elif len(nums) >= 2:
                        units, value = nums[0], nums[-1]
                        nav = str(round(float(value) / float(units), 4)) if float(units) > 0 else "0"
                    else:
                        units, nav, value = nums[0], "0", "0"

                    if float(units) <= 0 and float(value) <= 0:
                        continue

                    asset_type = "EQUITY" if isin.startswith("INE") else "MUTUAL_FUND"
                    dedup_key = f"{isin}_{current_folio}"

                    holdings_by_key[dedup_key] = {
                        "isin": isin,
                        "scheme_name": scheme_name[:200],
                        "folio_number": current_folio,
                        "units": units,
                        "nav": nav,
                        "value": value,
                        "asset_type": asset_type,
                    }

    if not holdings_by_key:
        raise ValueError("Unlocked PDF, but no ISIN holdings tables could be extracted.")

    return {"source": "NSDL_CDSL_ECAS", "holdings": list(holdings_by_key.values())}


@app.post("/parse-cas")
async def parse_cas(
    file: UploadFile = File(...),
    password: str = Form(...)
):
    if file.content_type != "application/pdf":
        raise HTTPException(status_code=400, detail="Only PDF files are accepted")

    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".pdf") as tmp:
            contents = await file.read()
            tmp.write(contents)
            tmp_path = tmp.name

        # 1. Try CAMS / KFintech parser first
        try:
            raw_cas = read_cas_pdf(tmp_path, password=password)
            return _normalize_casparser_output(raw_cas)
        except Exception as cas_err:
            # 2. Fall back to NSDL / CDSL e-CAS table parser
            try:
                return _parse_nsdl_ecas(tmp_path, password=password)
            except Exception as nsdl_err:
                raise HTTPException(
                    status_code=422,
                    detail=f"Failed to parse CAS PDF. CAMS error: {cas_err} | NSDL error: {nsdl_err}"
                )
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.remove(tmp_path)