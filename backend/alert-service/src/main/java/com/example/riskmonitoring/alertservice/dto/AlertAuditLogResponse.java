package com.example.riskmonitoring.alertservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for alert audit log entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertAuditLogResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("action_type")
    private String actionType;

    @JsonProperty("previous_status")
    private String previousStatus;

    @JsonProperty("new_status")
    private String newStatus;

    @JsonProperty("description")
    private String description;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("performed_by")
    private String performedBy;

    @JsonProperty("action_timestamp")
    private Instant actionTimestamp;

    @JsonProperty("metadata")
    private String metadata;
}
