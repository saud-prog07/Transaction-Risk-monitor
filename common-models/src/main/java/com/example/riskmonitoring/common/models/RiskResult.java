package com.example.riskmonitoring.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Risk assessment result for a transaction.
 * Contains the risk determination and supporting reason.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Reference to the analyzed transaction.
     */
    @JsonProperty("transaction_id")
    private UUID transactionId;

    /**
     * Risk level classification for the transaction.
     */
    @JsonProperty("risk_level")
    private RiskLevel riskLevel;

    /**
     * Reason for the risk assessment decision.
     * May include detected anomalies, rule violations, or other factors.
     */
    @JsonProperty("reason")
    private String reason;
}
