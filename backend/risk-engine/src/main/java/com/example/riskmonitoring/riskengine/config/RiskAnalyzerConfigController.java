package com.example.riskmonitoring.riskengine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing risk analyzer configurations.
 * 
 * Endpoints:
 * - GET  /api/risk/config           - Get all configurations
 * - GET  /api/risk/config/{id}      - Get configuration by ID
 * - GET  /api/risk/config/analyzer/{name} - Get by analyzer name
 * - POST /api/risk/config           - Create new configuration
 * - PUT  /api/risk/config/{id}      - Update configuration
 * - PATCH /api/risk/config/{id}/enabled - Enable/disable analyzer
 * - PATCH /api/risk/config/{id}/threshold - Update single threshold
 * - POST /api/risk/config/{id}/reset - Reset to defaults
 * - GET  /api/risk/config/enabled   - Get only enabled configurations
 */
@Slf4j
@RestController
@RequestMapping("/api/risk/config")
public class RiskAnalyzerConfigController {

    private final RiskAnalyzerConfigService configService;
    private final String defaultModifierId = "API";

    public RiskAnalyzerConfigController(RiskAnalyzerConfigService configService) {
        this.configService = configService;
    }

    /**
     * Get all analyzer configurations
     * @return list of all configurations
     */
    @GetMapping
    public ResponseEntity<List<RiskAnalyzerConfig>> getAllConfigs() {
        log.debug("GET /api/risk/config - Retrieve all configurations");
        List<RiskAnalyzerConfig> configs = configService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get only enabled analyzers
     * @return list of enabled configurations
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<RiskAnalyzerConfig>> getEnabledConfigs() {
        log.debug("GET /api/risk/config/enabled - Retrieve enabled configurations");
        List<RiskAnalyzerConfig> configs = configService.getEnabledConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get configuration by ID
     * @param id the configuration ID
     * @return the configuration
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getConfigById(@PathVariable Long id) {
        log.debug("GET /api/risk/config/{} - Retrieve configuration", id);

        var config = configService.getConfigById(id);
        if (config.isPresent()) {
            return ResponseEntity.ok(config.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse("Configuration not found", id));
        }
    }

    /**
     * Get configuration by analyzer name
     * @param analyzerName the analyzer name
     * @return the configuration
     */
    @GetMapping("/analyzer/{analyzerName}")
    public ResponseEntity<?> getConfigByName(@PathVariable String analyzerName) {
        log.debug("GET /api/risk/config/analyzer/{} - Retrieve configuration", analyzerName);

        var config = configService.getConfigByName(analyzerName);
        if (config.isPresent()) {
            return ResponseEntity.ok(config.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse("Analyzer not found", analyzerName));
        }
    }

    /**
     * Update configuration
     * @param id the configuration ID
     * @param config the updated configuration
     * @return the updated configuration
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConfig(
            @PathVariable Long id,
            @RequestBody RiskAnalyzerConfig config) {

        log.info("PUT /api/risk/config/{} - Update configuration", id);

        try {
            RiskAnalyzerConfig updated = configService.updateConfig(
                id, config, getModifiedBy());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Configuration not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse("Configuration not found", id));
        } catch (Exception e) {
            log.error("Error updating configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("Error updating configuration", e.getMessage()));
        }
    }

    /**
     * Toggle analyzer enabled/disabled status
     * @param id the configuration ID
     * @param request with "enabled" boolean field
     * @return the updated configuration
     */
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<?> setAnalyzerEnabled(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        log.info("PATCH /api/risk/config/{}/enabled - Set enabled status", id);

        try {
            RiskAnalyzerConfig config = configService.getConfigById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

            boolean enabled = request.getOrDefault("enabled", true);
            RiskAnalyzerConfig updated = configService.setAnalyzerEnabled(
                config.getAnalyzerName(), enabled, getModifiedBy());

            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse("Configuration not found", id));
        }
    }

    /**
     * Update a specific threshold value
     * @param id the configuration ID
     * @param request with "thresholdType" and "value"
     * @return the updated configuration
     */
    @PatchMapping("/{id}/threshold")
    public ResponseEntity<?> updateThreshold(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        log.info("PATCH /api/risk/config/{}/threshold - Update threshold", id);

        try {
            RiskAnalyzerConfig config = configService.getConfigById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

            String thresholdType = (String) request.get("thresholdType");
            Double value = ((Number) request.get("value")).doubleValue();

            RiskAnalyzerConfig updated = configService.updateThreshold(
                config.getAnalyzerName(), thresholdType, value, getModifiedBy());

            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating threshold", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("Error updating threshold", e.getMessage()));
        }
    }

    /**
     * Reset configuration to defaults
     * @param id the configuration ID
     * @return the reset configuration
     */
    @PostMapping("/{id}/reset")
    public ResponseEntity<?> resetToDefaults(@PathVariable Long id) {
        log.info("POST /api/risk/config/{}/reset - Reset to defaults", id);

        try {
            RiskAnalyzerConfig config = configService.getConfigById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

            RiskAnalyzerConfig reset = configService.resetToDefaults(
                config.getAnalyzerName(), getModifiedBy());

            return ResponseEntity.ok(reset);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse("Configuration not found", id));
        }
    }

    /**
     * Get configuration statistics
     * @return statistics about configurations
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getConfigStats() {
        log.debug("GET /api/risk/config/stats - Get configuration statistics");

        Map<String, Object> stats = new HashMap<>();
        List<RiskAnalyzerConfig> all = configService.getAllConfigs();
        long enabledCount = configService.countEnabledAnalyzers();

        stats.put("totalConfigurations", all.size());
        stats.put("enabledAnalyzers", enabledCount);
        stats.put("disabledAnalyzers", all.size() - enabledCount);
        stats.put("configurations", all);

        return ResponseEntity.ok(stats);
    }

    /**
     * Clear configuration cache (for admin use)
     * @return success response
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        log.info("POST /api/risk/config/cache/clear - Clear configuration cache");

        configService.clearCache();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Configuration cache cleared");

        return ResponseEntity.ok(response);
    }

    /**
     * Create error response
     */
    private Map<String, Object> errorResponse(String message, Object details) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("details", details);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * Get modified by user ID (from headers or defaults to API)
     */
    private String getModifiedBy() {
        return defaultModifierId;
    }
}
