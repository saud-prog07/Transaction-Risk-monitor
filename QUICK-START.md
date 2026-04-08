# Quick Start Guide

Get the risk monitoring system running in 10 minutes.

## Prerequisites

- Java 17+ installed
- Maven 3.8+ installed
- PostgreSQL 12+ installed and running
- IBM MQ 9.3+ running on localhost:1414

## Step 1: Setup Database (2 minutes)

### Windows
```bash
cd alert-service\src\main\resources\db
setup-postgres.bat
```

### Linux/macOS
```bash
cd alert-service/src/main/resources/db
chmod +x setup-postgres.sh
./setup-postgres.sh
```

Expected output:
```
PostgreSQL is running...
Creating database alert_service_db...
Running init-schema.sql...
Setup complete!
```

## Step 2: Build Project (3 minutes)

```bash
# From root directory
mvn clean install -DskipTests
```

Expected:
```
BUILD SUCCESS
Total time: 45s
```

## Step 3: Start Services (5 minutes)

Open 4 terminal windows and run each:

**Terminal 1 - Common Models** (Just builds, no service):
```bash
cd common-models
mvn spring-boot:run
```

**Terminal 2 - Producer Service (Port 8080)**:
```bash
cd producer-service
mvn spring-boot:run
```
Wait for: `Started TransactionProducerApplication in X.XXX seconds`

**Terminal 3 - Risk Engine (Port 8081)**:
```bash
cd risk-engine
mvn spring-boot:run
```
Wait for: `Started RiskEngineApplication in X.XXX seconds`

**Terminal 4 - Alert Service (Port 8082)**:
```bash
cd alert-service
mvn spring-boot:run
```
Wait for: `Started AlertServiceApplication in X.XXX seconds`

## Step 4: Verify System is Running

```bash
# Check producer service
curl http://localhost:8080/actuator/health

# Check risk engine
curl http://localhost:8081/actuator/health

# Check alert service
curl http://localhost:8082/actuator/health

# Expected response:
# {"status":"UP"}
```

## Test End-to-End Flow

### 1. Submit a Transaction

```bash
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER001",
    "amount": 10000.00,
    "location": "New York, NY"
  }'

# Response:
# {
#   "transactionId": "a1b2c3d4-...",
#   "userId": "USER001",
#   "amount": 10000.00,
#   "timestamp": "2024-01-15T10:30:00Z",
#   "location": "New York, NY"
# }
```

### 2. Submit Multiple Transactions (to trigger frequency analyzer)

```bash
# Run this script to submit 7 rapid transactions
#!/bin/bash
for i in {1..7}; do
  curl -s -X POST http://localhost:8080/transaction \
    -H "Content-Type: application/json" \
    -d '{
      "userId": "USER002",
      "amount": 500.00,
      "location": "Los Angeles, CA"
    }' > /dev/null
  echo "Submitted transaction $i"
  sleep 0.5
done
```

### 3. Query Alerts

```bash
# Wait 2-3 seconds for processing, then query:
curl http://localhost:8082/api/alerts

# Response:
# {
#   "content": [
#     {
#       "id": 1,
#       "transactionId": "a1b2c3d4-...",
#       "riskLevel": "HIGH",
#       "reason": "Frequency: 7 transactions in 5 minutes",
#       "createdAt": "2024-01-15T10:30:05Z",
#       "reviewed": false
#     }
#   ],
#   "totalElements": 1,
#   "number": 0,
#   "size": 20
# }
```

### 4. Get Statistics

```bash
curl http://localhost:8082/api/alerts/statistics

# Response:
# {
#   "totalAlerts": 1,
#   "unreviewedCount": 1,
#   "highRiskCount": 1,
#   "mediumRiskCount": 0
# }
```

### 5. Mark Alert as Reviewed

```bash
curl -X PUT http://localhost:8082/api/alerts/1/review \
  -H "Content-Type: application/json" \
  -d '{
    "investigationNotes": "Verified legitimate bulk transaction"
  }'
```

## Common Commands

### View All Endpoints
```bash
curl http://localhost:8082/actuator
```

### View Database Health
```bash
curl http://localhost:8082/actuator/health/databaseHealth

# Response:
# {
#   "status": "UP",
#   "pool_size": 5,
#   "active_connections": 1,
#   "idle_connections": 4,
#   "waiting_threads": 0
# }
```

### View Application Metrics
```bash
curl http://localhost:8082/actuator/metrics
```

### Change Log Level at Runtime
```bash
curl -X POST http://localhost:8082/actuator/loggers/com.example.riskmonitoring \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

## Stop Services

Press `Ctrl+C` in each terminal to stop services gracefully.

## Reset Database

If you need a clean slate:

```bash
# Drop and recreate database
psql -h localhost -U postgres -c "DROP DATABASE IF EXISTS alert_service_db;"
psql -h localhost -U postgres -c "CREATE DATABASE alert_service_db;"

# Re-run schema initialization
cd alert-service/src/main/resources/db
psql -h localhost -U postgres -d alert_service_db -f init/init-schema.sql
```

## Troubleshooting

### Port Already in Use
If port 8080, 8081, or 8082 is already in use:
```bash
# Find process using port (Linux/macOS)
lsof -i :8080

# Kill process
kill -9 <PID>

# Or start service on different port:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9000"
```

### PostgreSQL Connection Refused
```bash
# Check if PostgreSQL is running
# Windows
tasklist | findstr postgres

# Linux/macOS
ps aux | grep postgres

# Start PostgreSQL if not running
# Windows: net start postgresql-x64-14
# Linux: sudo systemctl start postgresql
# macOS: brew services start postgresql
```

### IBM MQ Not Available
The system gracefully continues without MQ but transactions won't be processed. Ensure:
```bash
# Check MQ is running
dspmq

# Or skip MQ for testing by using in-memory queue in producer-service
# (Requires code change - see DATABASE-SETUP.md for details)
```

### Slow Startup
First startup is slower due to:
- Hibernate DDL generation
- Compilation of aspect classes
- Connection pool initialization

Typical times:
- Each service: 10-15 seconds
- Total system: 60-90 seconds

## Next Steps

1. **Read** [SYSTEM-DOCUMENTATION.md](SYSTEM-DOCUMENTATION.md) for complete architecture
2. **Read** [DATABASE-SETUP.md](DATABASE-SETUP.md) for database configuration details
3. **Review** REST API endpoints in SYSTEM-DOCUMENTATION.md
4. **Explore** application.yml files for configuration options
5. **Implement** security (see "Security Considerations" in SYSTEM-DOCUMENTATION.md)

## Test Scenarios

### Scenario 1: High-Value Transaction Alert
```bash
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER100",
    "amount": 100000.00,
    "location": "London, UK"
  }'
```
Expected: HIGH risk (amount anomaly)

### Scenario 2: Frequency Anomaly Alert
```bash
# Submit 8 transactions in quick succession from same user
for i in {1..8}; do
  curl -s -X POST http://localhost:8080/transaction \
    -H "Content-Type: application/json" \
    -d "{\"userId\": \"USER200\", \"amount\": 100.00, \"location\": \"Paris, France\"}" > /dev/null
done
```
Expected: HIGH risk (frequency anomaly)

### Scenario 3: Location Anomaly Alert
```bash
# First establish location history
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER300",
    "amount": 500.00,
    "location": "Tokyo, Japan"
  }'

# Then submit from new location
sleep 2
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER300",
    "amount": 600.00,
    "location": "Sydney, Australia"
  }'
```
Expected: LOW-MEDIUM risk (location anomaly)

## Performance Tuning

For testing under load, adjust:

**alert-service/application.yml**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase from 10
      
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50      # Increase from 20
          fetch_size: 100     # Increase from 50
```

**risk-engine/application.yml**:
```yaml
spring:
  jms:
    listener:
      concurrency: 10-20     # Increase from 1-5
```

## Support

For detailed information, see:
- [SYSTEM-DOCUMENTATION.md](SYSTEM-DOCUMENTATION.md) - Complete architecture
- [DATABASE-SETUP.md](alert-service/DATABASE-SETUP.md) - Database configuration
- Service README.md files in each module folder
