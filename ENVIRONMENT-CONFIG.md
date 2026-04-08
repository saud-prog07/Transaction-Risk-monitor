# Environment Configuration Guide

This guide documents all environment variables and configuration settings required for deploying the event-driven pipeline across different environments.

## Overview

The microservices architecture uses Spring Profiles to support multiple deployment environments:

- **dev**: Development environment (local machine)
- **staging**: Staging/QA environment (pre-production)
- **prod**: Production environment (customer-facing)

Configuration is layered:
1. `application.yml` (base configuration, shared across all environments)
2. `application-{profile}.yml` (environment-specific overrides)
3. Environment variables (runtime overrides for sensitive data)

## Development Environment

### Running Development Profile

```bash
# Option 1: Maven
mvn spring-boot:run -Dspring.profiles.active=dev

# Option 2: Standalone JAR
java -jar alert-service.jar --spring.profiles.active=dev

# Option 3: IDE
# Set VM options: -Dspring.profiles.active=dev
```

### Required Environment Variables (None - uses defaults)

Development uses hardcoded defaults suitable for local development:

| Variable | Default | Purpose |
|----------|---------|---------|
| (All defaults hardcoded in application-dev.yml) | - | - |

### Settings Summary

| Setting | Value | Reason |
|---------|-------|--------|
| Database | localhost:5432 | Local PostgreSQL |
| Message Queue | localhost:1883 | Local IBM MQ |
| Hibernate | create-drop | Schema recreated on each start |
| Logging | DEBUG | Detailed troubleshooting |
| Endpoints | All exposed | Full actuator access |
| Retry Delay | 500ms initial, 1.5x multiplier | Fast feedback during development |

### Database Setup

```bash
# Start PostgreSQL (Docker)
docker run --name postgres-dev \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=alert_db \
  -p 5432:5432 \
  postgres:15

# Or via PostgreSQL installer
psql -U postgres
CREATE DATABASE alert_db;
```

### Verification

```bash
# Check configuration was loaded
curl http://localhost:8082/actuator/env | grep spring.profiles.active

# Should show: "spring.profiles.active": "dev"
```

## Staging Environment

### Running Staging Profile

```bash
# Option 1: Development machine pointing to staging resources
mvn spring-boot:run -Dspring.profiles.active=staging

# Option 2: Staging server (Docker)
docker run -e SPRING_PROFILES_ACTIVE=staging \
           -e DB_USERNAME=staginguser \
           -e DB_PASSWORD=$(cat /etc/secrets/db_password) \
           alert-service:latest

# Option 3: Kubernetes
kubectl set env deployment/alert-service \
  SPRING_PROFILES_ACTIVE=staging
```

### Required Environment Variables

These MUST be set before starting the application:

```bash
# Database configuration (required)
export DB_USERNAME=alert_staging_user
export DB_PASSWORD=$(cat /etc/secrets/staging/db_password)

# Message Queue configuration (required)
export MQ_USERNAME=staging_mq_user
export MQ_PASSWORD=$(cat /etc/secrets/staging/mq_password)

# Notification endpoints (required)
export ALERT_EMAIL_RECIPIENTS="devops@example.com,team@example.com"
export ALERT_WEBHOOK_URL="https://staging-slack.internal/hooks/notifications"

# Run the application
java -jar alert-service.jar --spring.profiles.active=staging
```

### Database Setup (Staging)

```bash
# Connect to staging PostgreSQL
psql -h staging-postgres.internal \
     -U postgres \
     -c "CREATE USER alert_staging_user WITH PASSWORD 'PASSWORD';"

psql -h staging-postgres.internal \
     -U postgres \
     -c "CREATE DATABASE alert_db OWNER alert_staging_user;"

# Grant permissions
psql -h staging-postgres.internal \
     -U postgres \
     -d alert_db \
     -c "GRANT ALL PRIVILEGES ON SCHEMA public TO alert_staging_user;"
```

### Settings Summary

| Setting | Value | Reason |
|---------|-------|--------|
| Hibernate | validate | Prevent accidental schema changes |
| Logging | INFO | Right balance of detail and noise |
| Retry Attempts | 3 | Match production behavior |
| Retry Delay | 2s initial, 2.0x multiplier | Same as production |
| Health Details | when-authorized | Information disclosure control |
| Endpoints | Limited (health, metrics, info) | Reduced attack surface |

### Verification

```bash
# Confirm staging profile active
curl -H "Authorization: Bearer $TOKEN" \
  http://staging-alert-service:8082/actuator/env | \
  grep spring.profiles.active

# Should show: "spring.profiles.active": "staging"

# Check database connectivity
curl http://staging-alert-service:8082/actuator/health/liveness

# Check alert notifications enabled
curl http://staging-alert-service:8082/api/alerts/statistics
```

## Production Environment

### Running Production Profile

```bash
# Option 1: Direct JAR (never use this - use container/orchestration)
java -jar alert-service.jar --spring.profiles.active=prod

# Option 2: Docker (recommended)
docker run \
  --env SPRING_PROFILES_ACTIVE=prod \
  --env DB_HOST=prod-postgres.internal \
  --env DB_USERNAME=${DB_USER} \
  --env DB_PASSWORD=${DB_PASS} \
  --env MQ_HOST=prod-mq.internal \
  --env MQ_USERNAME=${MQ_USER} \
  --env MQ_PASSWORD=${MQ_PASS} \
  --env ALERT_WEBHOOK_URL=${WEBHOOK_URL} \
  -p 8082:8082 \
  alert-service:vX.Y.Z

# Option 3: Kubernetes (recommended for scaling)
kubectl apply -f alert-service-deployment.yaml  # See below

# Option 4: AWS ECS/EKS with secrets manager
aws secrets create-secret --name alert-service-prod --secret-string '...'
```

### Required Environment Variables

**CRITICAL**: All sensitive values MUST be stored securely:

```bash
# Database (required - use AWS Secrets Manager, HashiCorp Vault, or K8s secrets)
DB_HOST=prod-postgres.rds.amazonaws.com        # RDS instance
DB_PORT=5432
DB_NAME=alert_db
DB_USERNAME=$(aws secretsmanager get-secret-value --secret-id prod/db/username --query SecretString --output text)
DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id prod/db/password --query SecretString --output text)

# Message Queue (required)
MQ_HOST=prod-mq.company.com
MQ_PORT=1883
MQ_USERNAME=$(aws secretsmanager get-secret-value --secret-id prod/mq/username --query SecretString --output text)
MQ_PASSWORD=$(aws secretsmanager get-secret-value --secret-id prod/mq/password --query SecretString --output text)

# Redis (for caching)
REDIS_HOST=prod-redis.company.com
REDIS_PORT=6379
REDIS_PASSWORD=$(aws secretsmanager get-secret-value --secret-id prod/redis/password --query SecretString --output text)

# JWT Security
JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id prod/jwt/secret --query SecretString --output text)

# Notifications (required)
ALERT_EMAIL_RECIPIENTS="security@company.com,devops@company.com"
ALERT_WEBHOOK_URL="https://prod-slack.company.com/hooks/alerts"

# CORS Security
CORS_ALLOWED_ORIGINS="https://app.company.com,https://api.company.com"

# Server configuration
SERVER_PORT=8082

# Environment identification
ENVIRONMENT=production
VERSION=$(cat /opt/app/VERSION)
```

### Kubernetes Deployment (Recommended)

```yaml
# File: alert-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: alert-service
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: alert-service
  template:
    metadata:
      labels:
        app: alert-service
    spec:
      serviceAccountName: alert-service
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
      containers:
      - name: alert-service
        image: company/alert-service:v1.0.0
        imagePullPolicy: IfNotPresent
        ports:
        - name: http
          containerPort: 8082
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: ENVIRONMENT
          value: "production"
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: alert-service-secrets
              key: db-host
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: alert-service-secrets
              key: db-username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: alert-service-secrets
              key: db-password
        - name: MQ_HOST
          valueFrom:
            secretKeyRef:
              name: alert-service-secrets
              key: mq-host
        - name: MQ_USERNAME
          valueFrom:
            secretKeyRef:
              name: alert-service-secrets
              key: mq-username
        - name: MQ_PASSWORD
          valueFrom:
            secretKeyRef:
              name: alert-service-secrets
              key: mq-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: alert-service-secrets
              key: jwt-secret
        - name: ALERT_WEBHOOK_URL
          valueFrom:
            secretKeyRef:
              name: alert-service-secrets
              key: webhook-url
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/live
            port: http
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/ready
            port: http
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        volumeMounts:
        - name: logs
          mountPath: /var/log/alert-service
      volumes:
      - name: logs
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: alert-service
  namespace: production
spec:
  type: ClusterIP
  ports:
  - name: http
    port: 8082
    targetPort: http
  selector:
    app: alert-service
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: alert-service-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: alert-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Create Kubernetes Secrets

```bash
# Create secret for production
kubectl create secret generic alert-service-secrets \
  --from-literal=db-host=prod-postgres.rds.amazonaws.com \
  --from-literal=db-username=$(vault kv get -field=username secret/prod/db) \
  --from-literal=db-password=$(vault kv get -field=password secret/prod/db) \
  --from-literal=mq-host=prod-mq.company.com \
  --from-literal=mq-username=$(vault kv get -field=username secret/prod/mq) \
  --from-literal=mq-password=$(vault kv get -field=password secret/prod/mq) \
  --from-literal=jwt-secret=$(vault kv get -field=secret secret/prod/jwt) \
  --from-literal=webhook-url=$(vault kv get -field=url secret/prod/webhooks) \
  -n production

# Verify
kubectl describe secret alert-service-secrets -n production
```

### Database Setup (Production)

```bash
# 1. Create dedicated database user (via RDS console or AWS CLI)
aws rds modify-db-instance \
  --db-instance-identifier prod-postgres-db \
  --master-user-password $(openssl rand -base64 32)

# 2. Create application database and user
psql -h prod-postgres.rds.amazonaws.com \
     -U postgres \
     -c "CREATE USER alert_prod_user WITH PASSWORD '$(vault kv get -field=password secret/prod/db)';"

psql -h prod-postgres.rds.amazonaws.com \
     -U postgres \
     -c "CREATE DATABASE alert_db OWNER alert_prod_user;"

# 3. Grant minimal permissions (principle of least privilege)
psql -h prod-postgres.rds.amazonaws.com \
     -U postgres \
     -d alert_db \
     -c "GRANT USAGE ON SCHEMA public TO alert_prod_user;"

psql -h prod-postgres.rds.amazonaws.com \
     -U postgres \
     -d alert_db \
     -c "GRANT CREATE ON SCHEMA public TO alert_prod_user;"

# 4. Initialize schema (application does this on startup with ddl-auto: validate)
psql -h prod-postgres.rds.amazonaws.com \
     -U alert_prod_user \
     -d alert_db \
     -f schema.sql

# 5. Create indexes for performance
psql -h prod-postgres.rds.amazonaws.com \
     -U alert_prod_user \
     -d alert_db \
     -c "CREATE INDEX idx_alerts_created_at ON alerts(created_at);"

psql -h prod-postgres.rds.amazonaws.com \
     -U alert_prod_user \
     -d alert_db \
     -c "CREATE INDEX idx_alerts_risk_level ON alerts(risk_level);"

psql -h prod-postgres.rds.amazonaws.com \
     -U alert_prod_user \
     -d alert_db \
     -c "CREATE INDEX idx_alerts_user_id ON alerts(user_id);"

# 6. Enable automated backups
aws rds modify-db-instance \
  --db-instance-identifier prod-postgres-db \
  --backup-retention-period 30 \
  --preferred-backup-window "03:00-04:00"
```

### Settings Summary (Production)

| Setting | Value | Reason |
|---------|-------|--------|
| Hibernate | validate | No schema changes without migration |
| Logging | WARN | Minimal log volume for performance |
| Retry Attempts | 3 | Balance reliability and latency |
| Retry Delay | 2-8 seconds exponential | Handle transient failures |
| Database Pool | 30 connections | Support 200 concurrent requests |
| Shutdown | graceful | Complete in-flight requests |
| Compression | enabled | Reduce network bandwidth |
| Health Details | when-authorized | Security - hide internals |
| Endpoints | minimal | health, metrics, info only |
| Circuit Breaker | enabled | Fail fast on cascade failures |
| Cache | enabled | Reduce database load |

### Verification (Production)

```bash
# 1. Confirm production profile active
curl -H "Authorization: Bearer $TOKEN" \
  https://alert-service.company.com/actuator/env | \
  grep spring.profiles.active
# Expected: "spring.profiles.active": "prod"

# 2. Check service health
curl https://alert-service.company.com/actuator/health/live
# Expected: { "status": "UP" }

# 3. Check database connectivity
curl -H "Authorization: Bearer $TOKEN" \
  https://alert-service.company.com/actuator/health/db
# Expected: healthy database connection

# 4. Monitor metrics
curl https://alert-service.company.com/actuator/metrics

# 5. Check alert statistics
curl -H "Authorization: Bearer $TOKEN" \
  https://alert-service.company.com/api/alerts/statistics
```

## Emergency Procedures

### Rollback Configuration

If a bad configuration is deployed:

```bash
# Kubernetes: Revert to previous deployment
kubectl rollout undo deployment/alert-service -n production

# Docker: Use previous image tag
docker pull company/alert-service:v1.0.0  # Previous known-good version
docker run ... alert-service:v1.0.0

# Check deployed version
curl https://alert-service.company.com/actuator/info | grep version
```

### Emergency Config Override

For critical production issues:

```bash
# Update Kubernetes secret with emergency override
kubectl patch secret alert-service-secrets \
  -p '{"data":{"db-host":"'$(echo -n "failover-db.company.com" | base64)'"}}'

# Restart pods to pick up secret change
kubectl rollout restart deployment/alert-service -n production
```

## Configuration Troubleshooting

### Issue: Wrong Environment Profile Active

**Symptom**: Using dev settings in staging

**Solution**:
```bash
# Verify active profile
curl http://localhost:8082/actuator/env | grep spring.profiles.active

# Restart with correct profile
SPRING_PROFILES_ACTIVE=staging java -jar app.jar
```

### Issue: Environment Variable Not Found

**Symptom**: "Cannot resolve placeholder 'DB_PASSWORD' in string value"

**Solution**:
```bash
# Check variable is exported
echo $DB_PASSWORD

# Set variable before running
export DB_PASSWORD=actual_password
java -jar app.jar --spring.profiles.active=prod

# Or use .env file
source .env.production
java -jar app.jar
```

### Issue: Database Connection Timeout

**Symptom**: "HikariPool connection timeout"

**Solution**:
```bash
# Check database is running and accessible
psql -h db_host -U db_user -c "SELECT 1;"

# Verify network connectivity
nc -v db_host 5432

# Increase timeout in profile
# connection-timeout: 30000  # 30 seconds
```
