package com.example.riskmonitoring.alertservice.repository;

import com.example.riskmonitoring.alertservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for User entity
 * Provides database operations for User authentication and authorization
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     * @param username the username
     * @param email the email
     * @return Optional containing user if found
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * Find user by email verification token
     * @param token the verification token
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailVerificationToken(String token);
    
    /**
     * Find user by password reset token
     * @param token the reset token
     * @return Optional containing the user if found
     */
    Optional<User> findByResetPasswordToken(String token);

    /**
     * Check if username exists
     * @param username the username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Update failed login attempts and last failed login time
     * @param userId the user ID
     * @param failedAttempts the new failed attempts count
     * @param lastFailedTime the last failed login timestamp
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = :failedAttempts, u.lastFailedLoginAttempt = :lastFailedTime WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, @Param("failedAttempts") int failedAttempts, @Param("lastFailedTime") Instant lastFailedTime);

    /**
     * Lock user account until specified time
     * @param userId the user ID
     * @param lockedUntil the time until which account is locked
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountLockedUntil = :lockedUntil, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void lockAccountUntil(@Param("userId") Long userId, @Param("lockedUntil") Instant lockedUntil);

    /**
     * Reset failed login attempts
     * @param userId the user ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lastFailedLoginAttempt = null WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") Long userId);

    /**
     * Clear email verification token after verification
     * @param userId the user ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.emailVerified = true, u.emailVerificationToken = null, u.emailVerificationTokenExpiry = null WHERE u.id = :userId")
    void clearEmailVerificationToken(@Param("userId") Long userId);

    /**
     * Clear password reset token
     * @param userId the user ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.resetPasswordToken = null, u.resetPasswordTokenExpiry = null WHERE u.id = :userId")
    void clearPasswordResetToken(@Param("userId") Long userId);

    /**
     * Update password hash
     * @param userId the user ID
     * @param newPasswordHash the new password hash
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    void updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String newPasswordHash);
}