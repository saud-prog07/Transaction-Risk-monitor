package com.example.riskmonitoring.alertservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating alert investigation status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertStatusUpdateRequest {

    @NotBlank(message = "Status cannot be blank")
    @JsonProperty("status")
    private String status; // NEW, REVIEWED, FRAUD, SAFE

    @JsonProperty("notes")
    private String notes; // Investigation notes

    @JsonProperty("investigated_by")
    private String investigatedBy; // User ID or system identifier

    @JsonProperty("metadata")
    private String metadata; // Additional context
}
