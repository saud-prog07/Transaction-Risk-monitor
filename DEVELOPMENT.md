# Development & Testing Guide

## Development Environment Setup

### Prerequisites

- Java 17+ (check with `java -version`)
- Maven 3.8+ (check with `mvn -v`)
- PostgreSQL 12+ (check with `psql --version`)
- Git (for version control)

### IDE Setup (VS Code)

**Recommended Extensions**:
- Extension Pack for Java (Microsoft)
- Spring Boot Extension Pack (Pivotal)
- Maven for Java (Microsoft)
- Postman (REST API testing)

**Workspace Settings** (settings.json):
```json
{
  "java.home": "/path/to/jdk17",
  "maven.executable.path": "/path/to/mvn",
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "redhat.java",
  "[java]": {
    "editor.formatOnSave": true,
    "editor.defaultFormatter": "redhat.java"
  },
  "java.compile.nullAnalysis.mode": "automatic",
  "java.test.defaultConfig": "JUnit 5"
}
```

### IDE Setup (IntelliJ IDEA)

**Recommended Plugins**:
- Spring Boot
- Hibernate
- PostgreSQL
- REST Client

**Run Configuration**:
```
Name: run-all-services
Working directory: $ProjectFileDir$
Modules: all modules
Boot Applications: All Spring Boot applications
```

---

## Build & Compilation

### Clean Build

```bash
# Remove all build artifacts
mvn clean

# Rebuild everything
mvn clean install -DskipTests

# With test execution
mvn clean install
```

### Compilation Options

```bash
# Skip tests (faster build)
mvn clean install -DskipTests

# Offline mode (no internet)
mvn clean install -o

# Parallel build (faster multi-module)
mvn clean install -T 1C

# Verbose output for debugging
mvn clean install -X

# Show dependency tree
mvn dependency:tree
```

### Dependency Management

```bash
# Check for outdated dependencies
mvn versions:display-dependency-updates

# Check for security vulnerabilities
mvn org.owasp:dependency-check-maven:check

# Update all dependencies to latest
mvn versions:use-latest-versions
```

---

## Running Services Locally

### Option 1: Individual Terminal Sessions

**Terminal 1 - producer-service**:
```bash
cd producer-service
mvn spring-boot:run
```

**Terminal 2 - risk-engine**:
```bash
cd risk-engine
mvn spring-boot:run
```

**Terminal 3 - alert-service**:
```bash
cd alert-service
mvn spring-boot:run
```

### Option 2: Parallel Execution with Maven

```bash
# Run all services in parallel (requires pom.xml configuration)
mvn -DskipTests -pl producer-service,risk-engine,alert-service exec:java -Dexec.mainClass="..." &
```

### Option 3: Using Spring Boot Maven Plugin

```bash
# Run with specific port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9080"

# With additional JVM options
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m"

# With active profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

---

## Testing

### Unit Testing

**Framework**: JUnit 5 with Mockito

**Test File Locations**: `src/test/java/com/example/riskmonitoring/...`

**Example Unit Test**:
```java
@ExtendWith(MockitoExtension.class)
class TransactionIngestionServiceTest {
    
    @Mock
    private MessagePublisher messagePublisher;
    
    @InjectMocks
    private TransactionIngestionService service;
    
    @Test
    void testCreateTransaction() {
        // Arrange
        TransactionIngestionRequest request = 
            new TransactionIngestionRequest("USER001", BigDecimal.valueOf(100), "NYC");
        
        // Act
        Transaction result = service.createAndPublishTransaction(request);
        
        // Assert
        assertNotNull(result.getTransactionId());
        assertEquals("USER001", result.getUserId());
        verify(messagePublisher).publish(result);
    }
}
```

**Run Unit Tests**:
```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=TransactionIngestionServiceTest

# Single test method
mvn test -Dtest=TransactionIngestionServiceTest#testCreateTransaction

# Through IDE (F5 in VS Code, Run icon in IntelliJ)
```

### Integration Testing

**Framework**: Spring Boot Test with @SpringBootTest

**Test File**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class AlertControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AlertRepository repository;
    
    @Test
    void testCreateAlert() throws Exception {
        AlertRequest request = new AlertRequest(
            UUID.randomUUID(),
            "USER001",
            BigDecimal.valueOf(5000),
            "New York",
            Instant.now(),
            "HIGH",
            "Test reason"
        );
        
        mockMvc.perform(post("/api/alerts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.riskLevel").value("HIGH"));
    }
    
    private String asJsonString(Object obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}
```

**Run Integration Tests**:
```bash
# All integration tests
mvn verify

# Single integration test
mvn verify -Dtest=AlertControllerIntegrationTest

# Integration tests only (skip unit tests)
mvn verify -DskipUnitTests
```

### Test Coverage

```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html    # macOS
start target/site/jacoco/index.html   # Windows
xdg-open target/site/jacoco/index.html # Linux
```

**Coverage Target**: Aim for >80% code coverage

---

## Manual API Testing

### Using cURL

**Test transaction submission**:
```bash
# Simple transaction
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "TEST_USER",
    "amount": 1000.00,
    "location": "Test City"
  }' | jq '.'

# Extract transactionId for later use
TRANSACTION_ID=$(curl -s -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "TEST_USER",
    "amount": 1000.00,
    "location": "Test City"
  }' | jq -r '.transactionId')
```

### Using Postman

1. Create collection: "Risk Monitoring System"
2. Add folder for each service
3. Create requests:

**POST /transaction**:
```
URL: {{producer-url}}/transaction
Method: POST
Headers: Content-Type: application/json
Body: {
  "userId": "TEST_USER",
  "amount": 1000.00,
  "location": "Test City"
}
```

**GET /api/alerts**:
```
URL: {{alert-url}}/api/alerts
Method: GET
Params: page=0, size=20
```

### Using REST Client (VS Code Extension)

Create `requests.http`:
```http
### Transaction Submission
POST http://localhost:8080/transaction
Content-Type: application/json

{
  "userId": "TEST_USER",
  "amount": 5000.00,
  "location": "Test City"
}

### Get All Alerts
GET http://localhost:8082/api/alerts?page=0&size=20

### Get Statistics
GET http://localhost:8082/api/alerts/statistics

### Mark Alert Reviewed
PUT http://localhost:8082/api/alerts/1/review
Content-Type: application/json

{
  "investigationNotes": "Verified transaction"
}
```

---

## Debugging

### Enable Debug Logging

**application.yml**:
```yaml
logging:
  level:
    root: INFO
    com.example.riskmonitoring: DEBUG
    org.springframework.jms: DEBUG
    org.springframework.data: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Remote Debugging

**Start service in debug mode**:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

**Connect IDE debugger**:
- VS Code: Launch → Add Configuration → Java: Attach to Java Process
- IntelliJ: Run → Edit Configurations → Add Remote → localhost:5005

### Common Debug Points

```java
// Set breakpoints at:
// 1. Controller entry points
@PostMapping("/transaction")
public ResponseEntity<Transaction> submit(...) {

// 2. Service logic
@Transactional
public void createAlert(AlertRequest request) {

// 3. Database operations
finder.save(entity);

// 4. Message publishing
publisher.publish(message);

// 5. Exception handlers
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handle(Exception e) {
```

### View Application Logs

```bash
# Tail logs from jar execution
tail -f service.log

# Filter logs by level
grep "ERROR\|WARN" service.log

# Track specific requests
grep "REQUEST\|RESPONSE" service.log

# Monitor database queries
grep "Hibernate\|SQL" service.log
```

---

## Database Testing & Debugging

### PostgreSQL CLI Commands

```bash
# Connect to database
psql -h localhost -U postgres -d alert_service_db

# Inside psql:
\dt                                    # List tables
\di                                    # List indexes
SELECT * FROM flagged_transactions;    # View all alerts
SELECT COUNT(*) FROM flagged_transactions;
SELECT COUNT(*) FROM flagged_transactions WHERE reviewed = false;
SELECT * FROM v_high_risk_alerts;      # View high-risk alerts
\d flagged_transactions                # Table structure
\l                                     # List all databases
```

### Data Inspection Queries

```sql
-- Count by risk level
SELECT risk_level, COUNT(*) as count FROM flagged_transactions 
GROUP BY risk_level;

-- Find unreviewed alerts older than 7 days
SELECT * FROM flagged_transactions 
WHERE reviewed = false AND created_at < NOW() - INTERVAL '7 days'
ORDER BY created_at DESC;

-- Performance: Find slowest queries
SELECT query_start, state, query FROM pg_stat_activity 
WHERE state != 'idle' 
ORDER BY query_start DESC;

-- Index usage statistics
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch 
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;

-- Table size
SELECT tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) 
FROM pg_tables 
WHERE schemaname='public';
```

### Reset Database for Testing

```bash
# Option 1: Drop and recreate
psql -h localhost -U postgres -c "DROP DATABASE IF EXISTS alert_service_db;"
psql -h localhost -U postgres -c "CREATE DATABASE alert_service_db;"

# Option 2: Clear all data (keep schema)
psql -h localhost -U postgres -d alert_service_db -c "TRUNCATE flagged_transactions CASCADE;"

# Option 3: Through Hibernate
# application-test.yml: spring.jpa.hibernate.ddl-auto: create-drop
```

---

## Performance Testing

### Load Testing with JMeter

**Setup**:
1. Download JMeter
2. Create test plan:
   - Thread Group (100 users, 10 second ramp-up)
   - HTTP Request (POST /transaction)
   - Response Assertion
   - View Results Tree

**Run**:
```bash
jmeter -n -t test_plan.jmx -l results.jtl -j jmeter.log
```

### Load Testing with Apache Bench

```bash
# Test alert query (50 concurrent, 1000 total)
ab -n 1000 -c 50 http://localhost:8082/api/alerts

# Test transaction submission
ab -p transaction.json -T application/json -n 1000 -c 50 http://localhost:8080/transaction
```

### Monitoring Performance

**JVM Metrics**:
```bash
# View GC activity
jstat -gc -h10 <pid> 1000

# Monitor heap usage
jmap -heap <pid>

# Generate thread dump
jstack <pid> > threads.txt
```

**Application Metrics**:
```bash
# View all metrics
curl http://localhost:8082/actuator/metrics | jq '.'

# View specific metric
curl http://localhost:8082/actuator/metrics/http.server.requests | jq '.'

# Database connection pool
curl http://localhost:8082/actuator/health/databaseHealth

# JVM memory
curl http://localhost:8082/actuator/metrics/jvm.memory.used | jq '.'
```

---

## Code Quality & Standards

### Checkstyle

Add to pom.xml:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.2.1</version>
    <configuration>
        <configLocation>google_checks.xml</configLocation>
    </configuration>
</plugin>
```

Run:
```bash
mvn checkstyle:check
```

### SpotBugs (Find Bugs)

```bash
mvn spotbugs:check
```

### SonarQube Integration

```bash
# Install SonarQube locally
docker run -d --name sonarqube -p 9000:9000 sonarqube

# Run analysis
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=risk-monitoring \
  -Dsonar.sources=. \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>
```

---

## Version Control Workflow

### Git Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: feat, fix, docs, style, refactor, test, chore

**Example**:
```
feat(risk-engine): add location anomaly analyzer

Add new LocationAnomalyAnalyzer strategy implementation
to detect transactions from new geographic locations.

Fixes #123
```

### Branch Strategy

```
main (stable production code)
├── release/v1.0 (release candidates)
└── develop (integration branch)
    ├── feature/high-amount-analyzer
    ├── feature/frequency-analyzer
    ├── bugfix/connection-pool-leak
    └── docs/api-documentation
```

### Pull Request Checklist

- [ ] Code compiles without warnings
- [ ] All tests pass (unit + integration)
- [ ] >80% code coverage on new code
- [ ] No security vulnerabilities (dependency-check)
- [ ] Code follows style guide (checkstyle, spotbugs)
- [ ] Commit messages follow format
- [ ] PR description explains changes
- [ ] Documentation updated if needed

---

## Troubleshooting Common Issues

### "Port already in use"

```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or start on different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9080"
```

### "Connection refused" (MQ/Database)

```bash
# Check PostgreSQL
psql -h localhost -U postgres -c "SELECT 1;"

# Check IBM MQ
dspmq

# View open ports
netstat -an | grep LISTEN
```

### "Out of memory"

```bash
# Increase heap size
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx1024m -Xms512m"

# Check memory usage
jmap -heap <pid>
```

### "Slow queries"

```bash
# Enable query logging
# application.yml: logging.level.org.hibernate.SQL: DEBUG

# View execution time
curl http://localhost:8082/actuator/metrics/http.server.requests
```

### "Transaction rollback"

```java
// Check:
// 1. @Transactional presence
@Service
@Transactional  // Missing?
public class AlertService { }

// 2. Exception handling (checked exceptions)
catch (IOException e) {  // Checked - no rollback
}

// 3. Database constraints
// Verify NOT NULL constraints match model

// 4. Connection pool exhaustion
curl http://localhost:8082/actuator/health/databaseHealth
```

---

## Release & Deployment

### Build Release Package

```bash
# Prepare release
mvn release:prepare

# Perform release
mvn release:perform

# This creates:
# - Tagged version in Git
# - JAR artifact in target/
# - Updated pom.xml versions
```

### Create Executable JAR

```bash
# Build fat JAR
mvn clean package

# Run JAR
java -jar alert-service/target/alert-service-1.0.0.jar

# With custom port
java -jar alert-service/target/alert-service-1.0.0.jar \
  --server.port=9082 \
  --spring.datasource.url=jdbc:postgresql://prod-host:5432/alert_prod
```

---

## Documentation Standards

### Code Comments

```java
/**
 * Analyzes a transaction for risk indicators.
 *
 * This method orchestrates all risk analyzers and aggregates results
 * into a single RiskLevel determination.
 *
 * @param transaction the transaction to analyze (non-null)
 * @return RiskResult containing risk level and reasons (non-null)
 * @throws RiskAnalysisException if analysis fails
 *
 * @see HighAmountAnalyzer
 * @see FrequencyAnalyzer
 * @see LocationAnomalyAnalyzer
 */
public RiskResult analyzeTransaction(Transaction transaction) {
    // Implementation...
}
```

### Repository Documentation

Update README.md files in each module with:
- Module purpose
- Key classes
- Configuration properties
- Example usage
- Common issues

### API Documentation

Endpoints should document:
- Request/response examples
- Required fields
- Valid values
- Error conditions
