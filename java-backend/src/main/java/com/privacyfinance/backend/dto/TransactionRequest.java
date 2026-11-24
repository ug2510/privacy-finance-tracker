package com.privacyfinance.backend.dto;

import com.privacyfinance.backend.model.Transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request payload sent by the mobile app when syncing a parsed SMS transaction.
 * Intentionally separate from the JPA entity to decouple the API contract.
 */
@Data
public class TransactionRequest {

    @NotBlank(message = "merchant must not be blank")
    private String merchant;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "type is required (DEBIT or CREDIT)")
    private TransactionType type;

    /** Optional: last 4 digits of the account number from the SMS. */
    private String accountLast4;

    /** Raw SMS body — preserved for audit and re-parsing. */
    private String rawSms;

    @NotNull(message = "transactionTime is required")
    private LocalDateTime transactionTime;
}
