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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Analyzer for detecting time-based anomalies.
 * Flags transactions occurring at unusual hours for a user.
 * 
 * Detects:
 * - Transactions outside typical user activity hours
 * - Unusual transaction patterns for the time of day
 * 
 * Thresholds configured dynamically via RiskAnalyzerConfigService (database-backed with caching)
 * Fallback to environment variables if not configured in database.
 */
@Slf4j
@Component
public class TimeAnomalyAnalyzer implements RiskAnalyzer {

    private final TransactionHistoryRepository transactionRepository;
    private final RiskAnalyzerConfigService configService;
    
    // Default values (used as fallback if not configured in database)
    private final int defaultTimeWindowDays;
    private final boolean defaultEnabled;
    private final int defaultUnusualHourThreshold;

    // Define typical business hours (9 AM to 6 PM)
    private static final int BUSINESS_HOURS_START = 9;
    private static final int BUSINESS_HOURS_END = 18;

    public TimeAnomalyAnalyzer(
            TransactionHistoryRepository transactionRepository,
            RiskAnalyzerConfigService configService,
            @Value("${risk.time-anomaly.time-window-days:30}") int defaultTimeWindowDays,
            @Value("${risk.time-anomaly.enabled:true}") boolean defaultEnabled,
            @Value("${risk.time-anomaly.unusual-hour-threshold:80}") int defaultUnusualHourThreshold) {
        this.transactionRepository = transactionRepository;
        this.configService = configService;
        this.defaultTimeWindowDays = defaultTimeWindowDays;
        this.defaultEnabled = defaultEnabled;
        this.defaultUnusualHourThreshold = defaultUnusualHourThreshold;
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
     * Get unusual hour threshold from config or default
     */
    private int getUnusualHourThreshold() {
        RiskAnalyzerConfig config = getConfig();
        if (config != null && config.getThresholdPrimary() != null) {
            return config.getThresholdPrimary().intValue();
        }
        return defaultUnusualHourThreshold;
    }

    @Override
    public RiskDetectionResult analyze(Transaction transaction) {
        if (!isEnabled()) {
            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason("Time anomaly detection disabled")
                    .analyzerName(getName())
                    .build();
        }

        try {
            // Get configurable threshold from database
            int threshold = getUnusualHourThreshold();
            int timeWindow = getTimeWindowDays();
            
            // Get transaction hour
            LocalDateTime transactionTime = LocalDateTime.ofInstant(
                    transaction.getTimestamp(),
                    ZoneId.systemDefault()
            );
            int transactionHour = transactionTime.getHour();

            // Check if transaction is outside business hours
            boolean isOutsideBusinessHours = 
                    transactionHour < BUSINESS_HOURS_START || 
                    transactionHour >= BUSINESS_HOURS_END;

            // Analyze historical transaction patterns
            Instant windowStart = Instant.now().minus(timeWindow, ChronoUnit.DAYS);
            Instant windowEnd = Instant.now();

            var recentTransactions = transactionRepository
                    .findByUserIdAndTimestampBetween(
                            transaction.getUserId(), windowStart, windowEnd);

            // If insufficient history, flag if outside business hours
            if (recentTransactions.isEmpty()) {
                log.debug("No time history for user: {}", transaction.getUserId());
                
                if (isOutsideBusinessHours) {
                    return RiskDetectionResult.builder()
                            .flagged(true)
                            .riskLevel(RiskLevel.MEDIUM)
                            .reason(String.format(
                                    "Transaction at unusual hour (%d:00) with no historical baseline",
                                    transactionHour))
                            .analyzerName(getName())
                            .build();
                }
                
                return RiskDetectionResult.builder()
                        .flagged(false)
                        .riskLevel(RiskLevel.LOW)
                        .reason("No time history available for analysis")
                        .analyzerName(getName())
                        .build();
            }

            // Calculate percentage of transactions in business hours
            long businessHourTransactions = recentTransactions.stream()
                    .filter(h -> {
                        LocalDateTime dt = LocalDateTime.ofInstant(h.getTimestamp(), ZoneId.systemDefault());
                        int hour = dt.getHour();
                        return hour >= BUSINESS_HOURS_START && hour < BUSINESS_HOURS_END;
                    })
                    .count();

            long totalTransactions = recentTransactions.size();
            double businessHourPercentage = (businessHourTransactions * 100.0) / totalTransactions;

            // If most transactions are in business hours and this is outside, flag it
            if (businessHourPercentage >= threshold && isOutsideBusinessHours) {
                String reason = String.format(
                        "Transaction at unusual hour (%d:00). User typically transacts during business hours (%.1f%% of transactions)",
                        transactionHour, businessHourPercentage);

                log.warn("Time anomaly detected for user: {}, Hour: {}, Business hour pattern: {:.1f}%",
                        transaction.getUserId(), transactionHour, businessHourPercentage);

                return RiskDetectionResult.builder()
                        .flagged(true)
                        .riskLevel(RiskLevel.MEDIUM)
                        .reason(reason)
                        .analyzerName(getName())
                        .build();
            }

            String reason = String.format(
                    "Transaction at hour %d:00 is within normal user activity pattern (%.1f%% in business hours)",
                    transactionHour, businessHourPercentage);

            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason(reason)
                    .analyzerName(getName())
                    .build();

        } catch (Exception ex) {
            log.error("Error in time anomaly analysis", ex);
            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason("Time analysis error: " + ex.getMessage())
                    .analyzerName(getName())
                    .build();
        }
    }

    @Override
    public String getName() {
        return "TimeAnomalyAnalyzer";
    }
}
