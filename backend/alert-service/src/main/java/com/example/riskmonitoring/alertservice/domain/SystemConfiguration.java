package com.example.riskmonitoring.alertservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * System Configuration Entity
 * Stores configurable parameters for risk detection and anomaly detection
 */
@Entity
@Table(name = "system_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {

    @Id
    private String id;

    // Risk Detection Configuration
    @Column(name = "high_risk_threshold")
    private Double highRiskThreshold;

    @Column(name = "medium_risk_threshold")
    private Double mediumRiskThreshold;

    @Column(name = "low_risk_threshold")
    private Double lowRiskThreshold;

    // Anomaly Detection Configuration
    @Column(name = "anomaly_multiplier")
    private Double anomalyMultiplier;

    @Column(name = "velocity_check_enabled")
    private Boolean velocityCheckEnabled;

    @Column(name = "velocity_threshold")
    private Integer velocityThreshold;  // Max transactions per minute

    // Rule Configuration
    @Column(name = "geolocation_check_enabled")
    private Boolean geolocationCheckEnabled;

    @Column(name = "amount_spike_check_enabled")
    private Boolean amountSpikeCheckEnabled;

    @Column(name = "amount_spike_multiplier")
    private Double amountSpikeMultiplier;

    // System Settings
    @Column(name = "enforce_mfa_for_high_risk")
    private Boolean enforceMfaForHighRisk;

    @Column(name = "auto_escalation_enabled")
    private Boolean autoEscalationEnabled;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * Pre-persist to set default values
     */
    @PrePersist
    public void prePersist() {
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.highRiskThreshold == null) {
            this.highRiskThreshold = 80.0;
        }
        if (this.mediumRiskThreshold == null) {
            this.mediumRiskThreshold = 50.0;
        }
        if (this.lowRiskThreshold == null) {
            this.lowRiskThreshold = 20.0;
        }
        if (this.anomalyMultiplier == null) {
            this.anomalyMultiplier = 1.5;
        }
        if (this.velocityCheckEnabled == null) {
            this.velocityCheckEnabled = true;
        }
        if (this.velocityThreshold == null) {
            this.velocityThreshold = 10;  // 10 transactions per minute
        }
        if (this.geolocationCheckEnabled == null) {
            this.geolocationCheckEnabled = true;
        }
        if (this.amountSpikeCheckEnabled == null) {
            this.amountSpikeCheckEnabled = true;
        }
        if (this.amountSpikeMultiplier == null) {
            this.amountSpikeMultiplier = 2.0;
        }
        if (this.enforceMfaForHighRisk == null) {
            this.enforceMfaForHighRisk = true;
        }
        if (this.autoEscalationEnabled == null) {
            this.autoEscalationEnabled = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
