package com.example.riskmonitoring.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Response DTO
 * Contains JWT access token, refresh token, and user information
 * 
 * Security Notes:
 * - Access token: Short-lived (1 hour), used for API requests
 * - Refresh token: Long-lived (7 days), used to get new access tokens
 * - Tokens should be stored in HTTPOnly cookies, not localStorage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private String username;

    private String userId;

    private Long expiresIn;  // Access token expiration in seconds

    private Long refreshExpiresIn;  // Refresh token expiration in seconds
}
