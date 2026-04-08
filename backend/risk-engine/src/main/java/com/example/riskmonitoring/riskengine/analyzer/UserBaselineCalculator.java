package com.example.riskmonitoring.riskengine.analyzer;

import com.example.riskmonitoring.riskengine.domain.TransactionHistory;
import com.example.riskmonitoring.riskengine.repository.TransactionHistoryRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * User Baseline Calculator
 * Calculates user's typical transaction patterns for anomaly detection
 * 
 * Provides metrics like:
 * - Average transaction amount
 * - Spending by hour of day
 * - Common locations
 * - Peak activity hours
 */
@Slf4j
public class UserBaselineCalculator {

    private final TransactionHistoryRepository repository;
    private final int timeWindowDays;

    public UserBaselineCalculator(
            TransactionHistoryRepository repository,
            int timeWindowDays) {
        this.repository = repository;
        this.timeWindowDays = timeWindowDays;
    }

    /**
     * Calculate comprehensive user baseline
     * @param userId the user ID
     * @return UserBaseline with all metrics
     */
    public UserBaseline calculateBaseline(String userId) {
        Instant windowStart = Instant.now().minus(timeWindowDays, ChronoUnit.DAYS);
        Instant windowEnd = Instant.now();

        List<TransactionHistory> transactions = repository
                .findByUserIdAndTimestampBetween(userId, windowStart, windowEnd);

        if (transactions.isEmpty()) {
            log.debug("No transaction history for user: {}", userId);
            return UserBaseline.empty();
        }

        return UserBaseline.builder()
                .userId(userId)
                .averageAmount(calculateAverageAmount(transactions))
                .stdDevAmount(calculateStandardDeviation(transactions))
                .minAmount(calculateMinAmount(transactions))
                .maxAmount(calculateMaxAmount(transactions))
                .commonLocations(calculateCommonLocations(transactions))
                .spendingByHour(calculateSpendingByHour(transactions))
                .peakActivityHours(calculatePeakActivityHours(transactions))
                .totalTransactions(transactions.size())
                .timeWindowDays(timeWindowDays)
                .build();
    }

    /**
     * Calculate average transaction amount
     */
    private BigDecimal calculateAverageAmount(List<TransactionHistory> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = transactions.stream()
                .map(TransactionHistory::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(transactions.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate standard deviation of amounts
     */
    private BigDecimal calculateStandardDeviation(List<TransactionHistory> transactions) {
        if (transactions.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal average = calculateAverageAmount(transactions);

        BigDecimal sumSquaredDiff = transactions.stream()
                .map(t -> t.getAmount().subtract(average).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = sumSquaredDiff.divide(
                BigDecimal.valueOf(transactions.size() - 1),
                2,
                java.math.RoundingMode.HALF_UP);

        // Return square root of variance
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    /**
     * Calculate minimum amount
     */
    private BigDecimal calculateMinAmount(List<TransactionHistory> transactions) {
        return transactions.stream()
                .map(TransactionHistory::getAmount)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Calculate maximum amount
     */
    private BigDecimal calculateMaxAmount(List<TransactionHistory> transactions) {
        return transactions.stream()
                .map(TransactionHistory::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Find most common locations
     */
    private Map<String, Integer> calculateCommonLocations(List<TransactionHistory> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        TransactionHistory::getLocation,
                        Collectors.summingInt(t -> 1)))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)  // Top 10 locations
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    /**
     * Calculate spending by hour of day
     */
    private Map<Integer, BigDecimal> calculateSpendingByHour(List<TransactionHistory> transactions) {
        Map<Integer, List<TransactionHistory>> byHour = transactions.stream()
                .collect(Collectors.groupingBy(t -> {
                    LocalDateTime dt = LocalDateTime.ofInstant(t.getTimestamp(), ZoneId.systemDefault());
                    return dt.getHour();
                }));

        Map<Integer, BigDecimal> spendingByHour = new HashMap<>();
        for (Map.Entry<Integer, List<TransactionHistory>> entry : byHour.entrySet()) {
            BigDecimal average = calculateAverageAmount(entry.getValue());
            spendingByHour.put(entry.getKey(), average);
        }

        return spendingByHour;
    }

    /**
     * Identify peak activity hours
     */
    private List<Integer> calculatePeakActivityHours(List<TransactionHistory> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(t -> {
                    LocalDateTime dt = LocalDateTime.ofInstant(t.getTimestamp(), ZoneId.systemDefault());
                    return dt.getHour();
                }, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)  // Top 5 hours
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * User Baseline Data Transfer Object
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserBaseline {
        private String userId;
        private BigDecimal averageAmount;
        private BigDecimal stdDevAmount;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private Map<String, Integer> commonLocations;  // Location -> Count
        private Map<Integer, BigDecimal> spendingByHour;  // Hour -> Average amount
        private List<Integer> peakActivityHours;  // Sorted by frequency
        private int totalTransactions;
        private int timeWindowDays;

        /**
         * Create empty baseline
         */
        public static UserBaseline empty() {
            return UserBaseline.builder()
                    .averageAmount(BigDecimal.ZERO)
                    .stdDevAmount(BigDecimal.ZERO)
                    .minAmount(BigDecimal.ZERO)
                    .maxAmount(BigDecimal.ZERO)
                    .commonLocations(new HashMap<>())
                    .spendingByHour(new HashMap<>())
                    .peakActivityHours(List.of())
                    .totalTransactions(0)
                    .build();
        }

        /**
         * Check if baseline has sufficient data
         */
        public boolean hasSufficientData() {
            return totalTransactions >= 5;  // Need at least 5 transactions
        }

        /**
         * Calculate upper spending threshold (average + 2 * stdDev)
         */
        public BigDecimal getUpperSpendingThreshold() {
            if (stdDevAmount.compareTo(BigDecimal.ZERO) == 0) {
                return averageAmount.multiply(BigDecimal.valueOf(2.0));
            }
            return averageAmount.add(
                    stdDevAmount.multiply(BigDecimal.valueOf(2.0)));
        }

        /**
         * Check if location is in user's common locations
         */
        public boolean isCommonLocation(String location) {
            return commonLocations.containsKey(location);
        }

        /**
         * Get frequency percentage for a location
         */
        public double getLocationFrequencyPercent(String location) {
            Integer count = commonLocations.get(location);
            if (count == null) {
                return 0.0;
            }
            return (count * 100.0) / totalTransactions;
        }
    }
}
