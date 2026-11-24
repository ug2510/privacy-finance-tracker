package com.privacyfinance.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single financial transaction extracted from an SMS bank alert.
 *
 * <p>All data is stored locally in SQLite. The {@code category} field is populated
 * by Spring AI / Gemini after the mobile app syncs the raw SMS data.
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Raw merchant name as parsed from the SMS (e.g., "ZOMATO*ORDER"). */
    @NotBlank
    @Column(nullable = false)
    private String merchant;

    /** Transaction amount in INR. */
    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** DEBIT or CREDIT. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /**
     * AI-assigned category (e.g., "Food & Dining", "Transport", "Groceries").
     * Null until categorised by the AI service.
     */
    @Column
    private String category;

    /** Bank account last-4 digits to distinguish multiple accounts. */
    @Column(length = 4)
    private String accountLast4;

    /** Raw SMS body — kept for audit / re-parsing. */
    @Column(columnDefinition = "TEXT")
    private String rawSms;

    /** When the transaction actually occurred (from SMS timestamp). */
    @NotNull
    @Column(nullable = false)
    private LocalDateTime transactionTime;

    /** When this record was created in our DB. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        DEBIT, CREDIT
    }
}
