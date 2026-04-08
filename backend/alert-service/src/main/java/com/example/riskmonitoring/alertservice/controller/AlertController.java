package com.example.riskmonitoring.alertservice.controller;

import com.example.riskmonitoring.alertservice.dto.AlertRequest;
import com.example.riskmonitoring.alertservice.dto.AlertResponse;
import com.example.riskmonitoring.alertservice.service.AlertService;
import com.example.riskmonitoring.alertservice.service.AlertStatistics;
import com.example.riskmonitoring.common.models.RiskLevel;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
     * Health check endpoint.
     *
     * @return status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Alert Service is healthy");
    }
}
