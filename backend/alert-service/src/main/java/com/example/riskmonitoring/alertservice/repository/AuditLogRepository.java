package com.example.riskmonitoring.alertservice.repository;

import com.example.riskmonitoring.alertservice.domain.AuditLog;
import com.example.riskmonitoring.alertservice.domain.AuditAction;
import com.example.riskmonitoring.alertservice.domain.AlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for accessing audit logs.
 * Provides specialized queries for audit trail retrieval and analysis.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * Find all audit logs for a specific alert, ordered by timestamp descending.
     */
    Page<AuditLog> findByAlertOrderByTimestampDesc(AlertEntity alert, Pageable pageable);
    
    /**
     * Find audit logs by alert ID, ordered by timestamp descending.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.alert.id = :alertId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByAlertId(@Param("alertId") Long alertId, Pageable pageable);
    
    /**
     * Find audit logs by action type.
     */
    List<AuditLog> findByActionOrderByTimestampDesc(AuditAction action);
    
    /**
     * Find audit logs by user ID.
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    
    /**
     * Find audit logs within a time range.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    Page<AuditLog> findByTimestampBetween(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable
    );
    
    /**
     * Find audit logs for an alert with specific action.
     */
    List<AuditLog> findByAlertAndActionOrderByTimestampDesc(AlertEntity alert, AuditAction action);
    
    /**
     * Count audit logs for an alert.
     */
    long countByAlert(AlertEntity alert);
    
    /**
     * Find status transition history for alert.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.alert.id = :alertId AND a.action = 'STATUS_CHANGED' ORDER BY a.timestamp DESC")
    List<AuditLog> findStatusTransitions(@Param("alertId") Long alertId);
}
