package com.example.riskmonitoring.alertservice.service;

import com.example.riskmonitoring.alertservice.domain.AlertAuditLog;
import com.example.riskmonitoring.alertservice.domain.AlertStatus;
import com.example.riskmonitoring.alertservice.domain.FlaggedTransaction;
import com.example.riskmonitoring.alertservice.dto.AlertAuditLogResponse;
import com.example.riskmonitoring.alertservice.dto.AlertRequest;
import com.example.riskmonitoring.alertservice.dto.AlertResponse;
import com.example.riskmonitoring.alertservice.dto.AlertStatusUpdateRequest;
import com.example.riskmonitoring.alertservice.exception.AlertAlreadyExistsException;
import com.example.riskmonitoring.alertservice.exception.AlertNotFoundException;
import com.example.riskmonitoring.alertservice.repository.AlertAuditLogRepository;
import com.example.riskmonitoring.alertservice.repository.FlaggedTransactionRepository;
import com.example.riskmonitoring.common.logging.StructuredLogger;
import com.example.riskmonitoring.common.models.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing flagged transaction alerts.
 * Handles creation, retrieval, and updating of alert records with full audit logging.
 */
@Slf4j
@Service
@Transactional
public class AlertService {

    private final FlaggedTransactionRepository flaggedTransactionRepository;
    private final AlertAuditLogRepository alertAuditLogRepository;
    private final NotificationService notificationService;
    private final StructuredLogger structuredLogger = StructuredLogger.getLogger(AlertService.class);

    public AlertService(FlaggedTransactionRepository flaggedTransactionRepository,
                        AlertAuditLogRepository alertAuditLogRepository,
                        NotificationService notificationService) {
        this.flaggedTransactionRepository = flaggedTransactionRepository;
        this.alertAuditLogRepository = alertAuditLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new alert for a flagged transaction.
     *
     * @param alertRequest the alert request containing transaction and risk details
     * @return the created alert response
     * @throws AlertAlreadyExistsException if the transaction was already flagged
     */
    public AlertResponse createAlert(AlertRequest alertRequest) {
        UUID transactionId = UUID.fromString(alertRequest.getTransactionId());
        structuredLogger.setTransactionId(transactionId.toString());

        if (flaggedTransactionRepository.findByTransactionId(transactionId).isPresent()) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "DUPLICATE_ALERT");
            structuredLogger.warn("Alert already exists for this transaction", context);
            throw new AlertAlreadyExistsException(
                    "Alert already exists for transaction: " + transactionId);
        }

        try {
            RiskLevel riskLevel = RiskLevel.valueOf(alertRequest.getRiskLevel().toUpperCase());

            FlaggedTransaction flaggedTransaction = FlaggedTransaction.builder()
                    .transactionId(transactionId)
                    .riskLevel(riskLevel)
                    .reason(alertRequest.getReason())
                    .status(AlertStatus.NEW)
                    .build();

            FlaggedTransaction saved = flaggedTransactionRepository.save(flaggedTransaction);

            // Create audit log for alert creation
            createAuditLog(saved, "CREATED", AlertStatus.NEW.toString(), AlertStatus.NEW.toString(),
                    "Alert created for flagged transaction", null, "SYSTEM");

            Map<String, Object> flaggedContext = new HashMap<>();
            flaggedContext.put("event", "TRANSACTION_FLAGGED");
            flaggedContext.put("riskLevel", riskLevel.toString());
            flaggedContext.put("reason", alertRequest.getReason());
            structuredLogger.info("Transaction flagged and alert created", flaggedContext);

            notificationService.notifyFlaggedTransaction(saved);

            return mapToResponse(saved);

        } catch (IllegalArgumentException ex) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("event", "INVALID_RISK_LEVEL");
            errorContext.put("invalidValue", alertRequest.getRiskLevel());
            structuredLogger.error("Invalid risk level provided", errorContext, ex);
            throw new IllegalArgumentException("Invalid risk level: " + alertRequest.getRiskLevel());
        } catch (Exception ex) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("event", "ALERT_CREATION_ERROR");
            errorContext.put("error", ex.getMessage());
            structuredLogger.error("Failed to create alert", errorContext, ex);
            throw new RuntimeException("Failed to create alert: " + ex.getMessage(), ex);
        }
    
    /**
     * Exports flagged transactions to CSV format.
     * 
     * @param status optional filter by alert status
     * @param riskLevel optional filter by risk level
     * @return CSV data as byte array
     */
    public byte[] exportAlertsToCsv(String status, String riskLevel) {
        StringBuilder csvBuilder = new StringBuilder();
        
        // CSV Header
        csvBuilder.append("Alert ID,Transaction ID,Risk Level,Reason,Status,Created At,Updated At,Investigated By,Investigation Notes\n");
        
        // Get filtered alerts
        List<FlaggedTransaction> transactions;
        if (status != null && !status.isEmpty() && riskLevel != null && !riskLevel.isEmpty()) {
            transactions = flaggedTransactionRepository.findByStatusAndRiskLevel(
                    AlertStatus.fromString(status), 
                    RiskLevel.valueOf(riskLevel.toUpperCase()));
        } else if (status != null && !status.isEmpty()) {
            transactions = flaggedTransactionRepository.findByStatus(
                    AlertStatus.fromString(status));
        } else if (riskLevel != null && !riskLevel.isEmpty()) {
            transactions = flaggedTransactionRepository.findByRiskLevel(
                    RiskLevel.valueOf(riskLevel.toUpperCase()));
        } else {
            transactions = flaggedTransactionRepository.findAll();
        }
        
        // Build CSV rows
        for (FlaggedTransaction transaction : transactions) {
            csvBuilder.append(transaction.getId()).append(',')
                    .append('"').append(transaction.getTransactionId()).append('"').append(',')
                    .append(transaction.getRiskLevel()).append(',')
                    .append('"').append(escapeCsv(transaction.getReason())).append('"').append(',')
                    .append(transaction.getStatus()).append(',')
                    .append('"').append(transaction.getCreatedAt()).append('"').append(',')
                    .append('"').append(transaction.getUpdatedAt()).append('"').append(',')
                    .append('"').append(transaction.getInvestigatedBy() != null ? transaction.getInvestigatedBy() : "").append('"').append(',')
                    .append('"').append(escapeCsv(transaction.getInvestigationNotes() != null ? transaction.getInvestigationNotes() : "")).append('"')
                    .append('\n');
        }
        
        return csvBuilder.toString().getBytes();
    }
    
    /**
     * Escapes special characters for CSV format.
     * 
     * @param input the input string
     * @return escaped string
     */
    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}

    /**
     * Retrieves an alert by ID.
     *
     * @param alertId the alert ID
     * @return the alert response with audit logs
     * @throws AlertNotFoundException if the alert does not exist
     */
    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long alertId) {
        FlaggedTransaction transaction = flaggedTransactionRepository.findById(alertId)
                .orElseThrow(() -> {
                    Map<String, Object> context = new HashMap<>();
                    context.put("event", "ALERT_NOT_FOUND");
                    context.put("alertId", alertId);
                    structuredLogger.warn("Alert not found with ID", context);
                    return new AlertNotFoundException("Alert not found with ID: " + alertId);
                });

        structuredLogger.setTransactionId(transaction.getTransactionId().toString());
        return mapToResponse(transaction);
    }

    /**
     * Retrieves all flagged transactions with pagination.
     *
     * @param pageable pagination information
     * @return page of alert responses
     */
    @Transactional(readOnly = true)
    public Page<AlertResponse> getAllAlerts(Pageable pageable) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "FETCH_ALL_ALERTS");
        context.put("page", pageable.getPageNumber());
        context.put("size", pageable.getPageSize());
        structuredLogger.debug("Fetching all alerts with pagination", context);
        return flaggedTransactionRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Retrieves flagged transactions by risk level.
     *
     * @param riskLevel the risk level to filter by
     * @param pageable pagination information
     * @return page of alert responses
     */
    @Transactional(readOnly = true)
    public Page<AlertResponse> getAlertsByRiskLevel(RiskLevel riskLevel, Pageable pageable) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "FETCH_ALERTS_BY_RISK");
        context.put("riskLevel", riskLevel.toString());
        context.put("page", pageable.getPageNumber());
        structuredLogger.debug("Fetching alerts by risk level", context);
        return flaggedTransactionRepository.findByRiskLevel(riskLevel, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Retrieves all unreviewed flagged transactions.
     *
     * @param pageable pagination information
     * @return page of unreviewed alert responses
     */
    @Transactional(readOnly = true)
    public Page<AlertResponse> getUnreviewedAlerts(Pageable pageable) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "FETCH_UNREVIEWED_ALERTS");
        context.put("page", pageable.getPageNumber());
        structuredLogger.debug("Fetching unreviewed alerts", context);
        return flaggedTransactionRepository.findByReviewedFalse(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Retrieves HIGH risk flagged transactions.
     *
     * @param pageable pagination information
     * @return page of high risk alert responses
     */
    @Transactional(readOnly = true)
    public Page<AlertResponse> getHighRiskAlerts(Pageable pageable) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "FETCH_HIGH_RISK_ALERTS");
        context.put("page", pageable.getPageNumber());
        structuredLogger.debug("Fetching high risk alerts", context);
        return flaggedTransactionRepository.findHighRiskTransactions(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Updates the investigation status of an alert.
     *
     * @param alertId the alert ID
     * @param updateRequest the status update request
     * @return the updated alert response
     * @throws AlertNotFoundException if the alert does not exist
     * @throws IllegalArgumentException if status transition is invalid
     */
    public AlertResponse updateAlertStatus(Long alertId, AlertStatusUpdateRequest updateRequest) {
        FlaggedTransaction transaction = flaggedTransactionRepository.findById(alertId)
                .orElseThrow(() -> {
                    Map<String, Object> context = new HashMap<>();
                    context.put("event", "ALERT_NOT_FOUND");
                    context.put("alertId", alertId);
                    structuredLogger.warn("Alert not found", context);
                    return new AlertNotFoundException("Alert not found with ID: " + alertId);
                });

        structuredLogger.setTransactionId(transaction.getTransactionId().toString());

        AlertStatus newStatus = AlertStatus.fromString(updateRequest.getStatus());
        AlertStatus currentStatus = transaction.getStatus();

        // Validate state transition
        if (!currentStatus.canTransitionTo(newStatus)) {
            String message = String.format("Invalid status transition from %s to %s", currentStatus, newStatus);
            Map<String, Object> context = new HashMap<>();
            context.put("event", "INVALID_STATUS_TRANSITION");
            context.put("from", currentStatus.toString());
            context.put("to", newStatus.toString());
            structuredLogger.warn(message, context);
            throw new IllegalArgumentException(message);
        }

        // Update transaction
        transaction.setStatus(newStatus);
        transaction.setInvestigatedAt(Instant.now());
        transaction.setInvestigatedBy(updateRequest.getInvestigatedBy());
        
        if (updateRequest.getNotes() != null && !updateRequest.getNotes().isBlank()) {
            transaction.setInvestigationNotes(updateRequest.getNotes());
        }

        if (newStatus == AlertStatus.REVIEWED) {
            transaction.setReviewed(true);
        }

        FlaggedTransaction updated = flaggedTransactionRepository.save(transaction);

        // Create comprehensive audit log
        createAuditLog(updated, "STATUS_CHANGED", currentStatus.toString(), newStatus.toString(),
                String.format("Alert status updated from %s to %s", currentStatus, newStatus),
                updateRequest.getNotes(),
                updateRequest.getInvestigatedBy() != null ? updateRequest.getInvestigatedBy() : "UNKNOWN");

        Map<String, Object> successContext = new HashMap<>();
        successContext.put("event", "ALERT_STATUS_UPDATED");
        successContext.put("from", currentStatus.toString());
        successContext.put("to", newStatus.toString());
        successContext.put("investigatedBy", updateRequest.getInvestigatedBy());
        structuredLogger.info("Alert status updated successfully", successContext);

        log.info("Alert status updated: AlertId={}, TransactionId={}, From={}, To={}, InvestigatedBy={}",
                alertId, transaction.getTransactionId(), currentStatus, newStatus, 
                updateRequest.getInvestigatedBy());

        return mapToResponse(updated);
    }

    /**
     * Marks an alert as reviewed (legacy method - now uses updateAlertStatus).
     *
     * @param alertId the alert ID
     * @param investigationNotes optional notes from review
     * @return the updated alert response
     * @throws AlertNotFoundException if the alert does not exist
     */
    public AlertResponse markAsReviewed(Long alertId, String investigationNotes) {
        AlertStatusUpdateRequest request = AlertStatusUpdateRequest.builder()
                .status(AlertStatus.REVIEWED.toString())
                .notes(investigationNotes)
                .investigatedBy("SYSTEM")
                .build();
        return updateAlertStatus(alertId, request);
    }

    /**
     * Gets audit log history for an alert.
     *
     * @param alertId the alert ID
     * @param pageable pagination information
     * @return page of audit logs ordered by timestamp descending
     * @throws AlertNotFoundException if the alert does not exist
     */
    @Transactional(readOnly = true)
    public Page<AlertAuditLogResponse> getAuditLog(Long alertId, Pageable pageable) {
        FlaggedTransaction transaction = flaggedTransactionRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with ID: " + alertId));

        return alertAuditLogRepository.findByFlaggedTransactionOrderByActionTimestampDesc(transaction, pageable)
                .map(this::mapAuditLogToResponse);
    }

    /**
     * Gets statistics on flagged transactions.
     *
     * @return alert statistics
     */
    @Transactional(readOnly = true)
    public AlertStatistics getStatistics() {
        long totalAlerts = flaggedTransactionRepository.count();
        long unreviewedAlerts = flaggedTransactionRepository.countByReviewedFalse();
        long highRiskCount = flaggedTransactionRepository.countByRiskLevel(RiskLevel.HIGH);
        long mediumRiskCount = flaggedTransactionRepository.countByRiskLevel(RiskLevel.MEDIUM);

        Map<String, Object> context = new HashMap<>();
        context.put("event", "FETCH_STATISTICS");
        context.put("totalAlerts", totalAlerts);
        context.put("unreviewedAlerts", unreviewedAlerts);
        context.put("highRiskCount", highRiskCount);
        context.put("mediumRiskCount", mediumRiskCount);
        structuredLogger.debug("Alert statistics retrieved", context);

        return AlertStatistics.builder()
                .totalAlerts(totalAlerts)
                .unreviewedAlerts(unreviewedAlerts)
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .build();
    }

    /**
     * Creates an audit log entry for alert actions.
     *
     * @param transaction the flagged transaction
     * @param actionType the type of action
     * @param previousStatus the previous status
     * @param newStatus the new status
     * @param description action description
     * @param notes additional notes
     * @param performedBy user ID or system identifier
     */
    private void createAuditLog(FlaggedTransaction transaction, String actionType, String previousStatus,
                                String newStatus, String description, String notes, String performedBy) {
        AlertAuditLog auditLog = AlertAuditLog.builder()
                .flaggedTransaction(transaction)
                .actionType(actionType)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .description(description)
                .notes(notes)
                .performedBy(performedBy)
                .build();

        alertAuditLogRepository.save(auditLog);

        Map<String, Object> context = new HashMap<>();
        context.put("event", "AUDIT_LOG_CREATED");
        context.put("actionType", actionType);
        context.put("performedBy", performedBy);
        structuredLogger.debug("Audit log created", context);
    }

    /**
     * Maps a FlaggedTransaction entity to AlertResponse DTO with audit logs.
     *
     * @param transaction the entity
     * @return the response DTO
     */
    private AlertResponse mapToResponse(FlaggedTransaction transaction) {
        List<AlertAuditLogResponse> auditLogs = transaction.getAuditLogs() != null ?
                transaction.getAuditLogs().stream()
                        .map(this::mapAuditLogToResponse)
                        .collect(Collectors.toList()) :
                List.of();

        return AlertResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .riskLevel(transaction.getRiskLevel())
                .reason(transaction.getReason())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .reviewed(transaction.isReviewed())
                .investigationNotes(transaction.getInvestigationNotes())
                .status(transaction.getStatus() != null ? transaction.getStatus().toString() : AlertStatus.NEW.toString())
                .investigatedAt(transaction.getInvestigatedAt())
                .investigatedBy(transaction.getInvestigatedBy())
                .auditLogs(auditLogs)
                .build();
    }

    /**
     * Maps an AlertAuditLog entity to its response DTO.
     *
     * @param auditLog the audit log entity
     * @return the response DTO
     */
    private AlertAuditLogResponse mapAuditLogToResponse(AlertAuditLog auditLog) {
        return AlertAuditLogResponse.builder()
                .id(auditLog.getId())
                .actionType(auditLog.getActionType())
                .previousStatus(auditLog.getPreviousStatus())
                .newStatus(auditLog.getNewStatus())
                .description(auditLog.getDescription())
                .notes(auditLog.getNotes())
                .performedBy(auditLog.getPerformedBy())
                .actionTimestamp(auditLog.getActionTimestamp())
                .metadata(auditLog.getMetadata())
                .build();
    }

    /**
     * Investigates an alert and marks it as FRAUD or SAFE with investigation notes.
     * Supports transitions: NEW -> FRAUD/SAFE or REVIEWED -> FRAUD/SAFE
     *
     * @param alertId the alert ID
     * @param investigateRequest the investigation request with decision, notes, and investigator info
     * @return the updated alert response
     */
    public AlertResponse investigateAlert(Long alertId, AlertInvestigateRequest investigateRequest) {
        FlaggedTransaction transaction = flaggedTransactionRepository.findById(alertId)
                .orElseThrow(() -> {
                    Map<String, Object> context = new HashMap<>();
                    context.put("event", "ALERT_NOT_FOUND");
                    context.put("alertId", alertId);
                    structuredLogger.warn("Alert not found for investigation", context);
                    return new AlertNotFoundException("Alert not found with ID: " + alertId);
                });

        structuredLogger.setTransactionId(transaction.getTransactionId().toString());

        String decision = investigateRequest.getDecision().toUpperCase();
        AlertStatus newStatus;
        
        // Validate decision is either FRAUD or SAFE
        if (!decision.equals("FRAUD") && !decision.equals("SAFE")) {
            throw new IllegalArgumentException("Invalid investigation decision. Must be FRAUD or SAFE.");
        }
        
        newStatus = AlertStatus.fromString(decision);
        AlertStatus currentStatus = transaction.getStatus();

        // Validate state transition
        if (!currentStatus.canTransitionTo(newStatus)) {
            String message = String.format("Invalid status transition from %s to %s for investigation", currentStatus, newStatus);
            Map<String, Object> context = new HashMap<>();
            context.put("event", "INVALID_INVESTIGATION_TRANSITION");
            context.put("from", currentStatus.toString());
            context.put("to", newStatus.toString());
            structuredLogger.warn(message, context);
            throw new IllegalArgumentException(message);
        }

        // Update transaction with investigation details
        transaction.setStatus(newStatus);
        transaction.setInvestigatedAt(Instant.now());
        transaction.setInvestigatedBy(investigateRequest.getInvestigatedBy());
        
        if (investigateRequest.getNotes() != null && !investigateRequest.getNotes().isBlank()) {
            transaction.setInvestigationNotes(investigateRequest.getNotes());
        }

        // If transitioning from NEW or REVIEWED to FRAUD/SAFE, mark as reviewed
        if (newStatus == AlertStatus.FRAUD || newStatus == AlertStatus.SAFE) {
            transaction.setReviewed(true);
        }

        FlaggedTransaction updated = flaggedTransactionRepository.save(transaction);

        // Create comprehensive audit log for investigation
        createAuditLog(updated, "INVESTIGATION_COMPLETED", currentStatus.toString(), newStatus.toString(),
                String.format("Alert investigated and marked as %s", newStatus),
                investigateRequest.getNotes(),
                investigateRequest.getInvestigatedBy());

        Map<String, Object> successContext = new HashMap<>();
        successContext.put("event", "ALERT_INVESTIGATED");
        successContext.put("from", currentStatus.toString());
        successContext.put("to", newStatus.toString());
        successContext.put("investigatedBy", investigateRequest.getInvestigatedBy());
        successContext.put("decision", newStatus.toString());
        structuredLogger.info("Alert investigation completed successfully", successContext);

        return mapToResponse(updated);
    }
}
