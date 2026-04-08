package com.example.riskmonitoring.alertservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.riskmonitoring.common.models.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for flagged transaction alerts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("transaction_id")
    private UUID transactionId;

    @JsonProperty("risk_level")
    private RiskLevel riskLevel;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("reviewed")
    private boolean reviewed;

    @JsonProperty("investigation_notes")
    private String investigationNotes;

    @JsonProperty("status")
    private String status;

    @JsonProperty("investigated_at")
    private Instant investigatedAt;

    @JsonProperty("investigated_by")
    private String investigatedBy;

    @JsonProperty("audit_logs")
    private List<AlertAuditLogResponse> auditLogs;
}
