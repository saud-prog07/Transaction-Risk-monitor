package com.example.riskmonitoring.riskengine.analyzer;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.config.RiskAnalyzerConfig;
import com.example.riskmonitoring.riskengine.config.RiskAnalyzerConfigService;
import com.example.riskmonitoring.riskengine.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/**
 * Analyzer for detecting high-value transaction anomalies.
 * Flags transactions significantly above user's typical spending patterns.
 * Uses user baseline calculation for sophisticated statistical analysis.
 * 
 * Thresholds configured dynamically via RiskAnalyzerConfigService (database-backed with caching)
 * Fallback to environment variables if not configured in database.
 */
@Slf4j
@Component
public class HighAmountAnalyzer implements RiskAnalyzer {

    private final TransactionHistoryRepository transactionRepository;
    private final RiskAnalyzerConfigService configService;
    
    // Default values (used as fallback if not configured in database)
    private final double defaultMultiplier;
    private final double defaultMinimumThreshold;
    private final int defaultTimeWindowDays;

    public HighAmountAnalyzer(
            TransactionHistoryRepository transactionRepository,
            RiskAnalyzerConfigService configService,
            @Value("${risk.high-amount.multiplier:2.0}") double defaultMultiplier,
            @Value("${risk.high-amount.minimum-threshold:1000.00}") double defaultMinimumThreshold,
            @Value("${risk.high-amount.time-window-days:30}") int defaultTimeWindowDays) {
        this.transactionRepository = transactionRepository;
        this.configService = configService;
        this.defaultMultiplier = defaultMultiplier;
        this.defaultMinimumThreshold = defaultMinimumThreshold;
        this.defaultTimeWindowDays = defaultTimeWindowDays;
    }
    
    /**
     * Get configuration from database with fallback to defaults
     */
    private RiskAnalyzerConfig getConfig() {
        return configService.getConfigByName(getName())
            .orElse(null);
    }
    
    /**
     * Get multiplier from config or default
     */
    private BigDecimal getMultiplier() {
        RiskAnalyzerConfig config = getConfig();
        if (config != null && config.getThresholdPrimary() != null) {
            return BigDecimal.valueOf(config.getThresholdPrimary());
        }
        return BigDecimal.valueOf(defaultMultiplier);
    }
    
    /**
     * Get minimum threshold from config or default
     */
    private BigDecimal getMinimumThreshold() {
        RiskAnalyzerConfig config = getConfig();
        if (config != null && config.getThresholdSecondary() != null) {
            return BigDecimal.valueOf(config.getThresholdSecondary());
        }
        return BigDecimal.valueOf(defaultMinimumThreshold);
    }
    
    /**
     * Get time window from config or default
     */
    private int getTimeWindowDays() {
        RiskAnalyzerConfig config = getConfig();
        if (config != null && config.getTimeWindowDays() != null) {
            return config.getTimeWindowDays();
        }
        return defaultTimeWindowDays;
    }

    @Override
    public RiskDetectionResult analyze(Transaction transaction) {
        try {
            // Get current time window from config
            int timeWindow = getTimeWindowDays();
            
            // Calculate user baseline for sophisticated analysis
            UserBaselineCalculator calculator = new UserBaselineCalculator(
                    transactionRepository, timeWindow);
            UserBaselineCalculator.UserBaseline baseline = 
                    calculator.calculateBaseline(transaction.getUserId());

            // No historical data available
            if (!baseline.hasSufficientData()) {
                log.debug("No historical data for user: {}", transaction.getUserId());
                return RiskDetectionResult.builder()
                        .flagged(false)
                        .riskLevel(RiskLevel.LOW)
                        .reason("No historical data available for comparison")
                        .analyzerName(getName())
                        .build();
            }

            BigDecimal averageAmount = baseline.getAverageAmount();
            BigDecimal stdDevAmount = baseline.getStdDevAmount();
            BigDecimal upperThreshold = baseline.getUpperSpendingThreshold();

            // Get configurable thresholds from database
            BigDecimal configuredMultiplier = getMultiplier();
            BigDecimal configuredMinimum = getMinimumThreshold();

            // Calculate multiple thresholds
            BigDecimal multiplierThreshold = averageAmount.multiply(configuredMultiplier);

            // Use the higher of multiplier-based or minimum threshold
            BigDecimal finalThreshold = multiplierThreshold.compareTo(configuredMinimum) > 0 
                    ? multiplierThreshold 
                    : configuredMinimum;

            // Check if transaction amount exceeds threshold
            if (transaction.getAmount().compareTo(finalThreshold) > 0) {
                double deviations = 0;
                if (stdDevAmount.compareTo(BigDecimal.ZERO) > 0) {
                    deviations = transaction.getAmount()
                            .subtract(averageAmount)
                            .divide(stdDevAmount, 2, java.math.RoundingMode.HALF_UP)
                            .doubleValue();
                }

                String reason = String.format(
                        "High amount detected. Transaction: %.2f (avg: %.2f, threshold: %.2f, std dev: %.2f, deviations: %.1f)",
                        transaction.getAmount(), averageAmount, finalThreshold, stdDevAmount, deviations);

                log.warn("High amount alert for user: {}, Amount: {}, Threshold: {}, StdDev multiplier: {:.1f}",
                        transaction.getUserId(), transaction.getAmount(), finalThreshold, deviations);

                // Calculate risk level based on how much it exceeds
                RiskLevel riskLevel = RiskLevel.MEDIUM;
                if (transaction.getAmount().compareTo(upperThreshold) > 0) {
                    riskLevel = RiskLevel.HIGH;
                }

                return RiskDetectionResult.builder()
                        .flagged(true)
                        .riskLevel(riskLevel)
                        .reason(reason)
                        .analyzerName(getName())
                        .build();
            }

            String reason = String.format(
                    "Amount within normal range. Transaction: %.2f (avg: %.2f, max threshold: %.2f)",
                    transaction.getAmount(), averageAmount, finalThreshold);

            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason(reason)
                    .analyzerName(getName())
                    .build();

        } catch (Exception ex) {
            log.error("Error in high amount analysis", ex);
            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason("Amount analysis error: " + ex.getMessage())
                    .analyzerName(getName())
                    .build();
        }
    }

    @Override
    public String getName() {
        return "HighAmountAnalyzer";
    }
}
