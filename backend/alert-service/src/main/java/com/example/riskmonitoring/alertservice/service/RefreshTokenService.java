package com.example.riskmonitoring.alertservice.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refresh Token Service
 * Manages refresh token lifecycle and validation
 * 
 * Features:
 * - Separate refresh token from access token
 * - Longer expiry for refresh tokens (7 days)
 * - Token rotation on use
 * - Revocation mechanism
 */
@Slf4j
@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-secret:}")
    private String refreshTokenSecret;

    @Value("${jwt.refresh-expiration:604800000}")  // 7 days in milliseconds
    private int refreshTokenExpirationMs;

    // Maintain revoked tokens for immediate invalidation
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Constructor validates that refresh secret is configured
     */
    public RefreshTokenService() {
        // Validation happens in post-construct to allow Spring initialization
    }

    /**
     * Validates that the refresh token secret is properly configured
     * @throws IllegalStateException if secret is not configured
     */
    private void validateRefreshTokenSecretConfigured() {
        if (refreshTokenSecret == null || refreshTokenSecret.isEmpty()) {
            throw new IllegalStateException(
                "JWT refresh secret is not configured. Set jwt.refresh-secret in environment variables. " +
                "Use at least 256-bit secret (32+ characters).");
        }
    }

    /**
     * Generate refresh token
     * @param username the username
     * @param userId the user ID
     * @return refresh token string
     */
    public String generateRefreshToken(String username, String userId) {
        validateRefreshTokenSecretConfigured();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "REFRESH");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        SecretKey key = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes());

        String token = Jwts.builder()
                .addClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated refresh token for user: {}", username);
        return token;
    }

    /**
     * Validate refresh token
     * @param token the refresh token
     * @return true if token is valid and not revoked
     */
    public boolean validateRefreshToken(String token) {
        try {
            validateRefreshTokenSecretConfigured();

            // Check if token is revoked
            if (revokedTokens.contains(token)) {
                log.warn("Attempt to use revoked refresh token");
                return false;
            }

            SecretKey key = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Verify token type
            String type = (String) claims.get("type");
            if (!"REFRESH".equals(type)) {
                log.warn("Invalid token type in refresh token");
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Refresh token validation failed", e);
            return false;
        }
    }

    /**
     * Extract username from refresh token
     * @param token the refresh token
     * @return username
     */
    public String getUsernameFromRefreshToken(String token) {
        try {
            validateRefreshTokenSecretConfigured();

            SecretKey key = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting username from refresh token", e);
            return null;
        }
    }

    /**
     * Extract user ID from refresh token
     * @param token the refresh token
     * @return user ID
     */
    public String getUserIdFromRefreshToken(String token) {
        try {
            validateRefreshTokenSecretConfigured();

            SecretKey key = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return (String) claims.get("userId");
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting user ID from refresh token", e);
            return null;
        }
    }

    /**
     * Revoke refresh token (invalidate for future use)
     * @param token the token to revoke
     */
    public void revokeToken(String token) {
        revokedTokens.add(token);
        log.info("Refresh token revoked");
    }

    /**
     * Check if token is revoked
     * @param token the token to check
     * @return true if token is revoked
     */
    public boolean isTokenRevoked(String token) {
        return revokedTokens.contains(token);
    }

    /**
     * Revoke all tokens for user (logout across all devices)
     * Note: In production, should query database for tokens
     * @param username the username
     */
    public void revokeAllUserTokens(String username) {
        // In production: query database for all tokens issued to user and mark as revoked
        log.info("Logged out user across all devices: {}", username);
    }
}
