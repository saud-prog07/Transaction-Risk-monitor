package com.example.riskmonitoring.riskengine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

/**
 * Custom error handler for JMS message processing failures.
 * Logs errors at appropriate levels for alerting and monitoring.
 */
@Slf4j
@Component
public class JmsErrorHandler implements ErrorHandler {

    /**
     * Handles errors that occur during JMS message processing.
     * These are errors that couldn't be handled by the message listener itself.
     *
     * @param throwable the exception that occurred
     */
    @Override
    public void handleError(Throwable throwable) {
        String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";
        String errorType = throwable != null ? throwable.getClass().getSimpleName() : "Unknown";

        // Log at ERROR level to ensure visibility in monitoring/alerting systems
        log.error("JMS_ERROR_HANDLER: Failed to process JMS message - ErrorType: {}, Message: {}",
                errorType, errorMessage, throwable);

        // Log structured error information for troubleshooting
        if (throwable != null) {
            if (throwable instanceof jakarta.jms.JMSException) {
                log.error("JMS_ERROR: JMS Connection/Message error - Check JMS setup and message broker");
            } else if (throwable instanceof org.springframework.messaging.MessageDeliveryException) {
                log.error("JMS_ERROR: Message delivery failed - Network or broker issue");
            } else if (throwable instanceof RuntimeException) {
                log.error("JMS_ERROR: Unexpected runtime error during message processing");
            } else {
                log.error("JMS_ERROR: Unexpected error type: {}", throwable.getClass().getName());
            }
        }

        // Note: In a production DLQ implementation, the TransactionMessageListener
        // would catch exceptions and record them to the DLQ before this handler is invoked.
        // This handler is a safety net for unexpected errors that escape the listener.
    }
}
