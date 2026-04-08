package com.example.riskmonitoring.alertservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request DTO for receiving flagged transaction alerts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRequest {

    @NotNull(message = "Transaction ID cannot be null")
    @JsonProperty("transaction_id")
    private String transactionId;

    @NotBlank(message = "User ID cannot be blank")
    @JsonProperty("user_id")
    private String userId;

    @NotNull(message = "Amount cannot be null")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "Location cannot be blank")
    @JsonProperty("location")
    private String location;

    @NotNull(message = "Timestamp cannot be null")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @NotBlank(message = "Risk level cannot be blank")
    @JsonProperty("risk_level")
    private String riskLevel;

    @NotBlank(message = "Reason cannot be blank")
    @JsonProperty("reason")
    private String reason;
}
