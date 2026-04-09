package com.example.riskmonitoring.alertservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Abuse Detection Service
 * Detects bot patterns, suspicious user agents, and automated script behavior
 * 
 * Features:
 * - User-Agent validation against known bot patterns
 * - IP-based abuse detection
 * - Request pattern analysis
 * - Honeypot field validation
 * - Behavioral analysis for bot detection
 */
@Slf4j
@Service
public class AbuseDetectionService {

    /**
     * Known bot user agents (regex patterns)
     */
    private static final List<String> BOT_USER_AGENTS = Arrays.asList(
        "(?i)bot", "(?i)crawler", "(?i)spider", "(?i)scraper", "(?i)curl", "(?i)wget",
        "(?i)python", "(?i)java", "(?i)perl", "(?i)php", "(?i)ruby", "(?i)go-http-client",
        "(?i)http\\.client", "(?i)okhttp", "(?i)axios", "(?i)requests", "(?i)fetch",
        "(?i)libwww", "(?i)nmap", "(?i)masscan", "(?i)nikto", "(?i)sqlmap",
        "(?i)sqlninja", "(?i)acunetix", "(?i)nessus", "(?i)openvas", "(?i)qualys",
        "(?i)googlebot", "(?i)bingbot", "(?i)slurp", "(?i)duckduckbot", "(?i)baiduspider",
        "(?i)yandexbot", "(?i)facebookexternalhit", "(?i)twitterbot", "(?i)linkedinbot",
        "(?i)whatsapp", "(?i)telegram", "(?i)slack", "(?i)discord"
    );

    /**
     * Suspicious patterns in user agents
     */
    private static final List<String> SUSPICIOUS_PATTERNS = Arrays.asList(
        "(?i)^[a-z0-9\\-]+/[0-9]+(\\.[0-9]+)?$",  // Generic version-only UAs
        "(?i)User-Agent:\\s*$",                      // Empty UA
        ".*[<>\"'%;()&+].*"                          // XSS/Injection patterns
    );

    /**
     * Suspicious IP patterns (private networks, loopback)
     */
    private static final List<String> SUSPICIOUS_IPS = Arrays.asList(
        "^127\\..*",      // localhost
        "^0\\.0\\.0\\.0", // invalid
        "^255\\.255\\.255\\.255", // broadcast
        "^192\\.168\\..*", // private (for API access)
        "^10\\..*",        // private
        "^172\\.(1[6-9]|2[0-9]|3[01])\\..*" // private
    );

    /**
     * Metrics for abuse detection
     */
    private static class AbuseMetrics {
        int failedLogins;
        int failedApiCalls;
        int requestsPerMinute;
        long firstRequestTime;
        Set<String> userAgents = new HashSet<>();
        Set<String> referrers = new HashSet<>();
    }

    // Track abuse metrics per IP
    private final Map<String, AbuseMetrics> ipMetrics = new ConcurrentHashMap<>();
    private final Map<String, AbuseMetrics> userMetrics = new ConcurrentHashMap<>();

    @Value("${security.abuse-detection.enable-strict-mode:false}")
    private boolean strictMode;

    @Value("${security.abuse-detection.require-valid-ua:true}")
    private boolean requireValidUserAgent;

    /**
     * Validate user agent - check if it looks like a bot
     * @param userAgent the user-agent header value
     * @return true if user agent is suspicious/bot-like, false if legitimate
     */
    public boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return true; // Empty UA is suspicious
        }

        // Check against known bot patterns
        for (String botPattern : BOT_USER_AGENTS) {
            if (Pattern.matches(botPattern, userAgent)) {
                log.warn("Detected bot user agent: {}", userAgent);
                return true;
            }
        }

        // Check for suspicious patterns
        for (String suspiciousPattern : SUSPICIOUS_PATTERNS) {
            if (Pattern.matches(suspiciousPattern, userAgent)) {
                log.debug("Detected suspicious user agent pattern: {}", userAgent);
                return true;
            }
        }

        return false;
    }

    /**
     * Validate IP address - check if it's from a suspicious network
     * @param ipAddress the client IP address
     * @return true if IP is suspicious, false if legitimate
     */
    public boolean isSuspiciousIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }

        // Remove port if present
        if (ipAddress.contains(":")) {
            ipAddress = ipAddress.substring(0, ipAddress.lastIndexOf(":"));
        }

        // Check against suspicious patterns
        for (String suspiciousPattern : SUSPICIOUS_IPS) {
            if (Pattern.matches(suspiciousPattern, ipAddress)) {
                log.debug("Detected suspicious IP: {}", ipAddress);
                return true;
            }
        }

        // Check for obviously invalid IPs
        if (ipAddress.contains("..") || ipAddress.split("\\.").length != 4) {
            return true;
        }

        return false;
    }

    /**
     * Check for honeypot field submission (bot trap)
     * Honeypot fields are hidden form fields that should remain empty
     * If a bot fills them, it indicates automated form submission
     * 
     * @param honeypotValue the value of the honeypot field (should be empty)
     * @return true if honeypot was filled (likely a bot), false if empty (legitimate)
     */
    public boolean isHoneypotTriggered(String honeypotValue) {
        boolean triggered = honeypotValue != null && !honeypotValue.isEmpty();
        if (triggered) {
            log.warn("Honeypot triggered - likely bot or automated submission");
        }
        return triggered;
    }

    /**
     * Check for suspicious request headers
     * @param userAgent user agent string
     * @param referer referer header
     * @param acceptLanguage accept-language header
     * @return true if suspicious, false if legitimate
     */
    public boolean isSuspiciousRequestHeaders(String userAgent, String referer, String acceptLanguage) {
        // Check user agent
        if (isSuspiciousUserAgent(userAgent)) {
            return true;
        }

        // Check referer - legitimate requests usually have one except for HTTPS->HTTP downgrade
        boolean hasReferer = referer != null && !referer.isEmpty();
        
        // Check accept-language - bots often don't send this
        boolean hasAcceptLanguage = acceptLanguage != null && !acceptLanguage.isEmpty();

        // Check accept-encoding - should be present in legitimate browsers
        // This requires passing it as a parameter if needed

        // In strict mode, missing headers is suspicious
        if (strictMode && (!hasReferer || !hasAcceptLanguage)) {
            log.debug("Suspicious headers detected in strict mode - referer present: {}, accept-language: {}",
                hasReferer, hasAcceptLanguage);
            return true;
        }

        return false;
    }

    /**
     * Track request for abuse pattern analysis
     * @param identifier IP address or user ID
     * @param isFailure true if request failed (login failure, API error, etc.)
     * @param userAgent user agent string
     * @param referer referer header
     */
    public void trackRequest(String identifier, boolean isFailure, String userAgent, String referer) {
        AbuseMetrics metrics = ipMetrics.computeIfAbsent(identifier, k -> new AbuseMetrics());
        
        if (isFailure) {
            metrics.failedApiCalls++;
        }
        if (metrics.firstRequestTime == 0) {
            metrics.firstRequestTime = System.currentTimeMillis();
        }
        if (userAgent != null) {
            metrics.userAgents.add(userAgent);
        }
        if (referer != null) {
            metrics.referrers.add(referer);
        }

        // Check for abuse patterns
        if (metrics.failedApiCalls > 50) {
            log.error("High failure rate detected for identifier: {} (failures: {})",
                identifier, metrics.failedApiCalls);
        }
    }

    /**
     * Check if an IP/user shows abuse patterns
     * @param identifier IP address or user ID
     * @return true if abuse pattern detected, false otherwise
     */
    public boolean showsAbusePattern(String identifier) {
        AbuseMetrics metrics = ipMetrics.get(identifier);
        if (metrics == null) {
            return false;
        }

        // Check for high failure rate
        long windowMs = System.currentTimeMillis() - metrics.firstRequestTime;
        double failureRate = metrics.failedApiCalls / Math.max(1, windowMs / 60000.0);
        
        if (failureRate > 20) { // More than 20 failures per minute
            log.warn("Abuse pattern detected for {}: {} failures/min", identifier, failureRate);
            return true;
        }

        // Check for switching user agents (likely bot trying different techniques)
        if (metrics.userAgents.size() > 5) {
            log.warn("Multiple user agents detected for {}: {}", identifier, metrics.userAgents.size());
            return true;
        }

        // Check for no referer (common in bots)
        if (metrics.userAgents.size() > 0 && 
            metrics.referrers.size() == 0 && 
            metrics.failedApiCalls > 5) {
            log.warn("No referer with multiple failures detected for {}", identifier);
            return true;
        }

        return false;
    }

    /**
     * Get abuse metrics for monitoring
     */
    public Map<String, Object> getAbuseMetrics(String identifier) {
        AbuseMetrics metrics = ipMetrics.get(identifier);
        Map<String, Object> result = new HashMap<>();
        
        if (metrics != null) {
            result.put("failures", metrics.failedApiCalls);
            result.put("userAgents", metrics.userAgents.size());
            result.put("referrers", metrics.referrers.size());
            result.put("firstRequest", metrics.firstRequestTime);
            result.put("showsAbuse", showsAbusePattern(identifier));
        }
        
        return result;
    }

    /**
     * Clear metrics for an identifier (for testing or after investigation)
     */
    public void clearMetrics(String identifier) {
        ipMetrics.remove(identifier);
        userMetrics.remove(identifier);
        log.info("Cleared abuse metrics for identifier: {}", identifier);
    }

    /**
     * Get all abuse metrics for monitoring/anomaly detection
     */
    public Map<String, Object> getAllAbuseMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("trackedIps", ipMetrics.size());
        metrics.put("trackedUsers", userMetrics.size());
        metrics.put("totalFailures", ipMetrics.values().stream()
            .mapToInt(m -> m.failedApiCalls).sum());
        return metrics;
    }

    /**
     * Import block list from external source (IPs known for abuse)
     * This can be integrated with services like AbuseIPDB
     */
    public static class BlockListEntry {
        public String ipAddress;
        public String reason;
        public long blockedUntil;
        
        public BlockListEntry(String ipAddress, String reason, long blockedUntilTime) {
            this.ipAddress = ipAddress;
            this.reason = reason;
            this.blockedUntil = blockedUntilTime;
        }

        public boolean isStillBlocked() {
            return System.currentTimeMillis() < blockedUntil;
        }
    }

    // Dynamic IP block list
    private final Map<String, BlockListEntry> blockedIps = new ConcurrentHashMap<>();

    /**
     * Add IP to block list
     */
    public void blockIp(String ipAddress, String reason, long blockDurationMs) {
        long blockedUntil = System.currentTimeMillis() + blockDurationMs;
        blockedIps.put(ipAddress, new BlockListEntry(ipAddress, reason, blockedUntil));
        log.warn("Blocked IP {} for reason: {} (duration: {} minutes)",
            ipAddress, reason, blockDurationMs / 60000);
    }

    /**
     * Check if IP is blocked
     */
    public boolean isIpBlocked(String ipAddress) {
        BlockListEntry entry = blockedIps.get(ipAddress);
        if (entry == null) {
            return false;
        }

        if (entry.isStillBlocked()) {
            log.debug("IP {} is blocked: {}", ipAddress, entry.reason);
            return true;
        }

        // Remove expired block
        blockedIps.remove(ipAddress);
        return false;
    }

    /**
     * Get IP block info
     */
    public BlockListEntry getIpBlockInfo(String ipAddress) {
        return blockedIps.get(ipAddress);
    }
}
