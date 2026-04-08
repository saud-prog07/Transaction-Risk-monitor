package com.example.riskmonitoring.riskengine.analyzer;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Analyzer for detecting unusual transaction frequency.
 * Flags users with abnormally high transaction frequency within short time windows.
 */
@Slf4j
@Component
public class FrequencyAnalyzer implements RiskAnalyzer {

    private final TransactionHistoryRepository transactionRepository;
    private final int timeWindowMinutes;
    private final int frequencyThreshold;

    public FrequencyAnalyzer(
            TransactionHistoryRepository transactionRepository,
            @Value("${risk.frequency.time-window-minutes:5}") int timeWindowMinutes,
            @Value("${risk.frequency.transaction-threshold:5}") int frequencyThreshold) {
        this.transactionRepository = transactionRepository;
        this.timeWindowMinutes = timeWindowMinutes;
        this.frequencyThreshold = frequencyThreshold;
    }

    @Override
    public RiskDetectionResult analyze(Transaction transaction) {
        try {
            // Define the time window
            Instant now = Instant.now();
            Instant windowStart = now.minus(timeWindowMinutes, ChronoUnit.MINUTES);

            // Count transactions in the time window (excluding current transaction)
            long transactionCount = transactionRepository.countByUserIdAndTimestampBetween(
                    transaction.getUserId(), windowStart, now);

            log.debug("User {} has {} transactions in last {} minutes",
                    transaction.getUserId(), transactionCount, timeWindowMinutes);

            // Check if frequency exceeds threshold
            if (transactionCount >= frequencyThreshold) {
                String reason = String.format(
                        "Abnormal transaction frequency detected: %d transactions in last %d minutes (threshold: %d)",
                        transactionCount, timeWindowMinutes, frequencyThreshold);

                log.warn("High frequency detected for user: {}, Count: {}, Window: {}min",
                        transaction.getUserId(), transactionCount, timeWindowMinutes);

                RiskLevel riskLevel = transactionCount >= frequencyThreshold * 2 
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
                            transactionCount, timeWindowMinutes))
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
