# Docker Deployment - Risk Monitoring System

## Quick Start (2 minutes)

### Windows (PowerShell)
```powershell
# 1. Check prerequisites
.\docker-manage.ps1 check

# 2. Setup environment
.\docker-manage.ps1 setup

# 3. Build images
.\docker-manage.ps1 build

# 4. Start all services
.\docker-manage.ps1 up

# 5. Check health
.\docker-manage.ps1 health

# 6. Test with sample transaction
.\docker-manage.ps1 test
```

### Linux/macOS (Bash)
```bash
# 1. Make script executable
chmod +x docker-manage.sh

# 2. Check prerequisites
./docker-manage.sh check

# 3. Setup environment
./docker-manage.sh setup

# 4. Build images
./docker-manage.sh build

# 5. Start all services
./docker-manage.sh up

# 6. Check health
./docker-manage.sh health

# 7. Test with sample transaction
./docker-manage.sh test
```

## What Gets Deployed

### Services
- **Producer Service** (Port 8080) - Transaction ingestion API
- **Risk Engine** (Port 8081) - Real-time risk analysis
- **Alert Service** (Port 8082) - Alert management and storage

### Infrastructure
- **PostgreSQL 15** (Port 5432) - Alert and metrics storage
- **IBM MQ** (Port 1414) - Message broker
- **Docker Network** - Internal service communication

## Key Features

✅ **Multi-stage Docker builds** - Optimized image size (~300MB per service)
✅ **Non-root containers** - Enhanced security
✅ **Health checks** - Automatic service monitoring
✅ **Persistent volumes** - Data survives container restarts
✅ **Environment configuration** - Easy customization
✅ **Comprehensive logging** - JSON-formatted logs
✅ **Service networking** - Docker network for inter-service communication

## Access Points After Deployment

| Service | URL | Port |
|---------|-----|------|
| Producer API | http://localhost:8080 | 8080 |
| Risk Engine API | http://localhost:8081 | 8081 |
| Alert Service API | http://localhost:8082 | 8082 |
| PostgreSQL | localhost:5432 | 5432 |
| IBM MQ | localhost:1414 | 1414 |
| IBM MQ Console | https://localhost:9443 | 9443 |

## Management Commands

### View Status
```bash
# Windows
.\docker-manage.ps1 ps

# Linux/macOS
./docker-manage.sh ps
```

### View Logs
```bash
# All services
# Windows
.\docker-manage.ps1 logs

# Linux/macOS
./docker-manage.sh logs

# Specific service
# Windows
.\docker-manage.ps1 logs alert-service

# Linux/macOS
./docker-manage.sh logs alert-service
```

### Stop Services
```bash
# Keep data
# Windows
.\docker-manage.ps1 down

# Linux/macOS
./docker-manage.sh down

# Delete everything (WARNING: data loss)
# Windows
.\docker-manage.ps1 clean

# Linux/macOS
./docker-manage.sh clean
```

## Configuration

### Using .env File

1. Copy the example file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your settings:
   ```
   DB_USER=riskmonitor
   DB_PASSWORD=your_secure_password
   MQ_PASSWORD=your_mq_password
   RISK_THRESHOLD_HIGH=0.8
   RISK_THRESHOLD_MEDIUM=0.5
   ```

3. Services automatically pick up these values from the environment

### Custom Configuration Files

Create `application-docker.yml` in each service to override Spring Boot settings:

**producer-service/src/main/resources/application-docker.yml**
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: producer-service
  
  jms:
    broker-url: tcp://ibm-mq:61616
```

## Testing

### Submit a Transaction
```bash
curl -X POST http://localhost:8080/api/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "amount": 5000.00,
    "location": "NYC, NY"
  }'
```

### Check Service Health
```bash
curl http://localhost:8080/api/actuator/health
curl http://localhost:8081/api/actuator/health
curl http://localhost:8082/api/actuator/health
```

### Get Alerts
```bash
curl http://localhost:8082/api/alerts
```

### Get Metrics
```bash
curl http://localhost:8082/api/metrics/transactions
```

## Troubleshooting

### Services Won't Start
```bash
# Check logs
# Windows
.\docker-manage.ps1 logs

# Linux/macOS
./docker-manage.sh logs

# Check specific service
# Windows
.\docker-manage.ps1 logs alert-service

# Linux/macOS
./docker-manage.sh logs alert-service
```

### Port Already in Use
If ports are already in use, you can modify `docker-compose.yml`:
```yaml
producer-service:
  ports:
    - "8090:8080"  # Map to different port
```

### Database Connection Issues
```bash
# Test PostgreSQL connection
docker exec risk-monitoring-postgres \
  psql -U riskmonitor -d risk_monitoring_db -c "SELECT 1"
```

### MQ Connection Issues
```bash
# Check MQ queue status
docker exec risk-monitoring-mq \
  echo "DISPLAY QUEUE(*)" | runmqsc QMGR
```

## Production Deployment Checklist

- [ ] Change all default passwords in `.env`
- [ ] Update JVM memory settings for your infrastructure
- [ ] Configure database backups
- [ ] Enable HTTPS/TLS for external endpoints
- [ ] Set up monitoring and alerting
- [ ] Review and adjust risk thresholds
- [ ] Test failover and recovery procedures
- [ ] Implement rate limiting configuration
- [ ] Review security group/firewall rules
- [ ] Plan capacity and scaling strategy

## Monitoring and Observability

### Docker Stats
```bash
docker stats risk-monitoring-*
```

### Container Metrics
```bash
docker inspect risk-monitoring-alert | grep -A 5 "Memory"
```

### Service Metrics Endpoints
```bash
# View all available metrics
curl http://localhost:8080/api/actuator/metrics

# Get specific metric
curl http://localhost:8080/api/actuator/metrics/jvm.memory.used
```

## Database Backups

### Create Backup
```bash
docker exec risk-monitoring-postgres pg_dump \
  -U riskmonitor risk_monitoring_db > backup.sql
```

### Restore Backup
```bash
docker exec -i risk-monitoring-postgres psql \
  -U riskmonitor risk_monitoring_db < backup.sql
```

## Clean Up

### Remove Services Only (Keep Data)
```bash
docker-compose down
```

### Remove Services and All Data
```bash
docker-compose down -v
```

### Remove All Images
```bash
docker image prune -a --filter="reference=risk-monitoring/*"
```

## Advanced Topics

For detailed information on:
- Docker security best practices
- Performance tuning
- Database optimization
- Multi-environment deployment
- Kubernetes deployment
- Registry setup

See [DOCKER-SETUP-GUIDE.md](DOCKER-SETUP-GUIDE.md)

## File Structure

```
.
├── docker-compose.yml           # Multi-service orchestration
├── .dockerignore               # Files to exclude from builds
├── .env.example                # Environment variables template
├── postgres-init.sql           # Database initialization script
├── docker-manage.ps1           # Windows management script
├── docker-manage.sh            # Linux/macOS management script
├── producer-service/
│   └── Dockerfile              # Builder multi-stage build
├── risk-engine/
│   └── Dockerfile              # Builder multi-stage build
├── alert-service/
│   └── Dockerfile              # Builder multi-stage build
└── DOCKER-SETUP-GUIDE.md       # Comprehensive guide
```

## Support

- Check logs: `./docker-manage.sh logs [service]`
- Detailed guide: See [DOCKER-SETUP-GUIDE.md](DOCKER-SETUP-GUIDE.md)
- API reference: See [API-REFERENCE.md](API-REFERENCE.md)
- System documentation: See [SYSTEM-DOCUMENTATION.md](SYSTEM-DOCUMENTATION.md)

## Next Steps

1. **Build and test** locally using Docker Compose
2. **Configure** environment variables (`.env` file)
3. **Validate** with health checks
4. **Test** transactions end-to-end
5. **Deploy** to production with proper infrastructure
6. **Monitor** using logs and metrics endpoints

## License

This project is part of the Risk Monitoring System microservices architecture.
