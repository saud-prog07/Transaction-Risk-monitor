package com.example.riskmonitoring.alertservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Unusual Traffic Detection Service
 * Detects and logs suspicious traffic patterns and attack indicators
 */
@Slf4j
@Service
public class UnusualTrafficDetectionService {

    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Log unusual traffic pattern detected
     */
    public void logUnusualTraffic(String ipAddress, String pattern, String details) {
        String message = String.format(
            "[UNUSUAL_TRAFFIC] IP: %s | Pattern: %s | Details: %s",
            ipAddress, pattern, details
        );
        log.warn(message);
    }

    /**
     * Detect and log high request rate per IP
     */
    public void checkHighRequestRate(String ipAddress, int requestsPerMinute, int threshold) {
        if (requestsPerMinute > threshold) {
            String message = String.format(
                "[HIGH_REQUEST_RATE] IP: %s | Requests: %d/min | Threshold: %d/min",
                ipAddress, requestsPerMinute, threshold
            );
            log.error(message);
        }
    }

    /**
     * Log geographic anomaly (impossible travel)
     */
    public void logGeographicAnomaly(String ipAddress, String previousLocation, 
                                     String currentLocation, long timeGapMinutes) {
        if (timeGapMinutes < 5) {
            String message = String.format(
                "[IMPOSSIBLE_TRAVEL] IP: %s | From: %s | To: %s | TimeGap: %d min",
                ipAddress, previousLocation, currentLocation, timeGapMinutes
            );
            log.error(message);
        }
    }

    /**
     * Log request pattern change for an IP
     */
    public void logRequestPatternChange(String ipAddress, String previousPattern, 
                                       String newPattern) {
        String message = String.format(
            "[PATTERN_CHANGE] IP: %s | Previous: %s | Current: %s",
            ipAddress, previousPattern, newPattern
        );
        log.warn(message);
    }

    /**
     * Detect and log endpoint scanning activity
     */
    public void logEndpointScanning(String ipAddress, int uniqueEndpointsHit) {
        if (uniqueEndpointsHit > 20) {
            String message = String.format(
                "[ENDPOINT_SCANNING] IP: %s | Unique Endpoints: %d",
                ipAddress, uniqueEndpointsHit
            );
            log.error(message);
        }
    }

    /**
     * Detect and log credential stuffing attack
     */
    public void logCredentialStuffing(String ipAddress, int failedAttempts, int uniqueUsernames) {
        if (failedAttempts >= 10 && uniqueUsernames >= 5) {
            String message = String.format(
                "[CREDENTIAL_STUFFING] IP: %s | Failed Attempts: %d | Unique Usernames: %d",
                ipAddress, failedAttempts, uniqueUsernames
            );
            log.error(message);
        }
    }

    /**
     * Detect and log SQL injection attempt
     */
    public void logSqlInjectionAttempt(String ipAddress, String parameter, String payload) {
        String message = String.format(
            "[SQL_INJECTION_ATTEMPT] IP: %s | Parameter: %s | Payload: %s",
            ipAddress, parameter, obscurePayload(payload)
        );
        log.error(message);
    }

    /**
     * Detect and log Cross-Site Scripting (XSS) attempt
     */
    public void logXssAttempt(String ipAddress, String parameter, String payload) {
        String message = String.format(
            "[XSS_ATTEMPT] IP: %s | Parameter: %s | Payload: %s",
            ipAddress, parameter, obscurePayload(payload)
        );
        log.error(message);
    }

    /**
     * Detect and log Path Traversal attempt
     */
    public void logPathTraversalAttempt(String ipAddress, String requestPath) {
        String message = String.format(
            "[PATH_TRAVERSAL_ATTEMPT] IP: %s | Path: %s",
            ipAddress, requestPath
        );
        log.error(message);
    }

    /**
     * Detect and log bot traffic
     */
    public void logBotTraffic(String ipAddress, String botIdentifier, String userAgent) {
        String message = String.format(
            "[BOT_TRAFFIC] IP: %s | Bot: %s | UserAgent: %s",
            ipAddress, botIdentifier, userAgent
        );
        log.warn(message);
    }

    /**
     * Detect and log suspicious header combination
     */
    public void logSuspiciousHeaders(String ipAddress, String headerIssue, String details) {
        String message = String.format(
            "[SUSPICIOUS_HEADERS] IP: %s | Issue: %s | Details: %s",
            ipAddress, headerIssue, details
        );
        log.warn(message);
    }

    /**
     * Detect and log large file upload attempt
     */
    public void logLargeFileUpload(String ipAddress, long fileSizeBytes, long maxAllowedBytes) {
        if (fileSizeBytes > maxAllowedBytes) {
            String message = String.format(
                "[LARGE_FILE_UPLOAD] IP: %s | Size: %d bytes | Max: %d bytes",
                ipAddress, fileSizeBytes, maxAllowedBytes
            );
            log.warn(message);
        }
    }

    /**
     * Detect and log DDoS-like behavior
     */
    public void logDdosBehavior(String ipAddress, int requestCount, long timeWindowSeconds) {
        String message = String.format(
            "[DDOS_BEHAVIOR] IP: %s | Requests: %d | TimeWindow: %d sec",
            ipAddress, requestCount, timeWindowSeconds
        );
        log.error(message);
    }

    /**
     * Detect and log account enumeration attempt
     */
    public void logAccountEnumeration(String ipAddress, int totalAttempts, String usernames) {
        String message = String.format(
            "[ACCOUNT_ENUMERATION] IP: %s | Attempts: %d | Usernames: %s",
            ipAddress, totalAttempts, obscurePayload(usernames)
        );
        log.warn(message);
    }

    /**
     * Detect and log API abuse (excessive requests to specific endpoint)
     */
    public void logApiAbuse(String ipAddress, String endpoint, int requestCount, int threshold) {
        if (requestCount > threshold) {
            String message = String.format(
                "[API_ABUSE] IP: %s | Endpoint: %s | Requests: %d | Threshold: %d",
                ipAddress, endpoint, requestCount, threshold
            );
            log.error(message);
        }
    }

    /**
     * Detect and log suspicious HTTP methods
     */
    public void logSuspiciousHttpMethod(String ipAddress, String method, String endpoint) {
        String message = String.format(
            "[SUSPICIOUS_HTTP_METHOD] IP: %s | Method: %s | Endpoint: %s",
            ipAddress, method, endpoint
        );
        log.warn(message);
    }

    /**
     * Detect and log missing required headers
     */
    public void logMissingHeaders(String ipAddress, String missingHeaders) {
        String message = String.format(
            "[MISSING_HEADERS] IP: %s | Missing: %s",
            ipAddress, missingHeaders
        );
        log.warn(message);
    }

    /**
     * Log rate limit bypass attempt
     */
    public void logRateLimitBypassAttempt(String ipAddress, String bypassMethod, String details) {
        String message = String.format(
            "[RATE_LIMIT_BYPASS_ATTEMPT] IP: %s | Method: %s | Details: %s",
            ipAddress, bypassMethod, details
        );
        log.error(message);
    }

    /**
     * Log suspicious user behavior (login from multiple locations rapidly)
     */
    public void logSuspiciousUserBehavior(String username, String behavior, String details) {
        String message = String.format(
            "[SUSPICIOUS_USER_BEHAVIOR] User: %s | Behavior: %s | Details: %s",
            username, behavior, details
        );
        log.warn(message);
    }

    /**
     * Log concurrent session detection
     */
    public void logConcurrentSessions(String username, int sessionCount, String ipAddresses) {
        if (sessionCount > 1) {
            String message = String.format(
                "[CONCURRENT_SESSIONS] User: %s | Sessions: %d | IPs: %s",
                username, sessionCount, ipAddresses
            );
            log.info(message);
        }
    }

    /**
     * Log data exfiltration attempt (unusual data size in response)
     */
    public void logDataExfiltrationAttempt(String ipAddress, String endpoint, long dataSizeBytes) {
        String message = String.format(
            "[DATA_EXFILTRATION_ATTEMPT] IP: %s | Endpoint: %s | DataSize: %d bytes",
            ipAddress, endpoint, dataSizeBytes
        );
        log.error(message);
    }

    /**
     * Obscure sensitive payload data in logs (prevent leaking data)
     */
    private String obscurePayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return "EMPTY";
        }
        if (payload.length() > 50) {
            return "[TRUNCATED_" + payload.substring(0, 10) + "..._" + 
                   payload.substring(payload.length() - 10) + "]";
        }
        return "[OBSCURED_" + payload.length() + "_CHARS]";
    }
}
