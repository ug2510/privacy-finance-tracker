package com.privacyfinance.backend.repository;

import com.privacyfinance.backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Transaction}.
 *
 * <p>All queries operate on the local SQLite database — no network calls.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Fetch all transactions within a date range (for monthly reports). */
    List<Transaction> findByTransactionTimeBetweenOrderByTransactionTimeDesc(
            LocalDateTime from,
            LocalDateTime to
    );

    /** Fetch uncategorised transactions so the AI service can process them in batch. */
    List<Transaction> findByCategoryIsNull();

    /** Sum of DEBIT transactions in a given period (monthly spend). */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.type = 'DEBIT'
              AND t.transactionTime BETWEEN :from AND :to
            """)
    BigDecimal sumDebitsInPeriod(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /** Spend grouped by category for a given period — used by budget insights. */
    @Query("""
            SELECT t.category, SUM(t.amount)
            FROM Transaction t
            WHERE t.type = 'DEBIT'
              AND t.transactionTime BETWEEN :from AND :to
              AND t.category IS NOT NULL
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> spendByCategoryInPeriod(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
