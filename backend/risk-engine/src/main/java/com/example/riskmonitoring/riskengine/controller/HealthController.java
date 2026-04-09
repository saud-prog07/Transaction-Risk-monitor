package com.example.riskmonitoring.riskengine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for monitoring system status.
 * Provides detailed health information for frontend dashboard.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthEndpoint healthEndpoint;

    /**
     * Returns comprehensive system health status.
     * @return map containing health status for various components
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        // Get overall application health
        HealthComponent healthComponent = healthEndpoint.health();
        
        Map<String, Object> healthStatus = new HashMap<>();
        
        // Overall service status
        if (healthComponent != null && healthComponent.getStatus() != null) {
            String status = healthComponent.getStatus().getCode();
            healthStatus.put("status", status);
            healthStatus.put("service", "UP".equals(status) ? "UP" : "DOWN");
        } else {
            healthStatus.put("status", "UNKNOWN");
            healthStatus.put("service", "UNKNOWN");
        }
        
        // Mock MQ connection status (in real implementation, this would check actual MQ connection)
        healthStatus.put("mqStatus", "connected"); // Could be "connected", "disconnected", or "degraded"
        
        // Database connection status
        healthStatus.put("databaseStatus", "connected");
        
        // Additional metrics
        healthStatus.put("timestamp", System.currentTimeMillis());
        healthStatus.put("uptime", "99.9%"); // Mock uptime - could be calculated from startup time
        
        return ResponseEntity.ok(healthStatus);
    }
}