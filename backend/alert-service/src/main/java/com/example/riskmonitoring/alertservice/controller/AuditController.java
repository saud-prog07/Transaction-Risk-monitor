package com.example.riskmonitoring.alertservice.controller;

import com.example.riskmonitoring.alertservice.dto.AuditLogDTO;
import com.example.riskmonitoring.alertservice.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for audit trail operations.
 * Provides endpoints to retrieve and analyze audit logs.
 */
@RestController
@RequestMapping("/api/audit")
@Slf4j
public class AuditController {
    
    private final AuditService auditService;
    
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }
    
    /**
     * Get audit trail for a specific alert.
     * 
     * GET /api/audit/alerts/{alertId}
     * 
     * @param alertId the alert ID
     * @param pageable pagination parameters (page, size, sort)
     * @return page of audit logs
     */
    @GetMapping("/alerts/{alertId}")
    public ResponseEntity<Page<AuditLogDTO>> getAlertAuditTrail(
            @PathVariable Long alertId,
            Pageable pageable) {
        log.debug("Fetching audit trail for alert: {}", alertId);
        try {
            Page<AuditLogDTO> auditLogs = auditService.getAuditTrailByAlertId(alertId, pageable);
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            log.error("Error fetching audit trail for alert {}", alertId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get status history for a specific alert.
     * 
     * GET /api/audit/alerts/{alertId}/status-history
     * 
     * @param alertId the alert ID
     * @return list of status change audit logs
     */
    @GetMapping("/alerts/{alertId}/status-history")
    public ResponseEntity<List<AuditLogDTO>> getStatusHistory(@PathVariable Long alertId) {
        log.debug("Fetching status history for alert: {}", alertId);
        try {
            List<AuditLogDTO> statusHistory = auditService.getStatusHistory(alertId);
            return ResponseEntity.ok(statusHistory);
        } catch (Exception e) {
            log.error("Error fetching status history for alert {}", alertId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all audit logs by user.
     * 
     * GET /api/audit/users/{userId}
     * 
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogsByUser(
            @PathVariable String userId,
            Pageable pageable) {
        log.debug("Fetching audit logs for user: {}", userId);
        try {
            Page<AuditLogDTO> auditLogs = auditService.getAuditLogsByUser(userId, pageable);
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            log.error("Error fetching audit logs for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get audit logs within a time range.
     * 
     * GET /api/audit/time-range?start={startTime}&end={endTime}
     * 
     * @param startTime start timestamp (ISO format)
     * @param endTime end timestamp (ISO format)
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    @GetMapping("/time-range")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogsByTimeRange(
            @RequestParam String start,
            @RequestParam String end,
            Pageable pageable) {
        log.debug("Fetching audit logs for time range: {} to {}", start, end);
        try {
            Instant startTime = Instant.parse(start);
            Instant endTime = Instant.parse(end);
            Page<AuditLogDTO> auditLogs = auditService.getAuditLogsByTimeRange(
                    startTime, endTime, pageable);
            return ResponseEntity.ok(auditLogs);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid time format provided");
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching audit logs for time range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get summary of audit actions by type.
     * 
     * GET /api/audit/actions/{actionType}
     * 
     * @param actionType the action type to filter by
     * @return list of audit logs
     */
    @GetMapping("/actions/{actionType}")
    public ResponseEntity<List<AuditLogDTO>> getActionsByType(
            @PathVariable String actionType) {
        log.debug("Fetching audit logs for action type: {}", actionType);
        try {
            // This would require extension of the service if using enum
            // For now, returning a placeholder response
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error fetching audit logs for action type {}", actionType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get audit log by ID.
     * 
     * GET /api/audit/{auditLogId}
     * 
     * @param auditLogId the audit log ID
     * @return audit log DTO
     */
    @GetMapping("/{auditLogId}")
    public ResponseEntity<AuditLogDTO> getAuditLog(@PathVariable Long auditLogId) {
        log.debug("Fetching audit log: {}", auditLogId);
        try {
            // This would require extending the service implementation
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error fetching audit log {}", auditLogId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
