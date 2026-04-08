package com.example.riskmonitoring.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Transaction model for monitoring system.
 * Represents a single transaction with its details for risk assessment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the transaction.
     */
    @JsonProperty("transaction_id")
    private UUID transactionId;

    /**
     * User ID associated with the transaction.
     */
    @JsonProperty("user_id")
    private String userId;

    /**
     * Transaction amount.
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Timestamp when the transaction was initiated.
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Geographic location or merchant location of the transaction.
     */
    @JsonProperty("location")
    private String location;
}
