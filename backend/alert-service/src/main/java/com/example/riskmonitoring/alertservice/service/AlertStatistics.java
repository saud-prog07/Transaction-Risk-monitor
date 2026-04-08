package com.example.riskmonitoring.alertservice.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for alert statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertStatistics {

    @JsonProperty("total_alerts")
    private long totalAlerts;

    @JsonProperty("unreviewed_alerts")
    private long unreviewedAlerts;

    @JsonProperty("high_risk_count")
    private long highRiskCount;

    @JsonProperty("medium_risk_count")
    private long mediumRiskCount;
}
