package com.example.riskmonitoring.common.monitoring;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

/**
 * Monitors transaction processing metrics
 * Tracks processed transactions, errors, and performance metrics
 */
public class TransactionMonitor {
    
    private static final TransactionMonitor INSTANCE = new TransactionMonitor();
    
    // Metrics
    private final AtomicLong totalTransactionsReceived = new AtomicLong(0);
    private final AtomicLong totalTransactionsProcessed = new AtomicLong(0);
    private final AtomicLong totalTransactionsFailed = new AtomicLong(0);
    private final AtomicLong totalDuplicatesDetected = new AtomicLong(0);
    private final AtomicLong totalAlertsGenerated = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    // Error tracking
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    // Risk level distribution
    private final ConcurrentHashMap<String, AtomicLong> riskLevelDistribution = new ConcurrentHashMap<>();
    
    // Timestamps
    private volatile Instant startTime;
    private volatile Instant lastResetTime;
    
    private TransactionMonitor() {
        this.startTime = Instant.now();
        this.lastResetTime = Instant.now();
        
        // Initialize risk level counters
        riskLevelDistribution.put("HIGH", new AtomicLong(0));
        riskLevelDistribution.put("MEDIUM", new AtomicLong(0));
        riskLevelDistribution.put("LOW", new AtomicLong(0));
    }
    
    public static TransactionMonitor getInstance() {
        return INSTANCE;
    }
    
    // Recording methods
    
    public void recordTransactionReceived() {
        totalTransactionsReceived.incrementAndGet();
    }
    
    public void recordTransactionProcessed(long durationMs) {
        totalTransactionsProcessed.incrementAndGet();
        totalProcessingTime.addAndGet(durationMs);
    }
    
    public void recordTransactionFailed(String errorType) {
        totalTransactionsFailed.incrementAndGet();
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public void recordDuplicateDetected() {
        totalDuplicatesDetected.incrementAndGet();
    }
    
    public void recordAlertGenerated(String riskLevel) {
        totalAlertsGenerated.incrementAndGet();
        if (riskLevelDistribution.containsKey(riskLevel)) {
            riskLevelDistribution.get(riskLevel).incrementAndGet();
        }
    }
    
    // Query methods
    
    public long getTotalReceived() {
        return totalTransactionsReceived.get();
    }
    
    public long getTotalProcessed() {
        return totalTransactionsProcessed.get();
    }
    
    public long getTotalFailed() {
        return totalTransactionsFailed.get();
    }
    
    public long getTotalDuplicates() {
        return totalDuplicatesDetected.get();
    }
    
    public long getTotalAlerts() {
        return totalAlertsGenerated.get();
    }
    
    public double getAverageProcessingTimeMs() {
        long processed = totalTransactionsProcessed.get();
        if (processed == 0) return 0;
        return (double) totalProcessingTime.get() / processed;
    }
    
    public double getProcessingRate() {
        long uptime = System.currentTimeMillis() - startTime.toEpochMilli();
        if (uptime == 0) return 0;
        return (totalTransactionsProcessed.get() * 1000.0) / uptime;
    }
    
    public double getSuccessRate() {
        long total = totalTransactionsReceived.get();
        if (total == 0) return 0;
        return (totalTransactionsProcessed.get() * 100.0) / total;
    }
    
    public double getErrorRate() {
        long total = totalTransactionsReceived.get();
        if (total == 0) return 0;
        return (totalTransactionsFailed.get() * 100.0) / total;
    }
    
    /**
     * Get error breakdown by type
     */
    public ConcurrentHashMap<String, Long> getErrorBreakdown() {
        ConcurrentHashMap<String, Long> breakdown = new ConcurrentHashMap<>();
        errorCounts.forEach((type, count) -> breakdown.put(type, count.get()));
        return breakdown;
    }
    
    /**
     * Get risk level distribution
     */
    public ConcurrentHashMap<String, Long> getRiskLevelDistribution() {
        ConcurrentHashMap<String, Long> distribution = new ConcurrentHashMap<>();
        riskLevelDistribution.forEach((level, count) -> distribution.put(level, count.get()));
        return distribution;
    }
    
    /**
     * Get comprehensive metrics snapshot
     */
    public MetricsSnapshot getMetricsSnapshot() {
        return new MetricsSnapshot(
                totalTransactionsReceived.get(),
                totalTransactionsProcessed.get(),
                totalTransactionsFailed.get(),
                totalDuplicatesDetected.get(),
                totalAlertsGenerated.get(),
                getAverageProcessingTimeMs(),
                getProcessingRate(),
                getSuccessRate(),
                getErrorRate(),
                startTime,
                lastResetTime,
                errorCounts,
                riskLevelDistribution
        );
    }
    
    /**
     * Reset all metrics (be careful with this in production)
     */
    public void resetMetrics() {
        totalTransactionsReceived.set(0);
        totalTransactionsProcessed.set(0);
        totalTransactionsFailed.set(0);
        totalDuplicatesDetected.set(0);
        totalAlertsGenerated.set(0);
        totalProcessingTime.set(0);
        errorCounts.clear();
        lastResetTime = Instant.now();
    }
    
    /**
     * Metrics snapshot - immutable snapshot of metrics at a point in time
     */
    public static class MetricsSnapshot {
        public final long totalReceived;
        public final long totalProcessed;
        public final long totalFailed;
        public final long totalDuplicates;
        public final long totalAlerts;
        public final double avgProcessingTimeMs;
        public final double processingRatePerSecond;
        public final double successRatePercent;
        public final double errorRatePercent;
        public final Instant startTime;
        public final Instant lastResetTime;
        public final ConcurrentHashMap<String, AtomicLong> errorBreakdown;
        public final ConcurrentHashMap<String, AtomicLong> riskDistribution;
        
        public MetricsSnapshot(long totalReceived, long totalProcessed, long totalFailed,
                             long totalDuplicates, long totalAlerts, double avgProcessingTimeMs,
                             double processingRatePerSecond, double successRatePercent,
                             double errorRatePercent, Instant startTime, Instant lastResetTime,
                             ConcurrentHashMap<String, AtomicLong> errorBreakdown,
                             ConcurrentHashMap<String, AtomicLong> riskDistribution) {
            this.totalReceived = totalReceived;
            this.totalProcessed = totalProcessed;
            this.totalFailed = totalFailed;
            this.totalDuplicates = totalDuplicates;
            this.totalAlerts = totalAlerts;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
            this.processingRatePerSecond = processingRatePerSecond;
            this.successRatePercent = successRatePercent;
            this.errorRatePercent = errorRatePercent;
            this.startTime = startTime;
            this.lastResetTime = lastResetTime;
            this.errorBreakdown = errorBreakdown;
            this.riskDistribution = riskDistribution;
        }
        
        @Override
        public String toString() {
            return "MetricsSnapshot{" +
                    "totalReceived=" + totalReceived +
                    ", totalProcessed=" + totalProcessed +
                    ", totalFailed=" + totalFailed +
                    ", totalDuplicates=" + totalDuplicates +
                    ", totalAlerts=" + totalAlerts +
                    ", avgProcessingTimeMs=" + String.format("%.2f", avgProcessingTimeMs) +
                    ", processingRatePerSecond=" + String.format("%.2f", processingRatePerSecond) +
                    ", successRatePercent=" + String.format("%.2f", successRatePercent) + "%" +
                    ", errorRatePercent=" + String.format("%.2f", errorRatePercent) + "%" +
                    '}';
        }
    }
}
