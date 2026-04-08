package com.example.riskmonitoring.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Response DTO
 * Contains JWT token and user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String tokenType;

    private String username;

    private String userId;

    private String role;

    private Long expiresIn;
}
