# Docker Deployment Checklist

## Pre-Deployment Setup

### ✅ Prerequisites Verification
- [ ] Docker installed (version 20.10+)
  ```bash
  docker --version
  ```
- [ ] Docker Compose installed (version 1.29+)
  ```bash
  docker-compose --version
  ```
- [ ] Minimum 10GB free disk space
- [ ] Minimum 4GB RAM, 2 CPU cores (8GB RAM, 4 cores recommended)
- [ ] Docker daemon running
  ```bash
  docker ps
  ```

### ✅ Environment Configuration
- [ ] Copy `.env.example` to `.env`
  ```bash
  cp .env.example .env
  ```
- [ ] Update `.env` with your configuration:
  - [ ] DB_PASSWORD (PostgreSQL)
  - [ ] MQ_PASSWORD (IBM MQ application user)
  - [ ] MQ_ADMIN_PASSWORD (IBM MQ admin user)
  - [ ] RISK_THRESHOLD_HIGH and RISK_THRESHOLD_MEDIUM (optional)
  - [ ] JAVA_OPTS memory settings (optional)

### ✅ File Verification
Ensure these files exist in project root:
- [ ] `docker-compose.yml`
- [ ] `postgres-init.sql`
- [ ] `.dockerignore`
- [ ] `.env.example`
- [ ] `docker-manage.sh` (Linux/macOS)
- [ ] `docker-manage.ps1` (Windows)
- [ ] Service Dockerfiles:
  - [ ] `producer-service/Dockerfile`
  - [ ] `risk-engine/Dockerfile`
  - [ ] `alert-service/Dockerfile`

## Build and Deployment

### Windows (PowerShell)
```powershell
# 1. Verify prerequisites
.\docker-manage.ps1 check

# 2. Setup environment
.\docker-manage.ps1 setup

# 3. Build images
.\docker-manage.ps1 build

# 4. Start services
.\docker-manage.ps1 up

# 5. Monitor startup
.\docker-manage.ps1 ps
```

### Linux/macOS (Bash)
```bash
# 1. Make script executable
chmod +x docker-manage.sh

# 2. Verify prerequisites
./docker-manage.sh check

# 3. Setup environment
./docker-manage.sh setup

# 4. Build images
./docker-manage.sh build

# 5. Start services
./docker-manage.sh up

# 6. Monitor startup
./docker-manage.sh ps
```

## Post-Deployment Verification

### ✅ Service Status
- [ ] Check all containers are running (Status = Up)
  ```bash
  # Windows
  .\docker-manage.ps1 ps
  
  # Linux/macOS
  ./docker-manage.sh ps
  ```

### ✅ Health Checks
All services should show "Healthy" status:
- [ ] PostgreSQL
- [ ] IBM MQ
- [ ] Producer Service
- [ ] Risk Engine
- [ ] Alert Service

```bash
# Windows
.\docker-manage.ps1 health

# Linux/macOS
./docker-manage.sh health
```

### ✅ Service Endpoints
Test that services are responding:

- [ ] **Producer Service** (Port 8080)
  ```bash
  curl http://localhost:8080/api/actuator/health
  # Expected: {"status":"UP",...}
  ```

- [ ] **Risk Engine** (Port 8081)
  ```bash
  curl http://localhost:8081/api/actuator/health
  # Expected: {"status":"UP",...}
  ```

- [ ] **Alert Service** (Port 8082)
  ```bash
  curl http://localhost:8082/api/actuator/health
  # Expected: {"status":"UP",...}
  ```

### ✅ Database Connectivity
- [ ] PostgreSQL is accessible
  ```bash
  docker exec risk-monitoring-postgres psql \
    -U riskmonitor -d risk_monitoring_db -c "SELECT 1"
  # Expected: 1
  ```

- [ ] Database schema is initialized
  ```bash
  docker exec risk-monitoring-postgres psql \
    -U riskmonitor -d risk_monitoring_db -c "\dt"
  # Expected: alerts, alert_history, transaction_cache, transaction_metrics tables
  ```

### ✅ Message Broker Connectivity
- [ ] IBM MQ is accessible
  ```bash
  docker exec risk-monitoring-mq \
    echo "DISPLAY QUEUE(*)" | runmqsc QMGR
  # Expected: MQ queue list
  ```

## Functional Testing

### ✅ Submit Test Transaction
```bash
curl -X POST http://localhost:8080/api/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "testuser",
    "amount": 5000.00,
    "location": "NYC, NY"
  }'
```
- [ ] Response code: 201 (Created)
- [ ] Response includes transactionId

### ✅ Verify Alert Processing
```bash
# Wait 5 seconds for message processing
sleep 5

# Get alerts
curl http://localhost:8082/api/alerts | jq '.'
```
- [ ] Alert is created for high-risk transaction
- [ ] Alert includes risk level and score

### ✅ Run Automated Tests
```bash
# Windows
.\docker-manage.ps1 test

# Linux/macOS
./docker-manage.sh test
```
- [ ] 3 transactions submitted
- [ ] All requests succeed with 201 status

## Monitoring and Logs

### ✅ View Logs
```bash
# View all services
# Windows
.\docker-manage.ps1 logs

# Linux/macOS
./docker-manage.sh logs

# View specific service (example: alert-service)
# Windows
.\docker-manage.ps1 logs alert-service

# Linux/macOS
./docker-manage.sh logs alert-service
```

### ✅ Monitor Resources
```bash
docker stats risk-monitoring-*
```
- [ ] Check memory usage per service
- [ ] Verify CPU utilization
- [ ] Monitor network I/O

### ✅ Check Service Metrics
```bash
# Transaction metrics
curl http://localhost:8082/api/metrics/transactions | jq '.'

# Idempotency cache stats
curl http://localhost:8082/api/metrics/idempotency | jq '.'

# Rate limit status
curl http://localhost:8082/api/metrics/rate-limit/testuser | jq '.'
```

## Configuration Validation

### ✅ Environment Variables
- [ ] Verify `.env` file exists and is not committed to git
  ```bash
  ls -la .env
  grep ".env" .gitignore
  ```

- [ ] Confirm services received environment variables
  ```bash
  docker inspect risk-monitoring-alert | grep -A 20 "Env"
  ```

### ✅ Database Configuration
- [ ] Database name matches a configuration
  ```bash
  docker exec risk-monitoring-postgres psql -U riskmonitor -l | grep risk_monitoring_db
  ```

- [ ] User permissions are correct
  ```bash
  docker exec risk-monitoring-postgres psql \
    -U riskmonitor -d risk_monitoring_db \
    -c "SELECT * FROM alerts LIMIT 1"
  # Should succeed without permission errors
  ```

### ✅ MQ Configuration
- [ ] Queue names are correct
  ```bash
  docker exec risk-monitoring-mq \
    echo "DISPLAY QUEUE(TRANSACTION_QUEUE)" | runmqsc QMGR
  ```

## Backup and Data Persistence

### ✅ Data Persistence
- [ ] Stop containers (data should persist)
  ```bash
  # Windows
  .\docker-manage.ps1 down
  
  # Linux/macOS
  ./docker-manage.sh down
  ```

- [ ] Restart containers
  ```bash
  # Windows
  .\docker-manage.ps1 up
  
  # Linux/macOS
  ./docker-manage.sh up
  ```

- [ ] Verify data still exists
  ```bash
  docker exec risk-monitoring-postgres psql \
    -U riskmonitor -d risk_monitoring_db \
    -c "SELECT COUNT(*) FROM alerts"
  ```

### ✅ Create Database Backup
```bash
docker exec risk-monitoring-postgres pg_dump \
  -U riskmonitor risk_monitoring_db > database-backup.sql
```
- [ ] Backup file created successfully
- [ ] File size > 1KB

## Troubleshooting Checklist

### ✅ Services Won't Start
- [ ] Check Docker daemon is running: `docker ps`
- [ ] Check logs for errors: `docker-compose logs`
- [ ] Verify ports are not in use: `netstat -an | grep 8080`
- [ ] Check disk space: `df -h`
- [ ] Verify `.env` file exists and is readable

### ✅ Database Connection Errors
- [ ] PostgreSQL container is healthy: `docker ps | grep postgres`
- [ ] Test connection: `docker exec risk-monitoring-postgres pg_isready`
- [ ] Check password in `.env` is correct
- [ ] Verify database name in `.env`

### ✅ MQ Connection Errors
- [ ] IBM MQ container is healthy: `docker ps | grep mq`
- [ ] Test connection: `docker exec risk-monitoring-mq runmqsc -w 5 -e QMGR`
- [ ] Check MQ credentials in `.env`
- [ ] Verify MQ port 1414 is accessible

### ✅ Health Check Failures
- [ ] Wait 30-40 seconds after starting (services need time to initialize)
- [ ] Check individual service logs
- [ ] Verify all dependencies are running
- [ ] Check heap memory allocation in JAVA_OPTS

## Production Deployment Checklist

### ✅ Security
- [ ] All passwords in `.env` changed from defaults
- [ ] `.env` file is in `.gitignore` (NOT committed to git)
- [ ] Database backups are encrypted
- [ ] Network access restricted via firewall rules
- [ ] HTTPS/TLS enabled for external endpoints

### ✅ Performance
- [ ] JVM heap size configured appropriately
- [ ] Database indexes verified: `\d alerts` in psql
- [ ] Connection pool size adjusted for expected load
- [ ] Log retention limits configured

### ✅ Monitoring
- [ ] Logging aggregation set up (ELK, Splunk, etc.)
- [ ] Alerting configured for service failures
- [ ] Metrics collection enabled (Prometheus, etc.)
- [ ] Health check endpoints monitored

### ✅ Disaster Recovery
- [ ] Regular database backups scheduled
- [ ] Backup restoration tested
- [ ] Failover procedures documented
- [ ] Recovery time objective (RTO) defined
- [ ] Recovery point objective (RPO) defined

### ✅ Documentation
- [ ] Deployment runbook created
- [ ] Troubleshooting guide documented
- [ ] Configuration specifications documented
- [ ] Service dependencies documented
- [ ] Escalation procedures defined

## Rollback Procedures

### If Deployment Fails

1. **Stop services (preserve data)**
   ```bash
   # Windows
   .\docker-manage.ps1 down
   
   # Linux/macOS
   ./docker-manage.sh down
   ```

2. **Review logs**
   ```bash
   docker-compose logs > deployment-logs.txt
   ```

3. **Check configuration**
   ```bash
   cat .env
   ```

4. **Fix issues and retry**
   - Update `.env` if needed
   - Check that all files exist
   - Verify prerequisites are met

5. **Redeploy**
   ```bash
   # Windows
   .\docker-manage.ps1 build
   .\docker-manage.ps1 up
   .\docker-manage.ps1 health
   
   # Linux/macOS
   ./docker-manage.sh build
   ./docker-manage.sh up
   ./docker-manage.sh health
   ```

## Support and Documentation

### Quick References
- Quick Start: [DOCKER-README.md](DOCKER-README.md)
- Detailed Guide: [DOCKER-SETUP-GUIDE.md](DOCKER-SETUP-GUIDE.md)
- Implementation Summary: [DOCKER-IMPLEMENTATION-SUMMARY.md](DOCKER-IMPLEMENTATION-SUMMARY.md)
- API Reference: [API-REFERENCE.md](API-REFERENCE.md)

### Getting Help
1. Check logs: `docker-compose logs [service]`
2. Review troubleshooting section in DOCKER-SETUP-GUIDE.md
3. Verify configuration in .env
4. Check health endpoints
5. Test database and MQ connectivity

---

**Deployment Status Tracking**

Date Started: ________________
Date Completed: ________________
Deployed By: ________________
Environment: ☐ Development ☐ Staging ☐ Production

**Sign-off**
- [ ] All checks passed
- [ ] Services operational
- [ ] Data persisting
- [ ] Backups working
- [ ] Ready for production use

Approved by: ________________
Date: ________________
