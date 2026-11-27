package com.privacyfinance.backend.controller;

import com.privacyfinance.backend.dto.TransactionRequest;
import com.privacyfinance.backend.model.Transaction;
import com.privacyfinance.backend.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@Valid @RequestBody TransactionRequest request) {
        log.info("Received new transaction from mobile: merchant={} amount={}", request.getMerchant(), request.getAmount());
        Transaction saved = transactionService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

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

    @PostMapping("/categorise")
public ResponseEntity<Map<String, Object>> categorise() {
    int count = transactionService.categoriseUncategorised();
    return ResponseEntity.ok(Map.of(
            "status", "done",
            "categorised", count
    ));
}

@GetMapping("/summary")
public ResponseEntity<Map<String, Object>> summary() {
    BigDecimal monthlySpend = transactionService.totalSpendForCurrentMonth();
    return ResponseEntity.ok(Map.of(
            "currency", "INR",
            "currentMonthSpend", monthlySpend != null ? monthlySpend : BigDecimal.ZERO
    ));
}
}