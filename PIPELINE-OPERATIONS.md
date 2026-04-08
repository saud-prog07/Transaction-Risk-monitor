# Event-Driven Pipeline Operations Guide

## Overview

This document describes the complete event-driven pipeline architecture for the transaction risk monitoring system and provides operational guidance for running, verifying, and troubleshooting the system.

## Pipeline Architecture

### Complete Transaction Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    EVENT-DRIVEN PIPELINE                            │
└─────────────────────────────────────────────────────────────────────┘

1. PRODUCER-SERVICE (Port 8080)
   └─> Receives transaction via REST API
   └─> Validates transaction data (userId, amount, location)
   └─> Generates UUID for tracking
   └─> Logs: [PIPELINE] "PRODUCER_RECEIVED"
   └─> Publishes to IBM MQ

2. IBM MESSAGE QUEUE
   └─> Brokers message between producer and risk-engine
   └─> Logs: [PIPELINE] "MQ_QUEUED"
   └─> Ensures message durability

3. RISK-ENGINE (Port 8081)
   └─> Consumes message from queue
   └─> Logs: [PIPELINE] "RISK_ENGINE_RECEIVED"
   └─> Analyzes risk using multiple strategies:
       ├─ Amount-based detection (>$10,000)
       ├─ Frequency anomaly (7+ transactions in 5 minutes)
       ├─ GeolocationChange (international travel detection)
       └─ UserHistory (deviation from baseline)
   └─> Logs: [PIPELINE] "RISK_ENGINE_ANALYZED"
   └─> Sends alert if HIGH or MEDIUM risk (WITH RETRY)

4. ALERT-SERVICE (Port 8082)
   └─> Receives alert via REST API with automatic retry
   └─> Logs: [PIPELINE] "ALERT_SERVICE_RECEIVED"
   └─> Persists to PostgreSQL database
   └─> Logs: [PIPELINE] "ALERT_SERVICE_STORED / PIPELINE_COMPLETE"
   └─> Provides query endpoints

5. RETRY LOGIC (AlertNotificationService)
   ├─ Configured: 3 total attempts
   ├─ Backoff Strategy: exponential (2s → 4s → 8s) + random jitter
   ├─ Retryable Exceptions:
   │  ├─ HttpServerErrorException (5xx status codes)
   │  └─ ResourceAccessException (connection failures)
   ├─ Non-retryable Exceptions:
   │  └─ Other errors logged as fatal
   └─ Total Max Latency: ~14 seconds (2s + 4s + 8s + processing time)
```

## Service Specifications

### Producer-Service

**Port**: 8080
**Framework**: Spring Boot 3.4.4
**Database**: None (stateless)

**API Endpoints**:
- `POST /transaction` - Submit new transaction
  - Request: `{ userId, amount, location }`
  - Response: `{ transactionId, timestamp, status }`
  - Status Codes: 201 (Created), 400 (Invalid), 500 (Error)

- `GET /actuator/health` - Service health check
  - Response: `{ status: "UP" }`

**Key Components**:
- `TransactionIngestionController` - REST endpoint
- `TransactionIngestionService` - Business logic
- `IBMMQPublisher` - Message publishing with retry

**Logging**:
- Format: `[PRODUCER] [PIPELINE] msg`
- Levels: INFO (normal), WARN (warnings), ERROR (failures)

### Risk-Engine

**Port**: 8081
**Framework**: Spring Boot 3.4.4
**Database**: H2 (in-memory) for transaction history

**API Endpoints**:
- `GET /actuator/health` - Service health check

**Key Components**:
- `TransactionMessageListener` - JMS message consumer
- `RiskAnalysisService` - Risk scoring orchestration
- `HighValueTransactionAnalyzer` - Amount-based detection
- `FrequencyAnomalyAnalyzer` - Temporal pattern detection
- `GeolocationChangeAnalyzer` - Geographic anomaly detection
- `UserHistoryAnalyzer` - Behavioral deviation detection
- `AlertNotificationService` - Alert dispatch with retry

**Risk Analysis**:
- Returns: `RiskResult { riskLevel, score, reason }`
- RiskLevels: HIGH (score ≥ 80), MEDIUM (score 50-79), LOW (score < 50)

**Logging**:
- Format: `[RISK-ENGINE] [PIPELINE] msg`
- Per-stage logging with transaction IDs
- Processing duration tracking

### Alert-Service

**Port**: 8082
**Framework**: Spring Boot 3.4.4
**Database**: PostgreSQL (persistent alert storage)

**API Endpoints**:
- `POST /api/alerts` - Create alert
  - Request: Alert payload from risk-engine
  - Response: `{ alertId, status }`
  - Status Codes: 201 (Created), 400 (Invalid), 500 (Error)

- `GET /api/alerts` - List alerts (paginated)
  - Query: `?page=0&size=20`
  - Response: Page of Alert objects

- `GET /api/alerts/statistics` - Get alert statistics
  - Response: `{ totalAlerts, unreviewedCount, highRiskCount, mediumRiskCount }`

- `PATCH /api/alerts/{id}/review` - Mark alert as reviewed
  - Request: `{ reviewed: true, reviewedBy }`
  - Status Codes: 200 (Updated), 404 (Not Found)

- `GET /actuator/health` - Service health check

**Key Components**:
- `AlertController` - REST endpoints
- `AlertService` - Business logic and persistence
- `Alert` entity - JPA model

**Logging**:
- Format: `[ALERT-SERVICE] [PIPELINE] msg`
- Transaction tracking with correlation IDs

## Pipeline Logging Correlation

All services log with `[PIPELINE]` prefix to enable end-to-end transaction tracing:

```
[2024-01-15 14:23:45.123] INFO  [PRODUCER]      [PIPELINE] PRODUCER_RECEIVED - txId: a1b2c3d4-...
[2024-01-15 14:23:45.456] INFO  [PRODUCER]      [PIPELINE] PRODUCER_VALIDATED - txId: a1b2c3d4-...
[2024-01-15 14:23:45.789] INFO  [MQ]            [PIPELINE] MQ_QUEUED - txId: a1b2c3d4-...
[2024-01-15 14:23:46.012] INFO  [RISK-ENGINE]   [PIPELINE] RISK_ENGINE_RECEIVED - txId: a1b2c3d4-...
[2024-01-15 14:23:46.234] INFO  [RISK-ENGINE]   [PIPELINE] ANALYZING_RISK - txId: a1b2c3d4-... (HighValue: TRIGGER)
[2024-01-15 14:23:46.567] WARN  [RISK-ENGINE]   [PIPELINE] SENDING_ALERT - txId: a1b2c3d4-... (RiskLevel: HIGH)
[2024-01-15 14:23:46.890] INFO  [RISK-ENGINE]   [PIPELINE] RISK_ENGINE_ANALYZED - txId: a1b2c3d4-...
[2024-01-15 14:23:47.234] INFO  [ALERT-SERVICE] [PIPELINE] ALERT_SERVICE_RECEIVED - txId: a1b2c3d4-...
[2024-01-15 14:23:47.567] INFO  [ALERT-SERVICE] [PIPELINE] ALERT_SERVICE_STORED - txId: a1b2c3d4-...
[2024-01-15 14:23:47.890] INFO  [ALERT-SERVICE] [PIPELINE] PIPELINE_COMPLETE - txId: a1b2c3d4-... (Duration: 2.767s)
```

**Tracing Steps**:
1. Copy transaction ID from first log entry
2. Search logs for this transaction ID
3. Follow path through all services
4. Identify where failures or delays occur

## Running the Pipeline

### Prerequisites

1. **Java 21+** installed
   ```bash
   java -version
   ```

2. **Maven 3.8+** installed
   ```bash
   mvn -version
   ```

3. **PostgreSQL 13+** running
   ```bash
   # Default credentials: postgres/password
   # Default port: 5432
   # Database: alert_db
   ```

4. **IBM MQ** running (or in-memory queue simulation)
   - Producer publishes to `TRANSACTION_QUEUE`
   - Risk-engine consumes from `TRANSACTION_QUEUE`

### Start All Services

#### Option 1: Individual Terminal Windows (Recommended for Development)

**Terminal 1 - Producer Service**:
```bash
cd producer-service
mvn clean spring-boot:run
# Expected: Started on port 8080
```

**Terminal 2 - Risk Engine**:
```bash
cd risk-engine
mvn clean spring-boot:run
# Expected: Started on port 8081
```

**Terminal 3 - Alert Service**:
```bash
cd alert-service
mvn clean spring-boot:run
# Expected: Started on port 8082
```

#### Option 2: Sequential Maven Build

```bash
# Build all modules
mvn clean package

# Run each service
java -jar producer-service/target/producer-service-1.0.jar
java -jar risk-engine/target/risk-engine-1.0.jar
java -jar alert-service/target/alert-service-1.0.jar
```

#### Option 3: Docker Compose (Recommended for Testing)

```bash
docker-compose up -d
# Automatically starts all services and PostgreSQL
```

### Verify Services Are Running

```powershell
# Using PowerShell (Windows)
.\verify-pipeline.ps1

# Or manually check each service
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## Testing the Pipeline

### Automated Verification Script

```powershell
# Run complete verification with all tests
.\verify-pipeline.ps1

# Custom parameters
.\verify-pipeline.ps1 -ProducerUrl "http://localhost:8080" `
                      -RiskEngineUrl "http://localhost:8081" `
                      -AlertServiceUrl "http://localhost:8082" `
                      -ProcessingDelaySeconds 5
```

**Test Suite**:
1. **Service Health Checks** - Verify all services are up
2. **High-Value Transaction** - Test amount-based detection (>$10K)
3. **Low-Risk Transaction** - Verify no alert for small transaction
4. **Frequency Anomaly** - Test temporal pattern detection (7+ in 5min)
5. **Database Connectivity** - Verify PostgreSQL connection
6. **Statistics** - Test metrics endpoints

### Manual Testing

#### Test 1: Submit High-Value Transaction

```bash
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER123",
    "amount": 15000,
    "location": "New York, NY"
  }'

# Response:
# {
#   "transactionId": "a1b2c3d4-e5f6-7890-abcd",
#   "timestamp": "2024-01-15T14:23:45.123Z",
#   "status": "RECEIVED"
# }
```

**Wait 3-5 seconds**, then verify alert was created:

```bash
curl http://localhost:8082/api/alerts
```

#### Test 2: Submit Low-Risk Transaction

```bash
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER456",
    "amount": 50,
    "location": "Chicago, IL"
  }'

# Wait 3-5 seconds, verify NO alert created:
curl http://localhost:8082/api/alerts
# Should be empty or not contain this transaction
```

#### Test 3: Frequency Anomaly Detection

```bash
# Submit 7 transactions rapidly from same user
for i in {1..7}; do
  curl -X POST http://localhost:8080/transaction \
    -H "Content-Type: application/json" \
    -d "{
      \"userId\": \"USER789\",
      \"amount\": $((100 + i)),
      \"location\": \"Los Angeles, CA\"
    }"
  sleep 0.5
done

# Wait 3-5 seconds, verify frequency alert
curl http://localhost:8082/api/alerts
```

## Troubleshooting

### Issue: Service Won't Start

**Symptom**: "Connection refused" or "Port already in use"

**Solution**:
```bash
# Check if port is in use (Windows)
netstat -ano | findstr :8080
netstat -ano | findstr :8082

# Kill process on port (replace PID with actual)
taskkill /PID <PID> /F

# Or start on different port
java -jar alert-service.jar --server.port=8083
```

### Issue: Messages Not Being Processed

**Symptom**: Transactions submitted but no alerts created

**Solution**:
1. Verify all three services are running:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/actuator/health
   curl http://localhost:8082/actuator/health
   ```

2. Check logs for errors:
   ```bash
   # Look for [PIPELINE] prefix in logs
   # Search for ERROR or WARN entries
   ```

3. Verify IBM MQ is accessible:
   ```bash
   # Check application.yml for MQ broker configuration
   # Default: localhost:1883 (or Docker container)
   ```

4. Check database connectivity:
   ```bash
   curl http://localhost:8082/actuator/health/databaseHealth
   ```

### Issue: Alerts Not Being Created

**Symptom**: High-value transactions processed but no alerts

**Symptom**: Transactions appear in risk-engine logs but stop there

**Solution**:
1. Verify Alert Service database:
   ```bash
   psql -U postgres -d alert_db -c "SELECT COUNT(*) FROM alerts;"
   ```

2. Check Alert Service logs for retry failures:
   ```bash
   # Search for "Failed to send alert after all retries"
   # Indicates exhausted retry attempts
   ```

3. Verify transaction amount exceeds threshold:
   ```bash
   # HighValueTransactionAnalyzer triggers on amount > 10000
   # Test with amount >= 10000
   ```

### Issue: Slow Processing / High Latency

**Symptom**: Alerts appear after 10+ seconds

**Causes**:
1. **Retry backoff** - If alert service is intermittently down, retry logic adds 2-4-8s delays
2. **Risk analysis** - Multiple analytical strategies take time
3. **Database** - PostgreSQL query performance
4. **Network** - Service-to-service communication

**Optimization**:
```yaml
# In alert-service/application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: none  # Don't validate schema on startup
    properties:
      hibernate.generate_statistics: false  # Disable logging
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase DB connection pool
```

### Issue: Messages Lost During Restart

**Symptom**: Transactions submitted while alert-service restarting are not processed

**Cause**: By default, messages are acknowledged immediately, so failures after acknowledgment lose the message

**Solution**: Implement Dead Letter Queue (DLQ) handling:
1. Configure IBM MQ DLQ in TransactionMessageListener
2. Route failed messages to DLQ after retries exhausted
3. Monitor DLQ and replay messages when alert-service is healthy

## Performance Characteristics

### Expected Latencies (End-to-End)

| Scenario | Latency | Notes |
|----------|---------|-------|
| Low-risk transaction | 500ms - 2s | Risk analysis + no alert |
| High-value alert (success) | 2s - 4s | Analysis + alert creation successful |
| High-value alert (1 retry) | 4s - 8s | Alert service temporarily down, 1 retry |
| High-value alert (2 retries) | 8s - 14s | Alert service down, 2 retries exhausted |
| Frequency anomaly | 2s - 5s | Requires wait for 7 transactions, then processing |

### Throughput

- **Producer-Service**: ~1000 requests/sec (limited by network)
- **Risk-Engine**: ~500 messages/sec (multiple analyzers)
- **Alert-Service**: ~100 alerts/sec (database write limited)
- **Overall Pipeline**: ~100 transactions/sec (alert-service bottleneck)

### Resource Usage

| Service | CPU | Memory | Notes |
|---------|-----|--------|-------|
| Producer | 5-10% | 256MB | Stateless, minimal |
| Risk-Engine | 10-20% | 512MB | Analysis computation |
| Alert-Service | 10-15% | 512MB | Database I/O |
| PostgreSQL | 5-10% | 256MB | With default indexes |

## Monitoring & Observability

### Health Endpoints

```bash
# Service health
curl http://localhost:8082/actuator/health

# Database health (Alert-Service only)
curl http://localhost:8082/actuator/health/databaseHealth

# Detailed response:
# {
#   "status": "UP",
#   "components": {
#     "db": { "status": "UP", ... },
#     "jms": { "status": "UP", ... },
#     ...
#   }
# }
```

### Alert Statistics

```bash
curl http://localhost:8082/api/alerts/statistics

# Response:
# {
#   "totalAlerts": 42,
#   "unreviewedCount": 15,
#   "highRiskCount": 8,
#   "mediumRiskCount": 34,
#   "createdAtLeast24h": 2,
#   "averageReviewTime": "PT45M"
# }
```

### Logging Best Practices

**Enable Debug Logging** (development only):
```properties
# application.yml
logging:
  level:
    com.example.riskmonitoring: DEBUG
    org.springframework: INFO
```

**Search for Transaction** by ID:
```bash
# In logs, search for transaction ID to trace entire flow
grep "a1b2c3d4-e5f6-7890-abcd" application.log

# For Docker logs
docker logs <service_name> | grep "a1b2c3d4-e5f6-7890-abcd"
```

## Common Operational Tasks

### Clear All Alerts

```bash
# Via curl (requires DELETE endpoint, may need to add)
curl -X DELETE http://localhost:8082/api/alerts

# Via SQL
psql -U postgres -d alert_db -c "DELETE FROM alerts;"
```

### Export Alerts to CSV

```bash
# Query PostgreSQL and export
psql -U postgres -d alert_db -c "
  SELECT 
    id, transaction_id, user_id, amount, risk_level, 
    reason, created_at
  FROM alerts
  ORDER BY created_at DESC
  LIMIT 1000;
" > alerts_export.csv
```

### Reset Transaction History (Risk-Engine)

```bash
# H2 in-memory database resets on service restart
# No action needed - just restart risk-engine service
```

### Check Service Dependencies

```bash
# Producer depends on: IBM MQ
# Risk-Engine depends on: IBM MQ
# Alert-Service depends on: PostgreSQL

# Verify all dependencies
.\verify-pipeline.ps1
```

## Scaling Considerations

### For 1000 transactions/sec

1. **Add Load Balancer**:
   - Multiple producer-service instances (stateless)
   - Multiple alert-service instances (with shared PostgreSQL)

2. **Message Queue Optimization**:
   - Increase IBM MQ thread pool
   - Use message partitioning by userId

3. **Database Scaling**:
   - Add read replicas for alert queries
   - Implement alert archiving (move old records to separate table)

4. **Risk-Engine Scaling**:
   - Same as producer (stateless)
   - May need to cache user history for performance

## Emergency Procedures

### If Alert-Service Is Down

- Producer and Risk-Engine continue running
- Transactions are queued in IBM MQ
- After alert-service restarts, queued messages are processed
- Alerts may be delayed but won't be lost

### If Risk-Engine Is Down

- Producer continues accepting transactions
- Messages accumulate in IBM MQ
- After risk-engine restarts, messages are processed in order
- Some message order may be lost if queue is cleared

### If PostgreSQL Is Down

- All three services can still run
- Alerts fail to persist (after retries exhausted)
- Messages are acknowledged without storing alerts
- DATA LOSS - alerts are lost

**Critical**: Always backup PostgreSQL before stopping it.

```bash
# Backup
pg_dump -U postgres alert_db > alert_db_backup.sql

# Restore
psql -U postgres alert_db < alert_db_backup.sql
```
