package com.example.riskmonitoring.producer.exception;

/**
 * Exception thrown when transaction ingestion validation fails.
 */
public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
