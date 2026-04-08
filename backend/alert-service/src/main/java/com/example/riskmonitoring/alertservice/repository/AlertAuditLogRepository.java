package com.example.riskmonitoring.alertservice.repository;

import com.example.riskmonitoring.alertservice.domain.AlertAuditLog;
import com.example.riskmonitoring.alertservice.domain.FlaggedTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for AlertAuditLog entities.
 * Provides query methods for audit trail retrieval and analysis.
 */
@Repository
public interface AlertAuditLogRepository extends JpaRepository<AlertAuditLog, Long> {

    /**
     * Find all audit logs for a specific alert, ordered by timestamp descending.
     *
     * @param flaggedTransaction the flagged transaction
     * @return list of audit logs ordered by timestamp (newest first)
     */
    List<AlertAuditLog> findByFlaggedTransactionOrderByActionTimestampDesc(FlaggedTransaction flaggedTransaction);

    /**
     * Find audit logs for a specific alert with pagination.
     *
     * @param flaggedTransaction the flagged transaction
     * @param pageable pagination information
     * @return page of audit logs ordered by timestamp
     */
    Page<AlertAuditLog> findByFlaggedTransactionOrderByActionTimestampDesc(FlaggedTransaction flaggedTransaction, 
                                                                              Pageable pageable);

    /**
     * Find audit logs by action type.
     *
     * @param actionType the action type (e.g., "STATUS_CHANGED", "NOTES_ADDED")
     * @param pageable pagination information
     * @return page of audit logs
     */
    Page<AlertAuditLog> findByActionType(String actionType, Pageable pageable);

    /**
     * Find audit logs performed by a specific user.
     *
     * @param performedBy the user ID or system identifier
     * @param pageable pagination information
     * @return page of audit logs
     */
    Page<AlertAuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    /**
     * Find audit logs within a time range.
     *
     * @param from start timestamp (inclusive)
     * @param to end timestamp (inclusive)
     * @param pageable pagination information
     * @return page of audit logs
     */
    @Query("SELECT log FROM AlertAuditLog log WHERE log.actionTimestamp BETWEEN :from AND :to ORDER BY log.actionTimestamp DESC")
    Page<AlertAuditLog> findByActionTimestampBetween(Instant from, Instant to, Pageable pageable);

    /**
     * Count audit logs for a specific alert.
     *
     * @param flaggedTransaction the flagged transaction
     * @return count of audit logs
     */
    long countByFlaggedTransaction(FlaggedTransaction flaggedTransaction);
}
