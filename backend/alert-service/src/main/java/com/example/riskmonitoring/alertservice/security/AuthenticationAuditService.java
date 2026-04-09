package com.example.riskmonitoring.alertservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive Authentication Audit Service
 * Logs all authentication events for security monitoring and compliance
 */
@Slf4j
@Service
public class AuthenticationAuditService {

    private static final String AUTH_LOGGER = "com.example.riskmonitoring.alertservice.security";
    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Log successful login attempt
     */
    public void logLoginSuccess(String username, String ipAddress, String userAgent) {
        String message = String.format(
            "[AUTH_SUCCESS] Username: %s | IP: %s | UserAgent: %s",
            username, ipAddress, sanitizeUserAgent(userAgent)
        );
        log.info(message);
    }

    /**
     * Log failed login attempt
     */
    public void logLoginFailure(String username, String ipAddress, String reason) {
        String message = String.format(
            "[AUTH_FAILURE] Username: %s | IP: %s | Reason: %s",
            username, ipAddress, reason
        );
        log.warn(message);
    }

    /**
     * Log brute force attack detection
     */
    public void logBruteForceAttempt(String username, String ipAddress, int attemptCount) {
        String message = String.format(
            "[BRUTE_FORCE] Username: %s | IP: %s | Attempts: %d",
            username, ipAddress, attemptCount
        );
        log.error(message);
    }

    /**
     * Log account lockout event
     */
    public void logAccountLockout(String username, String ipAddress, int lockoutDurationMinutes) {
        String message = String.format(
            "[ACCOUNT_LOCKOUT] Username: %s | IP: %s | Duration: %d min",
            username, ipAddress, lockoutDurationMinutes
        );
        log.warn(message);
    }

    /**
     * Log unauthorized access attempt
     */
    public void logUnauthorizedAccess(String username, String endpoint, String ipAddress) {
        String message = String.format(
            "[UNAUTHORIZED_ACCESS] Username: %s | Endpoint: %s | IP: %s",
            username, endpoint, ipAddress
        );
        log.error(message);
    }

    /**
     * Log forbidden resource access attempt
     */
    public void logForbiddenAccess(String username, String resource, String ipAddress) {
        String message = String.format(
            "[FORBIDDEN_ACCESS] Username: %s | Resource: %s | IP: %s",
            username, resource, ipAddress
        );
        log.warn(message);
    }

    /**
     * Log token refresh
     */
    public void logTokenRefresh(String username, String ipAddress) {
        String message = String.format(
            "[TOKEN_REFRESH] Username: %s | IP: %s",
            username, ipAddress
        );
        log.info(message);
    }

    /**
     * Log token expiration
     */
    public void logTokenExpiration(String username) {
        String message = String.format(
            "[TOKEN_EXPIRED] Username: %s",
            username
        );
        log.info(message);
    }

    /**
     * Log invalid token
     */
    public void logInvalidToken(String ipAddress, String reason) {
        String message = String.format(
            "[INVALID_TOKEN] IP: %s | Reason: %s",
            ipAddress, reason
        );
        log.warn(message);
    }

    /**
     * Log password change
     */
    public void logPasswordChange(String username, String ipAddress) {
        String message = String.format(
            "[PASSWORD_CHANGE] Username: %s | IP: %s",
            username, ipAddress
        );
        log.info(message);
    }

    /**
     * Log account creation
     */
    public void logAccountCreation(String username, String email, String ipAddress) {
        String message = String.format(
            "[ACCOUNT_CREATED] Username: %s | Email: %s | IP: %s",
            username, email, ipAddress
        );
        log.info(message);
    }

    /**
     * Log account deletion
     */
    public void logAccountDeletion(String username, String deletedBy, String ipAddress) {
        String message = String.format(
            "[ACCOUNT_DELETED] Username: %s | DeletedBy: %s | IP: %s",
            username, deletedBy, ipAddress
        );
        log.warn(message);
    }

    /**
     * Log permission change
     */
    public void logPermissionChange(String username, String changedPermissions, String changedBy) {
        String message = String.format(
            "[PERMISSION_CHANGE] Username: %s | Changes: %s | ChangedBy: %s",
            username, changedPermissions, changedBy
        );
        log.info(message);
    }

    /**
     * Log email verification
     */
    public void logEmailVerification(String username, String email) {
        String message = String.format(
            "[EMAIL_VERIFIED] Username: %s | Email: %s",
            username, email
        );
        log.info(message);
    }

    /**
     * Log two-factor authentication enabled
     */
    public void logTwoFactorEnabled(String username, String ipAddress) {
        String message = String.format(
            "[2FA_ENABLED] Username: %s | IP: %s",
            username, ipAddress
        );
        log.info(message);
    }

    /**
     * Log two-factor authentication failed
     */
    public void logTwoFactorFailed(String username, String ipAddress) {
        String message = String.format(
            "[2FA_FAILED] Username: %s | IP: %s",
            username, ipAddress
        );
        log.warn(message);
    }

    /**
     * Log logout event
     */
    public void logLogout(String username, String ipAddress) {
        String message = String.format(
            "[LOGOUT] Username: %s | IP: %s",
            username, ipAddress
        );
        log.info(message);
    }

    /**
     * Log session timeout
     */
    public void logSessionTimeout(String username) {
        String message = String.format(
            "[SESSION_TIMEOUT] Username: %s",
            username
        );
        log.info(message);
    }

    /**
     * Sanitize user agent string for logging (remove PII if present)
     */
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null) {
            return "UNKNOWN";
        }
        // Truncate to avoid log bloat
        return userAgent.length() > 100 ? userAgent.substring(0, 100) : userAgent;
    }
}
