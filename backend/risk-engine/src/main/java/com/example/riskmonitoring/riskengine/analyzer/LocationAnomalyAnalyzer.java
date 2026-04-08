package com.example.riskmonitoring.riskengine.analyzer;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analyzer for detecting location-based anomalies.
 * Flags transactions from unusual geographic locations for a user.
 * Optional: Can be disabled via configuration.
 */
@Slf4j
@Component
public class LocationAnomalyAnalyzer implements RiskAnalyzer {

    private final TransactionHistoryRepository transactionRepository;
    private final int timeWindowDays;
    private final boolean enabled;

    public LocationAnomalyAnalyzer(
            TransactionHistoryRepository transactionRepository,
            @Value("${risk.location-anomaly.time-window-days:30}") int timeWindowDays,
            @Value("${risk.location-anomaly.enabled:true}") boolean enabled) {
        this.transactionRepository = transactionRepository;
        this.timeWindowDays = timeWindowDays;
        this.enabled = enabled;
    }

    @Override
    public RiskDetectionResult analyze(Transaction transaction) {
        if (!enabled) {
            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason("Location anomaly detection disabled")
                    .analyzerName(getName())
                    .build();
        }

        try {
            // Get recent transactions for location pattern analysis
            Instant windowStart = Instant.now().minus(timeWindowDays, ChronoUnit.DAYS);
            Instant windowEnd = Instant.now();

            List<com.example.riskmonitoring.riskengine.domain.TransactionHistory> recentTransactions =
                    transactionRepository.findByUserIdAndTimestampBetween(
                            transaction.getUserId(), windowStart, windowEnd);

            // If not enough historical data, assume normal
            if (recentTransactions.isEmpty()) {
                log.debug("No location history for user: {}", transaction.getUserId());
                return RiskDetectionResult.builder()
                        .flagged(false)
                        .riskLevel(RiskLevel.LOW)
                        .reason("No location history available")
                        .analyzerName(getName())
                        .build();
            }

            // Extract unique locations from history
            Set<String> historicalLocations = new HashSet<>();
            for (var hist : recentTransactions) {
                historicalLocations.add(hist.getLocation());
            }

            // Check if current location is in historical set
            String currentLocation = transaction.getLocation();
            if (!historicalLocations.contains(currentLocation)) {
                log.warn("Location anomaly detected for user: {}, New location: {}",
                        transaction.getUserId(), currentLocation);

                // Get most common location for reference
                String commonLocation = recentTransactions.stream()
                        .map(com.example.riskmonitoring.riskengine.domain.TransactionHistory::getLocation)
                        .max(Comparator.comparingInt(loc -> countLocation(recentTransactions, loc)))
                        .orElse("Unknown");

                String reason = String.format(
                        "Unusual location detected: %s. User's typical location: %s. Historical locations: %s",
                        currentLocation, commonLocation, historicalLocations);

                return RiskDetectionResult.builder()
                        .flagged(true)
                        .riskLevel(RiskLevel.MEDIUM)
                        .reason(reason)
                        .analyzerName(getName())
                        .build();
            }

            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason(String.format("Location %s matches historical pattern", currentLocation))
                    .analyzerName(getName())
                    .build();

        } catch (Exception ex) {
            log.error("Error in location anomaly analysis for transaction: {}", transaction.getTransactionId(), ex);
            return RiskDetectionResult.builder()
                    .flagged(false)
                    .riskLevel(RiskLevel.LOW)
                    .reason("Analysis error: " + ex.getMessage())
                    .analyzerName(getName())
                    .build();
        }
    }

    private int countLocation(List<com.example.riskmonitoring.riskengine.domain.TransactionHistory> transactions,
                              String location) {
        return (int) transactions.stream()
                .filter(t -> location.equals(t.getLocation()))
                .count();
    }

    @Override
    public String getName() {
        return "LocationAnomalyAnalyzer";
    }
}
