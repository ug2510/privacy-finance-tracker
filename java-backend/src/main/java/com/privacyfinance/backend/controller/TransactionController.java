package com.privacyfinance.backend.controller;

import com.privacyfinance.backend.dto.TransactionRequest;
import com.privacyfinance.backend.model.Transaction;
import com.privacyfinance.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing financial transaction endpoints to the mobile app.
 *
 * <p>All routes are prefixed with {@code /api/v1/transactions}.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    // ------------------------------------------------------------------
    // POST /api/v1/transactions
    //   Called by the mobile app whenever a new SMS is parsed.
    // ------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<Transaction> create(@Valid @RequestBody TransactionRequest request) {
        log.info("Received new transaction from mobile: merchant={} amount={}", request.getMerchant(), request.getAmount());
        Transaction saved = transactionService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ------------------------------------------------------------------
    // GET /api/v1/transactions
    //   Optional date-range filter: ?from=2024-01-01T00:00:00&to=2024-01-31T23:59:59
    // ------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<Transaction>> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<Transaction> results = (from != null && to != null)
                ? transactionService.findByPeriod(from, to)
                : transactionService.findAll();
        return ResponseEntity.ok(results);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/transactions/categorise
    //   Triggers batch AI categorisation of all null-category transactions.
    //   Call this manually or wire it to a @Scheduled job.
    // ------------------------------------------------------------------
    @PostMapping("/categorise")
    public ResponseEntity<Map<String, Object>> categorise() {
        int count = transactionService.categoriseUncategorised();
        return ResponseEntity.ok(Map.of(
                "status", "done",
                "categorised", count
        ));
    }

    // ------------------------------------------------------------------
    // GET /api/v1/transactions/summary
    //   Quick monthly spend total for the dashboard widget on the app.
    // ------------------------------------------------------------------
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        BigDecimal monthlySpend = transactionService.totalSpendForCurrentMonth();
        return ResponseEntity.ok(Map.of(
                "currency", "INR",
                "currentMonthSpend", monthlySpend
        ));
    }
}
