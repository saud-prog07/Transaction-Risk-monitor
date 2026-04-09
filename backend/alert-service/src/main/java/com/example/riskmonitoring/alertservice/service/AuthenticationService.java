package com.example.riskmonitoring.alertservice.service;

import com.example.riskmonitoring.alertservice.domain.User;
import com.example.riskmonitoring.alertservice.dto.*;
import com.example.riskmonitoring.alertservice.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * Handles user authentication and JWT token generation
 * 
 * Security Features:
 * - Password verification using BCrypt
 * - Failed login attempt tracking
 * - Account lockout mechanism
 * - Email verification requirement
 * - Refresh token generation
 * - Rate limiting on login attempts
 */
@Slf4j
@Service
@Transactional
public class AuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    @Value("${jwt.expiration:3600000}")
    private int jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private int refreshTokenExpirationMs;

    public AuthenticationService(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            UserService userService,
            LoginAttemptService loginAttemptService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Authenticate user and generate JWT tokens
     * @param loginRequest the login request with username and password
     * @return LoginResponse with access and refresh tokens
     * @throws IllegalArgumentException if authentication fails
     */
    public LoginResponse authenticate(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        log.info("Authenticating user: {}", username);

        // Check rate limiting first
        if (loginAttemptService.isRateLimited(username)) {
            long remainingSeconds = loginAttemptService.getRemainingLockoutTime(username);
            log.warn("Login attempt for rate-limited user: {} (try again in {} seconds)", username, remainingSeconds);
            throw new IllegalArgumentException("Too many failed login attempts. Try again in " + remainingSeconds + " seconds.");
        }

        try {
            // Get user from database
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

            // Check if account can attempt login
            if (!userService.canLogin(username)) {
                long remainingMinutes = (user.getAccountLockedUntil().getEpochSecond() - System.currentTimeMillis() / 1000);
                throw new IllegalArgumentException("Account is locked. Try again after " + remainingMinutes + " minutes.");
            }

            // Verify password
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                loginAttemptService.recordFailedAttempt(username);
                userService.recordFailedLoginAttempt(username);
                log.warn("Authentication failed - invalid password for user: {}", username);
                throw new IllegalArgumentException("Invalid username or password");
            }

            // Check if email is verified
            if (!user.isEmailVerified()) {
                log.warn("Login attempt with unverified email: {}", username);
                throw new IllegalArgumentException("Email not verified. Please verify your email before logging in.");
            }

            // Check if user is enabled
            if (!user.isEnabled()) {
                log.warn("Login attempt for disabled user: {}", username);
                throw new IllegalArgumentException("User account is disabled");
            }

            // Clear failed login attempts on successful authentication
            loginAttemptService.clearFailedAttempts(username);
            userService.recordSuccessfulLogin(username);

            // Generate tokens
            String accessToken = jwtTokenProvider.generateToken(username, String.valueOf(user.getId()));
            String refreshToken = refreshTokenService.generateRefreshToken(username, String.valueOf(user.getId()));

            log.info("Authentication successful for user: {}", username);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .username(username)
                    .userId(String.valueOf(user.getId()))
                    .expiresIn((long) jwtExpirationMs / 1000)  // Convert to seconds
                    .refreshExpiresIn((long) refreshTokenExpirationMs / 1000)
                    .build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Authentication failed");
        }
    }

    /**
     * Refresh access token using refresh token
     * @param refreshTokenRequest contains refresh token
     * @return LoginResponse with new access token
     * @throws IllegalArgumentException if refresh token is invalid
     */
    public LoginResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        log.info("Refreshing access token");

        if (!refreshTokenService.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String username = refreshTokenService.getUsernameFromRefreshToken(refreshToken);
        String userId = refreshTokenService.getUserIdFromRefreshToken(refreshToken);

        // Verify user still exists and is enabled
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateToken(username, userId);

        log.info("Access token refreshed for user: {}", username);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)  // Return same refresh token
                .tokenType("Bearer")
                .username(username)
                .userId(userId)
                .expiresIn((long) jwtExpirationMs / 1000)
                .refreshExpiresIn((long) refreshTokenExpirationMs / 1000)
                .build();
    }

    /**
     * Logout user - revoke refresh token
     * @param username the username to logout
     * @param refreshToken the refresh token to revoke
     */
    public void logout(String username, String refreshToken) {
        log.info("Logging out user: {}", username);
        
        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
        }
        
        log.info("User logged out successfully: {}", username);
    }
}

