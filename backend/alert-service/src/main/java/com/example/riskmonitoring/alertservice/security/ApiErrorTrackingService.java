package com.example.riskmonitoring.alertservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * API Error Tracking Service
 * Logs and tracks API errors for monitoring, debugging, and incident response
 */
@Slf4j
@Service
public class ApiErrorTrackingService {

    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private static final int MAX_STACK_TRACE_LINES = 5;

    /**
     * Log API endpoint error with full context
     */
    public void logApiError(String endpoint, String method, String ipAddress, 
                           int httpStatus, String errorMessage, Throwable exception) {
        String stackTrace = exception != null ? getStackTraceString(exception) : "N/A";
        
        String logMessage = String.format(
            "[API_ERROR] Endpoint: %s | Method: %s | IP: %s | Status: %d | Error: %s | Stack: %s",
            endpoint, method, ipAddress, httpStatus, errorMessage, stackTrace
        );
        
        if (httpStatus >= 500) {
            log.error(logMessage);
        } else if (httpStatus >= 400) {
            log.warn(logMessage);
        } else {
            log.info(logMessage);
        }
    }

    /**
     * Log rate limit exceeded
     */
    public void logRateLimitExceeded(String endpoint, String ipAddress, String identifier) {
        String message = String.format(
            "[RATE_LIMIT_EXCEEDED] Endpoint: %s | IP: %s | Identifier: %s",
            endpoint, ipAddress, identifier
        );
        log.warn(message);
    }

    /**
     * Log validation error for specific field
     */
    public void logValidationError(String endpoint, String field, String errorMessage) {
        String message = String.format(
            "[VALIDATION_ERROR] Endpoint: %s | Field: %s | Error: %s",
            endpoint, field, errorMessage
        );
        log.warn(message);
    }

    /**
     * Log database operation error
     */
    public void logDatabaseError(String operation, String details, Throwable exception) {
        String stackTrace = exception != null ? getStackTraceString(exception) : "N/A";
        
        String message = String.format(
            "[DATABASE_ERROR] Operation: %s | Details: %s | Stack: %s",
            operation, details, stackTrace
        );
        log.error(message);
    }

    /**
     * Log external service error (MQ, API calls, etc.)
     */
    public void logExternalServiceError(String service, String operation, 
                                       String errorMessage, int retryCount) {
        String message = String.format(
            "[EXTERNAL_SERVICE_ERROR] Service: %s | Operation: %s | Error: %s | Retries: %d",
            service, operation, errorMessage, retryCount
        );
        log.error(message);
    }

    /**
     * Log authentication error
     */
    public void logAuthenticationError(String endpoint, String ipAddress, String reason) {
        String message = String.format(
            "[AUTHENTICATION_ERROR] Endpoint: %s | IP: %s | Reason: %s",
            endpoint, ipAddress, reason
        );
        log.warn(message);
    }

    /**
     * Log authorization error (403 Forbidden)
     */
    public void logAuthorizationError(String username, String endpoint, String ipAddress) {
        String message = String.format(
            "[AUTHORIZATION_ERROR] User: %s | Endpoint: %s | IP: %s",
            username, endpoint, ipAddress
        );
        log.warn(message);
    }

    /**
     * Log resource not found error (404)
     */
    public void logResourceNotFound(String endpoint, String ipAddress) {
        String message = String.format(
            "[RESOURCE_NOT_FOUND] Endpoint: %s | IP: %s",
            endpoint, ipAddress
        );
        log.info(message);
    }

    /**
     * Log malformed request (400 Bad Request)
     */
    public void logMalformedRequest(String endpoint, String ipAddress, String details) {
        String message = String.format(
            "[MALFORMED_REQUEST] Endpoint: %s | IP: %s | Details: %s",
            endpoint, ipAddress, details
        );
        log.warn(message);
    }

    /**
     * Log conflict error (409 Conflict)
     */
    public void logConflictError(String endpoint, String ipAddress, String description) {
        String message = String.format(
            "[CONFLICT_ERROR] Endpoint: %s | IP: %s | Description: %s",
            endpoint, ipAddress, description
        );
        log.warn(message);
    }

    /**
     * Log internal server error (500)
     */
    public void logInternalError(String endpoint, String ipAddress, String errorCode) {
        String message = String.format(
            "[INTERNAL_ERROR] Endpoint: %s | IP: %s | ErrorCode: %s",
            endpoint, ipAddress, errorCode
        );
        log.error(message);
    }

    /**
     * Log timeout error
     */
    public void logTimeoutError(String service, long durationMs) {
        String message = String.format(
            "[TIMEOUT_ERROR] Service: %s | Duration: %d ms",
            service, durationMs
        );
        log.error(message);
    }

    /**
     * Log connection pool exhaustion
     */
    public void logConnectionPoolExhausted(String poolName, int activeConnections, int maxSize) {
        String message = String.format(
            "[POOL_EXHAUSTED] Pool: %s | Active: %d | Max: %d",
            poolName, activeConnections, maxSize
        );
        log.error(message);
    }

    /**
     * Log slow query
     */
    public void logSlowQuery(String query, long executionTimeMs, long thresholdMs) {
        String message = String.format(
            "[SLOW_QUERY] Duration: %d ms (threshold: %d ms) | Query: %s",
            executionTimeMs, thresholdMs, truncateQuery(query)
        );
        log.warn(message);
    }

    /**
     * Log memory warning
     */
    public void logMemoryWarning(long usedMemory, long maxMemory) {
        double percentageUsed = (double) usedMemory / maxMemory * 100;
        String message = String.format(
            "[MEMORY_WARNING] Used: %d MB | Max: %d MB | Percentage: %.2f%%",
            usedMemory / 1024 / 1024, maxMemory / 1024 / 1024, percentageUsed
        );
        log.warn(message);
    }

    /**
     * Log deprecated API endpoint usage
     */
    public void logDeprecatedEndpoint(String endpoint, String ipAddress, String replacement) {
        String message = String.format(
            "[DEPRECATED_API] Endpoint: %s | IP: %s | Use instead: %s",
            endpoint, ipAddress, replacement
        );
        log.warn(message);
    }

    /**
     * Log API version mismatch
     */
    public void logVersionMismatch(String endpoint, String clientVersion, String serverVersion) {
        String message = String.format(
            "[VERSION_MISMATCH] Endpoint: %s | Client: %s | Server: %s",
            endpoint, clientVersion, serverVersion
        );
        log.info(message);
    }

    /**
     * Extract stack trace as string (limited to prevent log bloat)
     */
    private String getStackTraceString(Throwable exception) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] elements = exception.getStackTrace();
        int limit = Math.min(MAX_STACK_TRACE_LINES, elements.length);
        
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(elements[i].getClassName())
              .append(":")
              .append(elements[i].getLineNumber());
        }
        
        if (elements.length > MAX_STACK_TRACE_LINES) {
            sb.append(" | ... and ")
              .append(elements.length - MAX_STACK_TRACE_LINES)
              .append(" more");
        }
        
        return sb.toString();
    }

    /**
     * Truncate query for logging (prevent log bloat from large queries)
     */
    private String truncateQuery(String query) {
        if (query == null) return "N/A";
        if (query.length() > 200) {
            return query.substring(0, 200) + "...";
        }
        return query;
    }
}
