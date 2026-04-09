package com.example.riskmonitoring.alertservice.dto;

import com.example.riskmonitoring.common.models.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for transaction lifecycle trace information.
 * Shows the complete journey of a transaction through the system.
 * 
 * Lifecycle stages:
 * 1. Received - Transaction submitted to REST API
 * 2. Queued - Transaction placed in message queue
 * 3. Processed - Risk engine analyzed the transaction
 * 4. Flagged - Risk analysis completed and alert generated
 * 5. Alerted - Alert stored and notification sent
 * 
 * Security:
 * - Only displays non-sensitive transaction information
 * - Timestamps for audit trail visibility
 * - Risk score and reason for investigation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTraceResponse {
    
    /**
     * Unique transaction identifier
     */
    private String transactionId;
    
    /**
     * User associated with the transaction (sanitized, no PII)
     */
    private String userId;
    
    /**
     * Transaction amount (visible for context)
     */
    private Double amount;
    
    /**
     * Overall risk level assessment
     */
    private RiskLevel riskLevel;
    
    /**
     * Risk score (0-100)
     */
    private Double riskScore;
    
    /**
     * Reason for the alert
     */
    private String reason;
    
    /**
     * Complete lifecycle trace with timestamps
     */
    private List<TraceStage> stages;
    
    /**
     * Total processing time in milliseconds
     */
    private Long totalProcessingTimeMs;
    
    /**
     * Timestamp when alert was created
     */
    private Instant alertCreatedAt;
    
    /**
     * Current alert status
     */
    private String alertStatus;
    
    /**
     * Nested class representing each stage in the transaction lifecycle
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TraceStage {
        
        /**
         * Stage name: RECEIVED, QUEUED, PROCESSED, FLAGGED, ALERTED
         */
        private String stage;
        
        /**
         * Human-readable description of the stage
         */
        private String description;
        
        /**
         * ISO 8601 timestamp when this stage occurred
         */
        private Instant timestamp;
        
        /**
         * Duration from previous stage in milliseconds
         */
        private Long durationFromPreviousMs;
        
        /**
         * Duration from start in milliseconds
         */
        private Long cumulativeTimeMs;
        
        /**
         * Service that processed this stage (e.g., "producer-service", "risk-engine", "alert-service")
         */
        private String service;
        
        /**
         * Optional status message for this stage
         */
        private String statusMessage;
        
        /**
         * Optional additional data for this stage (e.g., risk score details)
         */
        private String additionalData;
    }
}
