package com.example.riskmonitoring.alertservice.dto;

import com.example.riskmonitoring.alertservice.domain.SystemConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * System Configuration Response DTO
 * Used for returning system configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigResponse {

    // Risk Detection Configuration
    private Double highRiskThreshold;
    private Double mediumRiskThreshold;
    private Double lowRiskThreshold;

    // Anomaly Detection Configuration
    private Double anomalyMultiplier;
    private Boolean velocityCheckEnabled;
    private Integer velocityThreshold;

    // Rule Configuration
    private Boolean geolocationCheckEnabled;
    private Boolean amountSpikeCheckEnabled;
    private Double amountSpikeMultiplier;

    // System Settings
    private Boolean enforceMfaForHighRisk;
    private Boolean autoEscalationEnabled;

    // Metadata
    private Instant updatedAt;
    private String updatedBy;

    /**
     * Convert SystemConfiguration entity to response DTO
     */
    public static SystemConfigResponse fromEntity(SystemConfiguration config) {
        return SystemConfigResponse.builder()
                .highRiskThreshold(config.getHighRiskThreshold())
                .mediumRiskThreshold(config.getMediumRiskThreshold())
                .lowRiskThreshold(config.getLowRiskThreshold())
                .anomalyMultiplier(config.getAnomalyMultiplier())
                .velocityCheckEnabled(config.getVelocityCheckEnabled())
                .velocityThreshold(config.getVelocityThreshold())
                .geolocationCheckEnabled(config.getGeolocationCheckEnabled())
                .amountSpikeCheckEnabled(config.getAmountSpikeCheckEnabled())
                .amountSpikeMultiplier(config.getAmountSpikeMultiplier())
                .enforceMfaForHighRisk(config.getEnforceMfaForHighRisk())
                .autoEscalationEnabled(config.getAutoEscalationEnabled())
                .updatedAt(config.getUpdatedAt())
                .updatedBy(config.getUpdatedBy())
                .build();
    }
}
