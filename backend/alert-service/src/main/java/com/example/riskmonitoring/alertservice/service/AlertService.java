package com.example.riskmonitoring.alertservice.service;

import com.example.riskmonitoring.alertservice.domain.FlaggedTransaction;
import com.example.riskmonitoring.alertservice.dto.AlertRequest;
import com.example.riskmonitoring.alertservice.dto.AlertResponse;
import com.example.riskmonitoring.alertservice.exception.AlertAlreadyExistsException;
import com.example.riskmonitoring.alertservice.exception.AlertNotFoundException;
import com.example.riskmonitoring.alertservice.repository.FlaggedTransactionRepository;
import com.example.riskmonitoring.common.models.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing flagged transaction alerts.
 * Handles creation, retrieval, and updating of alert records.
 */
@Slf4j
@Service
@Transactional
public class AlertService {

    private final FlaggedTransactionRepository flaggedTransactionRepository;

    public AlertService(FlaggedTransactionRepository flaggedTransactionRepository) {
        this.flaggedTransactionRepository = flaggedTransactionRepository;
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

        // Check if already exists
        if (flaggedTransactionRepository.findByTransactionId(transactionId).isPresent()) {
            log.warn("Alert already exists for transaction: {}", transactionId);
            throw new AlertAlreadyExistsException(
                    "Alert already exists for transaction: " + transactionId);
        }

        try {
            RiskLevel riskLevel = RiskLevel.valueOf(alertRequest.getRiskLevel().toUpperCase());

            FlaggedTransaction flaggedTransaction = FlaggedTransaction.builder()
                    .transactionId(transactionId)
                    .riskLevel(riskLevel)
                    .reason(alertRequest.getReason())
                    .build();

            FlaggedTransaction saved = flaggedTransactionRepository.save(flaggedTransaction);

            log.info("Alert created successfully: TransactionId={}, RiskLevel={}, AlertId={}",
                    transactionId, riskLevel, saved.getId());

            return mapToResponse(saved);

        } catch (IllegalArgumentException ex) {
            log.error("Invalid risk level: {}", alertRequest.getRiskLevel(), ex);
            throw new IllegalArgumentException("Invalid risk level: " + alertRequest.getRiskLevel());
        } catch (Exception ex) {
            log.error("Error creating alert for transaction: {}", transactionId, ex);
            throw new RuntimeException("Failed to create alert: " + ex.getMessage(), ex);
        }
    }

    /**
     * Retrieves an alert by ID.
     *
     * @param alertId the alert ID
     * @return the alert response
     * @throws AlertNotFoundException if the alert does not exist
     */
    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long alertId) {
        FlaggedTransaction transaction = flaggedTransactionRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.warn("Alert not found: {}", alertId);
                    return new AlertNotFoundException("Alert not found with ID: " + alertId);
                });

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
        log.debug("Fetching all alerts with pagination: {}", pageable);
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
        log.debug("Fetching alerts by risk level: {}", riskLevel);
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
        log.debug("Fetching unreviewed alerts");
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
        log.debug("Fetching high risk alerts");
        return flaggedTransactionRepository.findHighRiskTransactions(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Marks an alert as reviewed.
     *
     * @param alertId the alert ID
     * @param investigationNotes optional notes from review
     * @return the updated alert response
     * @throws AlertNotFoundException if the alert does not exist
     */
    public AlertResponse markAsReviewed(Long alertId, String investigationNotes) {
        FlaggedTransaction transaction = flaggedTransactionRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.warn("Alert not found: {}", alertId);
                    return new AlertNotFoundException("Alert not found with ID: " + alertId);
                });

        transaction.setReviewed(true);
        if (investigationNotes != null && !investigationNotes.isBlank()) {
            transaction.setInvestigationNotes(investigationNotes);
        }

        FlaggedTransaction updated = flaggedTransactionRepository.save(transaction);

        log.info("Alert marked as reviewed: AlertId={}, TransactionId={}",
                alertId, transaction.getTransactionId());

        return mapToResponse(updated);
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

        log.debug("Alert statistics: total={}, unreviewed={}, high={}, medium={}",
                totalAlerts, unreviewedAlerts, highRiskCount, mediumRiskCount);

        return AlertStatistics.builder()
                .totalAlerts(totalAlerts)
                .unreviewedAlerts(unreviewedAlerts)
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .build();
    }

    /**
     * Maps a FlaggedTransaction entity to AlertResponse DTO.
     *
     * @param transaction the entity
     * @return the response DTO
     */
    private AlertResponse mapToResponse(FlaggedTransaction transaction) {
        return AlertResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .riskLevel(transaction.getRiskLevel())
                .reason(transaction.getReason())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .reviewed(transaction.isReviewed())
                .investigationNotes(transaction.getInvestigationNotes())
                .build();
    }
}
