package com.example.riskmonitoring.alertservice.exception;

/**
 * Exception thrown when an alert is not found.
 */
public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(String message) {
        super(message);
    }

    public AlertNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
