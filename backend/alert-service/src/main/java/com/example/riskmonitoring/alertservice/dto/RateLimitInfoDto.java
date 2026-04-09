package com.example.riskmonitoring.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rate Limit Information (for response headers and monitoring)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitInfoDto {
    private int requestsAllowed;
    private int requestsRemaining;
    private long resetTimeMs;
    private boolean limited;
    private long retryAfterSeconds;
    
    public static RateLimitInfoDto fromMetrics(
            com.example.riskmonitoring.alertservice.service.RateLimitingService.RateLimitMetrics metrics,
            long retryAfterSeconds) {
        return RateLimitInfoDto.builder()
                .requestsAllowed(metrics.getRequestsAllowed())
                .requestsRemaining(metrics.getRequestsRemaining())
                .resetTimeMs(metrics.getResetTimeMs())
                .limited(metrics.isLimited())
                .retryAfterSeconds(retryAfterSeconds)
                .build();
    }
}
