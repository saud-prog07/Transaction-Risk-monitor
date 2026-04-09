package com.example.riskmonitoring.alertservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User entity for authentication and authorization
 * Implements Spring Security's UserDetails interface
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    private boolean credentialsNonExpired = true;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "verification_token")
    private String emailVerificationToken;

    @Column(name = "verification_token_expiry")
    private Instant emailVerificationTokenExpiry;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expiry")
    private Instant resetPasswordTokenExpiry;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "last_failed_login")
    private Instant lastFailedLoginAttempt;

    @Column(name = "locked_until")
    private Instant accountLockedUntil;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private java.util.Set<String> roles = new java.util.HashSet<>();

    // Constructors
    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = java.util.Collections.singleton("USER");
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Check if account is currently locked
        if (accountLockedUntil != null && accountLockedUntil.isAfter(Instant.now())) {
            return false;
        }
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Helper methods
    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
        this.emailVerificationToken = null;
        this.emailVerificationTokenExpiry = null;
    }

    public void setEmailVerificationToken(String token, Instant expiry) {
        this.emailVerificationToken = token;
        this.emailVerificationTokenExpiry = expiry;
    }

    public boolean isEmailVerificationTokenValid(String token) {
        return emailVerificationToken != null &&
                emailVerificationToken.equals(token) &&
                emailVerificationTokenExpiry != null &&
                emailVerificationTokenExpiry.isAfter(Instant.now());
    }

    public void setResetPasswordToken(String token, Instant expiry) {
        this.resetPasswordToken = token;
        this.resetPasswordTokenExpiry = expiry;
    }

    public boolean isResetPasswordTokenValid(String token) {
        return resetPasswordToken != null &&
                resetPasswordToken.equals(token) &&
                resetPasswordTokenExpiry != null &&
                resetPasswordTokenExpiry.isAfter(Instant.now());
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLoginAttempt = Instant.now();
        
        // Lock account after 5 failed attempts for 15 minutes
        if (failedLoginAttempts >= 5) {
            this.accountLockedUntil = Instant.now().plusSeconds(900); // 15 minutes
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAttempt = null;
        this.accountLockedUntil = null;
    }

    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(Instant.now());
    }
}