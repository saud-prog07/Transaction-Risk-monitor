# SECURITY HARDENING GUIDE

## Overview

This document outlines the security hardening measures implemented in the transaction risk monitoring system. All implementations follow **OWASP Top 10 2021** best practices.

---

## 1. RATE LIMITING (OWASP A04:2021 – Insecure Design)

### Implementation

**Global Rate Limiting Filter** (`GlobalRateLimitingFilter.java`)
- Protects all public and authenticated endpoints
- Uses token bucket algorithm (via Bucket4j) for smooth rate limiting
- Supports both IP-based and user-based rate limiting

### Default Rate Limits

| Endpoint Type | Rate Limit | Scope | Purpose |
|---------------|-----------|-------|---------|
| **Public Endpoints** | 100 req/min | Per IP | Prevents general abuse |
| **Auth Endpoints** | 5 req/min | Per IP | Prevents brute force attacks |
| **Authenticated Endpoints** | 1000 req/min | Per User | Prevents resource exhaustion |

### Response Headers

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1712700000
```

### Configuration

Edit `/backend/alert-service/src/main/java/com/example/riskmonitoring/alertservice/security/GlobalRateLimitingFilter.java`:

```java
private static final int PUBLIC_ENDPOINT_LIMIT = 100;        // Adjust as needed
private static final int AUTH_ENDPOINT_LIMIT = 5;             // Stricter for auth
private static final int AUTHENTICATED_ENDPOINT_LIMIT = 1000; // For logged-in users
```

### Graceful Degradation

- Rate-limited requests receive **429 Too Many Requests** response
- **Retry-After** header tells clients when to retry
- Clear error message in JSON response
- No blocking of legitimate traffic

---

## 2. INPUT VALIDATION & SANITIZATION (OWASP A03:2021 – Injection)

### Implementation

**Request Validation Filter** (`RequestValidationFilter.java`)
- Schema-based validation (defines allowed fields per endpoint)
- Type checking (ensures correct data types)
- Length limits (prevents buffer overflow)
- Rejects unexpected fields (reduces attack surface)
- SQL injection detection (pattern matching)
- XSS prevention (HTML/script tag detection)

### Validation Rules

#### Schema-Based Validation

Only allowed fields are accepted per endpoint:

```java
ALLOWED_FIELDS.put("/api/auth/login", Set.of("username", "password"));
ALLOWED_FIELDS.put("/api/auth/register", Set.of("username", "password", "passwordConfirm", "email"));
ALLOWED_FIELDS.put("/api/alerts", Set.of(
    "transactionId", "riskLevel", "riskScore", "userId", "amount", "reason"
));
```

**Example: Attack Prevention**

```json
// REJECTED - contains unexpected field
{
  "username": "attacker",
  "password": "pwd123",
  "admin": true  // ← Unexpected field, request rejected
}
```

#### Type Checking

- Ensures fields match expected types
- Rejects type mismatches

```java
// Numeric fields must be numbers
"id": "123"        // ✗ Rejected
"id": 123          // ✓ Accepted

// String fields must be strings
"reason": true     // ✗ Rejected
"reason": "test"   // ✓ Accepted
```

#### Length Limits

- Maximum body size: **10MB**
- Maximum string field length: **10,000 chars**
- Maximum JSON nesting depth: **10 levels**

#### Injection Prevention

**SQL Injection Detection:**
```regex
('|(\\-\\-)|(;)|(\\|\\|)|(\\*/)|(/\\*)|(xp_)|(sp_)|(exec)|(execute))
```

**XSS Detection:**
```regex
(<script|<iframe|<object|<embed|<img|javascript:|onerror=|onload=|onclick=)
```

### Configuration

Add endpoints to validation schema in `RequestValidationFilter.java`:

```java
// Add to ALLOWED_FIELDS static block
ALLOWED_FIELDS.put("/api/your-endpoint", Set.of("field1", "field2", ...));
```

### Response on Validation Failure

```json
HTTP/1.1 400 Bad Request

{
  "error": "Request validation failed. Invalid input format or content."
}
```

---

## 3. SECURE API KEY HANDLING (OWASP A02:2021 – Cryptographic Failures)

### Implementation

**API Key Manager** (`ApiKeyManager.java`)
- All keys stored in environment variables (NEVER hardcoded)
- Constant-time comparison to prevent timing attacks
- Key metadata tracking (creation, last used, expiration)
- Key rotation recording
- No keys exposed in logs

### Environment Variables

All API keys must be configured via environment variables:

```bash
# .env file (NEVER commit)
API_RISK_ENGINE_KEY=your-key-here
API_ALERT_NOTIFICATION_KEY=your-key-here
API_EXTERNAL_SERVICE_KEY=your-key-here
JWT_SECRET=your-jwt-secret-here
JWT_REFRESH_SECRET=your-refresh-secret-here
```

### Key Generation

Generate new keys with:

```bash
# Generate 256-bit (32 character) base64 key
openssl rand -base64 32

# Output example:
# aBcDeFgHiJkLmNoPqRsTuVwXyZ01234567890AB
```

### Access Patterns

**Safe - Returns masked info:**
```java
ApiKeyManager manager = new ApiKeyManager();
boolean isActive = manager.isKeyActive("risk-engine");  // ✓ Safe

// Response: true/false (no key exposed)
```

**Unsafe - Never do this:**
```java
String key = manager.getApiKey("risk-engine");  // ✓ Safe internally
log.info("Key: " + key);  // ✗ DANGER - logs the key!
response.getKey();        // ✗ DANGER - exposes key to client!
```

### Logging

Keys are NEVER logged:

```java
// ✓ Safe - only logs boolean
log.debug("API key accessed for service: {} (key configured: {})", 
    serviceName, key != null && !key.isEmpty());

// ✗ Wrong - NEVER do this
log.debug("API key: {}", key);  // SECURITY ISSUE!
```

### Key Rotation

Record rotation events for audit trail:

```java
manager.recordKeyRotation(
    "risk-engine",
    "Quarterly rotation",
    "admin@company.com"
);
```

### Configuration Validation

On startup, all required keys are validated:

```properties
# application.yaml
api:
  risk-engine:
    key: ${API_RISK_ENGINE_KEY:}  # Required
  alert-notification:
    key: ${API_ALERT_NOTIFICATION_KEY:}  # Required
jwt:
  secret: ${JWT_SECRET:}  # Required
  refresh-secret: ${JWT_REFRESH_SECRET:}  # Required
```

---

## 4. SECURITY HEADERS (OWASP A05:2021 – Security Misconfiguration)

### Implemented Headers

All responses include security headers:

```http
# Prevent clickjacking (XSS attacks)
X-Frame-Options: DENY

# Prevent MIME sniffing
X-Content-Type-Options: nosniff

# Enable browser XSS protection
X-XSS-Protection: 1; mode=block

# HTTP Strict Transport Security (HSTS)
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload

# Content Security Policy (CSP) - prevents XSS
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; 
    style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; 
    connect-src 'self'; frame-ancestors 'none'

# Referrer Policy - don't leak referrer to third parties
Referrer-Policy: strict-no-referrer

# Permissions Policy - restrict browser features
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
```

See `SecurityConfig.java` for configuration.

---

## 5. JWT BEST PRACTICES

### Token Generation

```java
// ✓ Secure token generation
JwtTokenProvider provider = new JwtTokenProvider();
String token = provider.generateToken(username, userId);

// Token contains:
// - Username (in subject claim)
// - User ID (in custom claim)
// - Issue time
// - Expiration (1 hour)
```

### Token Claims

```json
{
  "sub": "johndoe",
  "userId": "user-12345",
  "iat": 1712700000,
  "exp": 1712703600
}
```

**What's NOT included (for security):**
- Password ✗
- Email ✗
- Role (queried from database at request time) ✗
- Sensitive PII ✗

### Refresh Tokens

```java
// Generate refresh token (7 day expiration)
String refreshToken = refreshTokenService.generateRefreshToken(username, userId);

// Refresh token has separate signing key and longer expiration
// Can be rotated or revoked independently
```

### Token Validation

Tokens are validated for:
- Valid signature (prevents tampering)
- Not expired
- Correct issuer (if configured)
- Required claims present

---

## 6. AUTHENTICATION SECURITY

### Login Rate Limiting

Strict rate limiting prevents brute force:

```
POST /api/auth/login: 5 attempts per minute per IP
```

### Password Requirements

All passwords must be:
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 number
- At least 1 special character

See `PasswordValidator.java` for rules.

### Refresh Token Management

```java
// Refresh tokens are:
// - Stored in database
// - Can be revoked immediately
// - Checked for blacklist on each use
// - Have separate expiration from access tokens

refreshTokenService.revokeToken(refreshToken);  // Immediate revocation
```

---

## 7. ROLE-BASED ACCESS CONTROL (RBAC)

### Endpoint Protection

```java
// Require ADMIN role
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/api/admin/dlq/messages/retry")
public ResponseEntity<?> retryDLQMessage(...) {
    // Only ADMIN users can execute
}

// Require ANALYST or ADMIN role
@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
@GetMapping("/api/alerts")
public ResponseEntity<?> getAlerts(...) {
    // Both roles can execute
}

// Require any authenticated user
@Authenticated
@GetMapping("/api/transactions/trace/{id}")
public ResponseEntity<?> getTrace(...) {
    // Any authenticated user
}
```

### Role Hierarchy

```
ROLE_ADMIN
  ├── Full system access
  ├── DLQ management
  └── System configuration

ROLE_ANALYST
  ├── Alert investigation
  ├── Transaction viewing
  └── Report generation

ROLE_USER
  └── Basic read access
```

---

## 8. AUDIT LOGGING

All security events are logged:

```java
// Attempt: Invalid login
log.warn("Login failed for user: {}", username);

// Success: User registered
log.info("User registered successfully: {}", username);

// Admin action: DLQ retry
auditService.auditEvent("DLQ_RETRY_SUCCESS", "messageId", 
    messageId.toString(), "Retry initiated by admin", true);

// Rate limit exceeded
log.warn("Rate limit exceeded for {}", clientIp);

// Validation failure
log.warn("Request validation failed for: {} {}", method, endpoint);
```

---

## 9. DEPLOYMENT CHECKLIST

Before deploying to production:

### Security Configuration

- [ ] Set `JWT_SECRET` to strong random value (32+ chars)
- [ ] Set `JWT_REFRESH_SECRET` to strong random value (32+ chars)
- [ ] Set all `API_*_KEY` environment variables
- [ ] Set `DATABASE_PASSWORD` to strong value
- [ ] Set `MQ_PASSWORD` to strong value
- [ ] Set `REDIS_PASSWORD` (if using Redis)
- [ ] Review `CORS_ALLOWED_ORIGINS` - only include production domains
- [ ] Review rate limit values for production load

### HTTPS/TLS

- [ ] Enable HTTPS on all endpoints
- [ ] Use valid SSL certificate
- [ ] Redirect HTTP to HTTPS
- [ ] Set HSTS header (already configured)

### Network Security

- [ ] Database is not publicly accessible
- [ ] Message queue is not publicly accessible
- [ ] Only allow traffic from load balancer to backend
- [ ] Use Web Application Firewall (WAF) if available

### Monitoring

- [ ] Set up alerts for rate limit violations
- [ ] Monitor failed login attempts
- [ ] Monitor API key usage
- [ ] Monitor validation failures

### Secrets Management

- [ ] Use AWS Secrets Manager, Azure Key Vault, or HashiCorp Vault
- [ ] Do NOT commit `.env` files
- [ ] Rotate keys quarterly
- [ ] Audit key access logs
- [ ] Use different keys for each environment

---

## 10. OWASP MAPPING

### Implemented Controls

| OWASP ID | Vulnerability | Implementation | Status |
|----------|----------------|-----------------|--------|
| A01:2021 | Broken Access Control | RBAC + @PreAuthorize | ✓ Implemented |
| A02:2021 | Cryptographic Failures | ApiKeyManager + env vars | ✓ Implemented |
| A03:2021 | Injection | RequestValidationFilter | ✓ Implemented |
| A04:2021 | Insecure Design | GlobalRateLimitingFilter | ✓ Implemented |
| A05:2021 | Security Misconfiguration | SecurityConfig headers | ✓ Implemented |
| A06:2021 | Vulnerable Components | Dependency management | ✓ Partial* |
| A07:2021 | Identification Failures | JWT + refresh tokens | ✓ Implemented |
| A08:2021 | Software & Data Integrity | Signed JWT | ✓ Implemented |
| A09:2021 | Logging & Monitoring | StructuredLogger | ✓ Implemented |
| A10:2021 | SSRF | Input validation | ✓ Implemented |

*See Maven dependency management for CVE scanning

---

## 11. INCIDENT RESPONSE

### Security Event Detected

1. Check logs: `logs/application.log`
2. Verify in audit trail: `alert_audit_logs` table
3. Check metrics: Rate limit violations, auth failures
4. Take action: Block IP, revoke token, investigate

### Rate Limit Bypass Detected

```bash
# Check logs for unusual requests
grep "Rate limit exceeded" logs/application.log

# Identify pattern
# - Rotating IPs? → DDoS attack
# - Single IP, multiple users? → Compromised proxy
# - Single user, distributed IPs? → Botnet
```

---

## 12. MAINTENANCE & UPDATES

### Monthly Tasks

- [ ] Review security logs
- [ ] Check for dependency updates
- [ ] Verify rate limits are working
- [ ] Audit API key usage

### Quarterly Tasks

- [ ] Rotate API keys
- [ ] Rotate JWT secrets
- [ ] Update rate limit thresholds
- [ ] Security audit review

### Annually

- [ ] Security penetration test
- [ ] Update OWASP compliance
- [ ] Review encryption standards
- [ ] Compliance audit (GDPR, etc.)

---

##  References

- OWASP Top 10 2021: https://owasp.org/Top10/
- OWASP Cheat Sheets: https://cheatsheetseries.owasp.org/
- Spring Security Docs: https://docs.spring.io/spring-security/
- JWT Best Practices: https://tools.ietf.org/html/rfc8949
- Rate Limiting: https://en.wikipedia.org/wiki/Rate_limiting

---

**Last Updated:** April 9, 2026  
**Security Review:** Quarterly  
**Next Review:** July 9, 2026
