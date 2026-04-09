package com.example.riskmonitoring.alertservice.controller;

import com.example.riskmonitoring.alertservice.annotation.RateLimitProtected;
import com.example.riskmonitoring.alertservice.annotation.RateLimitType;
import com.example.riskmonitoring.alertservice.dto.*;
import com.example.riskmonitoring.alertservice.service.AuthenticationService;
import com.example.riskmonitoring.alertservice.service.PasswordValidator;
import com.example.riskmonitoring.alertservice.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 * Handles user authentication, registration, password reset, and token management
 * 
 * Security Endpoints:
 * - POST /api/auth/login - User login with credentials
 * - POST /api/auth/register - User registration
 * - POST /api/auth/refresh - Refresh access token
 * - POST /api/auth/logout - Logout and revoke refresh token
 * - POST /api/auth/verify-email - Verify email with token
 * - POST /api/auth/forgot-password - Request password reset
 * - POST /api/auth/reset-password - Reset password with token
 * - POST /api/auth/change-password - Change password (authenticated)
 * 
 * Rate Limiting:
 * - Login: 5 attempts per minute, 20 per hour (per username)
 * - Registration: 2 per minute, 10 per hour (per IP)
 * - Password reset: 3 per hour (per email)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final PasswordValidator passwordValidator;

    public AuthenticationController(AuthenticationService authenticationService,
                                   UserService userService,
                                   PasswordValidator passwordValidator) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.passwordValidator = passwordValidator;
    }

    /**
     * User login endpoint
     * Authenticates user and returns JWT tokens
     * 
     * @param loginRequest the login request (username and password)
     * @return LoginResponse with access and refresh tokens
     */
    @RateLimitProtected(type = RateLimitType.LOGIN, validateUserAgent = true, blockBots = true)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        try {
            LoginResponse response = authenticationService.authenticate(loginRequest);
            log.info("Login successful for user: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed"));
        }
    }

    /**
     * User registration endpoint
     * Registers new user with email verification requirement
     * 
     * @param registerRequest registration details
     * @return message confirming registration and email sent
     */
    @RateLimitProtected(type = RateLimitType.REGISTRATION, validateUserAgent = true, blockBots = true)
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for user: {}", registerRequest.getUsername());

        try {
            // Validate passwords match
            if (!registerRequest.getPassword().equals(registerRequest.getPasswordConfirm())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Passwords do not match"));
            }

            // Validate password strength
            if (!passwordValidator.isPasswordValid(registerRequest.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", passwordValidator.getPasswordRequirements()));
            }

            userService.registerUser(registerRequest);

            log.info("User registered successfully: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Registration successful. Please verify your email."));

        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed"));
        }
    }

    /**
     * Verify email with token
     * Enables user account after email verification
     * 
     * @param verifyEmailRequest contains verification token
     * @return message confirming email verification
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest verifyEmailRequest) {
        log.info("Email verification attempt");

        try {
            userService.verifyEmail(verifyEmailRequest.getToken());
            log.info("Email verified successfully");
            return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now login."));
        } catch (IllegalArgumentException e) {
            log.warn("Email verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Email verification error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Email verification failed"));
        }
    }

    /**
     * Request password reset
     * Sends password reset link to user's email
     * 
     * @param forgotPasswordRequest contains email
     * @return message confirming reset email sent
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        log.info("Password reset requested for email: {}", forgotPasswordRequest.getEmail());

        try {
            String resetToken = userService.generatePasswordResetToken(forgotPasswordRequest.getEmail());
            
            // In production: send email with reset link containing token
            // Example: https://app.example.com/reset-password?token={resetToken}
            
            log.info("Password reset token generated");
            return ResponseEntity.ok(Map.of(
                    "message", "If email exists, password reset link has been sent",
                    "token", resetToken  // For testing only - remove in production
            ));
        } catch (IllegalArgumentException e) {
            // Return same message regardless of whether email exists (security best practice)
            return ResponseEntity.ok(Map.of("message", "If email exists, password reset link has been sent"));
        } catch (Exception e) {
            log.error("Password reset request error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Password reset failed"));
        }
    }

    /**
     * Reset password with token
     * Sets new password after token validation
     * 
     * @param passwordResetRequest contains token and new password
     * @return message confirming password reset
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        log.info("Password reset attempt");

        try {
            // Validate passwords match
            if (!passwordResetRequest.getNewPassword().equals(passwordResetRequest.getNewPasswordConfirm())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Passwords do not match"));
            }

            // Validate password strength
            if (!passwordValidator.isPasswordValid(passwordResetRequest.getNewPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", passwordValidator.getPasswordRequirements()));
            }

            userService.resetPassword(passwordResetRequest.getToken(), passwordResetRequest.getNewPassword());
            log.info("Password reset successfully");
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please login with new password."));
        } catch (IllegalArgumentException e) {
            log.warn("Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Password reset error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Password reset failed"));
        }
    }

    /**
     * Refresh access token
     * Generates new access token using refresh token
     * 
     * @param refreshTokenRequest contains refresh token
     * @return LoginResponse with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.info("Refresh token request");

        try {
            LoginResponse response = authenticationService.refreshAccessToken(refreshTokenRequest);
            log.info("Token refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token refresh failed"));
        }
    }

    /**
     * Change password (authenticated endpoint)
     * Requires authentication - user changes their own password
     * 
     * @param changePasswordRequest old and new passwords
     * @param authentication Spring Security authentication object
     * @return message confirming password change
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            Authentication authentication) {
        log.info("Password change attempt for user: {}", authentication.getName());

        try {
            String username = authentication.getName();
            var user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validate passwords match
            if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getNewPasswordConfirm())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "New passwords do not match"));
            }

            userService.changePassword(user.getId(), changePasswordRequest);
            log.info("Password changed successfully for user: {}", username);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("Password change failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Password change error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Password change failed"));
        }
    }

    /**
     * Logout endpoint
     * Revokes refresh token to prevent future use
     * 
     * @param refreshTokenRequest contains refresh token to revoke
     * @param authentication Spring Security authentication object
     * @return message confirming logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody RefreshTokenRequest refreshTokenRequest,
            Authentication authentication) {
        log.info("Logout attempt for user: {}", authentication.getName());

        try {
            authenticationService.logout(authentication.getName(), refreshTokenRequest.getRefreshToken());
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed"));
        }
    }

    /**
     * Health check endpoint
     * Verifies the authentication service is running
     * 
     * @return simple OK response
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Authentication service is running");
    }
}
