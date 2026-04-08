package com.example.riskmonitoring.alertservice.controller;

import com.example.riskmonitoring.alertservice.dto.LoginRequest;
import com.example.riskmonitoring.alertservice.dto.LoginResponse;
import com.example.riskmonitoring.alertservice.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles user login and JWT token generation
 * 
 * Endpoints:
 * - POST /api/auth/login - User login
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * User login endpoint
     * Authenticates user and returns JWT token
     * 
     * @param loginRequest the login request (username and password)
     * @return LoginResponse with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        log.info("Login attempt for user: {}", loginRequest.getUsername());

        try {
            LoginResponse response = authenticationService.authenticate(loginRequest);
            log.info("Login successful for user: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
    }

    /**
     * Health check endpoint
     * Verifies the authentication service is running
     * 
     * @return simple ok response
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Authentication service is running");
    }
}
