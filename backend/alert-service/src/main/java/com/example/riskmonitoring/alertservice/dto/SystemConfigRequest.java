package com.example.riskmonitoring.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System Configuration Request DTO
 * Used for updating system configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigRequest {

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
}
