package com.example.riskmonitoring.riskengine.service;

import com.example.riskmonitoring.common.models.RiskResult;
import com.example.riskmonitoring.common.models.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;

/**
 * Service for sending alert notifications to the alert service.
 * Notifies alert-service of flagged transactions with automatic retry on transient failures.
 *
 * Retry Strategy:
 * - Retries on: HttpServerErrorException (5xx), ResourceAccessException (connection issues)
 * - Max attempts: 3
 * - Initial delay: 2000ms
 * - Backoff multiplier: 2.0 (exponential: 2s, 4s, 8s)
 * - Random jitter: enabled
 */
@Slf4j
@Service
public class AlertNotificationService {

    private final RestTemplate restTemplate;
    private final String alertServiceBaseUrl;
    private final int alertTimeoutMs;

    public AlertNotificationService(
            RestTemplate restTemplate,
            @Value("${alert-service.base-url:http://localhost:8082}") String alertServiceBaseUrl,
            @Value("${alert-service.timeout-ms:10000}") int alertTimeoutMs) {
        this.restTemplate = restTemplate;
        this.alertServiceBaseUrl = alertServiceBaseUrl;
        this.alertTimeoutMs = alertTimeoutMs;
        log.info("AlertNotificationService initialized - BaseURL: {}, Timeout: {}ms",
                alertServiceBaseUrl, alertTimeoutMs);
    }

    /**
     * Sends an alert for a flagged transaction to the alert service.
     * Implements automatic retry with exponential backoff on transient failures.
     *
     * @param transaction the transaction being flagged
     * @param riskResult the risk analysis result
     * @throws RuntimeException if all retry attempts fail
     */
    @Retryable(
            retryFor = {HttpServerErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0, random = true),
            label = "sendAlertToAlertService"
    )
    public void sendAlert(Transaction transaction, RiskResult riskResult) {
        String transactionId = transaction.getTransactionId().toString();
        String endpoint = alertServiceBaseUrl + "/api/alerts";

        try {
            log.debug("Preparing alert payload for transaction: {}", transactionId);

            AlertPayload payload = AlertPayload.builder()
                    .transactionId(transactionId)
                    .userId(transaction.getUserId())
                    .amount(transaction.getAmount())
                    .location(transaction.getLocation())
                    .timestamp(transaction.getTimestamp())
                    .riskLevel(riskResult.getRiskLevel().toString())
                    .reason(riskResult.getReason())
                    .timestamp(Instant.now())
                    .build();

            log.info("Sending alert to alert-service [Endpoint: {}, TransactionId: {}, RiskLevel: {}]",
                    endpoint, transactionId, riskResult.getRiskLevel());

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, payload, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Alert sent successfully to alert-service - TransactionId: {}, Status: {}",
                        transactionId, response.getStatusCode());
            } else {
                log.warn("Unexpected status from alert-service - Status: {}, TransactionId: {}",
                        response.getStatusCode(), transactionId);
                throw new RuntimeException("Alert service returned status: " + response.getStatusCode());
            }

        } catch (HttpServerErrorException serverEx) {
            // 5xx errors are retryable
            log.warn("Alert service returned server error (retrying) - TransactionId: {}, Status: {}, Message: {}",
                    transactionId, serverEx.getStatusCode(), serverEx.getMessage());
            throw serverEx;

        } catch (ResourceAccessException connectEx) {
            // Connection failures are retryable
            log.warn("Connection failed to alert-service (retrying) - TransactionId: {}, Message: {}",
                    transactionId, connectEx.getMessage());
            throw connectEx;

        } catch (Exception ex) {
            // Non-retryable errors
            log.error("Fatal error sending alert to alert-service - TransactionId: {}, Endpoint: {}, Error: {}",
                    transactionId, endpoint, ex.getMessage(), ex);
            throw new RuntimeException("Failed to send alert after all retries: " + ex.getMessage(), ex);
        }
    }

    /**
     * Payload DTO for alert API.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AlertPayload {
        private String transactionId;
        private String userId;
        private java.math.BigDecimal amount;
        private String location;
        private java.time.Instant timestamp;
        private String riskLevel;
        private String reason;
    }
}
