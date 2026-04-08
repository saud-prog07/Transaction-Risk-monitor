package com.example.riskmonitoring.alertservice.service;

import com.example.riskmonitoring.alertservice.dto.LoginRequest;
import com.example.riskmonitoring.alertservice.dto.LoginResponse;
import com.example.riskmonitoring.alertservice.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication Service
 * Handles user authentication and JWT token generation
 * 
 * Note: In a production environment, this would:
 * - Query a database for user credentials
 * - Validate against actual user records
 * - Use bcrypt-encrypted passwords
 * 
 * For this demo, we use hardcoded credentials (admin/admin123)
 */
@Slf4j
@Service
public class AuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:3600000}")
    private int jwtExpirationMs;

    // Hardcoded demo credentials (use database in production)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ANALYST_USERNAME = "analyst";
    private static final String DEMO_PASSWORD = "admin123";
    private static final String DEMO_USER_ID = "1";

    public AuthenticationService(
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate user and generate JWT token
     * @param loginRequest the login request with username and password
     * @return LoginResponse with JWT token
     */
    public LoginResponse authenticate(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        log.info("Authenticating user: {}", username);

        // Demo authentication (in production: query database)
        if (!isValidUser(username, password)) {
            log.warn("Authentication failed for user: {}", username);
            throw new RuntimeException("Invalid username or password");
        }

        // Determine user role based on username
        String role = ADMIN_USERNAME.equals(username) ? "ADMIN" : "ANALYST";

        // Generate JWT token with role
        String token = jwtTokenProvider.generateToken(username, DEMO_USER_ID, role);

        log.info("Authentication successful for user: {} with role: {}", username, role);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(username)
                .userId(DEMO_USER_ID)
                .role(role)
                .expiresIn((long) jwtExpirationMs / 1000) // Convert to seconds
                .build();
    }

    /**
     * Validate user credentials
     * In production, this would query the database
     * 
     * @param username the username
     * @param password the password
     * @return true if credentials are valid
     */
    private boolean isValidUser(String username, String password) {
        // Demo: Accept username "admin" with password "admin123"
        // In production: 
        // 1. Query user by username from database
        // 2. Use passwordEncoder.matches(password, encodedPasswordFromDB)
        // 3. Check if user is enabled/not locked
        
        return username.equals(DEMO_USERNAME) && password.equals(DEMO_PASSWORD);
    }
}
