package com.example.riskmonitoring.alertservice.service;

import com.example.riskmonitoring.alertservice.domain.User;
import com.example.riskmonitoring.alertservice.dto.ChangePasswordRequest;
import com.example.riskmonitoring.alertservice.dto.RegisterRequest;
import com.example.riskmonitoring.alertservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * User Service
 * Handles user registration, password management, and account operations
 * Implements secure password hashing and account lockout mechanisms
 */
@Slf4j
@Service
@Transactional
public class UserService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final int PASSWORD_RESET_TOKEN_VALIDITY_MINUTES = 15;
    private static final int EMAIL_VERIFICATION_TOKEN_VALIDITY_MINUTES = 24 * 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final LoginAttemptService loginAttemptService;

    public UserService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder,
                       PasswordValidator passwordValidator,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Register a new user
     * @param registerRequest registration details
     * @return registered User
     * @throws IllegalArgumentException if username/email already exists or password is weak
     */
    public User registerUser(RegisterRequest registerRequest) {
        log.info("Attempting to register user: {}", registerRequest.getUsername());

        // Validate password strength
        if (!passwordValidator.isPasswordValid(registerRequest.getPassword())) {
            throw new IllegalArgumentException("Password does not meet security requirements");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user with hashed password
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEmailVerified(false);
        user.getRoles().add("USER");

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken, 
            Instant.now().plusSeconds(EMAIL_VERIFICATION_TOKEN_VALIDITY_MINUTES * 60L));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", registerRequest.getUsername());

        return savedUser;
    }

    /**
     * Verify user's email with verification token
     * @param token the email verification token
     * @throws IllegalArgumentException if token is invalid or expired
     */
    public void verifyEmail(String token) {
        log.info("Verifying email with token");

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        // Check if token is expired
        if (user.getEmailVerificationTokenExpiry() == null || 
            user.getEmailVerificationTokenExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        // Mark email as verified
        user.markEmailVerified();
        userRepository.save(user);
        log.info("Email verified successfully for user: {}", user.getUsername());
    }

    /**
     * Generate password reset token for user
     * @param email the user's email
     * @return password reset token
     * @throws IllegalArgumentException if email not found
     */
    public String generatePasswordResetToken(String email) {
        log.info("Generating password reset token for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(Instant.now().plusSeconds(PASSWORD_RESET_TOKEN_VALIDITY_MINUTES * 60L));
        userRepository.save(user);

        return resetToken;
    }

    /**
     * Reset password using reset token
     * @param token the password reset token
     * @param newPassword the new password
     * @throws IllegalArgumentException if token invalid or password weak
     */
    public void resetPassword(String token, String newPassword) {
        log.info("Attempting password reset with token");

        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        // Check if token is expired
        if (user.getResetPasswordTokenExpiry() == null ||
            user.getResetPasswordTokenExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        // Validate new password strength
        if (!passwordValidator.isPasswordValid(newPassword)) {
            throw new IllegalArgumentException("New password does not meet security requirements");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.clearPasswordResetToken(user.getId());
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    /**
     * Change password for authenticated user
     * @param userId the user ID
     * @param changePasswordRequest old and new passwords
     * @throws IllegalArgumentException if old password incorrect or new password weak
     */
    public void changePassword(Long userId, ChangePasswordRequest changePasswordRequest) {
        log.info("Changing password for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        // Validate new password strength
        if (!passwordValidator.isPasswordValid(changePasswordRequest.getNewPassword())) {
            throw new IllegalArgumentException("New password does not meet security requirements");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {}", userId);
    }

    /**
     * Record successful login - reset failed attempts
     * @param username the username
     */
    public void recordSuccessfulLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getFailedLoginAttempts() > 0) {
            userRepository.resetFailedLoginAttempts(user.getId());
            log.info("Reset failed login attempts for user: {}", username);
        }
    }

    /**
     * Record failed login attempt
     * @param username the username
     */
    public void recordFailedLoginAttempt(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int failedAttempts = user.getFailedLoginAttempts() + 1;
        userRepository.updateFailedLoginAttempts(user.getId(), failedAttempts, Instant.now());

        // Lock account if max attempts exceeded
        if (failedAttempts >= MAX_LOGIN_ATTEMPTS) {
            Instant lockedUntil = Instant.now().plusSeconds(LOCKOUT_DURATION_MINUTES * 60L);
            userRepository.lockAccountUntil(user.getId(), lockedUntil);
            log.warn("Account locked for user due to too many failed login attempts: {}", username);
        }
    }

    /**
     * Check if user can attempt login
     * @param username the username
     * @return true if user can login, false if account is locked
     * @throws IllegalArgumentException if user not found
     */
    public boolean canLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if account is locked
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(Instant.now())) {
            log.warn("Login attempt for locked account: {}", username);
            return false;
        }

        // Unlock account if lock time has passed
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isBefore(Instant.now())) {
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }

        return true;
    }

    /**
     * Get user by username
     * @param username the username
     * @return Optional containing user if found
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by email
     * @param email the email
     * @return Optional containing user if found
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

}
