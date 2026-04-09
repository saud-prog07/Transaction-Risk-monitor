package com.example.riskmonitoring.alertservice.aspect;

import com.example.riskmonitoring.alertservice.annotation.RateLimitProtected;
import com.example.riskmonitoring.alertservice.annotation.RateLimitType;
import com.example.riskmonitoring.alertservice.exception.AbuseDetectedException;
import com.example.riskmonitoring.alertservice.exception.RateLimitExceededException;
import com.example.riskmonitoring.alertservice.service.AbuseDetectionService;
import com.example.riskmonitoring.alertservice.service.RateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * Aspect for handling @RateLimitProtected annotation
 * Intercepts controller methods and applies rate limiting
 */
@Slf4j
@Aspect
@Component
public class RateLimitingAspect {

    private final RateLimitingService rateLimitingService;
    private final AbuseDetectionService abuseDetectionService;

    public RateLimitingAspect(RateLimitingService rateLimitingService,
                            AbuseDetectionService abuseDetectionService) {
        this.rateLimitingService = rateLimitingService;
        this.abuseDetectionService = abuseDetectionService;
    }

    /**
     * Intercept methods annotated with @RateLimitProtected
     */
    @Before("@annotation(rateLimitProtected)")
    public void enforceRateLimit(JoinPoint joinPoint, RateLimitProtected rateLimitProtected) {
        HttpServletRequest request = getHttpServletRequest();

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // Extract identifier based on rate limit type
        String identifier = extractIdentifier(rateLimitProtected, clientIp, request);

        // Log rate limit enforcement
        log.debug("Rate limit check - Type: {}, Identifier: {}, IP: {}",
            rateLimitProtected.type(), identifier, clientIp);

        // Validate user agent if configured
        if (rateLimitProtected.validateUserAgent()) {
            if (abuseDetectionService.isSuspiciousUserAgent(userAgent)) {
                log.warn("Suspicious user agent for {}: {}", identifier, userAgent);
                
                if (rateLimitProtected.blockBots()) {
                    abuseDetectionService.trackRequest(clientIp, true, userAgent, null);
                    throw new AbuseDetectedException(
                        "Suspicious activity detected",
                        "SUSPICIOUS_USER_AGENT",
                        clientIp
                    );
                }
            }
        }

        // Check rate limit based on type
        boolean allowRequest = checkRateLimit(
            rateLimitProtected.type(),
            identifier,
            rateLimitProtected.requestsPerMinute()
        );

        if (!allowRequest) {
            log.warn("Rate limit exceeded for {} ({} attempts)",
                identifier, rateLimitProtected.type());
            abuseDetectionService.trackRequest(clientIp, true, userAgent, null);
            
            throw new RateLimitExceededException(
                "Rate limit exceeded. Please try again later.",
                identifier,
                60
            );
        }

        // Check for abuse patterns
        if (rateLimitProtected.blockBots() && abuseDetectionService.showsAbusePattern(clientIp)) {
            log.error("Abuse pattern detected for {} ({})", clientIp, rateLimitProtected.type());
            abuseDetectionService.blockIp(clientIp, "Abuse pattern detected", 3600000);
            
            throw new AbuseDetectedException(
                "Access denied due to suspicious activity",
                "ABUSE_PATTERN",
                clientIp
            );
        }
    }

    /**
     * Extract identifier for rate limiting based on type
     */
    private String extractIdentifier(RateLimitProtected rateLimitProtected,
                                     String clientIp,
                                     HttpServletRequest request) {
        switch (rateLimitProtected.type()) {
            case LOGIN:
                // For login, try to extract username from request parameters
                String username = request.getParameter("username");
                if (username != null && !username.isEmpty()) {
                    return "login_" + username;
                }
                return "login_" + clientIp;

            case REGISTRATION:
                // For registration, use IP address to prevent account creation abuse
                return "registration_" + clientIp;

            case AI_GENERATION:
                // For AI generation, use user ID if authenticated, otherwise IP
                Principal principal = request.getUserPrincipal();
                if (principal != null) {
                    return "ai_" + principal.getName();
                }
                return "ai_" + clientIp;

            case GLOBAL:
            default:
                return clientIp;
        }
    }

    /**
     * Check rate limit based on type
     */
    private boolean checkRateLimit(RateLimitType type, String identifier, int customLimit) {
        switch (type) {
            case LOGIN:
                return rateLimitingService.checkLoginRateLimit(identifier);

            case REGISTRATION:
                return rateLimitingService.checkRegistrationRateLimit(identifier);

            case AI_GENERATION:
                return rateLimitingService.checkAIGenerationRateLimit(identifier);

            case GLOBAL:
            default:
                return rateLimitingService.checkGlobalRateLimit(identifier);
        }
    }

    /**
     * Get HTTP servlet request from request context
     */
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

    /**
     * Extract client IP address from request (handles proxies)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
