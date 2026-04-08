package com.example.riskmonitoring.alertservice.controller;

import com.example.riskmonitoring.alertservice.dto.SystemConfigRequest;
import com.example.riskmonitoring.alertservice.dto.SystemConfigResponse;
import com.example.riskmonitoring.alertservice.service.SystemConfigurationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Configuration Management Controller
 * Handles system configuration endpoints
 *
 * Endpoints:
 * - GET /api/config - Retrieve current configuration
 * - PUT /api/config - Update configuration (admin only)
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
public class SystemConfigurationController {

    private final SystemConfigurationService configService;

    public SystemConfigurationController(SystemConfigurationService configService) {
        this.configService = configService;
    }

    /**
     * Retrieve current system configuration
     * Accessible to all authenticated users
     *
     * @return SystemConfigResponse with current configuration
     */
    @GetMapping
    public ResponseEntity<SystemConfigResponse> getConfiguration() {
        log.debug("Fetching system configuration");

        SystemConfigResponse config = configService.getConfiguration();

        return ResponseEntity.ok(config);
    }

    /**
     * Update system configuration
     * Requires authentication (admin only in production)
     *
     * @param configRequest the configuration update request
     * @return updated SystemConfigResponse
     */
    @PutMapping
    public ResponseEntity<SystemConfigResponse> updateConfiguration(
            @Valid @RequestBody SystemConfigRequest configRequest) {

        log.info("Configuration update requested");

        // Get current user from security context
        String updatedBy = "unknown";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            updatedBy = authentication.getName();
        }

        SystemConfigResponse updated = configService.updateConfiguration(configRequest, updatedBy);

        log.info("Configuration updated successfully by user: {}", updatedBy);

        return ResponseEntity.ok(updated);
    }
}
