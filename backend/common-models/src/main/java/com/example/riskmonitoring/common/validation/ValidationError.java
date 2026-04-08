package com.example.riskmonitoring.common.validation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents validation errors in API responses
 */
public class ValidationError {
    
    private int status;
    private String message;
    private String path;
    private Instant timestamp;
    private List<FieldError> fieldErrors;
    private String errorId;
    
    public ValidationError() {
        this.timestamp = Instant.now();
        this.fieldErrors = new ArrayList<>();
    }
    
    public ValidationError(int status, String message, String path) {
        this();
        this.status = status;
        this.message = message;
        this.path = path;
    }
    
    public ValidationError(int status, String message, String path, String errorId) {
        this(status, message, path);
        this.errorId = errorId;
    }
    
    // Getters and Setters
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
    
    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
    
    public void addFieldError(String field, String message) {
        this.fieldErrors.add(new FieldError(field, message));
    }
    
    public String getErrorId() {
        return errorId;
    }
    
    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }
    
    /**
     * Represents a single field validation error
     */
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
        
        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Object getRejectedValue() {
            return rejectedValue;
        }
        
        @Override
        public String toString() {
            return "FieldError{" +
                    "field='" + field + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "ValidationError{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", path='" + path + '\'' +
                ", fieldErrors=" + fieldErrors +
                ", errorId='" + errorId + '\'' +
                '}';
    }
}
