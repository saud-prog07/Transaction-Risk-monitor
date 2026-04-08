package com.example.riskmonitoring.common.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks event-driven pipeline execution for transactions.
 *
 * Provides end-to-end visibility into transaction processing:
 * 1. Producer receives transaction
 * 2. Message published to IBM MQ
 * 3. Risk-engine consumes and processes
 * 4. Alert-service receives and stores
 *
 * In-memory tracking for debugging and monitoring. In production,
 * consider using a dedicated tracing system (e.g., Jaeger, Zipkin).
 */
@Slf4j
public class PipelineEventTracker {

    private static final ConcurrentMap<UUID, TransactionPipelineTrace> traces = new ConcurrentHashMap<>();
    private static final int MAX_TRACES = 10000; // Prevent memory leak from old traces

    /**
     * Starts tracking a transaction through the pipeline.
     *
     * @param transactionId the transaction ID to track
     * @return TransactionPipelineTrace object for logging events
     */
    public static TransactionPipelineTrace startTracking(UUID transactionId) {
        TransactionPipelineTrace trace = TransactionPipelineTrace.builder()
                .transactionId(transactionId)
                .startTime(Instant.now())
                .build();

        traces.put(transactionId, trace);
        log.debug("[TRACE] Started tracking transaction: {}", transactionId);

        // Cleanup old traces to prevent memory leak
        if (traces.size() > MAX_TRACES) {
            cleanupOldTraces();
        }

        return trace;
    }

    /**
     * Gets or creates a trace for a transaction.
     *
     * @param transactionId the transaction ID
     * @return existing or new TransactionPipelineTrace
     */
    public static TransactionPipelineTrace getOrCreateTrace(UUID transactionId) {
        return traces.computeIfAbsent(transactionId, id -> {
            log.debug("[TRACE] Creating late-start trace for transaction: {}", transactionId);
            return TransactionPipelineTrace.builder()
                    .transactionId(transactionId)
                    .startTime(Instant.now())
                    .build();
        });
    }

    /**
     * Gets an existing trace (may not exist if tracking not enabled).
     *
     * @param transactionId the transaction ID
     * @return TransactionPipelineTrace or null if not tracking
     */
    public static TransactionPipelineTrace getTrace(UUID transactionId) {
        return traces.get(transactionId);
    }

    /**
     * Logs a pipeline event for a transaction.
     *
     * @param transactionId the transaction ID
     * @param stage the pipeline stage
     * @param status the event status
     * @param message the event message
     */
    public static void logEvent(UUID transactionId, PipelineStage stage, EventStatus status, String message) {
        TransactionPipelineTrace trace = traces.get(transactionId);
        if (trace == null) {
            trace = getOrCreateTrace(transactionId);
        }

        PipelineEvent event = PipelineEvent.builder()
                .stage(stage)
                .status(status)
                .timestamp(Instant.now())
                .message(message)
                .build();

        trace.events.add(event);

        if (status == EventStatus.ERROR) {
            log.error("[TRACE] Pipeline error at {} - {}", stage, message);
        } else if (status == EventStatus.WARN) {
            log.warn("[TRACE] Pipeline warning at {} - {}", stage, message);
        } else {
            log.debug("[TRACE] Pipeline event at {} - {}", stage, message);
        }
    }

    /**
     * Completes tracking for a transaction and returns summary.
     *
     * @param transactionId the transaction ID
     * @return TransactionPipelineTrace with all events
     */
    public static TransactionPipelineTrace completeTracking(UUID transactionId) {
        TransactionPipelineTrace trace = traces.get(transactionId);
        if (trace != null) {
            trace.endTime = Instant.now();
            trace.durationMs = trace.endTime.toEpochMilli() - trace.startTime.toEpochMilli();
            log.info("[TRACE] Completed tracking transaction {} - Duration: {}ms, Events: {}",
                    transactionId, trace.durationMs, trace.events.size());
        }
        return trace;
    }

    /**
     * Gets summary statistics of all tracked transactions.
     *
     * @return PipelineStatistics with aggregated data
     */
    public static PipelineStatistics getStatistics() {
        PipelineStatistics stats = new PipelineStatistics();
        stats.totalTrackedTransactions = traces.size();
        stats.completedTransactions = (int) traces.values().stream()
                .filter(t -> t.endTime != null)
                .count();

        traces.values().stream()
                .filter(t -> t.endTime != null)
                .forEach(t -> {
                    stats.totalDurationMs += t.durationMs;
                    stats.averageDurationMs = stats.totalDurationMs / Math.max(stats.completedTransactions, 1);
                    // Count errors
                    long errors = t.events.stream()
                            .filter(e -> e.status == EventStatus.ERROR)
                            .count();
                    stats.totalErrors += errors;
                });

        return stats;
    }

    /**
     * Clears all tracking data. Use with caution in production!
     */
    public static void clearAll() {
        int size = traces.size();
        traces.clear();
        log.warn("[TRACE] Cleared all tracking data - Cleared {} traces", size);
    }

    /**
     * Removes old traces to prevent memory leak.
     */
    private static void cleanupOldTraces() {
        int beforeSize = traces.size();
        traces.values().stream()
                .filter(t -> t.endTime != null)
                .filter(t -> {
                    long ageMinutes = (System.currentTimeMillis() - t.endTime.toEpochMilli()) / 60000;
                    return ageMinutes > 60; // Remove completed traces older than 1 hour
                })
                .map(t -> t.transactionId)
                .forEach(traces::remove);
        log.debug("[TRACE] Cleanup completed - Removed {} old traces", beforeSize - traces.size());
    }

    /**
     * Pipeline stages for tracking.
     */
    public enum PipelineStage {
        PRODUCER_RECEIVED,
        PRODUCER_VALIDATED,
        PRODUCER_PUBLISHING,
        PRODUCER_PUBLISHED,
        MQ_QUEUED,
        RISK_ENGINE_RECEIVED,
        RISK_ENGINE_ANALYZING,
        RISK_ENGINE_ANALYZED,
        RISK_ENGINE_ALERTING,
        ALERT_SERVICE_RECEIVED,
        ALERT_SERVICE_STORED,
        PIPELINE_COMPLETE
    }

    /**
     * Event status indicators.
     */
    public enum EventStatus {
        INFO, WARN, ERROR
    }

    /**
     * Single pipeline event.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PipelineEvent {
        private PipelineStage stage;
        private EventStatus status;
        private Instant timestamp;
        private String message;
    }

    /**
     * Complete transaction trace through pipeline.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionPipelineTrace {
        private UUID transactionId;
        private Instant startTime;
        private Instant endTime;
        private long durationMs;
        private List<PipelineEvent> events = new ArrayList<>();

        public String getSummary() {
            return String.format(
                    "TransactionId=%s, Duration=%dms, Events=%d, Errors=%d",
                    transactionId, durationMs, events.size(),
                    events.stream().filter(e -> e.status == EventStatus.ERROR).count()
            );
        }
    }

    /**
     * Pipeline statistics across all traced transactions.
     */
    @Data
    public static class PipelineStatistics {
        public int totalTrackedTransactions;
        public int completedTransactions;
        public long totalDurationMs;
        public long averageDurationMs;
        public long totalErrors;

        @Override
        public String toString() {
            return String.format(
                    "PipelineStatistics{tracked=%d, completed=%d, avgDuration=%dms, errors=%d}",
                    totalTrackedTransactions, completedTransactions, averageDurationMs, totalErrors
            );
        }
    }
}
