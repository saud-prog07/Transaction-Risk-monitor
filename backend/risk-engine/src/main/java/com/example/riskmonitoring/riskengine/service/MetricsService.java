package com.example.riskmonitoring.riskengine.service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

/**
 * Thread-safe metrics collection service for transaction monitoring.
 */
@Service
public class MetricsService {

    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeMillis = new AtomicLong(0);
    private final AtomicReference<Instant> startTime = new AtomicReference<>(Instant.now());

    public void incrementTotalProcessed() {
        totalProcessed.incrementAndGet();
    }

    public void incrementFlaggedCount() {
        flaggedCount.incrementAndGet();
    }

    public void incrementFailedCount() {
        failedCount.incrementAndGet();
    }

    public void addProcessingTimeMillis(long millis) {
        totalProcessingTimeMillis.addAndGet(millis);
    }

    public long getTotalProcessed() {
        return totalProcessed.get();
    }

    public long getFlaggedCount() {
        return flaggedCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }

    public double getAvgProcessingTimeMillis() {
        long total = totalProcessed.get();
        return total == 0 ? 0.0 : (double) totalProcessingTimeMillis.get() / total;
    }

    public double getThroughputPerSecond() {
        long seconds = java.time.Duration.between(startTime.get(), Instant.now()).getSeconds();
        return seconds == 0 ? 0.0 : (double) totalProcessed.get() / seconds;
    }

    public void resetStartTime() {
        startTime.set(Instant.now());
    }
}