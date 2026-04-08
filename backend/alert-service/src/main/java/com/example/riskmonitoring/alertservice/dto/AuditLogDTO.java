package com.example.riskmonitoring.alertservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for audit log responses.
 * Provides audit trail information to API consumers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    
    private Long id;
    
    @JsonProperty("alert_id")
    private String alertId;
    
    private String action;
    
    @JsonProperty("user_id")
    private String userId;
    
    private Instant timestamp;
    
    private String details;
    
    @JsonProperty("previous_status")
    private String previousStatus;
    
    @JsonProperty("new_status")
    private String newStatus;
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    @JsonProperty("request_id")
    private String requestId;
    
    /**
     * Get human-readable description of the action.
     */
    public String getActionDescription() {
        return switch (action) {
            case "CREATED" -> "Alert created";
            case "REVIEWED" -> "Alert marked as reviewed";
            case "UPDATED" -> "Alert updated";
            case "STATUS_CHANGED" -> "Status changed from " + previousStatus + " to " + newStatus;
            case "NOTES_ADDED" -> "Investigation notes added";
            case "APPROVED" -> "Alert approved";
            case "REJECTED" -> "Alert rejected";
            case "ESCALATED" -> "Alert escalated";
            case "RESOLVED" -> "Alert resolved";
            case "REOPENED" -> "Alert reopened";
            case "DELETED" -> "Alert deleted";
            case "EXPORTED" -> "Alert exported";
            case "SHARED" -> "Alert shared with team";
            default -> "Unknown action";
        };
    }
}
