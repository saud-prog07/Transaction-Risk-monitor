package com.example.riskmonitoring.alertservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Traffic Monitoring Filter
 * Detects and logs unusual traffic patterns in real-time
 */
@Slf4j
@Component
public class TrafficMonitoringFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private UnusualTrafficDetectionService trafficDetectionService;

    private static final Map<String, IPTrafficMetrics> trafficMetricsMap = 
        new ConcurrentHashMap<>();
    
    private static final int HIGH_REQUEST_THRESHOLD = 100; // requests per minute
    private static final int ENDPOINT_SCANNING_THRESHOLD = 20; // unique endpoints
    private static final long TIME_WINDOW_MS = 60000; // 1 minute

    private static class IPTrafficMetrics {
        AtomicInteger requestCount = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
        Set<String> uniqueEndpoints = new HashSet<>();
        AtomicInteger failedAuthAttempts = new AtomicInteger(0);
        Set<String> attemptedUsernames = new HashSet<>();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String ipAddress = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        try {
            // Skip monitoring for health checks and metrics
            if (isExcludedPath(endpoint)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Track traffic metrics
            IPTrafficMetrics metrics = trafficMetricsMap.computeIfAbsent(ipAddress, 
                k -> new IPTrafficMetrics());

            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - metrics.windowStart;

            // Reset metrics if window expired (1 minute)
            if (elapsed > TIME_WINDOW_MS) {
                synchronized (metrics) {
                    if (currentTime - metrics.windowStart > TIME_WINDOW_MS) {
                        metrics.requestCount.set(0);
                        metrics.uniqueEndpoints.clear();
                        metrics.failedAuthAttempts.set(0);
                        metrics.attemptedUsernames.clear();
                        metrics.windowStart = currentTime;
                    }
                }
            }

            // Increment request count
            int requestCount = metrics.requestCount.incrementAndGet();
            metrics.uniqueEndpoints.add(endpoint);

            // Detect high request rate (DDoS-like behavior)
            if (requestCount > HIGH_REQUEST_THRESHOLD && trafficDetectionService != null) {
                trafficDetectionService.logDdosBehavior(ipAddress, requestCount, 
                    TIME_WINDOW_MS / 1000);
            }

            // Detect endpoint scanning
            if (metrics.uniqueEndpoints.size() > ENDPOINT_SCANNING_THRESHOLD && 
                trafficDetectionService != null) {
                trafficDetectionService.logEndpointScanning(ipAddress, 
                    metrics.uniqueEndpoints.size());
            }

            // Check for suspicious methods (DEBUG, TRACE on non-development endpoints)
            if (isSuspiciousMethod(method, endpoint) && trafficDetectionService != null) {
                trafficDetectionService.logSuspiciousHttpMethod(ipAddress, method, endpoint);
            }

            // Check for SQL injection patterns
            if (hasSqlInjectionPattern(request) && trafficDetectionService != null) {
                extractAndLogSqlInjectionAttempt(request, ipAddress);
            }

            // Check for XSS patterns
            if (hasXssPattern(request) && trafficDetectionService != null) {
                extractAndLogXssAttempt(request, ipAddress);
            }

            // Check for path traversal
            if (hasPathTraversalPattern(endpoint) && trafficDetectionService != null) {
                trafficDetectionService.logPathTraversalAttempt(ipAddress, endpoint);
            }

            // Proceed with request
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error in traffic monitoring filter", e);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Check if endpoint should be excluded from monitoring
     */
    private boolean isExcludedPath(String path) {
        return path.equals("/health") ||
               path.equals("/metrics") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/static/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".ico");
    }

    /**
     * Check for suspicious HTTP methods
     */
    private boolean isSuspiciousMethod(String method, String endpoint) {
        // DEBUG, TRACE should not be on production endpoints
        if (("DEBUG".equals(method) || "TRACE".equals(method)) && 
            !endpoint.startsWith("/admin/")) {
            return true;
        }
        
        // PUT/DELETE on unexpected endpoints
        return false;
    }

    /**
     * Check for SQL injection patterns in request
     */
    private boolean hasSqlInjectionPattern(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString != null && hasSqlPattern(queryString)) {
            return true;
        }
        
        // Check request body if JSON (would need to parse, skipping for simplicity)
        return false;
    }

    /**
     * Check for XSS patterns in request
     */
    private boolean hasXssPattern(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString != null && hasXssPatterns(queryString)) {
            return true;
        }
        return false;
    }

    /**
     * Check for path traversal patterns
     */
    private boolean hasPathTraversalPattern(String path) {
        return path.contains("../") ||
               path.contains("..\\") ||
               path.contains("%2e%2e") ||
               path.contains("%252e") ||
               path.contains("....//");
    }

    /**
     * Detailed SQL injection pattern detection
     */
    private boolean hasSqlPattern(String input) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("union") ||
               lowerInput.contains("select") ||
               lowerInput.contains("insert") ||
               lowerInput.contains("update") ||
               lowerInput.contains("delete") ||
               lowerInput.contains("drop") ||
               lowerInput.contains("exec") ||
               lowerInput.contains("execute") ||
               lowerInput.contains("script") ||
               lowerInput.contains("javascript");
    }

    /**
     * Detailed XSS pattern detection
     */
    private boolean hasXssPatterns(String input) {
        return input.contains("<script") ||
               input.contains("javascript:") ||
               input.contains("onerror=") ||
               input.contains("onload=") ||
               input.contains("<iframe") ||
               input.contains("<img") ||
               input.contains("eval(") ||
               input.contains("expression(");
    }

    /**
     * Extract and log SQL injection attempt
     */
    private void extractAndLogSqlInjectionAttempt(HttpServletRequest request, String ipAddress) {
        if (trafficDetectionService != null) {
            String queryString = request.getQueryString();
            if (queryString != null) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (hasSqlPattern(param)) {
                        String[] kv = param.split("=");
                        String paramName = kv.length > 0 ? kv[0] : "unknown";
                        trafficDetectionService.logSqlInjectionAttempt(ipAddress, paramName, param);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Extract and log XSS attempt
     */
    private void extractAndLogXssAttempt(HttpServletRequest request, String ipAddress) {
        if (trafficDetectionService != null) {
            String queryString = request.getQueryString();
            if (queryString != null) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (hasXssPatterns(param)) {
                        String[] kv = param.split("=");
                        String paramName = kv.length > 0 ? kv[0] : "unknown";
                        trafficDetectionService.logXssAttempt(ipAddress, paramName, param);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Extract client IP address from request (handles proxies)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check X-Forwarded-For header first (proxy scenario)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Fallback to X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        
        // Last resort: remote address
        return request.getRemoteAddr();
    }

    /**
     * Cleanup old metrics periodically (runs when filter is destroyed)
     */
    @Override
    public void destroy() {
        trafficMetricsMap.clear();
    }
}
