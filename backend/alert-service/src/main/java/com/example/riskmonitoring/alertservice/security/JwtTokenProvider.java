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
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")
    private int jwtExpirationMs;

    /**
     * Generate JWT token for a user
     * @param username the username
     * @param userId the user ID
     * @param role the user role (ADMIN, ANALYST, etc.)
     * @return JWT token string
     */
    public String generateToken(String username, String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        
        return createToken(claims, username);
    }

    /**
     * Extract role from JWT token
     * @param token the JWT token
     * @return user role
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
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
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
