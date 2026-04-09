package com.example.riskmonitoring.alertservice.security;

import com.example.riskmonitoring.alertservice.domain.FlaggedTransaction;
import com.example.riskmonitoring.alertservice.domain.User;
import com.example.riskmonitoring.alertservice.exception.ForbiddenException;
import com.example.riskmonitoring.alertservice.exception.ResourceNotFoundException;
import com.example.riskmonitoring.alertservice.repository.FlaggedTransactionRepository;
import com.example.riskmonitoring.alertservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Authorization Service
 * Prevents IDOR (Insecure Direct Object Reference) vulnerabilities by verifying
 * that the authenticated user owns the resource they're trying to access.
 * 
 * Security Features:
 * - Verifies user ownership before allowing resource access
 * - Differentiates between user roles (USER, ADMIN, ANALYST)
 * - Admin/Analyst users have broader access for investigation purposes
 * - Audit logs all authorization failures
 * - Returns generic 403 Forbidden for security (no resource details)
 */
@Slf4j
@Service
public class AuthorizationService {

    private final FlaggedTransactionRepository flaggedTransactionRepository;
    private final UserRepository userRepository;

    public AuthorizationService(FlaggedTransactionRepository flaggedTransactionRepository,
                                UserRepository userRepository) {
        this.flaggedTransactionRepository = flaggedTransactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get the currently authenticated user
     * @return the authenticated user from SecurityContext
     * @throws IllegalStateException if no user is authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }

    /**
     * Get current user ID
     * @return the authenticated user's ID
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Get current username
     * @return the authenticated user's username
     */
    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    /**
     * Check if user has role
     * @param role the role to check (e.g., "ADMIN", "ANALYST", "USER")
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return getCurrentUser().getRoles().contains(role);
    }

    /**
     * Verify that current user has access to alert
     * - Regular USER: only their own data
     * - ANALYST/ADMIN: access to all alerts for analysis
     * 
     * @param alertId the alert ID
     * @return the FlaggedTransaction if authorized
     * @throws ForbiddenException if user not authorized
     * @throws ResourceNotFoundException if alert not found
     */
    public FlaggedTransaction verifyAlertAccess(Long alertId) {
        FlaggedTransaction alert = flaggedTransactionRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.warn("Access attempt to non-existent alert: {}", alertId);
                    return new ResourceNotFoundException("Alert not found");
                });

        // Check authorization
        if (isAuthorizedForAlert(alert)) {
            return alert;
        }

        // Log unauthorized access attempt
        String username = getCurrentUsername();
        log.warn("Unauthorized access attempt to alert {} by user {}", alertId, username);
        
        throw new ForbiddenException("You do not have permission to access this resource");
    }

    /**
     * Verify that current user can modify alert
     * - Regular USER: only their own alerts
     * - ANALYST: can modify for investigation
     * - ADMIN: can modify any alert
     * 
     * @param alertId the alert ID
     * @return the FlaggedTransaction if authorized
     * @throws ForbiddenException if user not authorized
     * @throws ResourceNotFoundException if alert not found
     */
    public FlaggedTransaction verifyAlertModification(Long alertId) {
        FlaggedTransaction alert = flaggedTransactionRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.warn("Modification attempt to non-existent alert: {}", alertId);
                    return new ResourceNotFoundException("Alert not found");
                });

        // Only ADMIN and ANALYST can modify alerts via API
        if (!hasRole("ADMIN") && !hasRole("ANALYST")) {
            String username = getCurrentUsername();
            log.warn("Unauthorized modification attempt by {} on alert {}", username, alertId);
            throw new ForbiddenException("You do not have permission to modify this resource");
        }

        return alert;
    }

    /**
     * Verify that current user can access audit logs for alert
     * @param alertId the alert ID
     * @return true if authorized
     */
    public boolean isAuthorizedForAlertAudit(Long alertId) {
        // ADMIN and ANALYST can access all audit logs
        if (hasRole("ADMIN") || hasRole("ANALYST")) {
            return true;
        }

        // Regular user can only access their own created alerts' audit logs
        FlaggedTransaction alert = flaggedTransactionRepository.findById(alertId).orElse(null);
        if (alert == null) {
            return false;
        }

        return isAlertOwner(alert);
    }

    /**
     * Verify that current user can access another user's audit logs
     * - Only ADMIN can access other users' audit logs
     * - Users can access their own audit logs
     * 
     * @param userId the user ID to access audit logs for
     * @return true if authorized
     */
    public boolean isAuthorizedForUserAudit(Long userId) {
        User currentUser = getCurrentUser();
        
        // Users can access their own audit logs
        if (currentUser.getId().equals(userId)) {
            return true;
        }

        // Only ADMIN can access other users' audit logs
        if (hasRole("ADMIN")) {
            return true;
        }

        String username = getCurrentUsername();
        log.warn("Unauthorized audit access attempt by {} for user {}", username, userId);
        return false;
    }

    /**
     * Check if current user is the owner/creator of alert
     * @param alert the alert to check
     * @return true if current user is owner
     */
    private boolean isAlertOwner(FlaggedTransaction alert) {
        if (alert == null || alert.getCreatedBy() == null) {
            return false;
        }
        
        String currentUsername = getCurrentUsername();
        return alert.getCreatedBy().equals(currentUsername);
    }

    /**
     * Check if user is authorized to access alert
     * - ADMIN: access all alerts
     * - ANALYST: access all alerts for investigation
     * - USER: access only owned alerts
     * 
     * @param alert the alert to check
     * @return true if authorized
     */
    private boolean isAuthorizedForAlert(FlaggedTransaction alert) {
        User currentUser = getCurrentUser();

        // ADMIN and ANALYST have full access
        if (currentUser.getRoles().contains("ADMIN") || currentUser.getRoles().contains("ANALYST")) {
            return true;
        }

        // Regular users can access their own alerts
        return isAlertOwner(alert);
    }

    /**
     * Audit authorization failure
     * @param resource the resource type
     * @param resourceId the resource ID
     * @param reason the reason for failure
     */
    private void auditAuthorizationFailure(String resource, String resourceId, String reason) {
        User user = getCurrentUser();
        log.warn("Authorization failure: User={}, Resource={}, ResourceId={}, Reason={}", 
                user.getUsername(), resource, resourceId, reason);
    }
}
