package com.example.riskmonitoring.riskengine.service;

import com.example.riskmonitoring.riskengine.domain.DeadLetterMessage;
import com.example.riskmonitoring.riskengine.service.MetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Service for handling failed message processing.
 * Records failures to DLQ and initiates retry mechanisms.
 */
@Slf4j
@Service
@Transactional
public class FailedMessageHandler {

    private final DeadLetterQueueService dlqService;
    private final MetricsService metricsService;

    public FailedMessageHandler(DeadLetterQueueService dlqService, MetricsService metricsService) {
        this.dlqService = dlqService;
        this.metricsService = metricsService;
    }

    /**
     * Handles a failed message by recording it in the DLQ.
     * Extracts transaction ID from either message content or error context.
     *
     * @param message the failed message content
     * @param transactionId optional transaction ID (may be null)
     * @param exception the exception that caused the failure
     * @return the recorded DLQ message
     */
    public DeadLetterMessage handleFailedMessage(
            String message,
            String transactionId,
            Exception exception) {

        try {
            String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
            String stackTrace = getStackTrace(exception);

            // Log the failure
            logFailure(message, transactionId, errorMessage);

            // Record in DLQ
            DeadLetterMessage dlqMessage = dlqService.recordFailedMessage(
                    transactionId,
                    message,
                    errorMessage,
                    stackTrace);

            // Log DLQ recording
            log.warn("DLQ: Failed message recorded - DLQ_ID: {}, TransactionId: {}, Status: PENDING for retry",
                    dlqMessage.getId(), dlqMessage.getTransactionId());

            // Increment failed count metric
            metricsService.incrementFailedCount();

            return dlqMessage;

        } catch (Exception ex) {
            log.error("DLQ: Error handling failed message", ex);
            throw new RuntimeException("Failed to record message in DLQ", ex);
        }
    }

    /**
     * Handles a deserialization failure (non-recoverable).
     * These messages typically cannot be retried as the format is invalid.
     *
     * @param message the malformed message
     * @param exception the serialization exception
     * @return the recorded DLQ message
     */
    public DeadLetterMessage handleDeserializationFailure(
            String message,
            Exception exception) {

        log.error("DLQ: Deserialization failure (non-recoverable) - Error: {}",
                exception.getMessage());

        DeadLetterMessage dlqMessage = handleFailedMessage(
                message,
                "DESERIALIZATION_ERROR",
                exception);

        // Mark as dead since this cannot be retried
        dlqService.markAsDead(dlqMessage.getId(),
                "Deserialization failure - Non-recoverable message format error");

        return dlqMessage;
    }

    /**
     * Handles a processing failure (potentially recoverable).
     * These messages may be retried if the underlying cause is transient.
     *
     * @param message the original message
     * @param transactionId the transaction ID
     * @param exception the processing exception
     * @return the recorded DLQ message
     */
    public DeadLetterMessage handleProcessingFailure(
            String message,
            String transactionId,
            Exception exception) {

        log.warn("DLQ: Processing failure (potentially recoverable) - TransactionId: {}, Error: {}",
                transactionId, exception.getMessage());

        return handleFailedMessage(message, transactionId, exception);
    }

    /**
     * Handles an external service failure (e.g., alert-service unavailable).
     * These are typically transient and should be retried.
     *
     * @param message the original message
     * @param transactionId the transaction ID
     * @param serviceName the name of the service that failed
     * @param exception the exception
     * @return the recorded DLQ message
     */
    public DeadLetterMessage handleExternalServiceFailure(
            String message,
            String transactionId,
            String serviceName,
            Exception exception) {

        String errorMsg = String.format("External service failure: %s - %s",
                serviceName, exception.getMessage());

        log.warn("DLQ: External service failure - Service: {}, TransactionId: {}, Error: {}",
                serviceName, transactionId, exception.getMessage());

        return handleFailedMessage(message, transactionId,
                new RuntimeException(errorMsg, exception));
    }

    /**
     * Extracts full stack trace from exception.
     *
     * @param exception the exception
     * @return stack trace as string
     */
    private String getStackTrace(Exception exception) {
        if (exception == null) {
            return "";
        }

        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Logs a message failure with context.
     *
     * @param message the failed message
     * @param transactionId the transaction ID
     * @param errorMessage the error description
     */
    private void logFailure(String message, String transactionId, String errorMessage) {
        String txnId = transactionId != null ? transactionId : "UNKNOWN";
        String messagePreview = message != null && message.length() > 200
                ? message.substring(0, 200) + "..."
                : message;

        log.error("DLQ: Message failure - TransactionId: {}, Error: {}, Message: {}",
                txnId, errorMessage, messagePreview);
    }

    /**
     * Gets DLQ statistics.
     *
     * @return DLQ statistics
     */
    public DeadLetterQueueService.DLQStatistics getStatistics() {
        return dlqService.getStatistics();
    }
}
