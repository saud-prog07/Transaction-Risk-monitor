package com.example.riskmonitoring.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized structured logging utility for consistent log formatting across all services.
 * Provides methods for logging at different stages with contextual information.
 * 
 * Usage:
 * StructuredLogger logger = StructuredLogger.getLogger(MyClass.class);
 * logger.logTransactionReceived(transactionId, userId, amount);
 * logger.logProcessingComplete(transactionId, "RISK_ANALYZED", riskLevel);
 */
public class StructuredLogger {
    
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final String SERVICE_NAME_KEY = "serviceName";
    private static final String STAGE_KEY = "stage";
    private static final String STATUS_KEY = "status";
    
    private final Logger logger;
    private final String loggerName;
    
    private StructuredLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.loggerName = clazz.getSimpleName();
    }
    
    public static StructuredLogger getLogger(Class<?> clazz) {
        return new StructuredLogger(clazz);
    }
    
    /**
     * Set correlation ID for request tracing
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }
    
    /**
     * Set transaction ID for transaction tracking
     */
    public void setTransactionId(String transactionId) {
        MDC.put(TRANSACTION_ID_KEY, transactionId);
    }
    
    /**
     * Generate and set new correlation ID
     */
    public String generateCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        setCorrelationId(correlationId);
        return correlationId;
    }
    
    /**
     * Clear MDC context
     */
    public void clearContext() {
        MDC.clear();
    }
    
    // Stage-specific logging methods
    
    public void logTransactionReceived(String transactionId, String userId, double amount) {
        setTransactionId(transactionId);
        Map<String, Object> context = new HashMap<>();
        context.put("event", "TRANSACTION_RECEIVED");
        context.put("userId", userId);
        context.put("amount", amount);
        info("Transaction received from producer", context);
    }
    
    public void logValidationSuccess(String transactionId) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "VALIDATION_SUCCESS");
        context.put("status", "VALID");
        info("Transaction validation passed", context);
    }
    
    public void logValidationFailure(String transactionId, String errorMessage) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "VALIDATION_FAILURE");
        context.put("status", "INVALID");
        context.put("error", errorMessage);
        warn("Transaction validation failed", context);
    }
    
    public void logMessagePublished(String transactionId, String destination) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "MESSAGE_PUBLISHED");
        context.put("destination", destination);
        context.put("status", "QUEUED");
        info("Message published to queue", context);
    }
    
    public void logMessageConsumed(String transactionId, String source) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "MESSAGE_CONSUMED");
        context.put("source", source);
        context.put("status", "PROCESSING");
        info("Message consumed from queue", context);
    }
    
    public void logRiskAnalysisStarted(String transactionId) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "ANALYSIS_STARTED");
        context.put("stage", "RISK_ANALYSIS");
        info("Starting risk analysis", context);
    }
    
    public void logRiskAnalysisComplete(String transactionId, String riskLevel, double score) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "ANALYSIS_COMPLETE");
        context.put("stage", "RISK_ANALYSIS");
        context.put("riskLevel", riskLevel);
        context.put("score", score);
        info("Risk analysis completed", context);
    }
    
    public void logAlertGenerated(String transactionId, String riskLevel) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "ALERT_GENERATED");
        context.put("riskLevel", riskLevel);
        context.put("status", "CREATED");
        info("Alert generated for high-risk transaction", context);
    }
    
    public void logAlertStored(String transactionId, String alertId) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "ALERT_STORED");
        context.put("alertId", alertId);
        context.put("status", "PERSISTED");
        info("Alert stored in database", context);
    }
    
    public void logProcessingComplete(String transactionId, long durationMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "PROCESSING_COMPLETE");
        context.put("durationMs", durationMs);
        context.put("status", "SUCCESS");
        info("Transaction processing completed", context);
    }
    
    public void logRetryAttempt(String transactionId, int attempt, String reason) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "RETRY_ATTEMPT");
        context.put("attempt", attempt);
        context.put("reason", reason);
        context.put("status", "RETRYING");
        warn("Retrying failed operation", context);
    }
    
    public void logRetryExhausted(String transactionId, String reason) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "RETRY_EXHAUSTED");
        context.put("reason", reason);
        context.put("status", "FAILED");
        error("Retry attempts exhausted", context);
    }
    
    public void logDuplicateDetected(String transactionId, String existingId) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "DUPLICATE_DETECTED");
        context.put("existingId", existingId);
        context.put("status", "SKIPPED");
        warn("Duplicate transaction detected, skipping processing", context);
    }
    
    public void logRateLimitExceeded(String userId, long currentRate) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "RATE_LIMIT_EXCEEDED");
        context.put("userId", userId);
        context.put("currentRate", currentRate);
        context.put("status", "REJECTED");
        warn("Rate limit exceeded for user", context);
    }
    
    // Core logging methods with context
    
    public void info(String message, Map<String, Object> context) {
        String jsonContext = mapToJsonString(context);
        logger.info("{} | {}", message, jsonContext);
    }
    
    public void warn(String message, Map<String, Object> context) {
        String jsonContext = mapToJsonString(context);
        logger.warn("{} | {}", message, jsonContext);
    }
    
    public void error(String message, Map<String, Object> context) {
        String jsonContext = mapToJsonString(context);
        logger.error("{} | {}", message, jsonContext);
    }
    
    public void error(String message, Map<String, Object> context, Throwable throwable) {
        String jsonContext = mapToJsonString(context);
        logger.error("{} | {}", message, jsonContext, throwable);
    }
    
    public void debug(String message, Map<String, Object> context) {
        String jsonContext = mapToJsonString(context);
        logger.debug("{} | {}", message, jsonContext);
    }
    
    // Utility methods
    
    private String mapToJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        map.forEach((key, value) -> {
            sb.append("\"").append(key).append("\":\"").append(value).append("\",");
        });
        sb.setLength(sb.length() - 1); // Remove last comma
        sb.append("}");
        return sb.toString();
    }
}
