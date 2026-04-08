package com.example.riskmonitoring.common.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator for transaction input data
 * Provides comprehensive validation for transaction requests
 */
public class TransactionValidator {
    
    // Validation constants
    private static final double MIN_AMOUNT = 0.01;
    private static final double MAX_AMOUNT = 10_000_000; // $10 million limit
    private static final int MAX_USER_ID_LENGTH = 100;
    private static final int MAX_LOCATION_LENGTH = 255;
    private static final int MIN_USER_ID_LENGTH = 1;
    private static final int MIN_LOCATION_LENGTH = 2;
    
    private final List<String> errors;
    
    public TransactionValidator() {
        this.errors = new ArrayList<>();
    }
    
    /**
     * Validate transaction data
     * @param userId The user identifier
     * @param amount The transaction amount
     * @param location The transaction location
     * @return true if all validations pass, false otherwise
     */
    public boolean validate(String userId, Double amount, String location) {
        errors.clear();
        
        validateUserId(userId);
        validateAmount(amount);
        validateLocation(location);
        
        return errors.isEmpty();
    }
    
    /**
     * Validate user ID
     */
    public boolean validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            errors.add("User ID is required");
            return false;
        }
        
        if (userId.length() < MIN_USER_ID_LENGTH) {
            errors.add("User ID must have at least " + MIN_USER_ID_LENGTH + " character");
            return false;
        }
        
        if (userId.length() > MAX_USER_ID_LENGTH) {
            errors.add("User ID must not exceed " + MAX_USER_ID_LENGTH + " characters");
            return false;
        }
        
        if (!userId.matches("^[a-zA-Z0-9_.-]+$")) {
            errors.add("User ID contains invalid characters. Only alphanumeric, underscore, dot, and hyphen allowed");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate transaction amount
     */
    public boolean validateAmount(Double amount) {
        if (amount == null) {
            errors.add("Amount is required");
            return false;
        }
        
        if (amount < MIN_AMOUNT) {
            errors.add("Amount must be at least " + MIN_AMOUNT);
            return false;
        }
        
        if (amount > MAX_AMOUNT) {
            errors.add("Amount exceeds maximum allowed value of " + MAX_AMOUNT);
            return false;
        }
        
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            errors.add("Amount must be a valid number");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate location
     */
    public boolean validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            errors.add("Location is required");
            return false;
        }
        
        if (location.length() < MIN_LOCATION_LENGTH) {
            errors.add("Location must have at least " + MIN_LOCATION_LENGTH + " characters");
            return false;
        }
        
        if (location.length() > MAX_LOCATION_LENGTH) {
            errors.add("Location must not exceed " + MAX_LOCATION_LENGTH + " characters");
            return false;
        }
        
        // Basic check for location format (city, state/country)
        if (!location.contains(",") && location.length() < 5) {
            errors.add("Location should be in format: City, State/Country");
            return false;
        }
        
        return true;
    }
    
    /**
     * Get all validation errors
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Get first validation error
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
    
    /**
     * Check if validation passed
     */
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    /**
     * Get error count
     */
    public int getErrorCount() {
        return errors.size();
    }
}
