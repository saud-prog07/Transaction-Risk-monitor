package com.example.riskmonitoring.alertservice.exception;

/**
 * Exception thrown when trying to create an alert that already exists.
 */
public class AlertAlreadyExistsException extends RuntimeException {

    public AlertAlreadyExistsException(String message) {
        super(message);
    }

    public AlertAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
