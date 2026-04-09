package com.example.riskmonitoring.alertservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.riskmonitoring.alertservice.dto.LoginRequest;
import com.example.riskmonitoring.alertservice.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Integration Tests for Abuse Protection at HTTP Endpoint Level
 * 
 * Tests the complete flow:
 * - HTTP Request → RateLimitingInterceptor → Endpoint
 * - @RateLimitProtected annotation → RateLimitingAspect → Service
 * - Exception → GlobalExceptionHandler → HTTP Response
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.rate-limiting.login.per-minute=3",
    "app.rate-limiting.registration.per-minute=2"
})
@DisplayName("HTTP Endpoint Integration Tests for Rate Limiting")
public class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String REGISTER_ENDPOINT = "/api/auth/register";

    // ======================== LOGIN ENDPOINT TESTS ========================

    @Test
    @DisplayName("Login - First attempt succeeds with rate limit headers")
    public void testLoginFirstAttemptSucceeds() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("Login - Multiple successful attempts count down remaining")
    public void testLoginMultipleAttemptsCountdown() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        // First attempt
        MvcResult result1 = mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "3"))
            .andExpect(header().string("X-RateLimit-Remaining", "2"))
            .andReturn();

        // Second attempt
        MvcResult result2 = mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Remaining", "1"))
            .andReturn();

        // Third attempt
        MvcResult result3 = mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Remaining", "0"))
            .andReturn();
    }

    @Test
    @DisplayName("Login - Exceeding rate limit returns 429 Too Many Requests")
    public void testLoginExceedsRateLimit() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("bruteforceuser")
            .password("password123")
            .build();

        // Exceed limit (3 per minute)
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"));
        }

        // 4th attempt should fail
        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error", containsString("rate limit")));
    }

    @Test
    @DisplayName("Login - Bot user-agent sends suspicious activity error")
    public void testLoginBotUserAgent() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "curl/7.64.1"))  // Bot user-agent
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error", containsString("denied")));
    }

    // ======================== REGISTRATION ENDPOINT TESTS ========================

    @Test
    @DisplayName("Registration - First attempt succeeds")
    public void testRegistrationFirstAttemptSucceeds() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser")
            .email("newuser@example.com")
            .password("password123")
            .build();

        mockMvc.perform(post(REGISTER_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", lessThanOrEqualTo("10")));
    }

    @Test
    @DisplayName("Registration - Rate limit per IP address")
    public void testRegistrationRateLimitPerIp() throws Exception {
        RegisterRequest request1 = RegisterRequest.builder()
            .username("user1")
            .email("user1@example.com")
            .password("password123")
            .build();

        RegisterRequest request2 = RegisterRequest.builder()
            .username("user2")
            .email("user2@example.com")
            .password("password123")
            .build();

        // Two registrations from same IP should impact limit
        mockMvc.perform(post(REGISTER_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk());

        mockMvc.perform(post(REGISTER_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk());

        // Third registration should be rate limited (limit is 2/min)
        RegisterRequest request3 = RegisterRequest.builder()
            .username("user3")
            .email("user3@example.com")
            .password("password123")
            .build();

        mockMvc.perform(post(REGISTER_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request3))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Registration - Honeypot field triggers silent success")
    public void testRegistrationHoneypotFiled() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("bot")
            .email("bot@example.com")
            .password("password123")
            .website("http://spam-site.com")  // Honeypot field filled
            .build();

        mockMvc.perform(post(REGISTER_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk())  // Silent success to deceive bot
            .andExpect(jsonPath("$.message", containsString("Success")));
    }

    // ======================== GLOBAL RATE LIMIT TESTS ========================

    @Test
    @DisplayName("Global rate limit - X-Forwarded-For header parsing")
    public void testGlobalRateLimitXForwardedFor() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        // With X-Forwarded-For header (proxy scenario)
        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("X-Forwarded-For", "203.0.113.100"))  // Client IP from proxy
            .andExpect(status().isOk())
            .andExpect(header().exists("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Global rate limit - X-Real-IP header fallback")
    public void testGlobalRateLimitXRealIp() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("X-Real-IP", "203.0.113.101"))  // Fallback header
            .andExpect(status().isOk());
    }

    // ======================== HEALTH CHECK BYPASS TESTS ========================

    @Test
    @DisplayName("Health endpoint bypasses rate limiting")
    public void testHealthCheckBypassesRateLimit() throws Exception {
        // Make many requests to health endpoint
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
        }
        // Should not be rate limited
    }

    @Test
    @DisplayName("Metrics endpoint bypasses rate limiting")
    public void testMetricsEndpointBypassesRateLimit() throws Exception {
        // Make many requests to metrics endpoint
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/metrics"))
                .andExpect(status().isAnyOf(
                    org.springframework.http.HttpStatus.OK,
                    org.springframework.http.HttpStatus.NOT_FOUND
                ));
        }
    }

    // ======================== RESPONSE HEADER VALIDATION TESTS ========================

    @Test
    @DisplayName("Response includes X-RateLimit headers")
    public void testResponseIncludesRateLimitHeaders() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"))
            .andExpect(header().string("X-RateLimit-Limit", matchesPattern("\\d+")))
            .andExpect(header().string("X-RateLimit-Remaining", matchesPattern("\\d+")));
    }

    @Test
    @DisplayName("429 response includes Retry-After header")
    public void testTooManyRequestsIncludesRetryAfter() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("bruteforceuser2")
            .password("password123")
            .build();

        // Exceed limit
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"));
        }

        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(header().string("Retry-After", matchesPattern("\\d+")));
    }

    // ======================== ERROR RESPONSE TESTS ========================

    @Test
    @DisplayName("400 Bad Request - Invalid JSON")
    public void testBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{invalid json}")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("429 response includes rate limit info in body")
    public void testTooManyRequestsResponseBody() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("bruteforceuser3")
            .password("password123")
            .build();

        // Exceed limit
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"));
        }

        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.retryAfter").exists())
            .andExpect(jsonPath("$.retryAfter", greaterThan(0)));
    }

    @Test
    @DisplayName("403 Forbidden - Abuse detected")
    public void testForbiddenAbuseDetected() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();

        mockMvc.perform(post(LOGIN_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("User-Agent", "Nmap Scripting Engine"))  // Scanner bot
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error", containsString("denied")));
    }
}
