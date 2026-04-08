package com.example.riskmonitoring.alertservice.service;

import com.example.riskmonitoring.alertservice.domain.SystemConfiguration;
import com.example.riskmonitoring.alertservice.dto.SystemConfigRequest;
import com.example.riskmonitoring.alertservice.dto.SystemConfigResponse;
import com.example.riskmonitoring.alertservice.repository.SystemConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * System Configuration Service
 * Handles fetching and updating system configuration
 * Configurations are cached and read dynamically by risk detection services
 */
@Slf4j
@Service
@Transactional
public class SystemConfigurationService {

    private static final String CONFIG_ID = "default";

    private final SystemConfigurationRepository configRepository;

    public SystemConfigurationService(SystemConfigurationRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * Get current system configuration
     * Initializes with defaults if not found
     *
     * @return SystemConfigResponse with current configuration
     */
    public SystemConfigResponse getConfiguration() {
        SystemConfiguration config = configRepository.findById(CONFIG_ID)
                .orElseGet(this::initializeDefaultConfiguration);

        return SystemConfigResponse.fromEntity(config);
    }

    /**
     * Update system configuration
     * Merges request values with existing configuration
     *
     * @param configRequest the configuration update request
     * @param updatedBy username of person making the update
     * @return updated SystemConfigResponse
     */
    public SystemConfigResponse updateConfiguration(
            SystemConfigRequest configRequest,
            String updatedBy) {

        log.info("Updating system configuration by user: {}", updatedBy);

        // Get existing configuration or create new
        SystemConfiguration config = configRepository.findById(CONFIG_ID)
                .orElseGet(this::initializeDefaultConfiguration);

        // Update non-null fields
        if (configRequest.getHighRiskThreshold() != null) {
            config.setHighRiskThreshold(configRequest.getHighRiskThreshold());
        }
        if (configRequest.getMediumRiskThreshold() != null) {
            config.setMediumRiskThreshold(configRequest.getMediumRiskThreshold());
        }
        if (configRequest.getLowRiskThreshold() != null) {
            config.setLowRiskThreshold(configRequest.getLowRiskThreshold());
        }
        if (configRequest.getAnomalyMultiplier() != null) {
            config.setAnomalyMultiplier(configRequest.getAnomalyMultiplier());
        }
        if (configRequest.getVelocityCheckEnabled() != null) {
            config.setVelocityCheckEnabled(configRequest.getVelocityCheckEnabled());
        }
        if (configRequest.getVelocityThreshold() != null) {
            config.setVelocityThreshold(configRequest.getVelocityThreshold());
        }
        if (configRequest.getGeolocationCheckEnabled() != null) {
            config.setGeolocationCheckEnabled(configRequest.getGeolocationCheckEnabled());
        }
        if (configRequest.getAmountSpikeCheckEnabled() != null) {
            config.setAmountSpikeCheckEnabled(configRequest.getAmountSpikeCheckEnabled());
        }
        if (configRequest.getAmountSpikeMultiplier() != null) {
            config.setAmountSpikeMultiplier(configRequest.getAmountSpikeMultiplier());
        }
        if (configRequest.getEnforceMfaForHighRisk() != null) {
            config.setEnforceMfaForHighRisk(configRequest.getEnforceMfaForHighRisk());
        }
        if (configRequest.getAutoEscalationEnabled() != null) {
            config.setAutoEscalationEnabled(configRequest.getAutoEscalationEnabled());
        }

        config.setUpdatedBy(updatedBy);

        // Save updated configuration
        SystemConfiguration updated = configRepository.save(config);

        log.info("System configuration updated successfully");

        return SystemConfigResponse.fromEntity(updated);
    }

    /**
     * Initialize default configuration
     *
     * @return default SystemConfiguration
     */
    private SystemConfiguration initializeDefaultConfiguration() {
        log.info("Initializing default system configuration");

        SystemConfiguration defaultConfig = SystemConfiguration.builder()
                .id(CONFIG_ID)
                .highRiskThreshold(80.0)
                .mediumRiskThreshold(50.0)
                .lowRiskThreshold(20.0)
                .anomalyMultiplier(1.5)
                .velocityCheckEnabled(true)
                .velocityThreshold(10)
                .geolocationCheckEnabled(true)
                .amountSpikeCheckEnabled(true)
                .amountSpikeMultiplier(2.0)
                .enforceMfaForHighRisk(true)
                .autoEscalationEnabled(true)
                .updatedBy("system")
                .build();

        return configRepository.save(defaultConfig);
    }
}
