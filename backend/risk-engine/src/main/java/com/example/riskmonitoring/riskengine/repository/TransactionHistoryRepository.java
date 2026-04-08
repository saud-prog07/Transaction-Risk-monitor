package com.example.riskmonitoring.riskengine.repository;

import com.example.riskmonitoring.riskengine.domain.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for TransactionHistory entities.
 * Provides query methods for risk analysis.
 */
@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, UUID> {

    /**
     * Finds all transactions for a user within a time window.
     *
     * @param userId the user ID
     * @param startTime the start of the time window
     * @param endTime the end of the time window
     * @return list of transactions
     */
    List<TransactionHistory> findByUserIdAndTimestampBetween(
            String userId, Instant startTime, Instant endTime);

    /**
     * Calculates the average transaction amount for a user over all transactions.
     *
     * @param userId the user ID
     * @return average amount, or null if no transactions
     */
    @Query("SELECT AVG(t.amount) FROM TransactionHistory t WHERE t.userId = ?1")
    BigDecimal findAverageAmountByUserId(String userId);

    /**
     * Counts transactions for a user within a time window.
     *
     * @param userId the user ID
     * @param startTime the start of the time window
     * @param endTime the end of the time window
     * @return transaction count
     */
    long countByUserIdAndTimestampBetween(
            String userId, Instant startTime, Instant endTime);

    /**
     * Finds all transactions for a user, ordered by timestamp descending.
     * Useful for trend analysis.
     *
     * @param userId the user ID
     * @param limit the maximum number of results (use limit to reduce memory)
     * @return list of recent transactions
     */
    @Query(value = "SELECT * FROM transaction_history WHERE user_id = ?1 ORDER BY timestamp DESC LIMIT ?2",
            nativeQuery = true)
    List<TransactionHistory> findRecentTransactionsByUserId(String userId, int limit);
}
