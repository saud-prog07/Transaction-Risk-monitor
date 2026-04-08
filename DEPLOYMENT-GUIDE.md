# Deployment Guide

Complete guide for deploying the event-driven transaction risk monitoring pipeline across development, staging, and production environments.

## Quick Start

### Development (Local Machine)

```bash
# Build all services
mvn clean package -DskipTests

# Terminal 1: Start PostgreSQL
docker run --name postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:15

# Terminal 2: Producer Service
cd producer-service
mvn spring-boot:run -Dspring.profiles.active=dev

# Terminal 3: Risk Engine
cd risk-engine
mvn spring-boot:run -Dspring.profiles.active=dev

# Terminal 4: Alert Service
cd alert-service
mvn spring-boot:run -Dspring.profiles.active=dev

# Terminal 5: Run verification script
.\verify-pipeline.ps1
```

### Staging (Cloud Infrastructure)

```bash
# 1. Build Docker images
docker login registry.company.com
mvn clean package docker:build

# 2. Push to registry
docker push registry.company.com/alert-service:latest
docker push registry.company.com/risk-engine:latest
docker push registry.company.com/producer-service:latest

# 3. Deploy to Kubernetes
kubectl apply -f k8s/staging/

# 4. Verify deployment
kubectl rollout status deployment/alert-service -n staging
kubectl rollout status deployment/risk-engine -n staging
kubectl rollout status deployment/producer-service -n staging

# 5. Run tests
.\verify-pipeline.ps1 -ProducerUrl "http://producer-service.staging.internal:8080" \
                       -RiskEngineUrl "http://risk-engine.staging.internal:8081" \
                       -AlertServiceUrl "http://alert-service.staging.internal:8082"
```

### Production (Business-Critical)

```bash
# 1. Verify all checks pass
./scripts/pre-deployment-checklist.sh

# 2. Create git tag for release
git tag -a v1.0.0 -m "Production release"
git push origin v1.0.0

# 3. Build production Docker images with security scanning
docker build \
  -t registry.company.com/alert-service:v1.0.0 \
  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
  -f Dockerfile .

# Scan for vulnerabilities
trivy image registry.company.com/alert-service:v1.0.0

# 4. Push to production registry
docker push registry.company.com/alert-service:v1.0.0

# 5. Deploy to production with blue-green strategy
kubectl apply -f k8s/production/alert-service-blue.yaml
# Monitor for 5 minutes
kubectl set service alert-service-blue to alert-service
# If issues, switch back: kubectl set service alert-service-green to alert-service

# 6. Verify all services healthy
for svc in producer risk-engine alert-service; do
  kubectl run -it --image=curlimages/curl:latest \
    health-check-$svc \
    -- curl http://$svc:8082/actuator/health/live
done

# 7. Monitor for 30 minutes
kubectl logs -f deployment/alert-service -n production
```

## Detailed Deployment Procedures

### Phase 1: Pre-Deployment Validation

#### Code Quality Checks

```bash
# Run static analysis
mvn clean verify spotbugs:check

# Check test coverage (must be >80%)
mvn jacoco:report
# Open: target/site/jacoco/index.html

# Run integration tests
mvn integration-test

# Security scan
mvn org.owasp:dependency-check-maven:check
```

#### Build Artifacts

```bash
# Create reproducible builds
mvn clean package -DskipTests \
  -Dproject.build.timestamp=$(date -u +'%Y-%m-%dT%H:%M:%SZ')

# Verify JAR integrity
md5sum alert-service/target/alert-service-*.jar
codesign -d alert-service/target/alert-service-*.jar  # macOS

# Create SBOM (Software Bill of Materials)
mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom
```

### Phase 2: Container Preparation

#### Build Multi-Stage Docker Image

```dockerfile
# File: Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
ARG JAR_FILE=target/alert-service-*.jar
COPY ${JAR_FILE} app.jar
RUN jar xf app.jar

FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="devops@company.com"
LABEL description="Alert Service - Transaction Risk Monitoring"

# Security: Create non-root user
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser

WORKDIR /opt/app
COPY --from=builder --chown=appuser:appuser /workspace/BOOT-INF/lib /opt/app/lib
COPY --from=builder --chown=appuser:appuser /workspace/BOOT-INF/classes /opt/app/classes
COPY --from=builder --chown=appuser:appuser /workspace/META-INF /opt/app/META-INF

USER appuser
EXPOSE 8082
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health/live || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-Dspring.profiles.active=prod", \
  "-Dlogging.config=classpath:logback-spring.xml", \
  "-cp", ".:lib/*", \
  "com.example.riskmonitoring.alertservice.AlertServiceApplication"]
```

#### Build and Push Images

```bash
# Build with metadata
docker build \
  -t registry.company.com/alert-service:v1.0.0 \
  -t registry.company.com/alert-service:latest \
  --label "git.commit=$(git rev-parse HEAD)" \
  --label "git.branch=$(git rev-parse --abbrev-ref HEAD)" \
  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
  -f Dockerfile .

# Scan for vulnerabilities BEFORE pushing
trivy image --type library --exit-code 1 \
  registry.company.com/alert-service:v1.0.0

# Step through results
trivy image --severity HIGH,CRITICAL \
  registry.company.com/alert-service:v1.0.0

# Push only if scanning passes
docker push registry.company.com/alert-service:v1.0.0
docker push registry.company.com/alert-service:latest
```

### Phase 3: Environment-Specific Deployment

#### Staging Deployment

```yaml
# File: k8s/staging/alert-service.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: staging
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: alert-service
  namespace: staging
  labels:
    app: alert-service
    environment: staging
spec:
  replicas: 2  # HA for staging
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: alert-service
  template:
    metadata:
      labels:
        app: alert-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8082"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - alert-service
            topologyKey: kubernetes.io/hostname
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
      serviceAccountName: alert-service
      containers:
      - name: alert-service
        image: registry.company.com/alert-service:v1.0.0
        imagePullPolicy: IfNotPresent
        securityContext:
          privileged: false
          capabilities:
            drop:
            - ALL
        ports:
        - name: http
          containerPort: 8082
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "staging"
        envFrom:
        - configMapRef:
            name: alert-service-config
        - secretRef:
            name: alert-service-secrets
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
          timeoutSeconds: 3
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/ready
            port: http
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        startupProbe:
          httpGet:
            path: /actuator/health/live
            port: http
          initialDelaySeconds: 0
          periodSeconds: 5
          failureThreshold: 30
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: alert-service-config
  namespace: staging
data:
  ALERT_EMAIL_RECIPIENTS: "devops@company.com"
  REDIS_HOST: "redis-staging.internal"
  REDIS_PORT: "6379"
---
apiVersion: v1
kind: Service
metadata:
  name: alert-service
  namespace: staging
spec:
  type: ClusterIP
  selector:
    app: alert-service
  ports:
  - name: http
    port: 8082
    targetPort: http
  sessionAffinity: None
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: alert-service-hpa
  namespace: staging
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: alert-service
  minReplicas: 2
  maxReplicas: 5
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

#### Production Blue-Green Deployment

```yaml
# File: k8s/production/alert-service-blue-green.yaml
---
# BLUE deployment (keep running)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: alert-service-blue
  namespace: production
spec:
  replicas: 3  # High availability
  strategy:
    type: RollingUpdate  # Rolling updates within deployment
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 1
  selector:
    matchLabels:
      app: alert-service
      version: blue
  template:
    metadata:
      labels:
        app: alert-service
        version: blue
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - alert-service
            topologyKey: kubernetes.io/hostname
      containers:
      - name: alert-service
        image: registry.company.com/alert-service:v1.0.0
        # ... same as staging but production settings
---
# GREEN deployment (new version - don't run initially)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: alert-service-green
  namespace: production
spec:
  replicas: 0  # Start with 0 - manually scale up for testing
  selector:
    matchLabels:
      app: alert-service
      version: green
  template:
    metadata:
      labels:
        app: alert-service
        version: green
    spec:
      containers:
      - name: alert-service
        image: registry.company.com/alert-service:v1.0.1  # New version
---
# Service that routes to active deployment
apiVersion: v1
kind: Service
metadata:
  name: alert-service
  namespace: production
spec:
  type: LoadBalancer
  selector:
    app: alert-service
    version: blue  # Change to green for switchover
  ports:
  - name: http
    port: 80
    targetPort: 8082
    protocol: TCP
```

#### Blue-Green Deployment Script

```bash
#!/bin/bash
# File: scripts/blue-green-deploy.sh

set -e

NAMESPACE="production"
SERVICE="alert-service"
NEW_VERSION="v1.0.1"
BLUE_REPLICAS=3
GREEN_REPLICAS=3
HEALTH_CHECK_TIMEOUT=300

echo "=== Blue-Green Deployment Script ==="
echo "Service: $SERVICE"
echo "New Version: $NEW_VERSION"
echo ""

# Step 1: Scale up GREEN deployment
echo "[Step 1] Scaling GREEN deployment to $GREEN_REPLICAS replicas..."
kubectl scale deployment ${SERVICE}-green \
  --replicas=$GREEN_REPLICAS \
  -n $NAMESPACE

# Step 2: Wait for GREEN pods to be ready
echo "[Step 2] Waiting for GREEN pods to be healthy..."
kubectl rollout status deployment/${SERVICE}-green \
  -n $NAMESPACE \
  --timeout=5m

# Step 3: Health check GREEN
echo "[Step 3] Performing health checks on GREEN..."
GREEN_POD=$(kubectl get pods -l version=green -n $NAMESPACE -o jsonpath='{.items[0].metadata.name}')
for i in {1..5}; do
  if kubectl exec -it $GREEN_POD -n $NAMESPACE -- curl -f http://localhost:8082/actuator/health/live; then
    echo "✓ GREEN pod $i health check PASSED"
  else
    echo "✗ GREEN pod $i health check FAILED"
    echo "Aborting deployment - GREEN is unhealthy"
    kubectl scale deployment ${SERVICE}-green --replicas=0 -n $NAMESPACE
    exit 1
  fi
done

# Step 4: Run smoke tests against GREEN
echo "[Step 4] Running smoke tests against GREEN..."
GREEN_SERVICE=$(kubectl get svc ${SERVICE}-green -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
TESTS_PASSED=true

# Test: Submit transaction
if ! curl -X POST http://${GREEN_SERVICE}:8082/transaction \
  -H "Content-Type: application/json" \
  -d '{"userId":"TEST","amount":100,"location":"Test"}'; then
  TESTS_PASSED=false
  echo "✗ Smoke test FAILED"
fi

if [ "$TESTS_PASSED" = false ]; then
  echo "Aborting deployment - smoke tests failed"
  kubectl scale deployment ${SERVICE}-green --replicas=0 -n $NAMESPACE
  exit 1
fi

echo "✓ Smoke tests PASSED"

# Step 5: Switch traffic from BLUE to GREEN
echo "[Step 5] Switching traffic from BLUE to GREEN..."
kubectl patch service ${SERVICE} \
  -p '{"spec":{"selector":{"version":"green"}}}' \
  -n $NAMESPACE

echo "✓ Traffic switched to GREEN"

# Step 6: Monitor GREEN for issues
echo "[Step 6] Monitoring GREEN for 5 minutes..."
MONITOR_END=$((SECONDS + 300))
ERRORS=0

while [ $SECONDS -lt $MONITOR_END ]; do
  READY=$(kubectl get deployment ${SERVICE}-green \
    -n $NAMESPACE \
    -o jsonpath='{.status.readyReplicas}')
  if [ "$READY" -lt "$GREEN_REPLICAS" ]; then
    ERRORS=$((ERRORS + 1))
    echo "✗ GREEN pods not healthy: $READY/$GREEN_REPLICAS ready"
    if [ $ERRORS -gt 3 ]; then
      echo "Too many health check failures - rolling back to BLUE"
      kubectl patch service ${SERVICE} \
        -p '{"spec":{"selector":{"version":"blue"}}}' \
        -n $NAMESPACE
      kubectl scale deployment ${SERVICE}-green --replicas=0 -n $NAMESPACE
      exit 1
    fi
  else
    echo "✓ GREEN healthy: $READY/$GREEN_REPLICAS pods ready"
    ERRORS=0
  fi
  sleep 30
done

echo "✓ GREEN stable after 5 minutes"

# Step 7: Scale down BLUE
echo "[Step 7] Scaling down BLUE deployment..."
kubectl scale deployment ${SERVICE}-blue \
  --replicas=0 \
  -n $NAMESPACE

echo ""
echo "=== Deployment Complete ==="
echo "✓ GREEN is now active as ${SERVICE}"
echo "✓ BLUE is scaled down (kept for quick rollback)"
echo ""
echo "To rollback:"
echo "  kubectl patch service ${SERVICE} -p '{\"spec\":{\"selector\":{\"version\":\"blue\"}}}' -n $NAMESPACE"
echo "  kubectl scale deployment ${SERVICE}-blue --replicas=$BLUE_REPLICAS -n $NAMESPACE"
```

### Phase 4: Post-Deployment Validation

#### Health Checks

```bash
#!/bin/bash
# File: scripts/post-deployment-checks.sh

echo "=== Post-Deployment Validation ==="

# Check 1: All pods running
echo "[Check 1] Verifying all pods are running..."
kubectl get pods -n production -l app=alert-service

# Check 2: Service endpoints
echo "[Check 2] Checking service endpoints..."
kubectl get endpoints alert-service -n production

# Check 3: Health endpoints
echo "[Check 3] Checking health endpoints..."
ENDPOINT=$(kubectl get svc alert-service -n production -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
curl -f http://$ENDPOINT/actuator/health/live
curl -f http://$ENDPOINT/actuator/health/ready

# Check 4: Database connectivity
echo "[Check 4] Verifying database connectivity..."
POD=$(kubectl get pods -l app=alert-service -n production -o jsonpath='{.items[0].metadata.name}')
kubectl exec -it $POD -n production -- curl -f http://localhost:8082/actuator/health/db

# Check 5: Message queue connectivity
echo "[Check 5] Verifying message queue connectivity..."
curl -f http://$ENDPOINT/actuator/health/jms

# Check 6: Run functional tests
echo "[Check 6] Running functional tests..."
./verify-pipeline.ps1 -ProducerUrl "http://$ENDPOINT:8080" \
                       -AlertServiceUrl "http://$ENDPOINT:8082"

# Check 7: Database consistency
echo "[Check 7] Checking database consistency..."
kubectl exec -it $POD -n production -- \
  psql -h $DB_HOST -U $DB_USER -d alert_db \
  -c "SELECT COUNT(*) as alert_count FROM alerts;"

# Check 8: Monitor logs for errors
echo "[Check 8] Checking logs for errors..."
kubectl logs -l app=alert-service -n production --tail=100 | grep -i error || echo "No errors found"

echo ""
echo "=== All Checks Complete ==="
```

#### Performance Baseline

```bash
#!/bin/bash
# File: scripts/performance-baseline.sh

echo "=== Establishing Performance Baseline ==="

ENDPOINT=$(kubectl get svc alert-service -n production -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
DURATION=300  # 5 minutes
TPS=100  # Transactions per second

# Run load test
ab -t $DURATION -r -c $TPS \
   -q \
   http://$ENDPOINT/api/alerts/statistics

# Extract metrics
echo ""
echo "Performance Baseline:"
echo "  - Requests/sec: $TPS"
echo "  - Duration: ${DURATION}s"
echo "  - Expected Latency: <200ms p50, <500ms p99"

# Get baseline from metrics
curl http://$ENDPOINT/actuator/metrics/http.server.requests | jq '.measurements'
```

## Rollback Procedures

### Quick Rollback (Kubernetes)

```bash
# Check rollout history
kubectl rollout history deployment/alert-service -n production

# Rollback to previous version
kubectl rollout undo deployment/alert-service -n production

# Rollback to specific revision
kubectl rollout undo deployment/alert-service -n production --to-revision=5

# Monitor rollback progress
kubectl rollout status deployment/alert-service -n production
```

### Manual Rollback (Docker)

```bash
# Get current running container
docker ps | grep alert-service

# Stop current container
docker stop <container-id>

# Start previous version
docker run -d \
  -p 8082:8082 \
  registry.company.com/alert-service:v1.0.0  # Previous working version
```

### Data Rollback (Database)

```bash
# If deployment caused data corruption:

# 1. Create backup before any changes
pg_dump -h prod-postgres.rds.amazonaws.com \
        -U postgres \
        alert_db > pre-deployment-backup.sql

# 2. If needed, restore from backup
psql -h prod-postgres.rds.amazonaws.com \
     -U postgres \
     alert_db < pre-deployment-backup.sql
```

## Monitoring & Alerting

### Prometheus Metrics (Auto-enabled)

```yaml
# alert-service/src/main/resources/prometheus-rules.yaml
groups:
- name: alert_service_rules
  interval: 30s
  rules:
  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
    for: 5m
    annotations:
      summary: "High error rate in {{ $labels.instance }}"
  
  - alert: HighLatency
    expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m])) > 5
    for: 5m
    annotations:
      summary: "High latency detected: {{ $value }}s"
  
  - alert: DatabaseConnectionPoolExhausted
    expr: hikaricp_connections_active{pool="default"} > 25
    for: 2m
    annotations:
      summary: "Database connection pool near capacity"
```

### Deploy Prometheus Rule

```bash
kubectl apply -f - <<EOF
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: alert-service-rules
  namespace: production
spec:
  groups:
  - name: alert_service
    interval: 30s
    rules:
    - alert: HighErrorRate
      expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
      for: 5m
      annotations:
        summary: "High error rate: {{ \$value | humanizePercentage }}"
EOF
```


