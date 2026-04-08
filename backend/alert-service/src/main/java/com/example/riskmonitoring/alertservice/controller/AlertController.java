package com.example.riskmonitoring.alertservice.controller;

import com.example.riskmonitoring.alertservice.dto.AlertRequest;
import com.example.riskmonitoring.alertservice.dto.AlertResponse;
import com.example.riskmonitoring.alertservice.dto.AlertStatusUpdateRequest;
import com.example.riskmonitoring.alertservice.dto.AlertInvestigateRequest;
import com.example.riskmonitoring.alertservice.dto.AlertAuditLogResponse;
import com.example.riskmonitoring.alertservice.service.AlertService;
import com.example.riskmonitoring.alertservice.service.AlertStatistics;
import com.example.riskmonitoring.common.models.RiskLevel;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for alert management endpoints.
 * Handles receiving alerts and fetching flagged transaction data.
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Receives and stores a flagged transaction alert.
     * Called by risk-engine when a transaction is flagged as high/medium risk.
     *
     * @param alertRequest the alert request
     * @return the created alert response
     */
    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(
            @Valid @RequestBody AlertRequest alertRequest) {

        log.info("Received alert for transaction: {}", alertRequest.getTransactionId());

        AlertResponse response = alertService.createAlert(alertRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all flagged transactions with pagination.
     *
     * @param pageable pagination information
     * @return page of alerts
     */
    @GetMapping
    public ResponseEntity<Page<AlertResponse>> getAllAlerts(Pageable pageable) {
        log.debug("Fetching all alerts");

        Page<AlertResponse> alerts = alertService.getAllAlerts(pageable);

        return ResponseEntity.ok(alerts);
    }

    /**
     * Retrieves a single alert by ID.
     *
     * @param id the alert ID
     * @return the alert response
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable Long id) {
        log.debug("Fetching alert: {}", id);

        AlertResponse alert = alertService.getAlertById(id);

        return ResponseEntity.ok(alert);
    }

    /**
     * Retrieves alerts filtered by risk level.
     *
     * @param riskLevel the risk level to filter by (HIGH, MEDIUM, LOW)
     * @param pageable pagination information
     * @return page of alerts
     */
    @GetMapping("/by-risk-level/{riskLevel}")
    public ResponseEntity<Page<AlertResponse>> getAlertsByRiskLevel(
            @PathVariable String riskLevel,
            Pageable pageable) {

        log.debug("Fetching alerts by risk level: {}", riskLevel);

        RiskLevel level = RiskLevel.valueOf(riskLevel.toUpperCase());
        Page<AlertResponse> alerts = alertService.getAlertsByRiskLevel(level, pageable);

        return ResponseEntity.ok(alerts);
    }

    /**
     * Retrieves only unreviewed alerts.
     *
     * @param pageable pagination information
     * @return page of unreviewed alerts
     */
    @GetMapping("/unreviewed")
    public ResponseEntity<Page<AlertResponse>> getUnreviewedAlerts(Pageable pageable) {
        log.debug("Fetching unreviewed alerts");

        Page<AlertResponse> alerts = alertService.getUnreviewedAlerts(pageable);

        return ResponseEntity.ok(alerts);
    }

    /**
     * Retrieves only HIGH risk alerts.
     *
     * @param pageable pagination information
     * @return page of high risk alerts
     */
    @GetMapping("/high-risk")
    public ResponseEntity<Page<AlertResponse>> getHighRiskAlerts(Pageable pageable) {
        log.debug("Fetching high risk alerts");

        Page<AlertResponse> alerts = alertService.getHighRiskAlerts(pageable);

        return ResponseEntity.ok(alerts);
    }

    /**
     * Marks an alert as reviewed.
     *
     * @param id the alert ID
     * @param notes optional investigation notes
     * @return the updated alert response
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<AlertResponse> markAlertAsReviewed(
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {

        log.info("Marking alert as reviewed: {}", id);

        AlertResponse response = alertService.markAsReviewed(id, notes);

        return ResponseEntity.ok(response);
    }

    /**
     * Gets statistics on flagged transactions.
     *
     * @return alert statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<AlertStatistics> getStatistics() {
        log.debug("Fetching alert statistics");

        AlertStatistics stats = alertService.getStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * Updates the investigation status of an alert.
     * Supports status transitions: NEW -> REVIEWED -> FRAUD/SAFE
     *
     * @param id the alert ID
     * @param updateRequest the status update request with status, notes, and investigator info
     * @return the updated alert response with full audit trail
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<AlertResponse> updateAlertStatus(
            @PathVariable Long id,
            @Valid @RequestBody AlertStatusUpdateRequest updateRequest) {

        log.info("Updating alert status: AlertId={}, NewStatus={}, InvestigatedBy={}",
                id, updateRequest.getStatus(), updateRequest.getInvestigatedBy());

        AlertResponse response = alertService.updateAlertStatus(id, updateRequest);

        return ResponseEntity.ok(response);
    }

    /**
     * Investigates an alert and marks it as FRAUD or SAFE with investigation notes.
     * This is a specialized endpoint for fraud investigation workflow.
     *
     * @param id the alert ID
     * @param investigateRequest the investigation request with fraud/safe decision and notes
     * @return the updated alert response
     */
    @PutMapping("/{id}/investigate")
    public ResponseEntity<AlertResponse> investigateAlert(
            @PathVariable Long id,
            @Valid @RequestBody AlertInvestigateRequest investigateRequest) {

        log.info("Investigating alert: AlertId={}, Decision={}, InvestigatedBy={}",
                id, investigateRequest.getDecision(), investigateRequest.getInvestigatedBy());

        AlertResponse response = alertService.investigateAlert(id, investigateRequest);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the audit log history for a specific alert.
     * Shows all status changes and investigations with timestamps and who performed the action.
     *
     * @param id the alert ID
     * @param pageable pagination information
     * @return page of audit log entries ordered by timestamp (newest first)
     */
    @GetMapping("/{id}/audit-log")
    public ResponseEntity<Page<AlertAuditLogResponse>> getAlertAuditLog(
            @PathVariable Long id,
            Pageable pageable) {

        log.debug("Fetching audit log for alert: {}", id);

        Page<AlertAuditLogResponse> auditLogs = alertService.getAuditLog(id, pageable);

        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Health check endpoint.
     *
     * @return status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Alert Service is healthy");
    }

    /**
     * Export flagged transactions to CSV format.
     * Supports filtering by status and risk level.
     *
     * @param status optional filter by alert status
     * @param riskLevel optional filter by risk level
     * @return CSV file download
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportAlertsToCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel) {
        try {
            byte[] csvData = alertService.exportAlertsToCsv(status, riskLevel);
            
            String filename = "flagged-transactions-" + 
                    java.time.LocalDate.now() + ".csv";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvData);
        } catch (Exception e) {
            log.error("Error exporting alerts to CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generating CSV: " + e.getMessage()).getBytes());
        }
    }
}
