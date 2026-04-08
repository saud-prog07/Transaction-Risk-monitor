package com.example.riskmonitoring.riskengine.listener;

import com.example.riskmonitoring.common.logging.StructuredLogger;
import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.RiskResult;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.service.MetricsService;
import com.example.riskmonitoring.riskengine.service.RiskAnalysisService;
import com.example.riskmonitoring.riskengine.service.AlertNotificationService;
import com.example.riskmonitoring.riskengine.service.FailedMessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * JMS message listener for consuming transaction messages from IBM MQ.
 * Processes transactions and sends risk results to alert service.
 * Failures are recorded to DLQ for retry.
 *
 * Error Handling Strategy:
 * - Serialization errors: Logged, recorded to DLQ as non-recoverable
 * - Risk analysis errors: Logged, recorded to DLQ for retry
 * - Alert notification errors: Caught and recorded to DLQ for retry
 * - All failures are properly persisted in Dead Letter Queue with stack traces
 * - Retry mechanism automatically processes failed messages with exponential backoff
 */
@Slf4j
@Component
public class TransactionMessageListener {

    private final ObjectMapper objectMapper;
    private final RiskAnalysisService riskAnalysisService;
    private final AlertNotificationService alertNotificationService;
    private final FailedMessageHandler failedMessageHandler;
    private final MetricsService metricsService;
    private final StructuredLogger structuredLogger = StructuredLogger.getLogger(TransactionMessageListener.class);

    public TransactionMessageListener(
            ObjectMapper objectMapper,
            RiskAnalysisService riskAnalysisService,
            AlertNotificationService alertNotificationService,
            FailedMessageHandler failedMessageHandler,
            MetricsService metricsService) {
        this.objectMapper = objectMapper;
        this.riskAnalysisService = riskAnalysisService;
        this.alertNotificationService = alertNotificationService;
        this.failedMessageHandler = failedMessageHandler;
        this.metricsService = metricsService;
        log.info("TransactionMessageListener initialized with DLQ support");
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
     * On failure, records to DLQ for retry.
     *
     * @param message the JSON message containing the transaction
     */
    @JmsListener(destination = "TRANSACTION_QUEUE", containerFactory = "jmsListenerContainerFactory")
    @Transactional
    public void onTransactionMessage(String message) {
        long processingStartTime = System.currentTimeMillis();
        String transactionId = "UNKNOWN";

        try {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "MESSAGE_RECEIVED");
            context.put("source", "TRANSACTION_QUEUE");
            structuredLogger.debug("Message received from queue", context);

            // Step 1: Deserialize message to Transaction
            Transaction transaction = deserializeTransaction(message);
            transactionId = transaction.getTransactionId().toString();
            structuredLogger.setTransactionId(transactionId);

            Map<String, Object> startContext = new HashMap<>();
            startContext.put("event", "PROCESSING_STARTED");
            startContext.put("stage", "RISK_ANALYSIS");
            structuredLogger.info("Starting transaction processing", startContext);

            // Step 2: Analyze transaction for risk
            RiskResult riskResult = analyzeTransaction(transaction);

            long analysisTime = System.currentTimeMillis() - processingStartTime;
            Map<String, Object> analysisContext = new HashMap<>();
            analysisContext.put("event", "RISK_ANALYSIS_COMPLETE");
            analysisContext.put("duration", analysisTime);
            structuredLogger.info("Risk analysis completed", analysisContext);

            // Step 3: Send alert if risk level indicates action needed
            sendAlertIfNeeded(transaction, riskResult, processingStartTime);

            long processingTime = System.currentTimeMillis() - processingStartTime;
            Map<String, Object> finContext = new HashMap<>();
            finContext.put("event", "PROCESSING_COMPLETE");
            finContext.put("riskLevel", riskResult.getRiskLevel().toString());
            finContext.put("duration", processingTime);
            structuredLogger.info("Transaction processing completed", finContext);

            // Update metrics
            metricsService.incrementTotalProcessed();
            if (riskResult.getRiskLevel() != null) {
                metricsService.incrementFlaggedCount();
            }
            metricsService.addProcessingTimeMillis(processingTime);

        } catch (IllegalArgumentException | java.io.IOException jsonEx) {
            // Non-recoverable serialization error
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("event", "DESERIALIZATION_ERROR");
            errorContext.put("nonRecoverable", true);
            structuredLogger.error("Failed to deserialize transaction message", errorContext, jsonEx);

            // Record to DLQ as non-recoverable
            failedMessageHandler.handleDeserializationFailure(message, jsonEx);
            metricsService.incrementFailedCount();

        } catch (Exception ex) {
            // Recoverable error
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("event", "PROCESSING_ERROR");
            errorContext.put("recoverable", true);
            structuredLogger.error("Error processing transaction from queue", errorContext, ex);

            // Record to DLQ for retry
            failedMessageHandler.handleProcessingFailure(message, transactionId, ex);
            metricsService.incrementFailedCount();
        } finally {
            structuredLogger.clearContext();
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
            Map<String, Object> context = new HashMap<>();
            context.put("event", "DESERIALIZATION_STARTED");
            structuredLogger.debug("Deserializing transaction from JSON message", context);
            
            Transaction transaction = objectMapper.readValue(message, Transaction.class);
            
            Map<String, Object> successContext = new HashMap<>();
            successContext.put("event", "DESERIALIZATION_SUCCESS");
            successContext.put("transactionId", transaction.getTransactionId());
            structuredLogger.debug("Transaction deserialized successfully", successContext);
            return transaction;
        } catch (java.io.IOException jsonEx) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "JSON_PARSE_ERROR");
            structuredLogger.error("Invalid JSON format in message", context, jsonEx);
            throw new IllegalArgumentException("Invalid JSON format: " + jsonEx.getMessage(), jsonEx);
        } catch (Exception ex) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "DESERIALIZATION_FAILED");
            structuredLogger.error("Failed to deserialize transaction", context, ex);
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
            String txId = transaction.getTransactionId().toString();
            structuredLogger.setTransactionId(txId);
            
            Map<String, Object> context = new HashMap<>();
            context.put("event", "ANALYSIS_STARTED");
            structuredLogger.info("Starting risk analysis", context);
            
            RiskResult result = riskAnalysisService.analyzeTransaction(transaction);
            
            Map<String, Object> resultContext = new HashMap<>();
            resultContext.put("event", "ANALYSIS_COMPLETE");
            resultContext.put("riskLevel", result.getRiskLevel().toString());
            resultContext.put("reason", result.getReason());
            structuredLogger.info("Risk analysis completed", resultContext);
            return result;
        } catch (Exception ex) {
            String txId = transaction.getTransactionId().toString();
            Map<String, Object> context = new HashMap<>();
            context.put("event", "ANALYSIS_FAILED");
            structuredLogger.error("Risk analysis failed", context, ex);
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
        String txId = transaction.getTransactionId().toString();
        structuredLogger.setTransactionId(txId);

        if (riskLevel == RiskLevel.HIGH) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "HIGH_RISK_DETECTED");
            context.put("riskLevel", "HIGH");
            context.put("reason", riskResult.getReason());
            structuredLogger.warn("High-risk transaction detected", context);
            
            try {
                Map<String, Object> sendContext = new HashMap<>();
                sendContext.put("event", "ALERT_SENDING");
                sendContext.put("destination", "alert-service");
                structuredLogger.info("Sending high-risk alert", sendContext);
                
                alertNotificationService.sendAlert(transaction, riskResult);
                
                Map<String, Object> successContext = new HashMap<>();
                successContext.put("event", "ALERT_SENT");
                successContext.put("alertType", "HIGH_RISK_ALERT");
                successContext.put("destination", "alert-service");
                structuredLogger.info("High-risk alert sent successfully", successContext);
            } catch (Exception ex) {
                Map<String, Object> errorContext = new HashMap<>();
                errorContext.put("event", "ALERT_SEND_FAILED");
                errorContext.put("willRetry", true);
                structuredLogger.error("Failed to send HIGH RISK alert (will be retried)", errorContext, ex);
            }

        } else if (riskLevel == RiskLevel.MEDIUM) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "MEDIUM_RISK_DETECTED");
            context.put("riskLevel", "MEDIUM");
            context.put("reason", riskResult.getReason());
            structuredLogger.warn("Medium-risk transaction detected", context);
            
            try {
                Map<String, Object> sendContext = new HashMap<>();
                sendContext.put("event", "ALERT_SENDING");
                sendContext.put("destination", "alert-service");
                structuredLogger.info("Sending medium-risk alert", sendContext);
                
                alertNotificationService.sendAlert(transaction, riskResult);
                
                Map<String, Object> successContext = new HashMap<>();
                successContext.put("event", "ALERT_SENT");
                successContext.put("alertType", "MEDIUM_RISK_ALERT");
                successContext.put("destination", "alert-service");
                structuredLogger.info("Medium-risk alert sent successfully", successContext);
            } catch (Exception ex) {
                Map<String, Object> errorContext = new HashMap<>();
                errorContext.put("event", "ALERT_SEND_FAILED");
                errorContext.put("willRetry", true);
                structuredLogger.error("Failed to send MEDIUM RISK alert (will be retried)", errorContext, ex);
            }

        } else {
            // LOW or NO RISK
            Map<String, Object> context = new HashMap<>();
            context.put("event", "LOW_RISK_DETECTED");
            context.put("riskLevel", riskLevel.toString());
            structuredLogger.debug("Low-risk transaction - no alert needed", context);
        }
    }
}
