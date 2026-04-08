# Docker Setup Guide - Risk Monitoring System

## Overview

This guide provides complete instructions for building and running the entire Risk Monitoring System using Docker and Docker Compose.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Network                               │
│            (risk-monitoring-network - bridge)                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐   │
│  │ Producer Service │  │  Risk Engine     │  │Alert Service │   │
│  │   (Port 8080)    │  │   (Port 8081)    │  │(Port 8082)   │   │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘   │
│           │                     │                    │           │
│           └─────────────────────┼────────────────────┘           │
│                                 │                                │
│  ┌──────────────────────────────┴──────────────────────────┐   │
│  │           IBM MQ (Port 1414)                            │   │
│  │     - TRANSACTION_QUEUE                                 │   │
│  │     - ALERT_QUEUE                                       │   │
│  └──────────────────────────────┬──────────────────────────┘   │
│                                 │                                │
│  ┌──────────────────────────────┴──────────────────────────┐   │
│  │      PostgreSQL (Port 5432)                             │   │
│  │  - alerts table                                         │   │
│  │  - alert_history table                                  │   │
│  │  - transaction_cache table                              │   │
│  │  - transaction_metrics table                            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Prerequisites

1. **Docker**: Version 20.10+
   ```bash
   docker --version
   ```

2. **Docker Compose**: Version 1.29+
   ```bash
   docker-compose --version
   ```

3. **Disk Space**: Minimum 10GB free space for images and volumes

4. **System Resources**:
   - Minimum: 4GB RAM, 2 CPU cores
   - Recommended: 8GB RAM, 4 CPU cores

## Quick Start

### 1. Build Docker Images

Build all service images:

```bash
# Build all images at once
docker-compose build

# Or build specific services
docker-compose build producer-service
docker-compose build risk-engine
docker-compose build alert-service
```

View built images:

```bash
docker images | grep risk-monitoring
```

### 2. Start the System

```bash
# Start all services in background
docker-compose up -d

# Or start in foreground to see logs
docker-compose up

# Custom environment variables
SPRING_PROFILES_ACTIVE=docker \
RISK_THRESHOLD_HIGH=0.8 \
RISK_THRESHOLD_MEDIUM=0.5 \
docker-compose up -d
```

### 3. Verify Services

Check service status:

```bash
# Check all containers
docker-compose ps

# Expected output:
# NAME                         STATUS            PORTS
# risk-monitoring-postgres     Up (healthy)      5432:5432
# risk-monitoring-mq           Up (healthy)      1414:1414, 9443:9443
# risk-monitoring-producer     Up (healthy)      8080:8080
# risk-monitoring-risk-engine  Up (healthy)      8081:8081
# risk-monitoring-alert        Up (healthy)      8082:8082
```

Check health endpoints:

```bash
# Producer Service
curl http://localhost:8080/api/actuator/health

# Risk Engine
curl http://localhost:8081/api/actuator/health

# Alert Service
curl http://localhost:8082/api/actuator/health
```

## Service Endpoints

### Producer Service (Port 8080)

```bash
# Submit a transaction
curl -X POST http://localhost:8080/api/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "amount": 5000.00,
    "location": "NYC, NY"
  }'

# Health check
curl http://localhost:8080/api/actuator/health

# Metrics
curl http://localhost:8080/api/actuator/metrics
```

### Risk Engine (Port 8081)

```bash
# Health check
curl http://localhost:8081/api/actuator/health

# Metrics
curl http://localhost:8081/api/actuator/metrics
```

### Alert Service (Port 8082)

```bash
# Get all alerts
curl http://localhost:8082/api/alerts

# Get alert statistics
curl http://localhost:8082/api/alerts/statistics

# Get transaction metrics
curl http://localhost:8082/api/metrics/transactions

# Health check
curl http://localhost:8082/api/actuator/health

# Metrics
curl http://localhost:8082/api/actuator/metrics
```

## Configuration

### Environment Variables

Create a `.env` file in the project root:

```bash
# Database configuration
DB_USER=riskmonitor
DB_PASSWORD=your_secure_password
DB_NAME=risk_monitoring_db
DB_DDL_AUTO=validate

# IBM MQ configuration
MQ_USER=app
MQ_PASSWORD=your_mq_password
MQ_ADMIN_PASSWORD=your_admin_password

# Spring profile
SPRING_PROFILES_ACTIVE=docker

# Risk thresholds
RISK_THRESHOLD_HIGH=0.8
RISK_THRESHOLD_MEDIUM=0.5
```

Then start with:

```bash
docker-compose --env-file .env up -d
```

### Service Configuration Files

Each service can be configured via environment variables or application.yml. Configuration files are in:

- `producer-service/src/main/resources/application-docker.yml`
- `risk-engine/src/main/resources/application-docker.yml`
- `alert-service/src/main/resources/application-docker.yml`

Create these files to override default settings:

```yaml
# application-docker.yml
server:
  port: 8080
  
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/risk_monitoring_db
    username: riskmonitor
    password: ${DB_PASSWORD:secure_password}
  
  jms:
    broker-url: tcp://ibm-mq:61616
```

## Database Management

### Connect to PostgreSQL

```bash
# From host machine
psql -h localhost -U riskmonitor -d risk_monitoring_db

# From Docker container
docker exec -it risk-monitoring-postgres \
  psql -U riskmonitor -d risk_monitoring_db
```

### View Database Schema

```bash
docker exec -it risk-monitoring-postgres psql \
  -U riskmonitor -d risk_monitoring_db \
  -c "\d"  # List all tables
```

### Backup Database

```bash
docker exec risk-monitoring-postgres pg_dump \
  -U riskmonitor risk_monitoring_db \
  > backup.sql
```

### Restore Database

```bash
docker exec -i risk-monitoring-postgres psql \
  -U riskmonitor risk_monitoring_db \
  < backup.sql
```

## IBM MQ Management

### Access MQ Management Console

Open browser to: `https://localhost:9443/ibmmq/console`

Default credentials:
- Username: admin
- Password: (see MQ_ADMIN_PASSWORD in .env or docker-compose.yml)

### Create MQ Queues (if not auto-created)

```bash
# Start interactive session with MQ container
docker exec -it risk-monitoring-mq bash

# Run MQ commands
# Create TRANSACTION_QUEUE
echo "DEFINE QLOCAL(TRANSACTION_QUEUE)" | runmqsc QMGR

# Create ALERT_QUEUE
echo "DEFINE QLOCAL(ALERT_QUEUE)" | runmqsc QMGR

# List all queues
echo "DISPLAY QUEUE(*)" | runmqsc QMGR
```

## Logs and Monitoring

### View Service Logs

```bash
# All services
docker-compose logs

# Specific service
docker-compose logs alert-service

# Follow logs in real-time
docker-compose logs -f producer-service

# Last 100 lines
docker-compose logs --tail=100 risk-engine

# With timestamps
docker-compose logs --timestamps
```

### Inspect Container

```bash
# Get container info
docker inspect risk-monitoring-producer

# View system resource usage
docker stats risk-monitoring-*

# Execute command in container
docker exec -it risk-monitoring-producer sh
```

### Check Network Connectivity

```bash
# Test service-to-service communication
docker exec risk-monitoring-producer \
  wget -O- http://alert-service:8082/api/actuator/health

# Ping between services
docker exec risk-monitoring-producer ping ibm-mq
```

## Common Operations

### Stop Services

```bash
# Stop all services but keep data
docker-compose stop

# Stop specific service
docker-compose stop alert-service

# Stop and remove containers (data persists in volumes)
docker-compose down

# Stop, remove containers AND volumes (WARNING: data loss!)
docker-compose down -v
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart producer-service

# Restart and rebuild
docker-compose down && docker-compose build && docker-compose up -d
```

### Update Service

```bash
# Rebuild and restart specific service
docker-compose up -d --build producer-service
```

### View Volume Data

```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect risk-monitoring-system_postgres_data

# Mount volume to inspect files
docker run -v risk-monitoring-system_postgres_data:/data \
  alpine ls -la /data
```

## Troubleshooting

### Service Fails to Start

```bash
# Check logs
docker logs risk-monitoring-producer

# Common issues:
# 1. Port already in use
docker ps | grep 8080

# 2. Out of memory
docker stats

# 3. Network issues
docker network ls
docker network inspect risk-monitoring-network
```

### Database Connection Errors

```bash
# Check if PostgreSQL is healthy
docker-compose ps postgres

# Test connection from producer service
docker exec risk-monitoring-producer \
  nc -zv postgres 5432
```

### IBM MQ Connection Errors

```bash
# Check MQ container logs
docker logs risk-monitoring-mq

# Verify MQ is listening
docker exec risk-monitoring-mq netstat -tuln | grep 1414

# Test connection from risk-engine
docker exec risk-monitoring-risk-engine \
  nc -zv ibm-mq 1414
```

### Health Check Failures

```bash
# If services show "unhealthy", check:
docker inspect --format='{{.State.Health}}' risk-monitoring-alert

# Detailed health info
docker inspect risk-monitoring-alert | grep -A 20 "Health"

# Test endpoint directly
docker exec risk-monitoring-alert curl \
  http://localhost:8082/api/actuator/health
```

## Performance Tuning

### Increase JVM Memory

Edit `docker-compose.yml`:

```yaml
environment:
  JAVA_OPTS: "-Xms512m -Xmx1g -XX:+UseG1GC"
```

### PostgreSQL Optimization

```yaml
postgres:
  environment:
    # Connection pooling
    POSTGRES_INIT_ARGS: "-c max_connections=200"
```

### Docker Resource Limits

```yaml
alert-service:
  deploy:
    resources:
      limits:
        cpus: '1'
        memory: 1G
      reservations:
        cpus: '0.5'
        memory: 512M
```

## Cleanup

### Remove All Containers and Volumes

```bash
# WARNING: This will delete all data!
docker-compose down -v

# Delete images
docker rmi risk-monitoring/producer-service:latest
docker rmi risk-monitoring/risk-engine:latest
docker rmi risk-monitoring/alert-service:latest
```

### Prune Unused Resources

```bash
# Remove unused images
docker image prune

# Remove unused volumes
docker volume prune

# Remove unused networks
docker network prune

# Complete cleanup
docker system prune -a --volumes
```

## Production Deployment

### Security Considerations

1. **Change default passwords** in environment variables
2. **Use secrets management** (Docker Secrets or Swarm)
3. **Enable authentication** for all services
4. **Use HTTPS** for external endpoints
5. **Implement rate limiting** and DDoS protection
6. **Monitor logs** for security events
7. **Regular backups** of database and MQ data

### Registry and Image Storage

```bash
# Tag image for registry
docker tag risk-monitoring/alert-service:latest \
  myregistry.azurecr.io/alert-service:latest

# Push to registry
docker push myregistry.azurecr.io/alert-service:latest

# Pull from registry
docker-compose.yml with custom image:
image: myregistry.azurecr.io/alert-service:latest
```

### Scaling Considerations

For production high-traffic scenarios:

1. **Database scaling**: Add read replicas
2. **Message broker scaling**: Use MQ clustering
3. **Service scaling**: Use Docker Swarm or Kubernetes
4. **Load balancing**: Add reverse proxy (Nginx, HAProxy)

## Health Check Strategy

Services include health checks with:

- **Interval**: 30 seconds (check frequency)
- **Timeout**: 10 seconds (wait for response)
- **Retries**: 3 failures before unhealthy
- **Start Period**: 40 seconds (grace period after start)

Configure in `docker-compose.yml`:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

## Support and Debugging

Enable debug logging:

```bash
SPRING_PROFILES_ACTIVE=docker,debug docker-compose up
```

Generate detailed logs:

```bash
docker-compose logs --timestamps -f > system.log
```

Collect diagnostic info:

```bash
docker-compose ps > status.txt
docker network inspect risk-monitoring-network > network.txt
docker volume ls > volumes.txt
docker stats --no-stream > stats.txt
```

## Next Steps

1. **Build and test** locally using Docker Compose
2. **Configure** environment variables for your deployment
3. **Backup** database and MQ data regularly
4. **Monitor** services using logs and health endpoints
5. **Scale** services as needed based on traffic
6. **Deploy** to production using Docker Swarm or Kubernetes

For more information, see [PRODUCTION-ENHANCEMENT-GUIDE.md](PRODUCTION-ENHANCEMENT-GUIDE.md)
