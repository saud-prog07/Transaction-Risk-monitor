# Production Enhancement Implementation Guide

This guide provides step-by-step instructions for integrating the new production-level features into your microservices.

## Overview of New Components

### 1. Structured Logging (`StructuredLogger`)
**Location**: `common-models/logging/StructuredLogger.java`

**Purpose**: Centralized, consistent structured logging across all services

**Usage**:
```java
StructuredLogger logger = StructuredLogger.getLogger(MyService.class);

// Log transaction received
logger.logTransactionReceived(transactionId, userId, amount);

// Log success
logger.logValidationSuccess(transactionId);

// Log error
logger.logRetryAttempt(transactionId, 1, "Connection timeout");
```

---

### 2. Idempotency Service (`IdempotencyService`)
**Location**: `common-models/idempotency/IdempotencyService.java`

**Purpose**: Prevent duplicate transaction processing

**Usage**:
```java
IdempotencyService idempotencyService = IdempotencyService.getInstance();

// Check if transaction is duplicate
if (idempotencyService.isDuplicate(txId, userId, amount, location)) {
    logger.logDuplicateDetected(txId, existingId);
    return existingResult;
}

// Record transaction after processing
idempotencyService.recordTransaction(txId, userId, amount, location, "SUCCESS");
```

---

### 3. Input Validation (`TransactionValidator`)
**Location**: `common-models/validation/TransactionValidator.java`

**Purpose**: Validate transaction input with detailed error messages

**Usage**:
```java
TransactionValidator validator = new TransactionValidator();

if (!validator.validate(userId, amount, location)) {
    ValidationError error = new ValidationError(400, "Validation failed", "/transaction");
    for (String err : validator.getErrors()) {
        error.addFieldError("input", err);
    }
    return ResponseEntity.badRequest().body(error);
}
```

---

### 4. Rate Limiter (`RateLimiter`)
**Location**: `common-models/ratelimiting/RateLimiter.java`

**Purpose**: Prevent API abuse - limit requests per user

**Configuration**:
- Default: 60 requests per minute per user
- Burst size: 5 requests
- Token bucket algorithm

**Usage**:
```java
RateLimiter rateLimiter = RateLimiter.getInstance();

if (!rateLimiter.allowRequest(userId)) {
    RateLimitStatus status = rateLimiter.getStatus(userId);
    return ResponseEntity
        .status(HttpStatus.TOO_MANY_REQUESTS)
        .header("X-RateLimit-Reset", String.valueOf(status.resetTimeMs))
        .body(new ErrorResponse("Rate limit exceeded"));
}
```

---

### 5. Transaction Monitor (`TransactionMonitor`)
**Location**: `common-models/monitoring/TransactionMonitor.java`

**Purpose**: Track metrics: processed transactions, errors, alerts, performance

**Usage**:
```java
TransactionMonitor monitor = TransactionMonitor.getInstance();

// Record metrics
monitor.recordTransactionReceived();
monitor.recordTransactionProcessed(123); // duration in ms
monitor.recordAlertGenerated("HIGH");

// Query metrics
long total = monitor.getTotalProcessed();
double successRate = monitor.getSuccessRate();
MetricsSnapshot snapshot = monitor.getMetricsSnapshot();
```

---

### 6. Retry Configuration (`RetryConfig` & `RetryExecutor`)
**Location**: `common-models/retry/RetryConfig.java` and `RetryExecutor.java`

**Purpose**: Configurable retry mechanism with exponential backoff

**Predefined Configs**:
- `RetryConfig.messageProcessingConfig()` - For JMS message processing
- `RetryConfig.httpCallConfig()` - For HTTP calls

**Usage**:
```java
RetryConfig config = RetryConfig.messageProcessingConfig();
RetryExecutor executor = new RetryExecutor(config);

try {
    String result = executor.execute(() -> {
        return riskyOperation();
    }, transactionId);
} catch (RetryExecutor.RetryException e) {
    logger.error("Operation failed after retries", null, e);
}
```

---

## Integration Steps

### Step 1: Update Producer Service

#### File: `producer-service/src/main/java/.../TransactionIngestionController.java`

```java
import com.example.riskmonitoring.common.logging.StructuredLogger;
import com.example.riskmonitoring.common.validation.TransactionValidator;
import com.example.riskmonitoring.common.validation.ValidationError;
import com.example.riskmonitoring.common.ratelimiting.RateLimiter;
import com.example.riskmonitoring.common.idempotency.IdempotencyService;
import com.example.riskmonitoring.common.monitoring.TransactionMonitor;

@RestController
@RequestMapping("/transaction")
public class TransactionIngestionController {
    
    private static final StructuredLogger logger = StructuredLogger.getLogger(
            TransactionIngestionController.class);
    
    @PostMapping
    public ResponseEntity<?> submitTransaction(@RequestBody TransactionRequest request) {
        String correlationId = logger.generateCorrelationId();
        String transactionId = UUID.randomUUID().toString();
        
        TransactionMonitor monitor = TransactionMonitor.getInstance();
        monitor.recordTransactionReceived();
        
        // Rate limiting check
        RateLimiter rateLimiter = RateLimiter.getInstance();
        if (!rateLimiter.allowRequest(request.getUserId())) {
            logger.logRateLimitExceeded(request.getUserId(), 0);
            ValidationError error = new ValidationError(429, "Rate limit exceeded", "/transaction");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }
        
        // Input validation
        TransactionValidator validator = new TransactionValidator();
        if (!validator.validate(request.getUserId(), request.getAmount(), request.getLocation())) {
            logger.logValidationFailure(transactionId, validator.getFirstError());
            ValidationError error = new ValidationError(400, "Validation failed", "/transaction");
            for (String err : validator.getErrors()) {
                error.addFieldError("input", err);
            }
            return ResponseEntity.badRequest().body(error);
        }
        
        logger.logValidationSuccess(transactionId);
        
        // Check for duplicates
        IdempotencyService idempotencyService = IdempotencyService.getInstance();
        if (idempotencyService.isDuplicate(transactionId, request.getUserId(), 
                                          request.getAmount(), request.getLocation())) {
            logger.logDuplicateDetected(transactionId, transactionId);
            monitor.recordDuplicateDetected();
            // Return previous response
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new TransactionSubmissionResponse(transactionId, Instant.now(), "DUPLICATE"));
        }
        
        // Process transaction
        TransactionSubmissionResponse response = ingestionService.ingestTransaction(request, transactionId);
        
        // Record successful processing
        idempotencyService.recordTransaction(transactionId, request.getUserId(), 
                                            request.getAmount(), request.getLocation(), "SUCCESS");
        logger.logMessagePublished(transactionId, "TRANSACTION_QUEUE");
        monitor.recordTransactionProcessed(10); // milliseconds
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

### Step 2: Update Risk Engine

#### File: `risk-engine/src/main/java/.../TransactionMessageListener.java`

```java
import com.example.riskmonitoring.common.logging.StructuredLogger;
import com.example.riskmonitoring.common.idempotency.IdempotencyService;
import com.example.riskmonitoring.common.monitoring.TransactionMonitor;
import com.example.riskmonitoring.common.retry.RetryConfig;
import com.example.riskmonitoring.common.retry.RetryExecutor;

@Component
public class TransactionMessageListener {
    
    private static final StructuredLogger logger = StructuredLogger.getLogger(
            TransactionMessageListener.class);
    
    private final RetryExecutor retryExecutor;
    private final IdempotencyService idempotencyService;
    private final TransactionMonitor monitor;
    
    public TransactionMessageListener() {
        this.retryExecutor = new RetryExecutor(RetryConfig.messageProcessingConfig());
        this.idempotencyService = IdempotencyService.getInstance();
        this.monitor = TransactionMonitor.getInstance();
    }
    
    @JmsListener(destination = "TRANSACTION_QUEUE", containerFactory = "jmsListenerContainerFactory")
    @Transactional
    public void onTransactionMessage(String message) {
        long startTime = System.currentTimeMillis();
        String transactionId = null;
        
        try {
            logger.logMessageConsumed(message, "TRANSACTION_QUEUE");
            
            // Deserialize with retry
            Transaction transaction = retryExecutor.execute(() -> {
                return deserializeTransaction(message);
            }, transactionId);
            
            transactionId = transaction.getTransactionId();
            logger.setTransactionId(transactionId);
            
            // Check for duplicates
            if (idempotencyService.isDuplicate(transaction.getTransactionId(), 
                    transaction.getUserId(), transaction.getAmount(), transaction.getLocation())) {
                logger.logDuplicateDetected(transactionId, transactionId);
                monitor.recordDuplicateDetected();
                return;
            }
            
            // Analyze risk
            logger.logRiskAnalysisStarted(transactionId);
            RiskResult riskResult = retryExecutor.execute(() -> {
                return analyzeTransaction(transaction);
            }, transactionId);
            
            logger.logRiskAnalysisComplete(transactionId, riskResult.getRiskLevel().toString(), 
                    riskResult.getRiskScore());
            
            // Send alert if needed
            if (shouldAlert(riskResult)) {
                logger.logAlertGenerated(transactionId, riskResult.getRiskLevel().toString());
                retryExecutor.execute(() -> {
                    sendAlert(transaction, riskResult);
                    return null;
                }, transactionId);
                
                monitor.recordAlertGenerated(riskResult.getRiskLevel().toString());
            }
            
            // Record successful processing
            idempotencyService.recordTransaction(transaction.getTransactionId(), 
                    transaction.getUserId(), transaction.getAmount(), 
                    transaction.getLocation(), "SUCCESS");
            
            long duration = System.currentTimeMillis() - startTime;
            monitor.recordTransactionProcessed(duration);
            logger.logProcessingComplete(transactionId, duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            monitor.recordTransactionFailed(e.getClass().getSimpleName());
            logger.error("Transaction processing failed", null, e);
        } finally {
            logger.clearContext();
        }
    }
    
    private Transaction deserializeTransaction(String message) throws Exception {
        // Deserialize JSON to Transaction
        return objectMapper.readValue(message, Transaction.class);
    }
    
    private RiskResult analyzeTransaction(Transaction transaction) throws Exception {
        return riskAnalysisService.analyzeTransaction(transaction);
    }
    
    private boolean shouldAlert(RiskResult result) {
        return result.getRiskLevel().equals(RiskLevel.HIGH) || 
               result.getRiskLevel().equals(RiskLevel.MEDIUM);
    }
    
    private void sendAlert(Transaction transaction, RiskResult result) throws Exception {
        alertNotificationService.sendAlert(transaction, result);
    }
}
```

---

### Step 3: Update Alert Service

#### File: `alert-service/src/main/java/.../AlertController.java`

```java
import com.example.riskmonitoring.common.logging.StructuredLogger;
import com.example.riskmonitoring.common.monitoring.TransactionMonitor;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    
    private static final StructuredLogger logger = StructuredLogger.getLogger(AlertController.class);
    
    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(@RequestBody AlertRequest request) {
        String transactionId = request.getTransactionId();
        logger.setTransactionId(transactionId);
        logger.logAlertStored(transactionId, null);
        
        Alert alert = alertService.createAlert(request);
        TransactionMonitor.getInstance().recordAlertGenerated(alert.getRiskLevel().toString());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AlertResponse(alert.getId(), "CREATED"));
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<AlertStatistics> getStatistics() {
        AlertStatistics stats = new AlertStatistics();
        stats.setTotalAlerts(alertService.countAll());
        stats.setHighRiskCount(alertService.countByRiskLevel("HIGH"));
        stats.setMediumRiskCount(alertService.countByRiskLevel("MEDIUM"));
        stats.setUnreviewedCount(alertService.countUnreviewed());
        
        // Add transaction monitoring stats
        TransactionMonitor.MetricsSnapshot snapshot = 
                TransactionMonitor.getInstance().getMetricsSnapshot();
        stats.setProcessingMetrics(snapshot);
        
        return ResponseEntity.ok(stats);
    }
}
```

---

## Monitoring & Metrics Endpoint

Create a new endpoint to expose metrics:

```java
// In alert-service/src/main/java/.../MetricsController.java

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    
    @GetMapping("/transactions")
    public ResponseEntity<TransactionMonitor.MetricsSnapshot> getTransactionMetrics() {
        TransactionMonitor.MetricsSnapshot snapshot = 
                TransactionMonitor.getInstance().getMetricsSnapshot();
        return ResponseEntity.ok(snapshot);
    }
    
    @GetMapping("/idempotency")
    public ResponseEntity<IdempotencyService.IdempotencyCacheStats> getIdempotencyStats() {
        IdempotencyService.IdempotencyCacheStats stats = 
                IdempotencyService.getInstance().getStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/rate-limit/{userId}")
    public ResponseEntity<RateLimiter.RateLimitStatus> getRateLimitStatus(@PathVariable String userId) {
        RateLimiter.RateLimitStatus status = 
                RateLimiter.getInstance().getStatus(userId);
        return ResponseEntity.ok(status);
    }
}
```

---

## Configuration in application.yml

```yaml
# application.yml
spring:
  application:
    name: alert-service

# Retry configuration (customize as needed)
retry:
  max-retries: 3
  initial-delay-ms: 1000
  max-delay-ms: 30000
  backoff-multiplier: 2.0
  random-delay-enabled: true

# Rate limiting configuration
rate-limit:
  tokens-per-minute: 60
  max-burst-size: 5

# Idempotency configuration
idempotency:
  enabled: true
  cache-ttl-hours: 24
  max-records: 100000

# Monitoring
monitoring:
  enabled: true
  metrics-path: /api/metrics
```

---

## Testing the New Features

### 1. Test Rate Limiting
```bash
# This should succeed
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","amount":100,"location":"NYC, NY"}'

# Repeat 60 times rapidly - 61st should fail with 429
```

### 2. Test Duplicate Detection
```bash
# First submit succeeds
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","amount":100,"location":"NYC, NY"}'

# Resubmit same transaction - should return 409 Conflict
```

### 3. Test Input Validation
```bash
# Invalid amount (negative)
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","amount":-100,"location":"NYC, NY"}'

# Should return 400 with validation errors
```

### 4. Test Metrics
```bash
# Get transaction metrics
curl http://localhost:8082/api/metrics/transactions

# Get idempotency stats
curl http://localhost:8082/api/metrics/idempotency

# Get rate limit status for user
curl http://localhost:8082/api/metrics/rate-limit/user1
```

---

## Production Considerations

1. **Idempotency Storage**: For high volumes, replace in-memory `IdempotencyService` with Redis or database-backed store
2. **Rate Limiting**: Configure tokens per minute based on your SLA
3. **Retry Configuration**: Adjust backoff strategy based on your infrastructure
4. **Logging**: Consider using Log4j2 with async appenders for production
5. **Monitoring**: Export metrics to Prometheus/Grafana for visualization

---

## Summary

You now have production-ready features:
✅ Structured logging with correlation IDs
✅ Idempotency to prevent duplicates
✅ Input validation with error handling
✅ Rate limiting to prevent abuse
✅ Retry mechanism with exponential backoff
✅ Transaction monitoring and metrics

All components are modular, reusable, and follow Spring patterns!
