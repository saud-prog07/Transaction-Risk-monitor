# Backend - Transaction Risk Monitoring System

Production-grade Java microservices for real-time fraud detection with advanced statistical analysis and resilience patterns.

## Overview

The backend consists of 4 intelligent microservices designed for high reliability and scalability:

| Service | Port | Responsibility | Highlights |
|---------|------|-----------------|------------|
| **Producer Service** | 8080 | Transaction ingestion REST API | Validation, duplication detection, idempotency |
| **Risk Engine** | 8081 | 4 parallel fraud detection algorithms | Statistical baselines, anomaly detection, configurable |
| **Alert Service** | 8082 | Alert persistence & retrieval API | PostgreSQL ACID, audit trail, search/filter |
| **Common Models** | - | Shared DTOs, entities, utilities | Centralized domain logic |

**Infrastructure:**
- **PostgreSQL 15** - ACID-compliant storage with audit mutations table
- **IBM MQ Enterprise** - Guaranteed message delivery with ACID transactions
- **Docker & Compose** - Multi-stage builds, health checks, production-ready

## Microservices Architecture

### Service Communication
```
Producer Service -> IBM MQ -> Risk Engine -> IBM MQ -> Alert Service -> PostgreSQL
    (REST)       (ASYNC)   (PARALLEL)    (ASYNC)   (Persistence)    (AUDIT)
```

### Risk Engine - 4 Parallel Analyzers
All implement `RiskAnalyzer` interface, Spring @Component registered, configurable via YAML:

1. **Enhanced Amount Analyzer** (~200 lines)
   - Statistical detection: avg user spend +/- 2x standard deviation
   - Prevents false positives for high-earners
   - Configurable multiplier (default: 2.0 sigma)

2. **Time Anomaly Analyzer** (~180 lines)
   - Business hour pattern detection
   - Flags unusual transaction times
   - User-specific baseline calculations

3. **Location Anomaly Analyzer** (~220 lines)
   - Frequency-based detection with baselines
   - Geolocation validation
   - Impossible travel route detection

4. **User Baseline Calculator** (~280 lines)
   - Comprehensive historical analysis
   - Statistical metrics: mean, std dev, min, max
   - Transaction pattern learning

### Resilience Patterns
- Retry Logic: Exponential backoff (max 3 attempts, 2s-4s delays)
- Idempotency: UUID deduplication + transaction cache
- Dead Letter Queue: Failed messages preserved for analysis
- Health Checks: Database, MQ broker, service readiness verified
- Circuit Breaker: MQ connection failure handling

## 📁 Structure

```
backend/
├── common-models/                          # Shared domain logic
│   └── src/main/java/
│       └── com/example/riskmonitoring/
│           ├── dto/                       # Data Transfer Objects
│           ├── entity/                    # JPA entities (Transaction, Alert)
│           └── util/                      # Helper utilities
├── producer-service/                       # Transaction Gateway (Port 8080)
│   └── src/main/java/
│       └── com/example/riskmonitoring/
│           ├── controller/                # REST endpoints
│           ├── service/                   # Business logic
│           └── mq/                        # MQ publishing
├── risk-engine/                            # Fraud Detection Engine (Port 8081)
│   └── src/main/java/
│       └── com/example/riskmonitoring/
│           ├── analyzer/                  # 4 Risk Analyzers (Statistical)
│           ├── config/                    # RiskAnalyzerConfig service
│           └── processor/                 # MQ message processing
├── alert-service/                          # Alert Management (Port 8082)
│   └── src/main/java/
│       └── com/example/riskmonitoring/
│           ├── controller/                # REST API endpoints
│           ├── persistence/               # JPA repositories
│           └── dto/                       # Alert response DTOs
├── docker-compose.yml                     # Full stack orchestration
├── postgres-init.sql                      # Schema + 6 Flyway migrations
├── pom.xml                                # Parent POM (dependency management)
└── .env.example                           # Configuration template
```

## Quick Start

### Prerequisites

```bash
java -version              # Java 17+
mvn --version             # Maven 3.9+
docker --version          # Docker 20.10+
docker-compose --version  # Docker Compose 1.29+
```

### Option A: Docker Compose (Recommended)

```bash
# 1. Start all services
docker-compose up -d

# 2. Verify health
curl http://localhost:8080/api/actuator/health
curl http://localhost:8081/api/actuator/health
curl http://localhost:8082/api/actuator/health

# 3. Test with a transaction
curl -X POST http://localhost:8080/api/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "amount": 50000,
    "location": "Singapore, SG"
  }'

# 4. View alerts
curl http://localhost:8082/api/alerts
```

### Option B: Local Development

```bash
# 1. Start infrastructure only
docker-compose up -d postgres ibm-mq

# 2. Build all services
mvn clean install

# 3. Start services (in separate terminals)

# Terminal 1 - Producer Service
cd producer-service
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Terminal 2 - Risk Engine
cd risk-engine
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Terminal 3 - Alert Service
cd alert-service
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

## Building

### Build All Services

```bash
mvn clean install
```

This builds:
- `common-models-1.0.0.jar`
- `producer-service-1.0.0.jar`
- `risk-engine-1.0.0.jar`
- `alert-service-1.0.0.jar`

### Build Specific Service

```bash
cd producer-service
mvn clean package -DskipTests
```

### Build Docker Images

```bash
docker-compose build
```

Images created:
- `producer-service:latest`
- `risk-engine:latest`
- `alert-service:latest`

## REST APIs

### Producer Service (8080)

**Submit Transaction**
```bash
POST /api/transaction

Request:
{
  "userId": "user123",
  "amount": 5000.00,
  "location": "New York, NY"
}

Response [201 Created]:
{
  "transactionId": "txn_abc123",
  "timestamp": "2026-04-08T14:30:45Z",
  "status": "SUBMITTED"
}
```

**Health Check**
```bash
GET /api/actuator/health

Response [200 OK]:
{
  "status": "UP"
}
```

### Alert Service (8082)

**Get Alerts**
```bash
GET /api/alerts?page=0&size=20&riskLevel=HIGH&status=NEW

Response [200 OK]:
{
  "content": [
    {
      "id": "alert_xyz",
      "transactionId": "txn_abc123",
      "userId": "user123",
      "amount": 50000.00,
      "riskLevel": "HIGH",
      "riskScore": 87.5,
      "reason": "Large amount + impossible travel",
      "createdAt": "2026-04-08T14:32:02Z",
      "status": "NEW"
    }
  ],
  "totalElements": 45,
  "totalPages": 5
}
```

**Update Alert Status**
```bash
PUT /api/alerts/{alertId}/status
Content-Type: application/json

Request:
{
  "status": "REVIEWED"
}

Response [200 OK]:
{
  "id": "alert_xyz",
  "status": "REVIEWED"
}
```

## 📊 Data Flow

```
1. POST /api/transaction (Producer) → Receives transaction
              ↓
2. Publish to TRANSACTION_QUEUE (IBM MQ) → Queue it
              ↓
3. Risk Engine subscribes → Analyze in parallel
   - Amount Detector
   - Frequency Analyzer
   - Geolocation Check
   - User History Analysis
              ↓
4. If HIGH/MEDIUM risk → Publish to ALERT_QUEUE
              ↓
5. Alert Service subscribes → Store in PostgreSQL
   - Insert into alerts table
   - Record in audit trail
   - Update metrics
              ↓
6. GET /api/alerts (via Dashboard) → Retrieve alerts
```

## ⚙️ Configuration

### Environment Variables

Create `.env` file (use `.env.example` as template):

```bash
# Database
DB_USER=postgres
DB_PASSWORD=postgres
DB_HOST=postgres
DB_PORT=5432
DB_NAME=riskmonitoring

# IBM MQ
MQ_HOST=ibm-mq
MQ_PORT=1414
MQ_CHANNEL=DEV.APP.SVRCONN
MQ_QUEUE_MANAGER=QM1

# Services
PRODUCER_PORT=8080
RISK_ENGINE_PORT=8081
ALERT_SERVICE_PORT=8082
```

### Per-Service Configuration

Each service has `application.yml`:

**Producer Service:** `producer-service/src/main/resources/application.yml`
```yaml
server:
  port: ${PRODUCER_PORT:8080}

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:riskmonitoring}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}

  jms:
    provider: ibmmq
```

Similar configurations for risk-engine and alert-service.

## 📦 Dependencies

### Core
- **Spring Boot 3.x** - Framework
- **Spring Cloud** - Distributed config, circuit breaker
- **Spring Data JPA** - Database access
- **Spring Integration** - Message handling
- **Hibernate ORM** - Object-relational mapping

### Message Queue
- **IBM MQ Java** - Message broker client
- **Jakarta JMS** - Messaging API

### Database
- **PostgreSQL JDBC** - Database driver
- **HikariCP** - Connection pooling
- **Liquibase/Flyway** - Schema migration

### Testing
- **JUnit 5** - Unit testing
- **Mockito** - Mocking
- **TestContainers** - Integration testing

## 🧪 Testing

### Run Tests

```bash
mvn test
```

### Run Integration Tests

Requires Docker (TestContainers):

```bash
mvn verify
```

### Manual Testing

```bash
# Test Producer API
curl http://localhost:8080/api/transaction

# Test Risk Engine health
curl http://localhost:8081/api/actuator/health

# Test Alert Service
curl http://localhost:8082/api/alerts
```

## 📊 Monitoring

### Health Checks

```bash
# Producer Service
curl http://localhost:8080/api/actuator/health

# Risk Engine
curl http://localhost:8081/api/actuator/health

# Alert Service
curl http://localhost:8082/api/actuator/health
```

### Metrics

```bash
# Producer metrics
curl http://localhost:8080/api/actuator/metrics

# Risk Engine metrics
curl http://localhost:8081/api/actuator/metrics

# Alert Service metrics
curl http://localhost:8082/api/actuator/metrics
```

### Logs

```bash
# Docker Compose
docker-compose logs -f producer-service
docker-compose logs -f risk-engine
docker-compose logs -f alert-service

# Local development
# Check console output in terminals
```

## 🔍 Debugging

### Enable Debug Logging

Edit `application.yml`:

```yaml
logging:
  level:
    com.example.riskmonitoring: DEBUG
    org.springframework.jms: DEBUG
```

### View Database

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d riskmonitoring

# List tables
\dt

# Query alerts
SELECT * FROM alerts LIMIT 10;
```

### Check Message Queue

IBM MQ provides a console at: `http://localhost:9443`

## 📈 Performance Tuning

### Database Connection Pool

Edit service `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      max-lifetime: 1800000
```

### JVM Settings

Edit `docker-compose.yml`:

```yaml
producer-service:
  environment:
    - JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC
```

## 🚀 Production Deployment

### Docker Compose

```bash
# Build images
docker-compose build

# Run in production
docker-compose -f docker-compose.prod.yml up -d

# Scale services
docker-compose up -d --scale risk-engine=3
```

### Kubernetes

Manual manifests are included. For advanced deployment:

```bash
# Create namespace
kubectl create namespace risk-monitoring

# Deploy services
kubectl apply -f k8s/ -n risk-monitoring

# Check status
kubectl get pods -n risk-monitoring
```

## 🔐 Security

### Secrets Management

Use environment variables or secrets files:

```bash
# Docker Compose
docker-compose --env-file .env.prod up -d

# Kubernetes
kubectl create secret generic db-credentials \
  --from-literal=password=securepassword
```

### CORS Configuration

For frontend integration, configure CORS in Alert Service:

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "PUT", "POST")
                    .allowCredentials(true);
            }
        };
    }
}
```

## 📞 Troubleshooting

### Port Already in Use

```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:8080 | xargs kill -9
```

### Database Connection Error

```bash
# Check PostgreSQL is running
docker-compose ps

# Verify connection
curl http://localhost:5432
```

### Message Queue Error

```bash
# Check IBM MQ is running
docker-compose logs ibm-mq

# Verify connection string in application.yml
```

## 🔗 Integration

### With Frontend Dashboard

The dashboard connects via:
- `REACT_APP_API_URL=http://localhost:8082`

See `frontend/dashboard/README.md` for frontend setup.

## 📝 Development Workflow

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Make Changes**
   ```bash
   # Edit code in service folder
   cd producer-service
   ```

3. **Test Locally**
   ```bash
   mvn test
   docker-compose up -d
   curl http://localhost:8080/api/transaction
   ```

4. **Commit & Push**
   ```bash
   git add .
   git commit -m "feat: add new feature"
   git push origin feature/my-feature
   ```

## 📚 Key Classes

| Service | Class | Purpose |
|---------|-------|---------|
| Producer | `TransactionController.java` | REST endpoint for transactions |
| Risk Engine | `RiskAnalysisService.java` | Main fraud detection logic |
| Alert Service | `AlertController.java` | REST API for alerts |
| Common | `AlertEntity.java` | JPA entity for alerts |

## 📊 Database Schema

**alerts table**
```sql
CREATE TABLE alerts (
  id UUID PRIMARY KEY,
  transaction_id VARCHAR(50) NOT NULL,
  user_id VARCHAR(100) NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  risk_level VARCHAR(20) NOT NULL,
  risk_score DECIMAL(5,2) NOT NULL,
  reason TEXT,
  status VARCHAR(20) DEFAULT 'NEW',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  INDEX idx_user_id (user_id),
  INDEX idx_risk_level (risk_level),
  INDEX idx_created_at (created_at)
);
```

## 🤝 Contributing

Follow these guidelines:
1. Code follows project conventions
2. All tests pass: `mvn test`
3. No breaking changes to APIs
4. Document changes in comments
5. Update README if adding features

## 📄 License

Production-ready implementation for the Transaction Risk Monitoring System.

---

**Backend Version:** 1.0.0  
**Last Updated:** April 8, 2026  
**Status:** Production Ready
