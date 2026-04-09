package com.example.riskmonitoring.riskengine.controller;

import com.example.riskmonitoring.riskengine.service.MetricsService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for exposing system metrics.
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    /**
     * Returns current system metrics.
     * @return map containing metrics data
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalProcessed", metricsService.getTotalProcessed());
        metrics.put("flaggedCount", metricsService.getFlaggedCount());
        metrics.put("failedCount", metricsService.getFailedCount());
        metrics.put("avgProcessingTime", metricsService.getAvgProcessingTimeMillis());
        metrics.put("throughput", metricsService.getThroughputPerSecond());
        
        return ResponseEntity.ok(metrics);
    }
}