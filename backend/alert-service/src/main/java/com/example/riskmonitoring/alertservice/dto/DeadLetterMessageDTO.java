package com.example.riskmonitoring.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for Dead Letter Queue messages.
 * Represents failed messages that need retry or investigation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadLetterMessageDTO {
    
    private Long id;
    
    private String transactionId;
    
    private String errorMessage;
    
    private Integer retryCount;
    
    private Integer maxRetries;
    
    private String status;  // PENDING, RETRYING, RESOLVED, DEAD
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    private Instant lastRetryAt;
    
    private Instant resolvedAt;
    
    private String resolutionNotes;
    
    /**
     * Get retry eligibility status
     */
    public boolean canRetry() {
        return "PENDING".equals(status) && retryCount < maxRetries;
    }
    
    /**
     * Get remaining retry attempts
     */
    public Integer getRemainingRetries() {
        return maxRetries - retryCount;
    }
}
