package com.example.riskmonitoring.riskengine.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA Entity for storing risk analyzer configurations.
 * Allows dynamic updating of risk thresholds without code changes.
 * 
 * Configuration is cached and refreshed on updates.
 * Supports all configurable risk analyzers:
 * - HighAmountAnalyzer
 * - FrequencyAnalyzer
 * - TimeAnomalyAnalyzer
 * - LocationAnomalyAnalyzer
 */
@Entity
@Table(name = "risk_analyzer_config", indexes = {
    @Index(name = "idx_analyzer_name", columnList = "analyzer_name"),
    @Index(name = "idx_enabled", columnList = "enabled"),
    @Index(name = "idx_updated_at", columnList = "updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAnalyzerConfig implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the analyzer.
     * Values: HighAmountAnalyzer, FrequencyAnalyzer, TimeAnomalyAnalyzer, LocationAnomalyAnalyzer
     */
    @Column(name = "analyzer_name", nullable = false, length = 100, unique = true)
    private String analyzerName;

    /**
     * Human-readable display name for the analyzer
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * Description of what this analyzer detects
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this analyzer is enabled
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Primary threshold value (varies by analyzer)
     * - HighAmountAnalyzer: multiplier value (2.0)
     * - FrequencyAnalyzer: transaction threshold (5)
     * - TimeAnomalyAnalyzer: unusual hour threshold (80)
     * - LocationAnomalyAnalyzer: frequency threshold (5.0)
     */
    @Column(name = "threshold_primary")
    private Double thresholdPrimary;

    /**
     * Secondary threshold value
     * - HighAmountAnalyzer: minimum threshold in dollars (1000.00)
     * - FrequencyAnalyzer: not used
     * - TimeAnomalyAnalyzer: not used
     * - LocationAnomalyAnalyzer: not used
     */
    @Column(name = "threshold_secondary")
    private Double thresholdSecondary;

    /**
     * Tertiary threshold value
     * - HighAmountAnalyzer: not used
     * - FrequencyAnalyzer: not used
     * - TimeAnomalyAnalyzer: not used
     * - LocationAnomalyAnalyzer: not used
     */
    @Column(name = "threshold_tertiary")
    private Double thresholdTertiary;

    /**
     * Time window in days (used by multiple analyzers)
     * - HighAmountAnalyzer: baseline window
     * - TimeAnomalyAnalyzer: historical window
     * - LocationAnomalyAnalyzer: historical window
     */
    @Column(name = "time_window_days")
    private Integer timeWindowDays;

    /**
     * Time window in minutes (used by FrequencyAnalyzer)
     */
    @Column(name = "time_window_minutes")
    private Integer timeWindowMinutes;

    /**
     * JSON configuration for advanced settings (optional)
     */
    @Column(name = "additional_config", columnDefinition = "TEXT")
    private String additionalConfig;

    /**
     * Audit trail: who last modified this config
     */
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Version for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Get primary threshold as double
     */
    public double getPrimaryThreshold() {
        return thresholdPrimary != null ? thresholdPrimary : 0.0;
    }

    /**
     * Get secondary threshold as double
     */
    public double getSecondaryThreshold() {
        return thresholdSecondary != null ? thresholdSecondary : 0.0;
    }

    /**
     * Get tertiary threshold as double
     */
    public double getTertiaryThreshold() {
        return thresholdTertiary != null ? thresholdTertiary : 0.0;
    }

    /**
     * Get time window in days
     */
    public int getTimeWindowDaysOrDefault(int defaultValue) {
        return timeWindowDays != null ? timeWindowDays : defaultValue;
    }

    /**
     * Get time window in minutes
     */
    public int getTimeWindowMinutesOrDefault(int defaultValue) {
        return timeWindowMinutes != null ? timeWindowMinutes : defaultValue;
    }

    @Override
    public String toString() {
        return String.format(
            "RiskAnalyzerConfig(name=%s, enabled=%s, primary=%.2f, secondary=%.2f, timeWindow=%d)",
            analyzerName, enabled, thresholdPrimary, thresholdSecondary, timeWindowDays
        );
    }
}
