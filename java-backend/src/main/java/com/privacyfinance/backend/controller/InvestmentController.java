package com.privacyfinance.backend.controller;

import com.privacyfinance.backend.service.CasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller exposing investment-portfolio endpoints.
 *
 *  POST /api/v1/investments/upload-cas  – parse a CAS PDF and sync holdings
 *  GET  /api/v1/investments             – return current holdings + total value
 */
@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
@Slf4j
public class InvestmentController {

    private final CasService casService;

    /**
     * Upload a CDSL/NSDL CAS PDF along with the investor's PAN as the password.
     * The file is forwarded to the Python micro-service for parsing; the resulting
     * holdings are upserted in SQLite and returned as JSON.
     */
    @PostMapping(
            value = "/upload-cas",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> uploadCas(
            @RequestPart("file") MultipartFile file,
            @RequestPart("password") String password
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Uploaded file is empty"));
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF files are accepted"));
        }

        try {
            byte[] bytes = file.getBytes();
            log.info("Received CAS PDF upload ({} bytes), forwarding to Python parser", bytes.length);
            Map<String, Object> result = casService.uploadAndProcess(bytes, password);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("CAS upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Return all holdings currently stored in SQLite along with total portfolio value.
     * No PDF upload required – reads directly from the local database.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listHoldings() {
        return ResponseEntity.ok(casService.getAllHoldings());
    }
}
