package com.example.riskmonitoring.alertservice.repository;

import com.example.riskmonitoring.alertservice.domain.FlaggedTransaction;
import com.example.riskmonitoring.common.models.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FlaggedTransaction entities.
 * Provides query methods for alert retrieval and analysis.
 */
@Repository
public interface FlaggedTransactionRepository extends JpaRepository<FlaggedTransaction, Long> {

    /**
     * Finds a flagged transaction by transaction ID.
     *
     * @param transactionId the transaction ID
     * @return Optional containing the flagged transaction if found
     */
    Optional<FlaggedTransaction> findByTransactionId(UUID transactionId);

    /**
     * Finds all flagged transactions with a specific risk level.
     *
     * @param riskLevel the risk level
     * @param pageable pagination information
     * @return page of flagged transactions
     */
    Page<FlaggedTransaction> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);

    /**
     * Finds all unreviewed flagged transactions.
     *
     * @param pageable pagination information
     * @return page of unreviewed flagged transactions
     */
    Page<FlaggedTransaction> findByReviewedFalse(Pageable pageable);

    /**
     * Finds all flagged transactions created within a time range.
     *
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @param pageable pagination information
     * @return page of flagged transactions within the range
     */
    Page<FlaggedTransaction> findByCreatedAtBetween(Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Counts flagged transactions by risk level.
     *
     * @param riskLevel the risk level
     * @return count of transactions
     */
    long countByRiskLevel(RiskLevel riskLevel);

    /**
     * Counts unreviewed flagged transactions.
     *
     * @return count of unreviewed transactions
     */
    long countByReviewedFalse();

    /**
     * Finds all HIGH risk transactions.
     *
     * @param pageable pagination information
     * @return page of high risk transactions
     */
    @Query("SELECT f FROM FlaggedTransaction f WHERE f.riskLevel = 'HIGH' ORDER BY f.createdAt DESC")
    Page<FlaggedTransaction> findHighRiskTransactions(Pageable pageable);

    /**
     * Finds all MEDIUM risk transactions.
     *
     * @param pageable pagination information
     * @return page of medium risk transactions
     */
    @Query("SELECT f FROM FlaggedTransaction f WHERE f.riskLevel = 'MEDIUM' ORDER BY f.createdAt DESC")
    Page<FlaggedTransaction> findMediumRiskTransactions(Pageable pageable);

    /**
     * Finds transactions created in the last N days.
     *
     * @param days the number of days
     * @param pageable pagination information
     * @return page of recent transactions
     */
    @Query(value = "SELECT f FROM FlaggedTransaction f WHERE f.createdAt >= CURRENT_TIMESTAMP - INTERVAL :days DAY ORDER BY f.createdAt DESC")
    Page<FlaggedTransaction> findRecentTransactions(int days, Pageable pageable);

    // ==================== USER-AWARE QUERY METHODS ====================
    // These methods filter by user (createdBy) to prevent IDOR vulnerabilities
    
    /**
     * Finds all flagged transactions created by a specific user.
     * Used for regular users to see only their own alerts.
     *
     * @param createdBy the username who created the alert
     * @param pageable pagination information
     * @return page of flagged transactions created by the user
     */
    Page<FlaggedTransaction> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Finds flagged transactions by risk level and creator.
     * Used for regular users to filter their alerts by risk level.
     *
     * @param riskLevel the risk level
     * @param createdBy the username who created the alert
     * @param pageable pagination information
     * @return page of flagged transactions
     */
    Page<FlaggedTransaction> findByRiskLevelAndCreatedBy(RiskLevel riskLevel, String createdBy, Pageable pageable);

    /**
     * Finds unreviewed flagged transactions created by a specific user.
     * Used for regular users to see their own unreviewed alerts.
     *
     * @param createdBy the username who created the alert
     * @param pageable pagination information
     * @return page of unreviewed flagged transactions created by the user
     */
    Page<FlaggedTransaction> findByReviewedFalseAndCreatedBy(String createdBy, Pageable pageable);

    /**
     * Finds HIGH risk transactions created by a specific user.
     * Used for regular users to see their own high-risk alerts.
     *
     * @param createdBy the username who created the alert
     * @param pageable pagination information
     * @return page of high risk transactions created by the user
     */
    @Query("SELECT f FROM FlaggedTransaction f WHERE f.riskLevel = 'HIGH' AND f.createdBy = :createdBy ORDER BY f.createdAt DESC")
    Page<FlaggedTransaction> findHighRiskTransactionsByUser(String createdBy, Pageable pageable);

    /**
     * Finds MEDIUM risk transactions created by a specific user.
     * Used for regular users to see their own medium-risk alerts.
     *
     * @param createdBy the username who created the alert
     * @param pageable pagination information
     * @return page of medium risk transactions created by the user
     */
    @Query("SELECT f FROM FlaggedTransaction f WHERE f.riskLevel = 'MEDIUM' AND f.createdBy = :createdBy ORDER BY f.createdAt DESC")
    Page<FlaggedTransaction> findMediumRiskTransactionsByUser(String createdBy, Pageable pageable);
}
