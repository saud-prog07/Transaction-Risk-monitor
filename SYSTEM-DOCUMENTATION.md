# Real-Time Transaction Risk Monitoring System - Complete Documentation

## System Overview

A production-grade microservices architecture for real-time transaction risk monitoring using Spring Boot, IBM MQ, and PostgreSQL. The system processes financial transactions, analyzes risk, and flags suspicious behavior in real-time.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Transaction Flow                              │
└─────────────────────────────────────────────────────────────────┘

1. CLIENT
   │
   ├──POST /transaction────────────────────────→ PRODUCER-SERVICE (Port 8080)
   │                                                    │
   │                                                    ├─ Validation
   │                                                    ├─ UUID Generation
   │                                                    └─ Timestamp Assignment
   │                                                           │
   │                                                           ▼
   │                                                      IBM MQ (1414)
   │                                                   TRANSACTION_QUEUE
   │                                                           ▲
   │                                                           │
   │                                                    [Message Structure]
   │                                                   {
   │                                                     transactionId: UUID
   │                                                     userId: string
   │                                                     amount: decimal
   │                                                     timestamp: instant
   │                                                     location: string
   │                                                   }
   │                                                           │
   │                                                           ▼
   │                                       RISK-ENGINE (Port 8081)
   │                                            │
   │                                            ├─ HighAmountAnalyzer
   │                                            │  (> 2x user average)
   │                                            ├─ FrequencyAnalyzer
   │                                            │  (≥5 txns in 5 min)
   │                                            └─ LocationAnomalyAnalyzer
   │                                               (new/unusual locations)
   │                                                    │
   │                                                    ├─ Persist TransactionHistory
   │                                                    ├─ Aggregate Risk Results
   │                                                    └─ Generate Alerts
   │                                                           │
   │                                        REST (POST /api/alerts)
   │                                                           │
   │                                                           ▼
   │                                       ALERT-SERVICE (Port 8082)
   │                                            │
   │                                            ├─ Validate Alert Request
   │                                            ├─ Persist FlaggedTransaction
   │                                            └─ Index for Search
   │                                                    │
   │                                                    ▼
   │                                            PostgreSQL Database
   │                                          (alert_service_db)
   │
   └──GET /api/alerts ──────────────────────────→ ALERT-SERVICE
      (Query flagged transactions)
```

## Microservices Architecture

### 1. common-models (Shared Domain Objects)

**Purpose**: Shared data models used across all services

**Key Classes**:
- `Transaction.java`: Input transaction model
  - Fields: transactionId (UUID), userId, amount (BigDecimal), timestamp (Instant), location
  - Annotations: @Data, @NoArgsConstructor, @AllArgsConstructor, @Builder
  - Serialization: Jackson JSON with snake_case field names

- `RiskResult.java`: Risk analysis output
  - Fields: transactionId (UUID), riskLevel (enum), reason (String)
  - Used by: risk-engine → alert-service

- `RiskLevel.java`: Enum with values: LOW, MEDIUM, HIGH

**Maven Coordinates**:
```xml
<groupId>com.example.riskmonitoring</groupId>
<artifactId>common-models</artifactId>
<version>1.0.0</version>
```

---

### 2. producer-service (Port 8080)

**Purpose**: REST API for transaction ingestion and MQ publishing

**Key Components**:

#### Controllers
- `TransactionIngestionController`
  - Endpoint: `POST /transaction`
  - Request: `TransactionIngestionRequest` (userId, amount, location)
  - Response: Created `Transaction` with auto-generated UUID and timestamp
  - Status: 201 CREATED on success, 400 BAD_REQUEST on validation error

#### Services
- `TransactionIngestionService`
  - Generates transactionId (UUID) and timestamp (Instant.now())
  - Converts request to Transaction model
  - Delegates publishing to MessagePublisher

#### Message Publishing
- `MessagePublisher` (interface)
- `IBMMQPublisher` (implementation)
  - Publishes to queue: `TRANSACTION_QUEUE`
  - Message format: JSON
  - Retry Strategy: 3 attempts, exponential backoff (1-4 seconds with jitter)
  - Annotation: `@Retryable`

#### Configuration
- `JmsConfiguration`
  - Queue Connection Factory: MQQueueConnectionFactory
  - Session Cache: 10 sessions
  - Connection Timeout: 5 seconds
  - Message Delivery: Persistent mode

#### Exception Handling
- `GlobalExceptionHandler`: Centralized exception mapping
- Custom Exceptions:
  - `InvalidTransactionException`
  - `TransactionPublishingException`

#### Health Monitoring
- `IBMMQHealthIndicator`: Reports MQ connection status
- Endpoint: `/actuator/health`

#### Logging
- Spring Retry logs: DEBUG level for retry attempts
- MQ connection events: INFO level

**Dependencies**:
```xml
<spring-boot-starter-activemq>
<com.ibm.mq.allclient:9.3.4.1>
<spring-retry>
<spring-boot-starter-aop>
<jackson-databind>
<lombok>
```

**Sample Request/Response**:
```bash
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER123",
    "amount": 5000.00,
    "location": "New York, NY"
  }'

# Response (201 CREATED):
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "USER123",
  "amount": 5000.00,
  "timestamp": "2024-01-15T10:30:00Z",
  "location": "New York, NY"
}
```

---

### 3. risk-engine (Port 8081)

**Purpose**: Consumes transactions, analyzes risk, notifies alerts

**Architecture Pattern**: Strategy Pattern for risk analysis

**Key Components**:

#### Entity & Persistence
- `TransactionHistory` (JPA Entity)
  - Tracks user transaction history
  - Indexes: transaction_id, risk_level, timestamp, created_at
  - Automatic lifecycle management with @PrePersist/@PreUpdate

- `TransactionHistoryRepository`
  - Spring Data JPA with custom @Query methods
  - Methods: findAverageAmountByUserId, countByUserIdAndTimestampBetween, etc.

#### Risk Analyzers (Strategy Pattern)
- `RiskAnalyzer` (interface)
  - Method: `analyze(Transaction)` → `RiskDetectionResult`

- `HighAmountAnalyzer`
  - Detects: Amounts > 2x user's average (configurable: 2.0)
  - Minimum Threshold: 1000.00 (optional)
  - Window: All-time history
  - Risk Level: HIGH if > 2x average, MEDIUM if > 1.5x

- `FrequencyAnalyzer`
  - Detects: ≥5 transactions within 5-minute window
  - Escalation: HIGH if ≥10 transactions
  - Risk Level: MEDIUM (default) or HIGH (escalated)
  - Handles: Users with no history gracefully

- `LocationAnomalyAnalyzer`
  - Detects: Transactions from new/unusual locations
  - Window: Last 30 days of user transactions
  - Risk Level: LOW for new locations, MEDIUM for unusual
  - Configurable: Can be disabled via properties

#### Service Orchestration
- `RiskAnalysisService`
  - Orchestrates all 3 analyzers
  - Aggregates results (HIGH > MEDIUM > LOW priority)
  - Persists TransactionHistory
  - Generates consolidated risk reasons
  - Sends alerts for risk level >= MEDIUM

#### Message Consumption
- `TransactionMessageListener`
  - Annotation: `@JmsListener(destination = "TRANSACTION_QUEUE")`
  - Deserializes JMS Message to Transaction
  - Triggers RiskAnalysisService
  - Sends alerts for detected risk

#### REST Integration
- `AlertNotificationService`
  - Calls alert-service endpoint: `POST /api/alerts`
  - Sends: Complete transaction + risk details
  - Error Handling: Logs failures without blocking risk analysis

#### REST Diagnostics
- `RiskAnalysisController`
  - `POST /api/risk/analyze`: Analyze transaction manually
  - `GET /api/risk/analyzers`: List available analyzers
  - `GET /api/risk/health`: Service health status

#### Configuration
- Analyzers enabled/disabled via properties
- Risk thresholds configurable: high-amount multiplier, frequency window, location window

#### Database
- In-memory H2 database for development
- Schema auto-created via Hibernate DDL

**Configurable Properties** (application.yml):
```yaml
risk-analysis:
  analyzers:
    high-amount:
      enabled: true
      multiplier: 2.0
      minimum-threshold: 1000.00
    frequency:
      enabled: true
      window-minutes: 5
      threshold: 5
      high-risk-threshold: 10
    location:
      enabled: true
      history-days: 30

jms:
  listener:
    concurrency: 1-5
```

**Sample Alert Sent to alert-service**:
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "USER123",
  "amount": 5000.00,
  "location": "New York, NY",
  "timestamp": "2024-01-15T10:30:00Z",
  "riskLevel": "HIGH",
  "reason": "Amount is 3.5x user average; Frequency: 7 transactions in 5 minutes"
}
```

---

### 4. alert-service (Port 8082)

**Purpose**: Receives flagged transactions, persists to PostgreSQL, provides REST API for querying

**Architecture Pattern**: Repository Pattern with layered architecture

#### Entity & Persistence
- `FlaggedTransaction` (JPA Entity)
  - Fields:
    - id (Long, auto-generated)
    - transactionId (UUID, unique)
    - riskLevel (RiskLevel enum, NOT NULL)
    - reason (String, NOT NULL, max 1000 chars)
    - createdAt (Instant, immutable)
    - updatedAt (Instant, auto-updated)
    - reviewed (boolean, default false)
    - investigationNotes (String, max 2000 chars)
  - Lifecycle: @PrePersist sets creation fields, @PreUpdate refreshes updatedAt
  - Indexes: idx_transaction_id (unique), idx_risk_level, idx_created_at, composite

- `FlaggedTransactionRepository` (Spring Data JPA)
  - Extends: JpaRepository<FlaggedTransaction, Long>
  - Custom Methods:
    - findByTransactionId(UUID)
    - findByRiskLevel(RiskLevel, Pageable)
    - findByReviewedFalse(Pageable)
    - findByCreatedAtBetween(Instant, Instant)
    - countByRiskLevel(RiskLevel)
    - countByReviewedFalse()
    - Custom @Query for complex queries

#### DTOs
- `AlertRequest`: Inbound from risk-engine
  - Fields: transactionId, userId, amount, location, timestamp, riskLevel, reason
  - Validation: @Valid, @NotNull, @NotBlank annotations

- `AlertResponse`: Outbound for all GET/POST responses
  - Maps to FlaggedTransaction fields

- `AlertStatistics`: Statistics response object
  - Fields: totalAlerts, unreviewedCount, highRiskCount, mediumRiskCount

#### Service Layer
- `AlertService`
  - createAlert(AlertRequest): Create, validate uniqueness, persist
  - getAlertById(Long): Retrieve with 404 if not found
  - getAllAlerts(Pageable): Paginated list
  - getAlertsByRiskLevel(RiskLevel, Pageable): Filter by risk
  - getUnreviewedAlerts(Pageable): Pending reviews
  - getHighRiskAlerts(Pageable): HIGH risk only
  - markAsReviewed(Long, notes): Update review status
  - getStatistics(): Returns alert counts
  - All operations: @Transactional with appropriate read-only flags

#### REST Controller
- `AlertController` (8 endpoints):
  ```
  POST   /api/alerts                          - Create alert
  GET    /api/alerts                          - List all (paginated)
  GET    /api/alerts/{id}                     - Get by ID
  GET    /api/alerts/by-risk-level/{level}    - Filter by risk
  GET    /api/alerts/unreviewed               - Pending reviews
  GET    /api/alerts/high-risk                - HIGH risk only
  PUT    /api/alerts/{id}/review              - Mark reviewed
  GET    /api/alerts/statistics               - Alert metrics
  GET    /api/alerts/health                   - Service health
  ```

#### Exception Handling
- `AlertNotFoundException` (404): Alert ID not found
- `AlertAlreadyExistsException` (409): Duplicate transaction_id
- `GlobalExceptionHandler`: Centralized exception mapping

#### Configuration
- `DatabaseConfiguration`: JPA repository + JpaTransactionManager
- `JpaAuditingConfiguration`: Entity lifecycle tracking
- `AuditorProvider`: Returns "SYSTEM" as current auditor
- `WebConfiguration`: Spring Data Web pagination support
- `DatabaseHealthIndicator`: Connection pool monitoring

#### Monitoring
- `DatabaseHealthIndicator`
  - Endpoint: `/actuator/health/databaseHealth`
  - Returns: pool_size, active_connections, idle_connections, waiting_threads

- `TransactionMonitoringAspect`
  - AOP @Aspect monitoring @Transactional methods
  - Logs: Duration, status, errors with method context
  - Useful for: Performance profiling, debugging slow queries

#### Database Configuration
- **Datasource**: PostgreSQL (jdbc:postgresql://localhost:5432/alert_service_db)
- **Authentication**: postgres/postgres (change in production)
- **HikariCP Pool**:
  - Maximum: 10 connections
  - Minimum Idle: 5 connections
  - Connection Timeout: 20 seconds
  - Idle Timeout: 5 minutes
  - Connection Validation: SELECT 1 query
  - Leak Detection: 60 seconds

- **Hibernate Settings**:
  - DDL Auto: update (safe for all environments)
  - Batch Size: 20 (bulk operations)
  - Fetch Size: 50 (result streaming)
  - Statement Cache: 250 (prepared statements)
  - Isolation Level: READ_COMMITTED (default)
  - Query Timeout: 30 seconds

#### Database Schema
- **Table**: flagged_transactions
  - Columns: id, transaction_id, risk_level, reason, created_at, updated_at, reviewed, investigation_notes
  - Constraints: NOT NULL on key fields, UNIQUE on transaction_id

- **Indexes**:
  - idx_transaction_id: Unique, for lookups
  - idx_risk_level: For filtering by risk
  - idx_created_at: For time-series queries
  - idx_risk_level_created: Composite for advanced queries

- **Views**:
  - v_unreviewed_alerts: Alerts with reviewed = false
  - v_high_risk_alerts: Alerts with risk_level = 'HIGH'
  - v_recent_alerts: Alerts from last 7 days

#### Setup Scripts
- `setup-postgres.sh`: Unix/Linux setup (bash)
- `setup-postgres.bat`: Windows setup (batch)
- Both: Create database, run init-schema.sql, verify connectivity

**Sample Request/Response**:
```bash
# Create alert
curl -X POST http://localhost:8082/api/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "USER123",
    "amount": 5000.00,
    "location": "New York, NY",
    "timestamp": "2024-01-15T10:30:00Z",
    "riskLevel": "HIGH",
    "reason": "Amount is 3.5x user average"
  }'

# Query alerts
curl http://localhost:8082/api/alerts?page=0&size=20

# Get statistics
curl http://localhost:8082/api/alerts/statistics
# Response:
{
  "totalAlerts": 42,
  "unreviewedCount": 12,
  "highRiskCount": 8,
  "mediumRiskCount": 19
}
```

---

## End-to-End Transaction Flow

### Scenario: Transaction Processing

1. **Client submits transaction**:
   ```
   POST /transaction
   {
     "userId": "USER123",
     "amount": 5000.00,
     "location": "New York, NY"
   }
   ```

2. **producer-service**:
   - Generates UUID: `550e8400-e29b-41d4-a716-446655440000`
   - Sets timestamp: `2024-01-15T10:30:00Z`
   - Publishes to IBM MQ: `TRANSACTION_QUEUE`
   - Returns 201 CREATED with full transaction object

3. **IBM MQ**: Brokers message (TRANSACTION_QUEUE)

4. **risk-engine**:
   - Receives message from listener
   - Runs 3 analyzers:
     - HighAmountAnalyzer: Checks average (assume $2000)
       - Current: $5000 / Average: $2000 = 2.5x → HIGH RISK
     - FrequencyAnalyzer: Counts transactions in 5-min window
       - Assume 6 transactions → MEDIUM RISK (escalates to HIGH if ≥10)
     - LocationAnomalyAnalyzer: Checks location history
       - Assume new location → LOW RISK
   - Aggregates: HIGH (highest priority)
   - Persists to TransactionHistory (H2)
   - Sends to alert-service

5. **alert-service**:
   - Receives alert payload
   - Validates uniqueness (checks transaction_id)
   - Persists to PostgreSQL FlaggedTransaction
   - Returns 201 CREATED

6. **Client queries alerts**:
   ```
   GET /api/alerts?page=0&size=20
   ```
   - Returns paginated list of all flagged transactions

---

## Technology Stack

### Core Framework
- **Spring Boot**: 3.4.4 (latest stable)
- **Spring Cloud**: Distributed systems components
- **Maven**: Build tool and dependency management

### Data Layer
- **Spring Data JPA**: ORM abstraction
- **Hibernate**: JPA implementation
- **PostgreSQL**: Relational database (alert-service)
- **H2**: In-memory database (risk-engine, testing)
- **HikariCP**: Connection pooling

### Messaging
- **IBM MQ**: Enterprise message broker (v9.3.4.1)
- **Spring JMS**: JMS client abstractions
- **JMS Template**: Message template pattern

### Cross-Cutting Concerns
- **Spring AOP**: Aspect-Oriented Programming
- **Spring Retry**: Automatic retry with exponential backoff
- **Spring Security**: (Ready for integration)
- **Spring Boot Actuator**: Operational metrics and monitoring

### Code Generation & Utilities
- **Lombok**: Reduces boilerplate code
- **Jackson**: JSON serialization/deserialization
- **SLF4J/Logback**: Logging

---

## API Endpoints Reference

### producer-service (Port 8080)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /transaction | Submit transaction for analysis | 201 |
| GET | /actuator/health | Service health status | 200 |
| GET | /actuator/health/mqHealth | IBM MQ connection status | 200 |

### risk-engine (Port 8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/risk/analyze | Manually analyze transaction |
| GET | /api/risk/analyzers | List available analyzers |
| GET | /api/risk/health | Service health |

### alert-service (Port 8082)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/alerts | Create alert | 201 |
| GET | /api/alerts | List all alerts (paginated) | 200 |
| GET | /api/alerts/{id} | Get alert by ID | 200/404 |
| GET | /api/alerts/by-risk-level/{level} | Filter by risk level | 200 |
| GET | /api/alerts/unreviewed | Get unreviewed alerts | 200 |
| GET | /api/alerts/high-risk | Get high-risk alerts | 200 |
| PUT | /api/alerts/{id}/review | Mark as reviewed | 200 |
| GET | /api/alerts/statistics | Alert statistics | 200 |
| GET | /actuator/health | Service health | 200 |
| GET | /actuator/health/databaseHealth | Database pool status | 200 |

---

## Configuration Properties

### Common Properties (All Services)

```yaml
spring:
  application:
    name: Service Name
  profiles:
    active: dev
  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
logging:
  level:
    root: INFO
    com.example.riskmonitoring: DEBUG
```

### producer-service specific

```yaml
ibm:
  mq:
    queue-manager: TRANSACTION_QM
    channel: DEV.APP.SVRCONN
    host: localhost
    port: 1414
spring:
  jms:
    listener:
      concurrency: 1-5
```

### risk-engine specific

```yaml
risk-analysis:
  analyzers:
    high-amount:
      enabled: true
      multiplier: 2.0
    frequency:
      enabled: true
      window-minutes: 5
      threshold: 5
    location:
      enabled: true
      history-days: 30
```

### alert-service specific

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/alert_service_db
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

---

## Deployment & Operations

### Local Development Setup

1. **Start PostgreSQL**:
   ```bash
   # Windows
   net start postgresql-x64-14
   
   # Linux
   sudo systemctl start postgresql
   ```

2. **Create Database**:
   ```bash
   cd alert-service/src/main/resources/db
   ./setup-postgres.sh      # Linux/macOS
   setup-postgres.bat       # Windows
   ```

3. **Build all services**:
   ```bash
   mvn clean install
   ```

4. **Start services in order**:
   ```bash
   # Terminal 1: Common models (required for others)
   cd common-models && mvn spring-boot:run
   
   # Terminal 2: Producer service
   cd producer-service && mvn spring-boot:run
   
   # Terminal 3: Risk engine
   cd risk-engine && mvn spring-boot:run
   
   # Terminal 4: Alert service
   cd alert-service && mvn spring-boot:run
   ```

### Health Check

```bash
# Check all services
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8082/actuator/health/databaseHealth
```

### Monitoring

```bash
# View metrics (local)
http://localhost:8082/actuator/metrics

# View environment properties
http://localhost:8082/actuator/env

# View loggers and change levels
http://localhost:8082/actuator/loggers
```

---

## Security Considerations (Future Implementation)

- [ ] API authentication (API keys or OAuth2)
- [ ] TLS/SSL encryption for all communications
- [ ] PostgreSQL user with restricted permissions
- [ ] IBM MQ encryption and authentication
- [ ] Input validation against injection attacks
- [ ] Rate limiting on REST endpoints
- [ ] Audit logging for all database changes
- [ ] Secrets management (Spring Cloud Config)

---

## Performance Tuning Guidelines

### Connection Pool
- Increase `maximum-pool-size` if connections exhaust
- Keep `minimum-idle` at 50% of maximum for responsiveness

### Message Processing
- Adjust `listener.concurrency` in JMS listener config based on CPU cores
- Batch size (20) suitable for typical 1-100KB payloads

### Database Queries
- Monitor slow query logs (enable slow-query-log in PostgreSQL)
- Analyze query execution plans (EXPLAIN command)
- Add indexes for frequently filtered columns

### Memory
- Increase `-Xmx` JVM setting if GC pauses occur
- Monitor with: `jmap`, `jstat`, Spring Boot Actuator `/metrics`

---

## Troubleshooting

### IBM MQ Connection Issues
- Verify IBM MQ running: `dspmq`
- Check queue manager status: `dspmqm -o all`
- Test connection: Use MQ explorer or `amqsput` test tool

### PostgreSQL Connection Issues
- Verify service running: `pg_isready -h localhost`
- Check credentials: `psql -h localhost -U postgres -d alert_service_db`
- Monitor connections: `SELECT count(*) FROM pg_stat_activity;`

### Transaction Persistence Issues
- Enable SQL logging: Set `hibernate.format_sql: true`
- Check transaction boundaries: Review @Transactional annotations
- Monitor pool: Call `/actuator/health/databaseHealth`

---

## References

- **Spring Boot**: https://spring.io/projects/spring-boot
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **IBM MQ**: https://www.ibm.com/products/mq
- **PostgreSQL**: https://www.postgresql.org/
- **HikariCP**: https://github.com/brettwooldridge/HikariCP
- **Hibernate**: https://hibernate.org/orm/
