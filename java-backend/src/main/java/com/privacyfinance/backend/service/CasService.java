package com.privacyfinance.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privacyfinance.backend.model.InvestmentHolding;
import com.privacyfinance.backend.repository.InvestmentHoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Orchestrates the CAS PDF parsing pipeline:
 *  1. Forwards the PDF + password to the Python FastAPI micro-service.
 *  2. Maps the JSON response to InvestmentHolding JPA entities.
 *  3. Upserts each holding in SQLite (insert new, update existing).
 *  4. Returns the full portfolio summary (holdings + total value).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CasService {

    private final InvestmentHoldingRepository holdingRepo;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String PYTHON_PARSER_URL = "http://localhost:8000/parse-cas";

    // -----------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------

    /**
     * Send the raw PDF bytes + PAN password to the Python parser,
     * upsert the parsed holdings and return a portfolio summary.
     */
    @Transactional
    public Map<String, Object> uploadAndProcess(byte[] pdfBytes, String password) {

        // 1️⃣ Call the Python micro-service
        String json = callPythonParser(pdfBytes, password);

        // 2️⃣ Parse JSON → list of InvestmentHolding entities
        List<InvestmentHolding> holdings = mapJsonToHoldings(json);

        // 3️⃣ Upsert each holding in SQLite
        List<InvestmentHolding> persisted = upsertAll(holdings);

        // 4️⃣ Build response
        BigDecimal totalValue = persisted.stream()
                .map(InvestmentHolding::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalValue", totalValue);
        result.put("currency", "INR");
        result.put("lastSynced", LocalDateTime.now().toString());
        result.put("holdings", persisted);
        return result;
    }

    /** Return all holdings stored in SQLite (read-only, no parser call). */
    public Map<String, Object> getAllHoldings() {
        List<InvestmentHolding> holdings = holdingRepo.findAll();
        BigDecimal totalValue = holdings.stream()
                .map(InvestmentHolding::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalValue", totalValue);
        result.put("currency", "INR");
        result.put("holdings", holdings);
        return result;
    }

    // -----------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------

    private String callPythonParser(byte[] pdfBytes, String password) {
        // Build a multipart/form-data body with the PDF file + password field
        ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return "cas_statement.pdf"; // filename required by multipart spec
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_PDF);

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(pdfResource, fileHeaders));
        body.add("password", password);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response = restTemplate.exchange(
                PYTHON_PARSER_URL,
                HttpMethod.POST,
                new HttpEntity<>(body, requestHeaders),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException(
                    "Python parser returned non-2xx: " + response.getStatusCode());
        }

        log.debug("Python parser response received ({} chars)", response.getBody().length());
        return response.getBody();
    }

    private List<InvestmentHolding> mapJsonToHoldings(String json) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse JSON from Python parser", e);
        }

        // casparser puts holdings inside a "holdings" array
        JsonNode holdingsNode = root.path("holdings");
        if (!holdingsNode.isArray()) {
            throw new IllegalArgumentException(
                    "Expected 'holdings' array in parser response but got: " + root.getNodeType());
        }

        List<InvestmentHolding> result = new ArrayList<>();
        for (JsonNode h : holdingsNode) {
            result.add(InvestmentHolding.builder()
                    .isin(h.path("isin").asText())
                    .schemeName(h.path("scheme_name").asText())
                    .folioNumber(h.path("folio_number").asText())
                    .units(new BigDecimal(h.path("units").asText("0")))
                    .nav(new BigDecimal(h.path("nav").asText("0")))
                    .currentValue(new BigDecimal(h.path("value").asText("0")))
                    .assetType(h.path("asset_type").asText("MUTUAL_FUND"))
                    .lastUpdated(LocalDateTime.now())
                    .build());
        }
        return result;
    }

    private List<InvestmentHolding> upsertAll(List<InvestmentHolding> holdings) {
        List<InvestmentHolding> saved = new ArrayList<>();
        for (InvestmentHolding h : holdings) {
            holdingRepo.findByIsinAndFolioNumber(h.getIsin(), h.getFolioNumber())
                    .ifPresentOrElse(
                            existing -> {
                                // Update mutable financial fields only
                                existing.setUnits(h.getUnits());
                                existing.setNav(h.getNav());
                                existing.setCurrentValue(h.getCurrentValue());
                                existing.setLastUpdated(h.getLastUpdated());
                                saved.add(holdingRepo.save(existing));
                            },
                            () -> saved.add(holdingRepo.save(h))
                    );
        }
        return saved;
    }
}
