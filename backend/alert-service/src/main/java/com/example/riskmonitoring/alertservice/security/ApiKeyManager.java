package com.example.riskmonitoring.alertservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Key Manager - Securely manages API keys for external integrations
 * 
 * Security Features:
 * - All keys stored in environment variables (never hardcoded)
 * - Key rotation tracking
 * - Active/inactive key management
 * - Access logging
 * - No keys exposed in logs or responses
 * 
 * OWASP: A02:2021 – Cryptographic Failures
 * - Never store secrets in code
 * - Rotate keys regularly
 * - Use environment variables or secure vaults
 */
@Slf4j
@Service
@EnableScheduling
public class ApiKeyManager {
    
    // ======================== EXTERNAL SERVICE KEYS ========================
    // All keys should be set via environment variables, NEVER hardcoded
    
    @Value("${api.risk-engine.key:}")
    private String riskEngineApiKey;
    
    @Value("${api.alert-notification.key:}")
    private String alertNotificationApiKey;
    
    @Value("${api.external-service.key:}")
    private String externalServiceApiKey;
    
    // ======================== KEY METADATA ========================
    private final Map<String, ApiKeyMetadata> keyMetadata = new ConcurrentHashMap<>();
    private final Map<String, ApiKeyRotationRecord> rotationHistory = new ConcurrentHashMap<>();
    
    /**
     * Metadata about an API key
     */
    public static class ApiKeyMetadata {
        public String keyName;
        public boolean isActive = true;
        public Instant createdAt;
        public Instant lastUsedAt;
        public Instant expiresAt;
        public String status;  // ACTIVE, ROTATED, REVOKED
        
        public ApiKeyMetadata(String keyName) {
            this.keyName = keyName;
            this.createdAt = Instant.now();
            this.status = "ACTIVE";
        }
    }
    
    /**
     * Record of API key rotation events
     */
    public static class ApiKeyRotationRecord {
        public String keyName;
        public Instant rotatedAt;
        public String reason;
        public String rotatedBy;
        
        public ApiKeyRotationRecord(String keyName, String reason, String rotatedBy) {
            this.keyName = keyName;
            this.rotatedAt = Instant.now();
            this.reason = reason;
            this.rotatedBy = rotatedBy;
        }
    }
    
    /**
     * Constructor - Validates that all required keys are configured
     */
    public ApiKeyManager() {
        // Validation happens in init method to allow Spring initialization
    }
    
    /**
     * Validates that all API keys are properly configured
     * Should be called after constructor through @PostConstruct
     */
    public void validateApiKeysConfigured() {
        if (riskEngineApiKey == null || riskEngineApiKey.isEmpty()) {
            log.warn("API_RISK_ENGINE_KEY not configured. External risk-engine calls will fail.");
        }
        if (alertNotificationApiKey == null || alertNotificationApiKey.isEmpty()) {
            log.warn("API_ALERT_NOTIFICATION_KEY not configured. Notifications may not work.");
        }
        if (externalServiceApiKey == null || externalServiceApiKey.isEmpty()) {
            log.warn("API_EXTERNAL_SERVICE_KEY not configured. External service calls will fail.");
        }
    }
    
    /**
     * Get API key for a service
     * NEVER logs the actual key value
     * 
     * @param serviceName the service name
     * @return the API key (or empty string if not configured)
     */
    public String getApiKey(String serviceName) {
        String key = null;
        
        switch (serviceName.toLowerCase()) {
            case "risk-engine":
                key = riskEngineApiKey;
                break;
            case "alert-notification":
                key = alertNotificationApiKey;
                break;
            case "external-service":
                key = externalServiceApiKey;
                break;
            default:
                log.warn("Unknown service: {}", serviceName);
                return "";
        }
        
        // Track key usage (without exposing the key)
        ApiKeyMetadata metadata = keyMetadata.computeIfAbsent(serviceName, 
            name -> new ApiKeyMetadata(name));
        metadata.lastUsedAt = Instant.now();
        
        log.debug("API key accessed for service: {} (key configured: {})", 
            serviceName, key != null && !key.isEmpty());
        
        return key != null ? key : "";
    }
    
    /**
     * Check if an API key is valid and active
     * 
     * @param serviceName the service name
     * @return true if key exists and is active
     */
    public boolean isKeyActive(String serviceName) {
        String key = getApiKey(serviceName);
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        ApiKeyMetadata metadata = keyMetadata.get(serviceName);
        if (metadata == null) {
            return false;
        }
        
        // Check if key is still within validity period
        if (metadata.expiresAt != null && Instant.now().isAfter(metadata.expiresAt)) {
            log.warn("API key for {} has expired at {}", serviceName, metadata.expiresAt);
            metadata.status = "EXPIRED";
            return false;
        }
        
        return metadata.isActive && "ACTIVE".equals(metadata.status);
    }
    
    /**
     * Validate incoming API key from client
     * 
     * @param incomingKey the key from request
     * @param expectedServiceKey the expected key for comparison
     * @return true if keys match
     */
    public boolean validateIncomingKey(String incomingKey, String expectedServiceKey) {
        if (incomingKey == null || expectedServiceKey == null) {
            return false;
        }
        
        // Use constant-time comparison to prevent timing attacks
        return constantTimeEquals(incomingKey, expectedServiceKey);
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks
     * 
     * @param a first string
     * @param b second string
     * @return true if strings are equal
     */
    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        
        int result = aBytes.length == bBytes.length ? 0 : 1;
        for (int i = 0; i < Math.min(aBytes.length, bBytes.length); i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
    
    /**
     * Record a key rotation event
     * Should be triggered when keys are rotated
     * 
     * @param serviceName the service name
     * @param reason rotation reason
     * @param rotatedBy user who initiated rotation
     */
    public void recordKeyRotation(String serviceName, String reason, String rotatedBy) {
        ApiKeyRotationRecord record = new ApiKeyRotationRecord(serviceName, reason, rotatedBy);
        rotationHistory.put(serviceName + "_" + System.currentTimeMillis(), record);
        
        log.info("API key rotation recorded for service: {} (reason: {})", serviceName, reason);
    }
    
    /**
     * Get key usage statistics
     * 
     * @return map of key metadata
     */
    public Map<String, ApiKeyMetadata> getKeyMetadata() {
        // Return a copy to prevent external modifications
        return new HashMap<>(keyMetadata);
    }
    
    /**
     * Get key rotation history
     * 
     * @return map of rotation records
     */
    public Map<String, ApiKeyRotationRecord> getRotationHistory() {
        return new HashMap<>(rotationHistory);
    }
}
