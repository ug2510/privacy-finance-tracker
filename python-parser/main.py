import os
import tempfile
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from casparser import read_cas_pdf

app = FastAPI(title="CAS PDF Parser")


@app.post("/parse-cas")
async def parse_cas(
    file: UploadFile = File(...),
    password: str = Form(...)
):
    """
    Accept a CDSL/NSDL Consolidated Account Statement (PDF) and the
    investor's PAN as the password.

    Returns the parsed statement as JSON.
    Raises:
        400 – if the uploaded file is not a PDF.
        422 – if the password is wrong or the PDF is corrupt / unsupported.
    """
    # ----------------------------------------------------------------
    # 1. Validate file type early so we never touch the FS needlessly.
    # ----------------------------------------------------------------
    if file.content_type != "application/pdf":
        raise HTTPException(status_code=400, detail="Only PDF files are accepted")

    tmp_path = None
    try:
        # ----------------------------------------------------------------
        # 2. Write the uploaded bytes to a named temp file.
        #    casparser.read_cas_pdf() needs a real path, not a file object.
        #    delete=False lets us close the file and pass the path to casparser.
        # ----------------------------------------------------------------
        with tempfile.NamedTemporaryFile(delete=False, suffix=".pdf") as tmp:
            contents = await file.read()
            tmp.write(contents)
            tmp_path = tmp.name

        # ----------------------------------------------------------------
        # 3. Parse. Returns a Python dict → FastAPI auto-serialises to JSON.
        # ----------------------------------------------------------------
        try:
            parsed = read_cas_pdf(tmp_path, password=password)
        except Exception as exc:
            # Wrong password, corrupted file, unsupported format, etc.
            raise HTTPException(status_code=422, detail=str(exc))

        return parsed

    finally:
        # ----------------------------------------------------------------
        # 4. Always delete the temp file — even if an exception was raised.
        #    This keeps private financial data off the disk.
        # ----------------------------------------------------------------
        if tmp_path and os.path.exists(tmp_path):
            os.remove(tmp_path)
