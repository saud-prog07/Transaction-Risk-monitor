package com.example.riskmonitoring.riskengine.listener;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.RiskResult;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.service.RiskAnalysisService;
import com.example.riskmonitoring.riskengine.service.AlertNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JMS message listener for consuming transaction messages from IBM MQ.
 * Processes transactions and sends risk results to alert service.
 *
 * Error Handling Strategy:
 * - Serialization errors: Logged and skipped (non-recoverable)
 * - Risk analysis errors: Logged but message acknowledged (prevents infinite retries)
 * - Alert notification errors: Caught and handled by AlertNotificationService retry logic
 * - All errors result in message being acknowledged (no poison message loop)
 */
@Slf4j
@Component
public class TransactionMessageListener {

    private final ObjectMapper objectMapper;
    private final RiskAnalysisService riskAnalysisService;
    private final AlertNotificationService alertNotificationService;

    public TransactionMessageListener(
            ObjectMapper objectMapper,
            RiskAnalysisService riskAnalysisService,
            AlertNotificationService alertNotificationService) {
        this.objectMapper = objectMapper;
        this.riskAnalysisService = riskAnalysisService;
        this.alertNotificationService = alertNotificationService;
        log.info("TransactionMessageListener initialized and ready to process transactions");
    }

    /**
     * Listens for transaction messages on TRANSACTION_QUEUE.
     * Deserializes JSON to Transaction, analyzes for risk, and sends alerts if needed.
     *
     * Message processing flow:
     * 1. Deserialize message to Transaction
     * 2. Analyze transaction using RiskAnalysisService
     * 3. Send alert to alert-service if risk >= MEDIUM
     * 4. Acknowledge message (commit)
     *
     * @param message the JSON message containing the transaction
     */
    @JmsListener(destination = "TRANSACTION_QUEUE", containerFactory = "jmsListenerContainerFactory")
    @Transactional
    public void onTransactionMessage(String message) {
        long processingStartTime = System.currentTimeMillis();
        String transactionId = "UNKNOWN";

        try {
            log.trace("Raw message received from TRANSACTION_QUEUE: {}", message);

            // Step 1: Deserialize JSON to Transaction
            Transaction transaction = deserializeTransaction(message);
            transactionId = transaction.getTransactionId().toString();

            log.info("[PIPELINE] Transaction received - TransactionId: {}, UserId: {}, Amount: {}, Location: {}",
                    transactionId, transaction.getUserId(), transaction.getAmount(), transaction.getLocation());

            // Step 2: Analyze transaction for risk
            RiskResult riskResult = analyzeTransaction(transaction);

            log.info("[PIPELINE] Risk analysis completed - TransactionId: {}, RiskLevel: {}, Reason: {}",
                    riskResult.getTransactionId(), riskResult.getRiskLevel(), riskResult.getReason());

            // Step 3: Send alert if risk level indicates action needed
            sendAlertIfNeeded(transaction, riskResult, processingStartTime);

            long processingTime = System.currentTimeMillis() - processingStartTime;
            log.info("[PIPELINE] Transaction processing completed successfully - TransactionId: {}, Duration: {}ms",
                    transactionId, processingTime);

        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonException jsonEx) {
            // Non-recoverable serialization error
            long processingTime = System.currentTimeMillis() - processingStartTime;
            log.error("[PIPELINE] Failed to deserialize transaction message (non-recoverable) - "
                    + "Message: {}, Error: {}, Duration: {}ms",
                    message, jsonEx.getMessage(), processingTime, jsonEx);
            // Message will be acknowledged anyway to prevent poison message loop
            // In production: send to DLQ

        } catch (Exception ex) {
            // Log any unexpected errors
            long processingTime = System.currentTimeMillis() - processingStartTime;
            log.error("[PIPELINE] Unexpected error processing transaction - TransactionId: {}, Error: {}, Duration: {}ms",
                    transactionId, ex.getMessage(), processingTime, ex);
            // Message will be acknowledged to prevent stuck queue
            // In production: send to DLQ for investigation
        }
    }

    /**
     * Deserializes JSON message to Transaction object.
     *
     * @param message the JSON message
     * @return Transaction object
     * @throws IllegalArgumentException if JSON is malformed
     */
    private Transaction deserializeTransaction(String message) {
        try {
            log.debug("Deserializing transaction from JSON message");
            Transaction transaction = objectMapper.readValue(message, Transaction.class);
            log.debug("Transaction deserialized successfully: {}", transaction.getTransactionId());
            return transaction;
        } catch (com.fasterxml.jackson.core.JsonException jsonEx) {
            log.error("JSON deserialization error - Message: {}, Error: {}", message, jsonEx.getMessage(), jsonEx);
            throw new IllegalArgumentException("Invalid JSON format: " + jsonEx.getMessage(), jsonEx);
        } catch (Exception ex) {
            log.error("Serialization error - Message: {}, Error: {}", message, ex.getMessage(), ex);
            throw new IllegalArgumentException("Failed to deserialize transaction: " + ex.getMessage(), ex);
        }
    }

    /**
     * Analyzes transaction for risk using RiskAnalysisService.
     *
     * @param transaction the transaction to analyze
     * @return RiskResult with risk assessment
     * @throws RuntimeException if analysis fails
     */
    private RiskResult analyzeTransaction(Transaction transaction) {
        try {
            log.debug("Starting risk analysis for transaction: {}", transaction.getTransactionId());
            RiskResult result = riskAnalysisService.analyzeTransaction(transaction);
            log.debug("Risk analysis completed - TransactionId: {}, RiskLevel: {}",
                    result.getTransactionId(), result.getRiskLevel());
            return result;
        } catch (Exception ex) {
            log.error("Risk analysis failed for transaction: {}, Error: {}",
                    transaction.getTransactionId(), ex.getMessage(), ex);
            throw new RuntimeException("Risk analysis failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Sends alert notification if transaction risk level warrants it.
     * Alerts are sent for HIGH and MEDIUM risk transactions.
     *
     * @param transaction the transaction being analyzed
     * @param riskResult the risk analysis result
     * @param processingStartTime the time processing began
     */
    private void sendAlertIfNeeded(Transaction transaction, RiskResult riskResult, long processingStartTime) {
        RiskLevel riskLevel = riskResult.getRiskLevel();

        if (riskLevel == RiskLevel.HIGH) {
            log.warn("[PIPELINE] >>> HIGH RISK TRANSACTION DETECTED <<<");
            log.warn("[PIPELINE] Flagging transaction: TransactionId: {}, UserId: {}, Amount: {}, Reason: {}",
                    transaction.getTransactionId(), transaction.getUserId(), transaction.getAmount(),
                    riskResult.getReason());
            try {
                alertNotificationService.sendAlert(transaction, riskResult);
            } catch (Exception ex) {
                log.error("[PIPELINE] Failed to send HIGH RISK alert - TransactionId: {}, Error: {} "
                        + "(alert service retry logic will attempt sending)",
                        transaction.getTransactionId(), ex.getMessage());
                // Note: AlertNotificationService has @Retryable, so it will retry
                // If all retries fail, exception is logged
            }

        } else if (riskLevel == RiskLevel.MEDIUM) {
            log.info("[PIPELINE] MEDIUM RISK transaction detected - TransactionId: {}, "
                    + "UserId: {}, Amount: {}, Reason: {}",
                    transaction.getTransactionId(), transaction.getUserId(), transaction.getAmount(),
                    riskResult.getReason());
            try {
                alertNotificationService.sendAlert(transaction, riskResult);
            } catch (Exception ex) {
                log.error("[PIPELINE] Failed to send MEDIUM RISK alert - TransactionId: {}, Error: {} "
                        + "(alert service retry logic will attempt sending)",
                        transaction.getTransactionId(), ex.getMessage());
                // Note: AlertNotificationService has @Retryable, so it will retry
            }

        } else {
            // LOW or NO RISK
            log.debug("[PIPELINE] Transaction low risk or no risk detected - TransactionId: {}, RiskLevel: {}",
                    transaction.getTransactionId(), riskLevel);
        }
    }
}
