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

/**
 * Analyzer for detecting unusual transaction frequency.
 * Flags users with abnormally high transaction frequency within short time windows.
 * 
 * Thresholds configured dynamically via RiskAnalyzerConfigService (database-backed with caching)
 * Fallback to environment variables if not configured in database.
 */
@Slf4j
@Component
public class FrequencyAnalyzer implements RiskAnalyzer {

    private final TransactionHistoryRepository transactionRepository;
    private final RiskAnalyzerConfigService configService;
    
    // Default values (used as fallback if not configured in database)
    private final int defaultTimeWindowMinutes;
    private final int defaultFrequencyThreshold;

    public FrequencyAnalyzer(
            TransactionHistoryRepository transactionRepository,
            RiskAnalyzerConfigService configService,
            @Value("${risk.frequency.time-window-minutes:5}") int defaultTimeWindowMinutes,
            @Value("${risk.frequency.transaction-threshold:5}") int defaultFrequencyThreshold) {
        this.transactionRepository = transactionRepository;
        this.configService = configService;
        this.defaultTimeWindowMinutes = defaultTimeWindowMinutes;
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
     * Get time window in minutes from config or default
     */
    private int getTimeWindowMinutes() {
        RiskAnalyzerConfig config = getConfig();
        return config != null && config.getTimeWindowMinutes() != null 
            ? config.getTimeWindowMinutes() 
            : defaultTimeWindowMinutes;
    }
    
    /**
     * Get frequency threshold from config or default
     */
    private int getFrequencyThreshold() {
        RiskAnalyzerConfig config = getConfig();
        if (config != null && config.getThresholdPrimary() != null) {
            return config.getThresholdPrimary().intValue();
        }
        return defaultFrequencyThreshold;
    }

    @Override
    public RiskDetectionResult analyze(Transaction transaction) {
        try {
            // Get configurable time window and threshold from database
            int timeWindow = getTimeWindowMinutes();
            int threshold = getFrequencyThreshold();
            
            // Define the time window
            Instant now = Instant.now();
            Instant windowStart = now.minus(timeWindow, ChronoUnit.MINUTES);

            // Count transactions in the time window (excluding current transaction)
            long transactionCount = transactionRepository.countByUserIdAndTimestampBetween(
                    transaction.getUserId(), windowStart, now);

            log.debug("User {} has {} transactions in last {} minutes",
                    transaction.getUserId(), transactionCount, timeWindow);

            // Check if frequency exceeds threshold
            if (transactionCount >= threshold) {
                String reason = String.format(
                        "Abnormal transaction frequency detected: %d transactions in last %d minutes (threshold: %d)",
                        transactionCount, timeWindow, threshold);

                log.warn("High frequency detected for user: {}, Count: {}, Window: {}min",
                        transaction.getUserId(), transactionCount, timeWindow);

                RiskLevel riskLevel = transactionCount >= threshold * 2 
                        ? RiskLevel.HIGH 
                        : RiskLevel.MEDIUM;

                return RiskDetectionResult.builder()
                        .flagged(true)
                        .riskLevel(riskLevel)
                        .reason(reason)
                        .analyzerName(getName())
                        .build();
            }

            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason(String.format("Transaction frequency normal: %d transactions in %d minutes",
                            transactionCount, timeWindow))
                    .analyzerName(getName())
                    .build();

        } catch (Exception ex) {
            log.error("Error in frequency analysis for transaction: {}", transaction.getTransactionId(), ex);
            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason("Analysis error: " + ex.getMessage())
                    .analyzerName(getName())
                    .build();
        }
    }

    @Override
    public String getName() {
        return "FrequencyAnalyzer";
    }
}
