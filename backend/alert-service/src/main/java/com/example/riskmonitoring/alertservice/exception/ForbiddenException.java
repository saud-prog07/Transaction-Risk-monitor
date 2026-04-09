package com.example.riskmonitoring.alertservice.exception;

/**
 * Thrown when user is not authorized to access a resource
 * Extends RuntimeException for unchecked exception handling
 */
public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
