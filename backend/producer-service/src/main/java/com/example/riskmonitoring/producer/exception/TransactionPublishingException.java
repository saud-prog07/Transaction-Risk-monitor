package com.example.riskmonitoring.producer.exception;

/**
 * Exception thrown when a transaction cannot be published to the message queue.
 */
public class TransactionPublishingException extends RuntimeException {

    public TransactionPublishingException(String message) {
        super(message);
    }

    public TransactionPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
