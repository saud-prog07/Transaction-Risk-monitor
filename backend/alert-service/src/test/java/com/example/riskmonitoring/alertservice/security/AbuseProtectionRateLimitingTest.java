package com.example.riskmonitoring.alertservice.security;

import com.example.riskmonitoring.alertservice.annotation.RateLimitType;
import com.example.riskmonitoring.alertservice.dto.RateLimitInfoDto;
import com.example.riskmonitoring.alertservice.exception.AbuseDetectedException;
import com.example.riskmonitoring.alertservice.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Comprehensive Unit Tests for Abuse Protection and Rate Limiting System
 * 
 * This test suite validates:
 * - Token bucket rate limiting with multiple limits
 * - Bot detection and user-agent validation
 * - IP-based blocking and time-based expiration
 * - Abuse pattern detection (behavioral analysis)
 * - Honeypot field validation
 * - Duplicate request detection
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.rate-limiting.login.per-minute=5",
    "app.rate-limiting.registration.per-minute=2",
    "app.rate-limiting.ai-generation.per-minute=10",
    "app.rate-limiting.global.per-minute=60"
})
@DisplayName("Abuse Protection and Rate Limiting Test Suite")
public class AbuseProtectionRateLimitingTest {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private AbuseDetectionService abuseDetectionService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_USER_AGENT_BOT = "curl/7.64.1";
    private static final String TEST_USER_AGENT_HUMAN = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @BeforeEach
    public void setUp() {
        // Clean up before each test
        rateLimitingService.cleanup();
        abuseDetectionService.clearMetrics();
    }

    // ======================== RATE LIMITING TESTS ========================

    @Test
    @DisplayName("Login rate limit - 5 attempts per minute")
    public void testLoginRateLimit() {
        String identifier = "login_" + TEST_USERNAME;

        // First 5 attempts should succeed
        for (int i = 1; i <= 5; i++) {
            assertDoesNotThrow(() -> 
                rateLimitingService.checkLoginRateLimit(identifier),
                "Attempt " + i + " should succeed"
            );
        }

        // 6th attempt should fail
        RateLimitExceededException exception = assertThrows(
            RateLimitExceededException.class,
            () -> rateLimitingService.checkLoginRateLimit(identifier)
        );
        
        assertEquals(429, exception.getHttpStatus().value());
        assertTrue(exception.getRetryAfterSeconds() > 0);
    }

    @Test
    @DisplayName("Registration rate limit - 2 attempts per minute per IP")
    public void testRegistrationRateLimit() {
        String identifier = "registration_" + TEST_IP;

        // First 2 attempts should succeed
        rateLimitingService.checkRegistrationRateLimit(identifier);
        rateLimitingService.checkRegistrationRateLimit(identifier);

        // 3rd attempt should fail
        RateLimitExceededException exception = assertThrows(
            RateLimitExceededException.class,
            () -> rateLimitingService.checkRegistrationRateLimit(identifier)
        );
        
        assertEquals(429, exception.getHttpStatus().value());
    }

    @Test
    @DisplayName("AI generation rate limit - 10 attempts per minute")
    public void testAIGenerationRateLimit() {
        String identifier = "ai_user123";

        // First 10 attempts should succeed
        for (int i = 1; i <= 10; i++) {
            assertDoesNotThrow(() -> 
                rateLimitingService.checkAIGenerationRateLimit(identifier)
            );
        }

        // 11th attempt should fail
        assertThrows(RateLimitExceededException.class,
            () -> rateLimitingService.checkAIGenerationRateLimit(identifier)
        );
    }

    @Test
    @DisplayName("Global rate limit - 60 attempts per minute")
    public void testGlobalRateLimit() {
        String identifier = TEST_IP;

        // First 60 attempts should succeed
        for (int i = 1; i <= 60; i++) {
            assertDoesNotThrow(() -> 
                rateLimitingService.checkGlobalRateLimit(identifier)
            );
        }

        // 61st attempt should fail
        assertThrows(RateLimitExceededException.class,
            () -> rateLimitingService.checkGlobalRateLimit(identifier)
        );
    }

    @Test
    @DisplayName("Rate limit metrics provide correct information")
    public void testRateLimitMetrics() {
        String identifier = "login_" + TEST_USERNAME;

        // Use 2 of 5 allowed attempts
        rateLimitingService.checkLoginRateLimit(identifier);
        rateLimitingService.checkLoginRateLimit(identifier);

        // Check metrics
        RateLimitInfoDto metrics = rateLimitingService.getLoginMetrics(identifier);
        assertNotNull(metrics);
        assertEquals(5, metrics.getRequestsAllowed());
        assertEquals(3, metrics.getRequestsRemaining()); // 5 - 2
        assertTrue(metrics.getResetTimeMs() > System.currentTimeMillis());
        assertFalse(metrics.isLimited());
    }

    // ======================== BOT DETECTION TESTS ========================

    @Test
    @DisplayName("Bot detection - curl user-agent")
    public void testBotDetectionCurl() {
        assertTrue(
            abuseDetectionService.isSuspiciousUserAgent(TEST_USER_AGENT_BOT),
            "curl user-agent should be detected as bot"
        );
    }

    @Test
    @DisplayName("Bot detection - wget user-agent")
    public void testBotDetectionWget() {
        assertTrue(
            abuseDetectionService.isSuspiciousUserAgent("wget/1.20.3"),
            "wget should be detected as bot"
        );
    }

    @Test
    @DisplayName("Bot detection - Nmap scanner")
    public void testBotDetectionNmap() {
        assertTrue(
            abuseDetectionService.isSuspiciousUserAgent("Nmap Scripting Engine"),
            "Nmap should be detected as bot"
        );
    }

    @Test
    @DisplayName("Normal user-agent passes bot detection")
    public void testNormalUserAgentPassesBotDetection() {
        assertFalse(
            abuseDetectionService.isSuspiciousUserAgent(TEST_USER_AGENT_HUMAN),
            "Normal browser user-agent should pass bot detection"
        );
    }

    // ======================== IP BLOCKING TESTS ========================

    @Test
    @DisplayName("IP blocking - block and verify")
    public void testIPBlocking() {
        String ipToBlock = "192.168.1.50";
        assertFalse(abuseDetectionService.isIpBlocked(ipToBlock));

        // Block IP
        abuseDetectionService.blockIp(ipToBlock, "Testing", 1000); // 1 second

        // Should be blocked
        assertTrue(abuseDetectionService.isIpBlocked(ipToBlock));
    }

    @Test
    @DisplayName("IP blocking - time-based expiration")
    public void testIPBlockingExpiration() throws InterruptedException {
        String ipToBlock = "192.168.1.51";

        // Block IP for 100ms
        abuseDetectionService.blockIp(ipToBlock, "Testing", 100);
        assertTrue(abuseDetectionService.isIpBlocked(ipToBlock));

        // Wait for expiration
        Thread.sleep(150);

        // Should no longer be blocked
        assertFalse(abuseDetectionService.isIpBlocked(ipToBlock),
            "IP block should expire after duration"
        );
    }

    // ======================== HONEYPOT TESTS ========================

    @Test
    @DisplayName("Honeypot detection - empty field passes")
    public void testHoneypotEmptyFieldPasses() {
        assertFalse(abuseDetectionService.isHoneypotTriggered(""));
        assertFalse(abuseDetectionService.isHoneypotTriggered(null));
    }

    @Test
    @DisplayName("Honeypot detection - filled field triggers")
    public void testHoneypotFilledFieldTriggers() {
        assertTrue(abuseDetectionService.isHoneypotTriggered("http://bot-site.com"));
        assertTrue(abuseDetectionService.isHoneypotTriggered("www.spam.com"));
    }

    // ======================== DUPLICATE REQUEST DETECTION TESTS ========================

    @Test
    @DisplayName("Duplicate request detection - within 1 second")
    public void testDuplicateRequestDetection() {
        String requestKey = "register_192.168.1.100_test@example.com";

        // First request
        assertFalse(rateLimitingService.isDuplicateRequest(requestKey),
            "First request should not be duplicate"
        );

        // Second request immediately after
        assertTrue(rateLimitingService.isDuplicateRequest(requestKey),
            "Second request within 1 second should be duplicate"
        );
    }

    @Test
    @DisplayName("Duplicate request detection - after 1+ second expires")
    public void testDuplicateRequestExpiresAfterOneSecond() throws InterruptedException {
        String requestKey = "register_192.168.1.100_test@example.com";

        // First request
        assertFalse(rateLimitingService.isDuplicateRequest(requestKey));

        // Wait 1+ second
        Thread.sleep(1100);

        // Should no longer be duplicate
        assertFalse(rateLimitingService.isDuplicateRequest(requestKey),
            "Duplicate detection should expire after 1 second"
        );
    }

    // ======================== ABUSE PATTERN DETECTION TESTS ========================

    @Test
    @DisplayName("Abuse pattern detection - high failure rate")
    public void testAbusePatternHighFailureRate() {
        String identifier = "login_attacker";

        // Simulate 25 failed attempts in 1 minute
        for (int i = 0; i < 25; i++) {
            abuseDetectionService.trackRequest(
                identifier, 
                true,  // failure
                TEST_USER_AGENT_HUMAN,
                "https://yourapp.com"
            );
        }

        assertTrue(abuseDetectionService.showsAbusePattern(identifier),
            "25 failures in 1 minute should show abuse pattern"
        );
    }

    @Test
    @DisplayName("Abuse pattern detection - user-agent switching")
    public void testAbusePatternUserAgentSwitching() {
        String identifier = "login_attacker";

        // Simulate switching between 6 different user-agents
        String[] userAgents = {
            "curl/7.64.1",
            "wget/1.20.3",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            "Python-requests/2.25.1",
            "Apache-HttpClient/4.5.13",
            "Java/11.0.10"
        };

        for (String ua : userAgents) {
            abuseDetectionService.trackRequest(
                identifier,
                true,  // failure
                ua,
                "https://yourapp.com"
            );
        }

        assertTrue(abuseDetectionService.showsAbusePattern(identifier),
            "Switching between 6 user-agents should show abuse pattern"
        );
    }

    @Test
    @DisplayName("Abuse pattern detection - missing referrer with failures")
    public void testAbusePatternMissingReferrer() {
        String identifier = "login_attacker";

        // Simulate 30 failed requests with no referrer
        for (int i = 0; i < 30; i++) {
            abuseDetectionService.trackRequest(
                identifier,
                true,  // failure
                TEST_USER_AGENT_HUMAN,
                null   // no referrer
            );
        }

        assertTrue(abuseDetectionService.showsAbusePattern(identifier),
            "High failures with no referrer should show abuse pattern"
        );
    }

    // ======================== EDGE CASE TESTS ========================

    @Test
    @DisplayName("Multiple identifiers tracked independently")
    public void testMultipleIdentifiersTrackedIndependently() {
        String user1 = "login_user1";
        String user2 = "login_user2";

        // Use up user1's limit
        for (int i = 0; i < 5; i++) {
            rateLimitingService.checkLoginRateLimit(user1);
        }

        // user2 should still have attempts
        assertDoesNotThrow(() -> 
            rateLimitingService.checkLoginRateLimit(user2),
            "Different identifier should have separate limit"
        );
    }

    @Test
    @DisplayName("Rate limit recovery after burst")
    public void testRateLimitRecoveryAfterBurst() {
        String identifier = "login_" + TEST_USERNAME;

        // Use up all 5 attempts
        for (int i = 0; i < 5; i++) {
            rateLimitingService.checkLoginRateLimit(identifier);
        }

        // Should be rate limited
        assertThrows(RateLimitExceededException.class,
            () -> rateLimitingService.checkLoginRateLimit(identifier)
        );

        // After 1 minute + 1 second, should recover
        try {
            Thread.sleep(61000); // Wait for rate limit to expire
            assertDoesNotThrow(() ->
                rateLimitingService.checkLoginRateLimit(identifier),
                "Should recover after 1 minute + 1 second"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    @Test
    @DisplayName("Suspicious IP validation - private networks")
    public void testSuspiciousIPPrivateNetworks() {
        assertTrue(abuseDetectionService.isSuspiciousIp("192.168.1.1"));
        assertTrue(abuseDetectionService.isSuspiciousIp("10.0.0.1"));
        assertTrue(abuseDetectionService.isSuspiciousIp("172.16.0.1"));
    }

    @Test
    @DisplayName("Suspicious IP validation - public IPs")
    public void testSuspiciousIPPublicIPs() {
        assertFalse(abuseDetectionService.isSuspiciousIp("8.8.8.8"));
        assertFalse(abuseDetectionService.isSuspiciousIp("1.1.1.1"));
    }

    // ======================== METRICS EXPORT TESTS ========================

    @Test
    @DisplayName("Export all metrics for monitoring")
    public void testMetricsExport() {
        // Generate some activity
        for (int i = 0; i < 3; i++) {
            try {
                rateLimitingService.checkLoginRateLimit("login_user1");
            } catch (RateLimitExceededException e) {
                // Expected after limit
            }
        }

        Map<String, Object> allMetrics = rateLimitingService.getAllMetrics();
        assertNotNull(allMetrics);
        assertTrue(allMetrics.size() > 0, "Should have exported metrics");
    }
}
