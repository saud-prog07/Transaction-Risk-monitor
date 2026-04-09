package com.example.riskmonitoring.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for DLQ statistics and status information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DLQStatisticsDTO {
    
    private Long totalMessages;
    
    private Long pendingMessages;
    
    private Long retryingMessages;
    
    private Long resolvedMessages;
    
    private Long deadMessages;
    
    private Long activeMessages;  // PENDING + RETRYING
    
    private Double successRate;  // Percentage of resolved messages
    
    public Double calculateSuccessRate() {
        if (totalMessages == 0) {
            return 0.0;
        }
        return (resolvedMessages * 100.0) / totalMessages;
    }
}
