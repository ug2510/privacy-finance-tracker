package com.privacyfinance.backend.service;

import com.privacyfinance.backend.dto.TransactionRequest;
import com.privacyfinance.backend.model.Transaction;
import com.privacyfinance.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core business logic for financial transactions.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Persist transactions synced from the mobile app.</li>
 *   <li>Trigger AI categorisation via Spring AI + Gemini (async-safe — can be
 *       called in a @Scheduled job or on-demand).</li>
 *   <li>Provide aggregated data for budget insights.</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ChatClient chatClient;

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Transactional
    public Transaction save(TransactionRequest req) {
        Transaction tx = Transaction.builder()
                .merchant(req.getMerchant())
                .amount(req.getAmount())
                .type(req.getType())
                .accountLast4(req.getAccountLast4())
                .rawSms(req.getRawSms())
                .transactionTime(req.getTransactionTime())
                .build();
        Transaction saved = transactionRepository.save(tx);
        log.debug("Saved transaction id={} merchant={} amount={}", saved.getId(), saved.getMerchant(), saved.getAmount());
        return saved;
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public List<Transaction> findByPeriod(LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findByTransactionTimeBetweenOrderByTransactionTimeDesc(from, to);
    }

    // -------------------------------------------------------------------------
    // AI Categorisation via Spring AI + Gemini
    // -------------------------------------------------------------------------

    /**
     * Categorise all transactions whose {@code category} is null.
     * Safe to call in a @Scheduled job or as a one-shot POST endpoint.
     *
     * @return number of transactions categorised in this run
     */
    @Transactional
    public int categoriseUncategorised() {
        List<Transaction> uncategorised = transactionRepository.findByCategoryIsNull();
        if (uncategorised.isEmpty()) {
            log.info("No uncategorised transactions found.");
            return 0;
        }

        for (Transaction tx : uncategorised) {
            String category = categoriseWithAi(tx.getMerchant(), tx.getAmount());
            tx.setCategory(category);
            transactionRepository.save(tx);
            log.debug("Categorised id={} merchant='{}' → '{}'", tx.getId(), tx.getMerchant(), category);
        }
        return uncategorised.size();
    }

    /**
     * Calls Gemini via Spring AI to infer the best expense category for a merchant.
     *
     * <p>The prompt is intentionally terse — Gemini is asked to respond with a
     * single category label only (no explanation) to keep parsing trivial.
     */
    private String categoriseWithAi(String merchant, BigDecimal amount) {
        String prompt = """
                You are an Indian personal-finance assistant. Given a merchant name and amount,
                respond with ONLY the single best expense category from this list:
                Food & Dining, Groceries, Transport, Fuel, Shopping, Entertainment,
                Utilities, Healthcare, Education, Investments, Transfers, Salary, Other.

                Merchant: %s
                Amount: ₹%s

                Category:""".formatted(merchant, amount.toPlainString());

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            // Trim whitespace/newlines and take only the first line in case of verbose models
            return response == null ? "Other" : response.strip().lines().findFirst().orElse("Other");
        } catch (Exception e) {
            log.warn("AI categorisation failed for merchant='{}': {}", merchant, e.getMessage());
            return "Other";
        }
    }

    // -------------------------------------------------------------------------
    // Budget Insights
    // -------------------------------------------------------------------------

    public BigDecimal totalSpendForCurrentMonth() {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end   = LocalDateTime.now();
        return transactionRepository.sumDebitsInPeriod(start, end);
    }
}
