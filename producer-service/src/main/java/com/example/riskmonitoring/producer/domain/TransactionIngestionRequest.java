package com.example.riskmonitoring.producer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request DTO for transaction ingestion endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionIngestionRequest {

    /**
     * User identifier.
     */
    @NotBlank(message = "User ID cannot be blank")
    @JsonProperty("user_id")
    private String userId;

    /**
     * Transaction amount.
     */
    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be greater than 0")
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Geographic location or merchant location.
     */
    @NotBlank(message = "Location cannot be blank")
    @JsonProperty("location")
    private String location;
}
