package com.example.riskmonitoring.riskengine.analyzer;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.config.RiskAnalyzerConfig;
import com.example.riskmonitoring.riskengine.config.RiskAnalyzerConfigService;
import com.example.riskmonitoring.riskengine.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analyzer for detecting location-based anomalies.
 * Flags transactions from unusual geographic locations for a user.
 * Uses user baseline calculation for more sophisticated analysis.
 * 
 * Thresholds configured dynamically via RiskAnalyzerConfigService (database-backed with caching)
 * Fallback to environment variables if not configured in database.
 */
@Slf4j
@Component
public class LocationAnomalyAnalyzer implements RiskAnalyzer {

    private final TransactionHistoryRepository transactionRepository;
    private final RiskAnalyzerConfigService configService;
    
    // Default values (used as fallback if not configured in database)
    private final int defaultTimeWindowDays;
    private final boolean defaultEnabled;
    private final double defaultFrequencyThreshold;

    public LocationAnomalyAnalyzer(
            TransactionHistoryRepository transactionRepository,
            RiskAnalyzerConfigService configService,
            @Value("${risk.location-anomaly.time-window-days:30}") int defaultTimeWindowDays,
            @Value("${risk.location-anomaly.enabled:true}") boolean defaultEnabled,
            @Value("${risk.location-anomaly.frequency-threshold:5.0}") double defaultFrequencyThreshold) {
        this.transactionRepository = transactionRepository;
        this.configService = configService;
        this.defaultTimeWindowDays = defaultTimeWindowDays;
        this.defaultEnabled = defaultEnabled;
        this.defaultFrequencyThreshold = defaultFrequencyThreshold;
    }
    
    /**
     * Get configuration from database with fallback to defaults
     */
    private RiskAnalyzerConfig getConfig() {
        return configService.getConfigByName(getName())
            .orElse(null);
    }
    
    /**
     * Check if analyzer is enabled from config or default
     */
    private boolean isEnabled() {
        RiskAnalyzerConfig config = getConfig();
        return config != null ? config.getEnabled() : defaultEnabled;
    }
    
    /**
     * Get time window in days from config or default
     */
    private int getTimeWindowDays() {
        RiskAnalyzerConfig config = getConfig();
        return config != null && config.getTimeWindowDays() != null 
            ? config.getTimeWindowDays() 
            : defaultTimeWindowDays;
    }
    
    /**
     * Get frequency threshold from config or default
     */
    private double getFrequencyThreshold() {
        RiskAnalyzerConfig config = getConfig();
        if (config != null && config.getThresholdPrimary() != null) {
            return config.getThresholdPrimary();
        }
        return defaultFrequencyThreshold;
    }

    @Override
    public RiskDetectionResult analyze(Transaction transaction) {
        if (!isEnabled()) {
            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason("Location anomaly detection disabled")
                    .analyzerName(getName())
                    .build();
        }

        try {
            // Get configurable threshold and time window from database
            double threshold = getFrequencyThreshold();
            int timeWindow = getTimeWindowDays();
            
            // Calculate user baseline
            UserBaselineCalculator calculator = new UserBaselineCalculator(
                    transactionRepository, timeWindow);
            UserBaselineCalculator.UserBaseline baseline = 
                    calculator.calculateBaseline(transaction.getUserId());

            // If not enough historical data, only flag if no location history at all
            if (!baseline.hasSufficientData()) {
                log.debug("Insufficient location history for user: {}", transaction.getUserId());
                return RiskDetectionResult.builder()
                        .flagged(false)
                        .riskLevel(RiskLevel.LOW)
                        .reason("Insufficient location history for analysis")
                        .analyzerName(getName())
                        .build();
            }

            String currentLocation = transaction.getLocation();
            boolean isCommonLocation = baseline.isCommonLocation(currentLocation);
            double locationFrequency = baseline.getLocationFrequencyPercent(currentLocation);

            // Check if location is unusual
            if (!isCommonLocation || locationFrequency < threshold) {
                String reason = String.format(
                        "Unusual location detected: '%s'. User location frequency: %.1f%% (threshold: %.1f%%). Common locations: %s",
                        currentLocation,
                        locationFrequency,
                        threshold,
                        String.join(", ", baseline.getCommonLocations().keySet()));

                log.warn("Location anomaly detected for user: {}, Location: {}, Frequency: {:.1f}%",
                        transaction.getUserId(), currentLocation, locationFrequency);

                return RiskDetectionResult.builder()
                        .flagged(true)
                        .riskLevel(RiskLevel.MEDIUM)
                        .reason(reason)
                        .analyzerName(getName())
                        .build();
            }

            String reason = String.format(
                    "Location '%s' is normal for this user (frequency: %.1f%%)",
                    currentLocation, locationFrequency);

            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason(reason)
                    .analyzerName(getName())
                    .build();

        } catch (Exception ex) {
            log.error("Error in location anomaly analysis", ex);
            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason("Location analysis error: " + ex.getMessage())
                    .analyzerName(getName())
                    .build();
        }
    }

    @Override
    public String getName() {
        return "LocationAnomalyAnalyzer";
    }
}
