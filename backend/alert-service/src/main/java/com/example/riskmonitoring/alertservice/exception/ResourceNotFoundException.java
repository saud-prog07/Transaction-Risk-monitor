package com.example.riskmonitoring.alertservice.exception;

/**
 * Thrown when a requested resource is not found
 * Extends RuntimeException for unchecked exception handling
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
