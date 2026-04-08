package com.example.riskmonitoring.riskengine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing risk analyzer configurations.
 * 
 * Features:
 * - CRUD operations for risk analyzer configurations
 * - Caching for performance (with cache invalidation)
 * - Default configuration initialization
 * - Audit trail support
 * 
 * Configuration changes take effect immediately due to caching and service refresh.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class RiskAnalyzerConfigService {

    private final RiskAnalyzerConfigRepository configRepository;
    private static final String CACHE_NAME = "riskAnalyzerConfigs";
    private static final String CACHE_KEY_BY_NAME = "ANALYZER_CONFIG";

    public RiskAnalyzerConfigService(RiskAnalyzerConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * Initialize default configurations if they don't exist.
     * Called on application startup.
     */
    @Transactional
    public void initializeDefaults() {
        log.info("Initializing default risk analyzer configurations");

        // HighAmountAnalyzer
        initializeIfNeeded(
            "HighAmountAnalyzer",
            "High Amount Analyzer",
            "Detects transactions significantly above user's typical spending patterns",
            true,
            2.0,      // multiplier
            1000.00,  // minimum threshold
            null,     // tertiary
            30,       // timeWindowDays
            null      // timeWindowMinutes
        );

        // FrequencyAnalyzer
        initializeIfNeeded(
            "FrequencyAnalyzer",
            "Frequency Analyzer",
            "Detects abnormally high transaction frequency within short time windows",
            true,
            5.0,      // threshold (transaction count)
            null,     // secondary
            null,     // tertiary
            null,     // timeWindowDays
            5         // timeWindowMinutes
        );

        // TimeAnomalyAnalyzer
        initializeIfNeeded(
            "TimeAnomalyAnalyzer",
            "Time Anomaly Analyzer",
            "Detects transactions occurring at unusual hours for a user",
            true,
            80.0,     // unusual hour threshold
            null,     // secondary
            null,     // tertiary
            30,       // timeWindowDays
            null      // timeWindowMinutes
        );

        // LocationAnomalyAnalyzer
        initializeIfNeeded(
            "LocationAnomalyAnalyzer",
            "Location Anomaly Analyzer",
            "Detects transactions from unusual geographic locations",
            true,
            5.0,      // frequency threshold
            null,     // secondary
            null,     // tertiary
            30,       // timeWindowDays
            null      // timeWindowMinutes
        );

        log.info("Default risk analyzer configurations initialized");
    }

    /**
     * Initialize a configuration if it doesn't already exist
     */
    @Transactional
    private void initializeIfNeeded(
            String analyzerName,
            String displayName,
            String description,
            boolean enabled,
            Double primaryThreshold,
            Double secondaryThreshold,
            Double tertiaryThreshold,
            Integer timeWindowDays,
            Integer timeWindowMinutes) {

        if (!configRepository.existsByAnalyzerName(analyzerName)) {
            RiskAnalyzerConfig config = RiskAnalyzerConfig.builder()
                .analyzerName(analyzerName)
                .displayName(displayName)
                .description(description)
                .enabled(enabled)
                .thresholdPrimary(primaryThreshold)
                .thresholdSecondary(secondaryThreshold)
                .thresholdTertiary(tertiaryThreshold)
                .timeWindowDays(timeWindowDays)
                .timeWindowMinutes(timeWindowMinutes)
                .modifiedBy("SYSTEM")
                .build();

            configRepository.save(config);
            log.info("Initialized default config for: {}", analyzerName);
        }
    }

    /**
     * Get configuration by analyzer name with caching
     * @param analyzerName the analyzer name
     * @return RiskAnalyzerConfig
     */
    @Cacheable(value = CACHE_NAME, key = "#analyzerName")
    public Optional<RiskAnalyzerConfig> getConfigByName(String analyzerName) {
        log.debug("Fetching config for analyzer: {}", analyzerName);
        return configRepository.findByAnalyzerName(analyzerName);
    }

    /**
     * Get all configurations
     * @return list of all configurations
     */
    @Cacheable(value = CACHE_NAME, key = "'ALL'")
    public List<RiskAnalyzerConfig> getAllConfigs() {
        log.debug("Fetching all risk analyzer configurations");
        return configRepository.findAll();
    }

    /**
     * Get all enabled configurations
     * @return list of enabled configurations
     */
    @Cacheable(value = CACHE_NAME, key = "'ENABLED'")
    public List<RiskAnalyzerConfig> getEnabledConfigs() {
        log.debug("Fetching enabled risk analyzer configurations");
        return configRepository.findByEnabledTrue();
    }

    /**
     * Get configuration by ID
     * @param id the configuration ID
     * @return RiskAnalyzerConfig
     */
    public Optional<RiskAnalyzerConfig> getConfigById(Long id) {
        return configRepository.findById(id);
    }

    /**
     * Update configuration and invalidate cache
     * @param id the configuration ID
     * @param config the updated configuration
     * @param modifiedBy who modified the configuration
     * @return updated RiskAnalyzerConfig
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RiskAnalyzerConfig updateConfig(Long id, RiskAnalyzerConfig config, String modifiedBy) {
        log.info("Updating risk analyzer configuration: {}", id);

        RiskAnalyzerConfig existing = configRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + id));

        // Update fields (preserve immutable fields)
        existing.setDisplayName(config.getDisplayName());
        existing.setDescription(config.getDescription());
        existing.setEnabled(config.getEnabled());
        existing.setThresholdPrimary(config.getThresholdPrimary());
        existing.setThresholdSecondary(config.getThresholdSecondary());
        existing.setThresholdTertiary(config.getThresholdTertiary());
        existing.setTimeWindowDays(config.getTimeWindowDays());
        existing.setTimeWindowMinutes(config.getTimeWindowMinutes());
        existing.setAdditionalConfig(config.getAdditionalConfig());
        existing.setModifiedBy(modifiedBy);
        existing.setUpdatedAt(LocalDateTime.now());

        RiskAnalyzerConfig updated = configRepository.save(existing);
        log.info("Configuration updated: {} by {}", id, modifiedBy);
        return updated;
    }

    /**
     * Update configuration by analyzer name
     * @param analyzerName the analyzer name
     * @param config the updated configuration
     * @param modifiedBy who modified the configuration
     * @return updated RiskAnalyzerConfig
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RiskAnalyzerConfig updateConfigByName(String analyzerName, RiskAnalyzerConfig config, String modifiedBy) {
        log.info("Updating configuration for analyzer: {}", analyzerName);

        RiskAnalyzerConfig existing = configRepository.findByAnalyzerName(analyzerName)
            .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + analyzerName));

        return updateConfig(existing.getId(), config, modifiedBy);
    }

    /**
     * Enable or disable an analyzer
     * @param analyzerName the analyzer name
     * @param enabled whether to enable
     * @param modifiedBy who made the change
     * @return updated RiskAnalyzerConfig
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RiskAnalyzerConfig setAnalyzerEnabled(String analyzerName, boolean enabled, String modifiedBy) {
        log.info("Setting analyzer {} enabled={} by {}", analyzerName, enabled, modifiedBy);

        RiskAnalyzerConfig config = configRepository.findByAnalyzerName(analyzerName)
            .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + analyzerName));

        config.setEnabled(enabled);
        config.setModifiedBy(modifiedBy);
        config.setUpdatedAt(LocalDateTime.now());

        return configRepository.save(config);
    }

    /**
     * Update threshold value for an analyzer
     * @param analyzerName the analyzer name
     * @param thresholdType which threshold (PRIMARY, SECONDARY, TERTIARY)
     * @param value the new value
     * @param modifiedBy who made the change
     * @return updated RiskAnalyzerConfig
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RiskAnalyzerConfig updateThreshold(
            String analyzerName,
            String thresholdType,
            double value,
            String modifiedBy) {

        log.info("Updating {}.{} = {} by {}", analyzerName, thresholdType, value, modifiedBy);

        RiskAnalyzerConfig config = configRepository.findByAnalyzerName(analyzerName)
            .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + analyzerName));

        switch (thresholdType.toUpperCase()) {
            case "PRIMARY":
                config.setThresholdPrimary(value);
                break;
            case "SECONDARY":
                config.setThresholdSecondary(value);
                break;
            case "TERTIARY":
                config.setThresholdTertiary(value);
                break;
            default:
                throw new IllegalArgumentException("Invalid threshold type: " + thresholdType);
        }

        config.setModifiedBy(modifiedBy);
        config.setUpdatedAt(LocalDateTime.now());
        return configRepository.save(config);
    }

    /**
     * Count enabled analyzers
     * @return number of enabled analyzers
     */
    public long countEnabledAnalyzers() {
        return configRepository.countByEnabledTrue();
    }

    /**
     * Reset a configuration to its defaults
     * @param analyzerName the analyzer name
     * @param modifiedBy who made the change
     * @return reset RiskAnalyzerConfig
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public RiskAnalyzerConfig resetToDefaults(String analyzerName, String modifiedBy) {
        log.info("Resetting {} to defaults by {}", analyzerName, modifiedBy);

        RiskAnalyzerConfig config = configRepository.findByAnalyzerName(analyzerName)
            .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + analyzerName));

        // Reset based on analyzer type
        switch (analyzerName) {
            case "HighAmountAnalyzer":
                config.setThresholdPrimary(2.0);
                config.setThresholdSecondary(1000.0);
                config.setTimeWindowDays(30);
                break;
            case "FrequencyAnalyzer":
                config.setThresholdPrimary(5.0);
                config.setTimeWindowMinutes(5);
                break;
            case "TimeAnomalyAnalyzer":
                config.setThresholdPrimary(80.0);
                config.setTimeWindowDays(30);
                break;
            case "LocationAnomalyAnalyzer":
                config.setThresholdPrimary(5.0);
                config.setTimeWindowDays(30);
                break;
            default:
                throw new IllegalArgumentException("Unknown analyzer: " + analyzerName);
        }

        config.setModifiedBy(modifiedBy);
        config.setUpdatedAt(LocalDateTime.now());
        return configRepository.save(config);
    }

    /**
     * Clear the configuration cache (e.g., after manual updates)
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        log.info("Cache cleared for risk analyzer configurations");
    }
}
