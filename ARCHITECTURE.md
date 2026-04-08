# System Architecture

## High-Level Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                    CLIENT / EXTERNAL SYSTEM                         │
│                   (REST API Consumers)                              │
└────────────────────┬────────────────────────────────────────────────┘
                     │
        ┌────────────┴────────────────┐
        │                             │
        ▼                             ▼
   PRODUCER-SERVICE              ALERT-SERVICE
   (Port 8080)                   (Port 8082)
   ├─ Transaction Controller     ├─ Alert REST API
   ├─ Ingestion Service          ├─ Alert Service (Logic)
   ├─ IBM MQ Publisher           ├─ Spring Data JPA
   └─ Validation/Error Handling  └─ PostgreSQL Connector
             │
             │ JMS Message
             │ (TRANSACTION_QUEUE)
             ▼
        IBM MQ (1414)
        [Asynchronous Broker]
             │
             ▼
      RISK-ENGINE
      (Port 8081)
      ├─ Transaction Listener
      ├─ Risk Analysis Service
      ├─ 3 Risk Analyzers:
      │  ├─ HighAmountAnalyzer
      │  ├─ FrequencyAnalyzer
      │  └─ LocationAnomalyAnalyzer
      ├─ H2 Database
      └─ Alert REST Client
             │
             │ HTTP POST
             │ /api/alerts
             ▼
       ALERT-SERVICE
       (Persistence)
             │
             ▼
        PostgreSQL
        Database
        (alert_service_db)
```

## Detailed Service Architecture

### 1. producer-service Architecture

```
┌──────────────────────────────────────────────────────┐
│          producer-service (Port 8080)                │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │     HTTP Request Handler                       │ │
│  │  POST /transaction                             │ │
│  │  Content-Type: application/json                │ │
│  └─────────────┬──────────────────────────────────┘ │
│                │                                     │
│  ┌─────────────▼──────────────────────────────────┐ │
│  │  TransactionIngestionController                │ │
│  │  ├─ @PostMapping("/transaction")               │ │
│  │  └─ calls TransactionIngestionService          │ │
│  └─────────────┬──────────────────────────────────┘ │
│                │                                     │
│  ┌─────────────▼──────────────────────────────────┐ │
│  │  TransactionIngestionService                   │ │
│  │  ├─ Generates: transactionId (UUID)            │ │
│  │  ├─ Sets: timestamp (Instant.now())            │ │
│  │  └─ Calls: MessagePublisher.publish()          │ │
│  └─────────────┬──────────────────────────────────┘ │
│                │                                     │
│  ┌─────────────▼──────────────────────────────────┐ │
│  │  MessagePublisher (Interface)                  │ │
│  │  └─ publish(Transaction)                       │ │
│  └─────────────┬──────────────────────────────────┘ │
│                │                                     │
│  ┌─────────────▼──────────────────────────────────┐ │
│  │  IBMMQPublisher (Implementation)               │ │
│  │  ├─ @Retryable(maxAttempts=3)                 │ │
│  │  ├─ Converts Transaction → JSON Message        │ │
│  │  ├─ Sends via JmsTemplate.convertAndSend()     │ │
│  │  └─ Queue: TRANSACTION_QUEUE                   │ │
│  └─────────────┬──────────────────────────────────┘ │
│                │                                     │
│  ┌─────────────▼──────────────────────────────────┐ │
│  │  JmsConfiguration                              │ │
│  │  ├─ MQQueueConnectionFactory                   │ │
│  │  ├─ CachingConnectionFactory (10 sessions)     │ │
│  │  ├─ JmsTemplate (5s timeout)                   │ │
│  │  └─ ObjectMapper (ISO date format)             │ │
│  └─────────────┬──────────────────────────────────┘ │
│                │                                     │
└────────────────┼─────────────────────────────────────┘
                 │ Retry Logic (3 attempts)
                 │ Exponential Backoff: 1-4 seconds
                 ▼
             IBM MQ
        TRANSACTION_QUEUE
```

**Request/Response Cycle**:
```
1. Client POST /transaction with TransactionIngestionRequest
   {
     "userId": "USER123",
     "amount": 5000.00,
     "location": "New York, NY"
   }

2. Service generates:
   {
     "transactionId": "550e8400-e29b-41d4-a716-446655440000",  // UUID
     "userId": "USER123",
     "amount": 5000.00,
     "timestamp": "2024-01-15T10:30:00Z",  // Current instant
     "location": "New York, NY"
   }

3. Publishes to MQ with retry logic (if fails:)
   Attempt 1: Immediate
   Attempt 2: Wait 1 second + jitter
   Attempt 3: Wait 2-4 seconds + jitter

4. Returns 201 CREATED with full transaction object
```

---

### 2. risk-engine Architecture

```
┌──────────────────────────────────────────────────────────────┐
│              risk-engine (Port 8081)                         │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  JMS Message Listener                                 │  │
│  │  @JmsListener(destination = "TRANSACTION_QUEUE")      │  │
│  │  onMessage(Message message)                           │  │
│  └───────────────┬─────────────────────────────────────┘  │
│                  │                                         │
│  ┌───────────────▼─────────────────────────────────────┐  │
│  │  MessageConverter (Jackson)                          │  │
│  │  Deserialize JSON → Transaction object               │  │
│  └───────────────┬─────────────────────────────────────┘  │
│                  │                                         │
│  ┌───────────────▼─────────────────────────────────────┐  │
│  │  RiskAnalysisService (Orchestrator)                 │  │
│  │  ├─ Receives: Transaction                            │  │
│  │  ├─ 1. Load user history from H2                    │  │
│  │  ├─ 2. Run all analyzers (parallel executes)        │  │
│  │  ├─ 3. Aggregate results (HIGH > MEDIUM > LOW)      │  │
│  │  ├─ 4. Persist transaction history                  │  │
│  │  └─ 5. Send alert if risk >= MEDIUM               │  │
│  └───────────────┬─────────────────────────────────────┘  │
│                  │                                         │
│  ┌───────────────┴──────────────────────────┐             │
│  │         Risk Analyzers                   │             │
│  │      (Strategy Pattern)                  │             │
│  │                                          │             │
│  │  ┌──────────────────────────────────┐    │             │
│  │  │ HighAmountAnalyzer               │    │             │
│  │  │ ├─ Query: avg(amount) by userId  │    │             │
│  │  │ ├─ Rule: amount > 2.0 * average  │    │             │
│  │  │ ├─ Window: All history           │    │             │
│  │  │ └─ Result: HIGH/MEDIUM/NONE      │    │             │
│  │  └──────────────────────────────────┘    │             │
│  │                                          │             │
│  │  ┌──────────────────────────────────┐    │             │
│  │  │ FrequencyAnalyzer                │    │             │
│  │  │ ├─ Query: count by userId + time │    │             │
│  │  │ ├─ Window: Last 5 minutes        │    │             │
│  │  │ ├─ Rule: >= 5 transactions       │    │             │
│  │  │ ├─ Escalation: >= 10 → HIGH      │    │             │
│  │  │ └─ Result: MEDIUM/HIGH/NONE      │    │             │
│  │  └──────────────────────────────────┘    │             │
│  │                                          │             │
│  │  ┌──────────────────────────────────┐    │             │
│  │  │ LocationAnomalyAnalyzer          │    │             │
│  │  │ ├─ Query: locations by userId    │    │             │
│  │  │ ├─ Window: Last 30 days          │    │             │
│  │  │ ├─ Rule: Location not in history │    │             │
│  │  │ └─ Result: LOW/NONE              │    │             │
│  │  └──────────────────────────────────┘    │             │
│  │                                          │             │
│  └──────────────┬───────────────────────────┘             │
│                 │                                         │
│  ┌──────────────▼────────────────────────────────────┐  │
│  │  Result Aggregation                               │  │
│  │  ├─ Combine all analyzer results                  │  │
│  │  ├─ Priority: HIGH > MEDIUM > LOW                 │  │
│  │  ├─ Concatenate reasons                           │  │
│  │  └─ Determine final RiskResult                    │  │
│  └──────────────┬────────────────────────────────────┘  │
│                 │                                        │
│  ┌──────────────┼────────────────────────────────────┐  │
│  │              │                                    │  │
│  │              ├─ Persist to H2 ────────┐         │  │
│  │              │                         │         │  │
│  │  ┌───────────▼──────────────┐  ┌──────▼──────┐  │  │
│  │  │ TransactionHistory (JPA) │  │ H2 Database │  │  │
│  │  │ ├─ transactionId         │  │ (In-Memory) │  │  │
│  │  │ ├─ userId                │  │             │  │  │
│  │  │ ├─ amount                │  │ Indexes:    │  │  │
│  │  │ ├─ timestamp             │  │ - userId    │  │  │
│  │  │ ├─ location              │  │ - timestamp │  │  │
│  │  │ ├─ createdAt             │  │ - riskLevel │  │  │
│  │  │ └─ updatedAt             │  │             │  │  │
│  │  └──────────────────────────┘  └─────────────┘  │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
│                 │                                        │
│  ┌──────────────▼────────────────────────────────────┐  │
│  │  AlertNotificationService                         │  │
│  │  ├─ Prepares AlertPayload (complete data)         │  │
│  │  ├─ POST to: alert-service /api/alerts            │  │
│  │  ├─ Handles: HTTP connection errors                │  │
│  │  └─ Logs: Success/failure at INFO level            │  │
│  └──────────────┬────────────────────────────────────┘  │
│                 │                                        │
└─────────────────┼────────────────────────────────────────┘
                  │ HTTP POST (AlertPayload)
                  │ {
                  │   transactionId, userId, amount,
                  │   location, timestamp, riskLevel, reason
                  │ }
                  ▼
            ALERT-SERVICE
            /api/alerts
```

**Risk Scoring Logic**:
```
Input: Transaction
  userId: "USER001"
  amount: 5000.00
  location: "New York, NY"
  timestamp: 2024-01-15T10:30:00Z

Step 1: HighAmountAnalyzer
  Query: avg(amount) for USER001 = 2000.00
  Check: 5000 / 2000 = 2.5x (threshold: 2.0x)
  Result: HIGH_RISK (reason: "Amount is 2.5x average")

Step 2: FrequencyAnalyzer
  Query: count in last 5 minutes = 4 txns
  Check: 4 < 5 (threshold)
  Result: NO_RISK

Step 3: LocationAnomalyAnalyzer
  Query: locations in last 30 days = ["Chicago", "Los Angeles"]
  Check: "New York" not in list
  Result: LOW_RISK (reason: "New location detected")

Step 4: Aggregate
  Priority: HIGH > MEDIUM > LOW
  Final: HIGH_RISK
  Reason: "Amount is 2.5x average; New location detected"

Step 5: Send Alert
  POST /api/alerts with complete details
```

---

### 3. alert-service Architecture

```
┌─────────────────────────────────────────────────────────┐
│        alert-service (Port 8082)                        │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │     REST API Controller (AlertController)         │  │
│  │                                                  │  │
│  │  POST   /api/alerts              ─┐              │  │
│  │  GET    /api/alerts               │              │  │
│  │  GET    /api/alerts/{id}          │              │  │
│  │  GET    /api/alerts/.../*         │ HTTP Routes  │  │
│  │  PUT    /api/alerts/{id}/review   │              │  │
│  │  GET    /api/alerts/statistics    │              │  │
│  │  GET    /actuator/health          ─┘              │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                       │
│  ┌──────────────▼───────────────────────────────────┐  │
│  │     Service Layer (AlertService)                 │  │
│  │                                                  │  │
│  │  ├─ createAlert(AlertRequest)                    │  │
│  │  │  ├─ Validate request (@Valid)                │  │
│  │  │  ├─ Check uniqueness (transactionId)         │  │
│  │  │  ├─ Parse RiskLevel enum → convert string    │  │
│  │  │  ├─ Create FlaggedTransaction entity         │  │
│  │  │  └─ @Transactional persist to database       │  │
│  │  │                                               │  │
│  │  ├─ getAlertById(Long id)                       │  │
│  │  │  ├─ Query repository                         │  │
│  │  │  └─ Throw 404 if not found                   │  │
│  │  │                                               │  │
│  │  ├─ getAllAlerts(Pageable)                      │  │
│  │  │  ├─ Support sorting: createdAt, riskLevel    │  │
│  │  │  └─ Return: Page<AlertResponse>              │  │
│  │  │                                               │  │
│  │  ├─ getAlertsByRiskLevel(RiskLevel, Pageable)   │  │
│  │  │  └─ Custom repository query                  │  │
│  │  │                                               │  │
│  │  ├─ getUnreviewedAlerts(Pageable)               │  │
│  │  │  └─ Filter: reviewed = false                 │  │
│  │  │                                               │  │
│  │  ├─ markAsReviewed(Long id, notes)              │  │
│  │  │  ├─ Find alert                               │  │
│  │  │  ├─ Set reviewed = true                      │  │
│  │  │  ├─ Add investigation notes (optional)       │  │
│  │  │  └─ @Transactional persist                   │  │
│  │  │                                               │  │
│  │  └─ getStatistics()                             │  │
│  │     ├─ Count all alerts (GROUP BY risk_level)   │  │
│  │     └─ Return: {total, unreviewed, high, med}   │  │
│  │                                                  │  │
│  │  @Transactional annotations:                    │  │
│  │  ├─ readOnly=true on GET methods                │  │
│  │  ├─ readOnly=false on POST, PUT, DELETE         │  │
│  │  └─ Propagation=REQUIRED (default)              │  │
│  │                                                  │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                       │
│  ┌──────────────▼───────────────────────────────────┐  │
│  │     Repository Layer (Spring Data JPA)           │  │
│  │  FlaggedTransactionRepository                    │  │
│  │                                                  │  │
│  │  ├─ save(FlaggedTransaction)                    │  │
│  │  ├─ findById(Long)                              │  │
│  │  ├─ findByTransactionId(UUID)                   │  │
│  │  ├─ findByRiskLevel(RiskLevel, Pageable)        │  │
│  │  ├─ findByReviewedFalse(Pageable)               │  │
│  │  ├─ findByCreatedAtBetween(Instant, Instant)    │  │
│  │  ├─ countByRiskLevel(RiskLevel)                 │  │
│  │  ├─ countByReviewedFalse()                      │  │
│  │  └─ Custom @Query for complex queries           │  │
│  │                                                  │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                       │
│  ┌──────────────▼───────────────────────────────────┐  │
│  │     Entity Layer (JPA)                           │  │
│  │  FlaggedTransaction                             │  │
│  │                                                  │  │
│  │  @Entity @Table("flagged_transactions")         │  │
│  │  ├─ @Id Long id (auto-generated)                │  │
│  │  ├─ @Column(unique=true) UUID transactionId     │  │
│  │  ├─ @Enumerated RiskLevel riskLevel             │  │
│  │  ├─ @Column(length=1000) String reason          │  │
│  │  ├─ @CreationTimestamp Instant createdAt        │  │
│  │  ├─ @UpdateTimestamp Instant updatedAt          │  │
│  │  ├─ boolean reviewed (default: false)           │  │
│  │  ├─ @Column(length=2000) String notes           │  │
│  │  │                                               │  │
│  │  @Indexes:                                      │  │
│  │  ├─ idx_transaction_id (unique)                 │  │
│  │  ├─ idx_risk_level                              │  │
│  │  ├─ idx_created_at                              │  │
│  │  └─ idx_risk_level_created (composite)          │  │
│  │                                                  │  │
│  │  @PrePersist:                                   │  │
│  │  ├─ SET createdAt = NOW()                       │  │
│  │  ├─ SET updatedAt = NOW()                       │  │
│  │  └─ SET reviewed = false                        │  │
│  │                                                  │  │
│  │  @PreUpdate:                                    │  │
│  │  └─ SET updatedAt = NOW()                       │  │
│  │                                                  │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                       │
│  ┌──────────────▼───────────────────────────────────┐  │
│  │     Configuration Layer                         │  │
│  │                                                  │  │
│  │  ├─ DatabaseConfiguration                       │  │
│  │  │  ├─ @EnableJpaRepositories                   │  │
│  │  │  ├─ @EnableTransactionManagement             │  │
│  │  │  └─ JpaTransactionManager bean               │  │
│  │  │                                               │  │
│  │  ├─ JpaAuditingConfiguration                    │  │
│  │  │  ├─ @EnableJpaAuditing                       │  │
│  │  │  └─ auditorAwareRef: AuditorProvider         │  │
│  │  │                                               │  │
│  │  ├─ WebConfiguration                            │  │
│  │  │  └─ @EnableSpringDataWebSupport              │  │
│  │  │                                               │  │
│  │  └─ AuditorProvider                             │  │
│  │     └─ Returns: "SYSTEM"                        │  │
│  │                                                  │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                       │
│  ┌──────────────▼───────────────────────────────────┐  │
│  │     Monitoring & Observability                   │  │
│  │                                                  │  │
│  │  ├─ DatabaseHealthIndicator                     │  │
│  │  │  ├─ Component name: "databaseHealth"         │  │
│  │  │  ├─ Endpoint: /actuator/health/databaseHealth│  │
│  │  │  └─ Returns: {status, pool metrics}          │  │
│  │  │                                               │  │
│  │  ├─ TransactionMonitoringAspect                 │  │
│  │  │  ├─ @Aspect advises @Transactional methods   │  │
│  │  │  ├─ Logs: Duration, status, errors           │  │
│  │  │  └─ Level: DEBUG (performance tracking)      │  │
│  │  │                                               │  │
│  │  └─ GlobalExceptionHandler                      │  │
│  │     ├─ @ControllerAdvice                        │  │
│  │     ├─ Handles: AlertNotFoundException,         │  │
│  │     │           AlertAlreadyExistsException,    │  │
│  │     │           BindException,                  │  │
│  │     │           HttpMessageNotReadableException │  │
│  │     └─ Returns: ErrorResponse DTO               │  │
│  │                                                  │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                       │
└─────────────────┼───────────────────────────────────────┘
                  │ JDBC/Hibernate
                  ▼
         ┌─────────────────────┐
         │   PostgreSQL DB     │
         │                     │
         │ Database:           │
         │ alert_service_db    │
         │                     │
         │ Connection Pool:    │
         │ ├─ Max: 10          │
         │ ├─ Min: 5           │
         │ └─ Timeout: 20s     │
         │                     │
         │ Table:              │
         │ flagged_transactions│
         │                     │
         │ Indexes:            │
         │ ├─ idx_trans_id     │
         │ ├─ idx_risk_level   │
         │ ├─ idx_created_at   │
         │ └─ idx_composite    │
         │                     │
         │ Views:              │
         │ ├─ v_unreviewed     │
         │ ├─ v_high_risk      │
         │ └─ v_recent         │
         │                     │
         └─────────────────────┘
```

**Transaction Lifecycle In alert-service**:
```
1. REST Request arrives (AlertRequest)
   {
     "transactionId": "550e8400-...",
     "userId": "USER001",
     "amount": 5000.00,
     "location": "New York, NY",
     "timestamp": "2024-01-15T10:30:00Z",
     "riskLevel": "HIGH",
     "reason": "Amount is 2.5x average"
   }

2. Controller receives request
   ├─ @Valid triggers validation
   ├─ All @NotNull/@NotBlank fields checked
   └─ If invalid → Return 400 BAD_REQUEST

3. Service createAlert() executes @Transactional
   ├─ Check uniqueness: SELECT by transactionId
   ├─ If exists → Throw AlertAlreadyExistsException (409)
   ├─ Parse RiskLevel enum: "HIGH" → RiskLevel.HIGH
   ├─ Create FlaggedTransaction entity
   ├─ Entity @PrePersist lifecycle:
   │  ├─ SET createdAt = Instant.now()
   │  ├─ SET updatedAt = Instant.now()
   │  └─ SET reviewed = false
   ├─ Repository.save() persistence:
   │  ├─ Hibernate converts entity → SQL INSERT
   │  ├─ HikariCP acquires connection from pool
   │  ├─ Execute: INSERT INTO flagged_transactions (...)
   │  ├─ Return generated id
   │  └─ Release connection back to pool
   ├─ Transaction @Transactional COMMIT
   ├─ AOP aspect logs:
   │  └─ "Transaction completed in 45ms"
   └─ Return AlertResponse (id assigned from DB)

4. Response sent to client
   {
     "id": 1,
     "transactionId": "550e8400-...",
     "riskLevel": "HIGH",
     "reason": "Amount is 2.5x average",
     "createdAt": "2024-01-15T10:30:05Z",
     "reviewed": false
   }

5. Database state persisted to PostgreSQL
   ├─ Row inserted with auto-generated SEQUENCE
   ├─ Indexes updated: idx_transaction_id, idx_risk_level, idx_created_at
   └─ COMMIT ensures durability (ACID)
```

---

## Message Flow Diagram

```
START
  │
  ├─→ Client submits transaction
  │   POST /transaction
  │   {userId, amount, location}
  │
  ├─→ producer-service
  │   ├─ Validates input
  │   ├─ Generates transactionId (UUID)
  │   ├─ Sets timestamp (Instant.now())
  │   ├─ Publishes to MQ TRANSACTION_QUEUE
  │   │  (With retry: 3 attempts, exponential backoff)
  │   └─ Returns 201 CREATED
  │
  ├─→ IBM MQ TRANSACTION_QUEUE (Broker)
  │   └─ Message persisted on disk
  │
  ├─→ risk-engine listener receives message
  │   ├─ Deserializes JSON → Transaction
  │   └─ Calls RiskAnalysisService
  │
  ├─→ RiskAnalysisService runs analyzers
  │   ├─ Loads user transaction history from H2
  │   ├─ HighAmountAnalyzer: Check amount vs average
  │   ├─ FrequencyAnalyzer: Count recent transactions
  │   ├─ LocationAnomalyAnalyzer: Check location history
  │   ├─ Aggregates results (HIGH > MEDIUM > LOW)
  │   ├─ Persists transaction to H2 TransactionHistory
  │   └─ Sends alert if risk >= MEDIUM
  │
  ├─→ AlertNotificationService makes REST call
  │   POST /api/alerts
  │   {
  │     transactionId, userId, amount, location,
  │     timestamp, riskLevel, reason
  │   }
  │
  ├─→ alert-service receives alert
  │   ├─ Validates AlertRequest (@Valid)
  │   ├─ Checks uniqueness (transactionId)
  │   ├─ Creates FlaggedTransaction entity
  │   ├─ @PrePersist sets timestamps
  │   ├─ @Transactional persists to PostgreSQL
  │   └─ Returns 201 CREATED
  │
  ├─→ PostgreSQL persists to flagged_transactions table
  │   ├─ Row inserted with auto-generated id
  │   ├─ Indexes updated
  │   ├─ COMMIT ensures durability
  │   └─ View data reflects immediately
  │
  └─→ Client can query alerts
      GET /api/alerts
      Returns: Paginated list of all flagged transactions
```

---

## Data Models

### Transaction (Shared Model)
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction implements Serializable {
    private UUID transactionId;        // Generated by producer
    private String userId;             // From client
    private BigDecimal amount;         // From client
    private Instant timestamp;         // Generated by producer
    private String location;           // From client
}
```

### RiskResult (Shared Model)
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskResult {
    private UUID transactionId;        // Reference to transaction
    private RiskLevel riskLevel;      // Enum: LOW, MEDIUM, HIGH
    private String reason;             // Detailed risk reason
}
```

### FlaggedTransaction (Persistent Entity)
```java
@Entity
@Table(name = "flagged_transactions")
public class FlaggedTransaction {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;                   // Database PK
    
    @Column(unique = true)
    private UUID transactionId;        // Business key
    
    @Enumerated(STRING)
    private RiskLevel riskLevel;      // HIGH, MEDIUM, LOW
    
    @Column(length = 1000)
    private String reason;             // Risk explanation
    
    @CreationTimestamp
    private Instant createdAt;         // Record creation time
    
    @UpdateTimestamp
    private Instant updatedAt;         // Last update time
    
    private boolean reviewed;          // Investigation flag
    
    @Column(length = 2000)
    private String investigationNotes; // Analyst notes
}
```

### AlertRequest (Inbound DTO)
```java
@Valid
public class AlertRequest {
    @NotNull
    private UUID transactionId;
    
    @NotBlank
    private String userId;
    
    @NotNull
    @Positive
    private BigDecimal amount;
    
    @NotBlank
    private String location;
    
    @NotNull
    private Instant timestamp;
    
    @NotNull
    private String riskLevel;         // String: "HIGH", "MEDIUM", "LOW"
    
    @NotBlank
    private String reason;
}
```

---

## Technology Stack Justification

| Technology | Purpose | Why Chosen |
|-----------|---------|-----------|
| Spring Boot 3.4.4 | Core framework | Latest stable, extensive ecosystem |
| Spring Data JPA | ORM abstraction | Convention-based, reduces boilerplate |
| Hibernate | JPA implementation | Battle-tested, excellent JDBC generation |
| PostgreSQL | Persistent database | ACID compliance, reliability, performance |
| IBM MQ | Message broker | Enterprise-grade, reliability, clustering |
| Spring JMS | JMS abstraction | Decouples from MQ API, easy testing |
| HikariCP | Connection pool | Lightweight, high-performance, battle-tested |
| Lombok | Code generation | Reduces boilerplate (getters, setters, builders) |
| Jackson | JSON serialization | Standard for Spring, customizable |
| Spring Retry | Retry logic | Exponential backoff, configurable |
| Spring AOP | Cross-cutting concerns | Transaction monitoring, performance tracking |
| Spring Actuator | Monitoring | Health checks, metrics, environment inspection |
