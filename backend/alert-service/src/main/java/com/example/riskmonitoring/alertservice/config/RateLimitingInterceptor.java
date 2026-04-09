package com.example.riskmonitoring.alertservice.config;

import com.example.riskmonitoring.alertservice.service.AbuseDetectionService;
import com.example.riskmonitoring.alertservice.service.RateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate Limiting and Abuse Detection Interceptor
 * Applies rate limiting and abuse detection to all HTTP requests
 * 
 * Features:
 * - IP-based rate limiting for general API endpoints
 * - User-agent validation
 * - IP blocking for known abusers
 * - Request logging for security audit
 */
@Slf4j
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;
    private final AbuseDetectionService abuseDetectionService;

    public RateLimitingInterceptor(RateLimitingService rateLimitingService,
                                  AbuseDetectionService abuseDetectionService) {
        this.rateLimitingService = rateLimitingService;
        this.abuseDetectionService = abuseDetectionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String remoteIp = getClientIpAddress(request);
        String requestUri = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");

        // Skip rate limiting for health check and metrics endpoints
        if (isExcludedPath(requestUri)) {
            return true;
        }

        // Check if IP is blocked
        if (abuseDetectionService.isIpBlocked(remoteIp)) {
            AbuseDetectionService.BlockListEntry blockInfo = abuseDetectionService.getIpBlockInfo(remoteIp);
            log.warn("Blocked request from blocked IP {}: {} (reason: {})",
                remoteIp, requestUri, blockInfo.reason);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("IP is blocked due to abuse: " + blockInfo.reason);
            return false;
        }

        // Check for suspicious user agent
        if (abuseDetectionService.isSuspiciousUserAgent(userAgent)) {
            log.warn("Suspicious user agent detected: {} from IP {}", userAgent, remoteIp);
            // Log the attempt but don't block (we'll track pattern)
            abuseDetectionService.trackRequest(remoteIp, true, userAgent, null);
        }

        // Check global rate limit
        if (!rateLimitingService.checkGlobalRateLimit(remoteIp)) {
            log.warn("Rate limit exceeded for IP {} on endpoint: {}", remoteIp, requestUri);
            response.setStatus(429);  // 429 Too Many Requests
            response.setHeader("Retry-After", "60");
            response.getWriter().write("Rate limit exceeded. Please try again later.");
            
            // Track as failure for abuse detection
            abuseDetectionService.trackRequest(remoteIp, true, userAgent, null);
            return false;
        }

        // Add rate limit info to response headers
        RateLimitingService.RateLimitMetrics metrics = rateLimitingService.getGlobalMetrics(remoteIp);
        response.setHeader("X-RateLimit-Limit", String.valueOf(metrics.getRequestsAllowed()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(metrics.getRequestsRemaining()));

        // Check for abuse patterns
        if (abuseDetectionService.showsAbusePattern(remoteIp)) {
            log.error("Abuse pattern detected for IP: {}", remoteIp);
            abuseDetectionService.blockIp(remoteIp, "Abuse pattern detected", 3600000); // 1 hour
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Access denied due to abuse patterns.");
            return false;
        }

        return true;
    }

    /**
     * Extract client IP address from request
     * Considers X-Forwarded-For (for proxies) and X-Real-IP headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check X-Forwarded-For for proxied requests
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP (common in nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Check if path is excluded from rate limiting
     */
    private boolean isExcludedPath(String requestUri) {
        return requestUri.contains("/health") ||
               requestUri.contains("/metrics") ||
               requestUri.contains("/actuator") ||
               requestUri.contains("/static") ||
               requestUri.contains("/css") ||
               requestUri.contains("/js");
    }
}
