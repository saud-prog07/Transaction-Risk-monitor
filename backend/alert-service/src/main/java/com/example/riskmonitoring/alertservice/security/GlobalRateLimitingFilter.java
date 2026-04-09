package com.example.riskmonitoring.alertservice.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global Rate Limiting Filter - IP and user-based rate limiting
 * 
 * Features:
 * - Per-IP rate limiting (prevents DDoS attacks)
 * - Per-user rate limiting (prevents brute force)
 * - Graceful 429 responses with Retry-After header
 * - Different limits for public vs. authenticated endpoints
 * - Token bucket algorithm via Bucket4j
 * 
 * OWASP: A04:2021 – Insecure Design
 * - Rate limiting prevents brute force/DDoS attacks
 * - Implements sensible defaults
 * - Graceful degradation
 * 
 * Default Limits:
 * - Public endpoints: 100 requests/minute per IP
 * - Auth endpoints: 5 requests/minute per IP
 * - Authenticated endpoints: 1000 requests/minute per user
 */
@Slf4j
@Component
public class GlobalRateLimitingFilter extends OncePerRequestFilter {
    
    // Token buckets keyed by IP or user ID
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();
    
    // Rate limit configurations (requests per minute)
    private static final int PUBLIC_ENDPOINT_LIMIT = 100;      // 100 req/min per IP
    private static final int AUTH_ENDPOINT_LIMIT = 5;          // 5 req/min per IP (prevent brute force)
    private static final int AUTHENTICATED_ENDPOINT_LIMIT = 1000;  // 1000 req/min per user
    
    // Paths that have special rate limiting rules
    private static final String[] AUTH_PATHS = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot-password",
        "/api/auth/reset-password"
    };
    
    private static final String[] ADMIN_PATHS = {
        "/api/admin/"
    };
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Get client IP address (accounting for proxies)
            String clientIp = getClientIpAddress(request);
            String username = getUsername(request);
            
            // Determine which rate limit bucket to use
            if (!isRateLimitApplicable(request)) {
                // Skip rate limiting for excluded paths
                filterChain.doFilter(request, response);
                return;
            }
            
            // Apply rate limiting
            if (!checkRateLimit(request, clientIp, username)) {
                // Rate limit exceeded - send 429 response
                sendRateLimitExceededResponse(response);
                return;
            }
            
            // Proceed with request
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            // Don't block request on filter error - just log and continue
            try {
                filterChain.doFilter(request, response);
            } catch (Exception ex) {
                log.error("Error proceeding after rate limit filter failure", ex);
            }
        }
    }
    
    /**
     * Check if rate limit has been exceeded
     * Uses token bucket algorithm for smooth rate limiting
     */
    private boolean checkRateLimit(HttpServletRequest request, String clientIp, String username) {
        String path = request.getRequestURI();
        
        // Authenticated endpoints - rate limit by user
        if (username != null && !username.equals("anonymousUser")) {
            Bucket userBucket = userBuckets.computeIfAbsent(
                username,
                key -> createBucket(AUTHENTICATED_ENDPOINT_LIMIT)
            );
            
            if (!userBucket.tryConsume(1)) {
                log.warn("User rate limit exceeded: {} ({})", username, clientIp);
                return false;
            }
            return true;
        }
        
        // Auth endpoints - strict IP-based limiting
        if (isAuthEndpoint(path)) {
            Bucket ipBucket = ipBuckets.computeIfAbsent(
                clientIp + "_auth",
                key -> createBucket(AUTH_ENDPOINT_LIMIT)
            );
            
            if (!ipBucket.tryConsume(1)) {
                log.warn("Auth endpoint rate limit exceeded: {}", clientIp);
                return false;
            }
            return true;
        }
        
        // Public endpoints - standard IP-based limiting
        Bucket ipBucket = ipBuckets.computeIfAbsent(
            clientIp,
            key -> createBucket(PUBLIC_ENDPOINT_LIMIT)
        );
        
        if (!ipBucket.tryConsume(1)) {
            log.warn("Public endpoint rate limit exceeded: {}", clientIp);
            return false;
        }
        
        return true;
    }
    
    /**
     * Create a token bucket for rate limiting
     * Uses Bandwidth configuration for smooth rate limiting
     */
    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute, Refill.intervally(
            requestsPerMinute,
            Duration.ofMinutes(1)
        ));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
    
    /**
     * Check if rate limiting should be applied to this request
     */
    private boolean isRateLimitApplicable(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip health/actuator endpoints
        if (path.contains("/health") || path.contains("/actuator") || 
            path.contains("/metrics") || path.contains("/swagger-ui")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if this is an authentication endpoint
     */
    private boolean isAuthEndpoint(String path) {
        for (String authPath : AUTH_PATHS) {
            if (path.contains(authPath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get client IP address accounting for proxies
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check X-Forwarded-For header (from reverse proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take first IP in the list
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fall back to remote address
        return request.getRemoteAddr();
    }
    
    /**
     * Get authenticated username from request
     */
    private String getUsername(HttpServletRequest request) {
        try {
            if (request.getUserPrincipal() != null) {
                return request.getUserPrincipal().getName();
            }
        } catch (Exception e) {
            log.debug("Error getting username from request", e);
        }
        return null;
    }
    
    /**
     * Send 429 Too Many Requests response
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // Add Retry-After header (seconds)
        response.setHeader("Retry-After", "60");
        
        // Add rate limit headers for client awareness
        response.setHeader("X-RateLimit-Limit", String.valueOf(PUBLIC_ENDPOINT_LIMIT));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf((System.currentTimeMillis() / 1000) + 60));
        
        response.getWriter().write(
            "{\"error\": \"Rate limit exceeded. Please try again later.\", " +
            "\"retryAfter\": 60}"
        );
    }
}
