package com.example.riskmonitoring.alertservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Request Validation Filter - Strict input validation and sanitization
 * 
 * Enforces OWASP best practices:
 * - Schema-based validation (validates against expected fields)
 * - Type checking (ensures correct data types)
 * - Length limits (prevents buffer overflow)
 * - Rejects unexpected fields (reduces attack surface)
 * - SQL injection prevention (input sanitization)
 * - XSS prevention (HTML/script tag detection)
 * 
 * OWASP: A03:2021 – Injection
 * - Validates all user inputs
 * - Sanitizes before processing
 * - Rejects malicious patterns
 */
@Slf4j
@Component
public class RequestValidationFilter extends OncePerRequestFilter {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;  // 10MB
    private static final int MAX_STRING_LENGTH = 10000;
    private static final int MAX_NESTED_DEPTH = 10;
    
    // Patterns for detecting malicious input
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('|(\\-\\-)|(;)|(\\|\\|)|(\\*/)|(/\\*)|(xp_)|(sp_)|(exec)|(execute))",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(<script|<iframe|<object|<embed|<img|javascript:|onerror=|onload=|onclick=)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Define allowed fields for each endpoint (schema-based validation)
    private static final Map<String, Set<String>> ALLOWED_FIELDS = new HashMap<>();
    
    static {
        // Auth endpoints
        ALLOWED_FIELDS.put("/api/auth/login", Set.of("username", "password"));
        ALLOWED_FIELDS.put("/api/auth/register", Set.of("username", "password", "passwordConfirm", "email"));
        ALLOWED_FIELDS.put("/api/auth/refresh", Set.of("refreshToken"));
        
        // Alert endpoints
        ALLOWED_FIELDS.put("/api/alerts", Set.of(
            "transactionId", "riskLevel", "riskScore",  "userId", "amount", "reason"
        ));
        
        // DLQ retry endpoint
        ALLOWED_FIELDS.put("/api/admin/dlq/messages/retry", Set.of("messageId", "reason"));
        
        // Add more as needed...
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Skip validation for GET requests and non-JSON content
        if (!"POST".equalsIgnoreCase(request.getMethod()) && 
            !"PUT".equalsIgnoreCase(request.getMethod()) &&
            !"PATCH".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (!isJsonRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Wrap request to cache the body
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            
            // Validate request body
            boolean isValid = validateRequestBody(wrappedRequest);
            
            if (!isValid) {
                log.warn("Request validation failed for: {} {}", request.getMethod(), request.getRequestURI());
                sendValidationErrorResponse(response);
                return;
            }
            
            // Proceed with the request
            filterChain.doFilter(wrappedRequest, response);
            
        } catch (Exception e) {
            log.error("Error during request validation", e);
            sendValidationErrorResponse(response);
        }
    }
    
    /**
     * Validate request body against schema and security rules
     */
    private boolean validateRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] body = request.getContentAsByteArray();
            
            if (body.length == 0) {
                // Allow empty body for some endpoints
                return true;
            }
            
            // Check maximum body size
            if (body.length > MAX_BODY_SIZE) {
                log.warn("Request body exceeds maximum size: {} bytes", body.length);
                return false;
            }
            
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(bodyStr);
            
            // Validate structure
            if (!jsonNode.isObject() && !jsonNode.isArray()) {
                log.warn("Invalid JSON structure: not an object or array");
                return false;
            }
            
            // Validate depth to prevent DoS
            if (getJsonDepth(jsonNode) > MAX_NESTED_DEPTH) {
                log.warn("JSON nesting depth exceeds maximum: {}", MAX_NESTED_DEPTH);
                return false;
            }
            
            // Validate fields based on endpoint
            String endpoint = getEndpointPath(request);
            if (ALLOWED_FIELDS.containsKey(endpoint)) {
                if (!validateAllowedFields(jsonNode, ALLOWED_FIELDS.get(endpoint))) {
                    log.warn("Request contains unexpected fields for endpoint: {}", endpoint);
                    return false;
                }
            }
            
            // Validate field content
            return validateFieldContent(jsonNode);
            
        } catch (Exception e) {
            log.warn("Error parsing request body: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate that only allowed fields are present
     */
    private boolean validateAllowedFields(JsonNode node, Set<String> allowedFields) {
        if (!node.isObject()) {
            return true;
        }
        
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!allowedFields.contains(fieldName)) {
                log.warn("Unexpected field in request: {}", fieldName);
                return false;  // Reject unexpected fields
            }
        }
        
        return true;
    }
    
    /**
     * Validate field content (type, length, dangerous patterns)
     */
    private boolean validateFieldContent(JsonNode node) {
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = node.get(fieldName);
                if (!validateFieldValue(fieldValue)) {
                    return false;
                }
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                if (!validateFieldValue(node.get(i))) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Validate individual field value
     */
    private boolean validateFieldValue(JsonNode value) {
        if (value.isTextual()) {
            String stringValue = value.asText();
            
            // Check length
            if (stringValue.length() > MAX_STRING_LENGTH) {
                log.warn("Field value exceeds maximum length: {}", stringValue.length());
                return false;
            }
            
            // Check for SQL injection
            if (SQL_INJECTION_PATTERN.matcher(stringValue).find()) {
                log.warn("Potential SQL injection detected in field value");
                return false;
            }
            
            // Check for XSS
            if (XSS_PATTERN.matcher(stringValue).find()) {
                log.warn("Potential XSS detected in field value");
                return false;
            }
            
        } else if (value.isObject() || value.isArray()) {
            return validateFieldContent(value);
        }
        
        return true;
    }
    
    /**
     * Get maximum nesting depth of JSON structure
     */
    private int getJsonDepth(JsonNode node) {
        if (node.isLeaf()) {
            return 1;
        }
        
        int maxDepth = 1;
        if (node.isObject()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                maxDepth = Math.max(maxDepth, 1 + getJsonDepth(elements.next()));
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                maxDepth = Math.max(maxDepth, 1 + getJsonDepth(node.get(i)));
            }
        }
        
        return maxDepth;
    }
    
    /**
     * Get endpoint path from request
     */
    private String getEndpointPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Remove version prefix if present
        if (path.contains("/api/")) {
            return path.substring(path.indexOf("/api/"));
        }
        return path;
    }
    
    /**
     * Check if request is JSON
     */
    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
    
    /**
     * Send validation error response
     */
    private void sendValidationErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\": \"Request validation failed. Invalid input format or content.\"}"
        );
    }
}
