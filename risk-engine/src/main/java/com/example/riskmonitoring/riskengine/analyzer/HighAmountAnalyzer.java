package com.example.riskmonitoring.riskengine.analyzer;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Analyzer for detecting high-value transactions.
 * Flags transactions significantly above user's average amount.
 */
@Slf4j
@Component
public class HighAmountAnalyzer implements RiskAnalyzer {

    private final TransactionHistoryRepository transactionRepository;
    private final BigDecimal highAmountMultiplier;
    private final BigDecimal minimumThreshold;

    public HighAmountAnalyzer(
            TransactionHistoryRepository transactionRepository,
            @Value("${risk.high-amount.multiplier:2.0}") double highAmountMultiplier,
            @Value("${risk.high-amount.minimum-threshold:1000.00}") double minimumThreshold) {
        this.transactionRepository = transactionRepository;
        this.highAmountMultiplier = BigDecimal.valueOf(highAmountMultiplier);
        this.minimumThreshold = BigDecimal.valueOf(minimumThreshold);
    }

    @Override
    public RiskDetectionResult analyze(Transaction transaction) {
        try {
            BigDecimal averageAmount = transactionRepository.findAverageAmountByUserId(transaction.getUserId());

            // No historical data available
            if (averageAmount == null) {
                log.debug("No historical data for user: {}", transaction.getUserId());
                return RiskDetectionResult.builder()
                        .flagged(false)
                        .riskLevel(RiskLevel.LOW)
                        .reason("No historical data available for comparison")
                        .analyzerName(getName())
                        .build();
            }

            // Calculate high amount threshold
            BigDecimal threshold = averageAmount.multiply(highAmountMultiplier);

            // Use minimum threshold if calculated threshold is too low
            if (threshold.compareTo(minimumThreshold) < 0) {
                threshold = minimumThreshold;
            }

            // Check if transaction amount exceeds threshold
            if (transaction.getAmount().compareTo(threshold) > 0) {
                String reason = String.format(
                        "Transaction amount %.2f exceeds %.2fx user average (%.2f)",
                        transaction.getAmount(), highAmountMultiplier, averageAmount);

                log.warn("High amount detected for user: {}, Amount: {}, Threshold: {}",
                        transaction.getUserId(), transaction.getAmount(), threshold);

                return RiskDetectionResult.builder()
                        .flagged(true)
                        .riskLevel(RiskLevel.HIGH)
                        .reason(reason)
                        .analyzerName(getName())
                        .build();
            }

            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason(String.format("Amount within acceptable range (%.2f <= %.2f)",
                            transaction.getAmount(), threshold))
                    .analyzerName(getName())
                    .build();

        } catch (Exception ex) {
            log.error("Error in high amount analysis for transaction: {}", transaction.getTransactionId(), ex);
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
        return "HighAmountAnalyzer";
    }
}
