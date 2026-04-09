package com.example.riskmonitoring.alertservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for DLQ message retry requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DLQRetryRequest {
    
    @NotNull(message = "Message ID is required")
    private Long messageId;
    
    private String reason;  // Optional reason for manual retry
}
