package com.example.riskmonitoring.alertservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for alert investigation requests.
 * Used when marking an alert as FRAUD or SAFE with investigation notes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertInvestigateRequest {

    /**
     * The investigation decision - either FRAUD or SAFE
     */
    @NotNull
    private String decision;

    /**
     * Investigation notes and findings
     */
    @NotBlank
    private String notes;

    /**
     * ID of the investigator performing the investigation
     */
    @NotBlank
    private String investigatedBy;
}