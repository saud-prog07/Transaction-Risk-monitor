package com.example.riskmonitoring.riskengine.service;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.RiskResult;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.domain.DeadLetterMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for processing retry of failed messages from DLQ.
 * Implements exponential backoff retry strategy.
 */
@Slf4j
@Service
@Transactional
public class RetryableMessageProcessor {

    private final DeadLetterQueueService dlqService;
    private final RiskAnalysisService riskAnalysisService;
    private final AlertNotificationService alertNotificationService;
    private final ObjectMapper objectMapper;
    private final boolean dlqRetryEnabled;

    public RetryableMessageProcessor(
            DeadLetterQueueService dlqService,
            RiskAnalysisService riskAnalysisService,
            AlertNotificationService alertNotificationService,
            ObjectMapper objectMapper,
            @Value("${dlq.retry.enabled:true}") boolean dlqRetryEnabled) {
        this.dlqService = dlqService;
        this.riskAnalysisService = riskAnalysisService;
        this.alertNotificationService = alertNotificationService;
        this.objectMapper = objectMapper;
        this.dlqRetryEnabled = dlqRetryEnabled;
    }

    /**
     * Scheduled task to process retryable messages from DLQ.
     * Runs every 30 seconds with exponential backoff delay.
     * Configured via dlq.retry.enabled property.
     */
    @Scheduled(fixedDelayString = "${dlq.retry.interval:30000}",
               initialDelayString = "${dlq.retry.initial-delay:10000}")
    public void processRetryableMessages() {
        if (!dlqRetryEnabled) {
            return;
        }

        try {
            List<DeadLetterMessage> retryableMessages = dlqService.getRetryableMessages();

            if (retryableMessages.isEmpty()) {
                log.trace("DLQ: No retryable messages found");
                return;
            }

            log.info("DLQ: Processing {} retryable messages", retryableMessages.size());

            for (DeadLetterMessage message : retryableMessages) {
                processRetryMessage(message);
            }

        } catch (Exception ex) {
            log.error("DLQ: Error processing retryable messages", ex);
        }
    }

    /**
     * Processes a single retry message with exponential backoff.
     *
     * @param dlqMessage the DLQ message to retry
     */
    private void processRetryMessage(DeadLetterMessage dlqMessage) {
        Long dlqId = dlqMessage.getId();
        String transactionId = dlqMessage.getTransactionId();

        try {
            // Get message and mark as RETRYING
            dlqService.getAndMarkAsRetrying(dlqId);

            // Calculate exponential backoff delay
            long delayMs = calculateBackoffDelay(dlqMessage.getRetryCount());
            Thread.sleep(delayMs);

            log.info("DLQ: Retrying message - DLQ_ID: {}, TransactionId: {}, Attempt: {}/{}",
                    dlqId, transactionId, dlqMessage.getRetryCount() + 1, dlqMessage.getMaxRetries());

            // Deserialize and process message
            String messageContent = dlqMessage.getOriginalMessage();
            Transaction transaction = objectMapper.readValue(messageContent, Transaction.class);

            // Analyze transaction
            RiskResult riskResult = riskAnalysisService.analyzeTransaction(transaction);

            // Send alert if needed
            if (riskResult.getRiskLevel() == RiskLevel.HIGH || 
                riskResult.getRiskLevel() == RiskLevel.MEDIUM) {
                alertNotificationService.sendAlert(transaction, riskResult);
            }

            // Mark as resolved after successful retry
            dlqService.recordRetryAttempt(dlqId, true, null);
            log.info("DLQ: Message successfully retried - DLQ_ID: {}, TransactionId: {}", dlqId, transactionId);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("DLQ: Retry thread interrupted - DLQ_ID: {}, TransactionId: {}", dlqId, transactionId);
            dlqService.recordRetryAttempt(dlqId, false, "Thread interrupted: " + ex.getMessage());

        } catch (Exception ex) {
            log.warn("DLQ: Retry attempt failed - DLQ_ID: {}, TransactionId: {}, Error: {}",
                    dlqId, transactionId, ex.getMessage());
            dlqService.recordRetryAttempt(dlqId, false, ex.getMessage());
        }
    }

    /**
     * Calculates exponential backoff delay in milliseconds.
     * Formula: min(baseDelay * 2^retryCount, maxDelay)
     *
     * @param retryCount current retry attempt number (0-based)
     * @return delay in milliseconds
     */
    private long calculateBackoffDelay(int retryCount) {
        long baseDelay = 1000;  // 1 second
        long maxDelay = 60000;  // 60 seconds
        long delay = baseDelay * (long) Math.pow(2, retryCount);
        return Math.min(delay, maxDelay);
    }

    /**
     * Manually triggers retry for a specific DLQ message.
     * Can be called via REST API.
     *
     * @param dlqId the DLQ message ID
     * @return success status
     */
    public boolean retryMessage(Long dlqId) {
        try {
            DeadLetterMessage message = dlqService.getAndMarkAsRetrying(dlqId)
                    .orElseThrow(() -> new RuntimeException("DLQ message not found: " + dlqId));

            processRetryMessage(message);
            return true;

        } catch (Exception ex) {
            log.error("DLQ: Manual retry failed for DLQ_ID: {}", dlqId, ex);
            return false;
        }
    }

    /**
     * Manually mark a DLQ message as dead (skip further retries).
     *
     * @param dlqId the DLQ message ID
     * @param notes notes about why it's being marked as dead
     */
    public void skipRetries(Long dlqId, String notes) {
        dlqService.markAsDead(dlqId, notes + " (Manually skipped)");
        log.info("DLQ: Message marked as DEAD (manual skip) - DLQ_ID: {}", dlqId);
    }

    /**
     * Checks if DLQ retry is enabled.
     */
    public boolean isRetryEnabled() {
        return dlqRetryEnabled;
    }
}
