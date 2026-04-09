package com.example.riskmonitoring.alertservice.service;

import com.example.riskmonitoring.alertservice.domain.AlertEntity;
import com.example.riskmonitoring.alertservice.domain.AuditAction;
import com.example.riskmonitoring.alertservice.domain.AuditLog;
import com.example.riskmonitoring.alertservice.dto.AuditLogDTO;
import com.example.riskmonitoring.alertservice.exception.ForbiddenException;
import com.example.riskmonitoring.alertservice.exception.ResourceNotFoundException;
import com.example.riskmonitoring.alertservice.repository.AuditLogRepository;
import com.example.riskmonitoring.alertservice.repository.FlaggedTransactionRepository;
import com.example.riskmonitoring.alertservice.security.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for recording and retrieving audit logs.
 * Provides comprehensive audit trail functionality for compliance and investigation.
 * 
 * Security: All audit log access is verified through AuthorizationService to prevent
 * unauthorized access to audit trails. Users can only access audit logs they have
 * permission to view based on role and resource ownership.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final FlaggedTransactionRepository flaggedTransactionRepository;
    private final AuthorizationService authorizationService;
    
    public AuditService(AuditLogRepository auditLogRepository,
                        FlaggedTransactionRepository flaggedTransactionRepository,
                        AuthorizationService authorizationService) {
        this.auditLogRepository = auditLogRepository;
        this.flaggedTransactionRepository = flaggedTransactionRepository;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Record an action performed on an alert.
     * 
     * @param alert the alert being acted upon
     * @param action the action performed
     * @param userId the user performing the action
     * @param details optional details about the action
     * @return the created audit log
     */
    @Transactional
    public AuditLog recordAction(AlertEntity alert, AuditAction action, String userId, String details) {
        return recordAction(alert, action, userId, details, null, null, null, null);
    }
    
    /**
     * Record a status change action.
     * 
     * @param alert the alert whose status changed
     * @param userId the user making the change
     * @param previousStatus the previous status
     * @param newStatus the new status
     * @param details optional details about the change
     * @return the created audit log
     */
    @Transactional
    public AuditLog recordStatusChange(AlertEntity alert, String userId, String previousStatus, 
                                       String newStatus, String details) {
        return recordAction(alert, AuditAction.STATUS_CHANGED, userId, details, 
                           previousStatus, newStatus, null, null);
    }
    
    /**
     * Record an action with full context information.
     * 
     * @param alert the alert being acted upon
     * @param action the action performed
     * @param userId the user performing the action
     * @param details optional details about the action
     * @param previousStatus previous status (for status changes)
     * @param newStatus new status (for status changes)
     * @param ipAddress IP address of the request
     * @param requestId correlation ID for the request
     * @return the created audit log
     */
    @Transactional
    public AuditLog recordAction(AlertEntity alert, AuditAction action, String userId, String details,
                                String previousStatus, String newStatus, 
                                String ipAddress, String requestId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .alert(alert)
                    .action(action)
                    .userId(userId)
                    .details(details)
                    .previousStatus(previousStatus)
                    .newStatus(newStatus)
                    .ipAddress(ipAddress)
                    .requestId(requestId)
                    .timestamp(Instant.now())
                    .build();
            
            AuditLog saved = auditLogRepository.save(auditLog);
            log.info("Audit log recorded: alertId={}, action={}, userId={}", 
                    alert.getId(), action, userId);
            return saved;
        } catch (Exception e) {
            log.error("Failed to record audit log for alert {} with action {}", 
                    alert.getId(), action, e);
            throw new RuntimeException("Failed to record audit log", e);
        }
    }
    
    /**
     * Get audit trail for an alert.
     * 
     * @param alert the alert to get audit logs for
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    public Page<AuditLogDTO> getAuditTrail(AlertEntity alert, Pageable pageable) {
        return auditLogRepository.findByAlertOrderByTimestampDesc(alert, pageable)
                .map(this::toDTO);
    }
    
    /**
     * Get audit trail for an alert by ID.
     * Authorization: User must be owner of alert or have ADMIN/ANALYST role
     * 
     * @param alertId the alert ID
     * @param pageable pagination parameters
     * @return page of audit logs
     * @throws ForbiddenException if user not authorized to access this alert's audit
     * @throws ResourceNotFoundException if alert not found
     */
    public Page<AuditLogDTO> getAuditTrailByAlertId(Long alertId, Pageable pageable) {
        // Verify authorization using AuthorizationService
        authorizationService.verifyAlertAccess(alertId);
        
        return auditLogRepository.findByAlertId(alertId, pageable)
                .map(this::toDTO);
    }
    
    /**
     * Get audit logs by user.
     * Authorization: Only ADMIN can access other users' logs. Users access their own logs.
     * 
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of audit logs
     * @throws ForbiddenException if user not authorized to access these logs
     */
    public Page<AuditLogDTO> getAuditLogsByUser(String userId, Pageable pageable) {
        // Verify authorization - users can only access their own or ADMIN can access all
        if (!authorizationService.isAuthorizedForUserAudit(Long.parseLong(userId))) {
            String currentUser = authorizationService.getCurrentUsername();
            log.warn("Unauthorized attempt to access audit logs for user {} by {}", userId, currentUser);
            throw new ForbiddenException("You do not have permission to access these audit logs");
        }
        
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(this::toDTO);
    }
    
    /**
     * Get audit logs within a time range.
     * 
     * @param startTime start of time range
     * @param endTime end of time range
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    public Page<AuditLogDTO> getAuditLogsByTimeRange(Instant startTime, Instant endTime, 
                                                      Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(startTime, endTime, pageable)
                .map(this::toDTO);
    }
    
    /**
     * Get all status changes for an alert.
     * Authorization: User must be owner of alert or have ADMIN/ANALYST role
     * 
     * @param alertId the alert ID
     * @return list of status change audit logs
     * @throws ForbiddenException if user not authorized
     * @throws ResourceNotFoundException if alert not found
     */
    public List<AuditLogDTO> getStatusHistory(Long alertId) {
        // Verify authorization using AuthorizationService
        authorizationService.verifyAlertAccess(alertId);
        
        return auditLogRepository.findStatusTransitions(alertId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get count of audit logs for an alert.
     * 
     * @param alert the alert
     * @return count of audit logs
     */
    public long getAuditLogCount(AlertEntity alert) {
        return auditLogRepository.countByAlert(alert);
    }
    
    /**
     * Get actions by type.
     * 
     * @param action the action type
     * @return list of audit logs
     */
    public List<AuditLogDTO> getActionsByType(AuditAction action) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert AuditLog entity to DTO.
     */
    private AuditLogDTO toDTO(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .alertId(auditLog.getAlert() != null ? auditLog.getAlert().getId() : null)
                .action(auditLog.getAction().name())
                .userId(auditLog.getUserId())
                .timestamp(auditLog.getTimestamp())
                .details(auditLog.getDetails())
                .previousStatus(auditLog.getPreviousStatus())
                .newStatus(auditLog.getNewStatus())
                .ipAddress(auditLog.getIpAddress())
                .requestId(auditLog.getRequestId())
                .build();
    }
}
