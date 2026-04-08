package com.example.riskmonitoring.riskengine.analyzer;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a risk detection result from a single analyzer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskDetectionResult {

    /**
     * Whether this analyzer detected high risk.
     */
    private boolean flagged;

    /**
     * The risk level assigned by this analyzer.
     */
    private RiskLevel riskLevel;

    /**
     * Detailed reason for the risk detection.
     */
    private String reason;

    /**
     * Name of the analyzer that produced this result.
     */
    private String analyzerName;
}
