package com.example.riskmonitoring.alertservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security Headers Filter
 * Implements OWASP recommended security headers for all HTTP responses
 */
@Slf4j
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Prevent Clickjacking (UI Redressing)
        response.setHeader("X-Frame-Options", "DENY");
        
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Enable XSS Protection (browser-based)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Content Security Policy - strict enforcement
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self'; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'"
        );
        
        // Referrer Policy - prevent referrer leakage
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Feature/Permissions Policy - disable dangerous features
        response.setHeader("Permissions-Policy", 
            "geolocation=(), " +
            "microphone=(), " +
            "camera=(), " +
            "payment=(), " +
            "usb=(), " +
            "vr=(), " +
            "xr-spatial-tracking=()"
        );
        
        // HTTPS Strict Transport Security (HSTS)
        // 1 year max-age, include subdomains, preload (standard)
        response.setHeader("Strict-Transport-Security",
            "max-age=31536000; includeSubDomains; preload"
        );
        
        // Prevent caching of sensitive data
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        // Additional headers for API responses
        if (isApiRequest(request)) {
            // No content sniffing for API responses
            response.setHeader("X-Content-Type-Options", "nosniff");
            
            // Disable DNS prefetching
            response.setHeader("X-DNS-Prefetch-Control", "off");
            
            // Disable caching for API responses
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        }
        
        // Prevent browser download helpers
        response.setHeader("X-Download-Options", "noopen");
        
        filterChain.doFilter(request, response);
    }

    /**
     * Check if this is an API request (vs static content)
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/") || 
               request.getContentType() != null && 
               request.getContentType().contains("application/json");
    }
}
