package com.example.riskmonitoring.common.idempotency;

import java.time.Instant;

/**
 * Represents a processed transaction record for idempotency checking.
 * Prevents duplicate processing of the same transaction.
 */
public class IdempotencyRecord {
    
    private String transactionId;
    private String userId;
    private double amount;
    private String location;
    private String processingResult;
    private Instant processedAt;
    private Instant expiresAt;
    
    public IdempotencyRecord() {
    }
    
    public IdempotencyRecord(String transactionId, String userId, double amount, 
                           String location, String processingResult) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.location = location;
        this.processingResult = processingResult;
        this.processedAt = Instant.now();
        // Records expire after 24 hours
        this.expiresAt = Instant.now().plusSeconds(86400);
    }
    
    // Getters and Setters
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getProcessingResult() {
        return processingResult;
    }
    
    public void setProcessingResult(String processingResult) {
        this.processingResult = processingResult;
    }
    
    public Instant getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    @Override
    public String toString() {
        return "IdempotencyRecord{" +
                "transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", location='" + location + '\'' +
                ", processingResult='" + processingResult + '\'' +
                ", processedAt=" + processedAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
