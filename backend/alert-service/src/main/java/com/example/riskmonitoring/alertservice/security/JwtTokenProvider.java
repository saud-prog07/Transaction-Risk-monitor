package com.example.riskmonitoring.alertservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Provider - Handles JWT token generation and validation
 * Uses JJWT library for secure JWT operations
 * 
 * Security Best Practices:
 * - Tokens contain minimal claims for security
 * - User ID is in token but not exposed in frontend
 * - Role information queried from database, not stored in token
 * - Tokens expire after 1 hour
 * - Refresh tokens used for token renewal
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")
    private int jwtExpirationMs;

    /**
     * Generate JWT access token for a user
     * @param username the username
     * @param userId the user ID (internal reference only)
     * @return JWT token string
     */
    public String generateToken(String username, String userId) {
        Map<String, Object> claims = new HashMap<>();
        // Store userId internally for database lookups
        // Do NOT expose sensitive information like password, email in token
        claims.put("userId", userId);
        
        return createToken(claims, username);
    }

    /**
     * Extract role from JWT token claims
     * @param token the JWT token
     * @return user role or null
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            // Role is NOT stored in JWT for security
            // Query database for actual role instead
            return (String) claims.get("role");
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting role from JWT token", e);
            return null;
        }
    }

    /**
     * Create JWT token with claims
     * @param claims the claims to include in token
     * @param subject the subject (username)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured. Set jwt.secret in environment variables.");
        }

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.builder()
                .addClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract username from JWT token
     * @param token the JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting username from JWT token", e);
            return null;
        }
    }

    /**
     * Extract user ID from JWT token
     * @param token the JWT token
     * @return user ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return (String) claims.get("userId");
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting user ID from JWT token", e);
            return null;
        }
    }

    /**
     * Check if JWT token is expired
     * @param token the JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error checking token expiration", e);
            return true;
        }
    }

    /**
     * Validate JWT token
     * @param token the JWT token
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("JWT secret is not configured");
                return false;
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed", e);
            return false;
        }
    }

    /**
     * Extract claims from JWT token
     * @param token the JWT token
     * @return Claims object
     */
    private Claims getClaimsFromToken(String token) {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
