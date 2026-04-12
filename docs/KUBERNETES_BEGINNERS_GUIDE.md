# Kubernetes for Beginners: Deploy Your App

**This guide explains Kubernetes step-by-step. No prior experience needed.**

---

## What is Kubernetes? (Simple Version)

Imagine you have 3 restaurant employees (servers), and you need them to:
- Run reliably (restart if they get sick)
- Handle more customers by hiring more staff (scale up)
- Handle fewer customers by reducing staff (scale down)
- Be reachable by phone (get traffic to the right place)

**Kubernetes is a manager that does all that automatically for your application.**

Instead of employees, Kubernetes manages **containers** (small packages with your app inside).

---

## Key Concepts (5 Minutes to Understand)

### 1. **Pod** = One Container Running

A **Pod** is like a lunch box with your application inside.

```
┌─────────────────────┐
│ Pod                 │
│ ┌─────────────────┐ │
│ │ Your App        │ │
│ │ (Java Service)  │ │
│ └─────────────────┘ │
└─────────────────────┘
```

**Simple facts:**
- 1 pod = 1 container running (usually)
- Pods can start, stop, and restart
- Each pod gets an IP address (like a phone number)
- Pods are **temporary** — they can disappear and be replaced

**Example in your system:**
```
Pod: producer-service-6dff7f89b9-2pgjn
  - Running: producer-service app
  - IP: 10.244.0.34
  - Port: 8080
```

---

### 2. **Deployment** = "Keep N Pods Running"

A **Deployment** is like a boss who says: "I want exactly 3 lunch boxes ready at all times."

If a lunch box breaks, the boss immediately makes a new one.

```
┌──────────────────────────────────────┐
│ Deployment (producer-service)        │
│  "Keep 1 pod running"                │
├──────────────────────────────────────┤
│ Pod 1: producer-service (Running)    │
│ Pod 2: [empty - pod crashed]         │
│        → Kubernetes restarts Pod 1   │
└──────────────────────────────────────┘
```

**Simple facts:**
- Deployments manage pods automatically
- If a pod crashes → Deployment creates a new one
- You tell Kubernetes: "Run 3 copies of my app" → it keeps exactly 3 running
- Updates are automatic: change the image → old pods stop, new pods start

**Example in your system:**
```yaml
Deployment: producer-service
  Desired: 1 pod
  Current: 1 pod
  Ready: 1 pod ✓
```

---

### 3. **Service** = "Find My Pods"

A **Service** is like a phone number that forwards to multiple people.

When you call a service, it routes your call to one of N working pods.

```
External caller wants to reach your app
           ↓
    Service (Phone Number)
    - Port 30080 (NodePort)
           ↓
    ┌──────────────────────┐
    │ 3 Running Pods:      │
    │  - Pod A (8080)  ← routes to one
    │  - Pod B (8080)  ← load balanced
    │  - Pod C (8080)  ← automatically
    └──────────────────────┘
```

**3 Types of Services:**

| Type | Use Case | How to Access |
|------|----------|---------------|
| **ClusterIP** | Internal only (service to service) | `http://postgres:5432` (only from inside) |
| **NodePort** | Access from outside | `http://localhost:30080` (from your machine) |
| **LoadBalancer** | Cloud provider load balancer | `http://myapp.com` (public internet) |

**Simple facts:**
- Service = stable address for your pods
- Pods come and go, but services stay the same
- Traffic is sent to healthy pods only (automatic filtering)
- Multiple pods = traffic split automatically (load balancing)

**Example in your system:**
```
Service: producer-service
  Type: NodePort
  Port: 8080 (internal)
  NodePort: 30080 (accessible externally)
  Pods: 1 pod ready
  
Access it: http://localhost:30080
```

---

### 4. **ConfigMap & Secret** = Configuration

- **ConfigMap** = Non-secret settings (database host, log level, port numbers)
- **Secret** = Sensitive settings (passwords, API keys, JWT secrets)

**Example:**
```yaml
ConfigMap (stored as plain text):
  DB_HOST: postgres
  DB_PORT: 5432
  LOG_LEVEL: INFO

Secret (encrypted):
  DB_PASSWORD: postgres
  MQ_PASSWORD: guest
  JWT_SECRET: xxxxx
```

---

### 5. **HPA (Horizontal Pod Autoscaler)** = Automatic Scaling

An **HPA** watches your pods and says: "If CPU usage > 70%, add more pods."

```
Normal traffic:     1 pod running
         ↓
   Traffic spike
         ↓
   CPU usage: 95%
         ↓
   HPA sees CPU > 70%
         ↓
   HPA starts 2 more pods automatically
         ↓
   Now: 3 pods, CPU drops to 40%
         ↓
   Traffic calms down
         ↓
   CPU stays at 40% for 5 minutes
         ↓
   HPA removes 2 extra pods → back to 1
```

**In your system:**
```
HPA: risk-engine-hpa
  Min pods: 1
  Max pods: 3
  Trigger: CPU > 70%
  
Currently: 1 pod (low traffic)
If traffic spikes → up to 3 pods automatically
If traffic drops → back to 1 pod automatically
```

---

## Your File Structure

```
k8s/
├── configmap.yaml           ← Non-secret config (database host, ports)
├── secret.yaml              ← Secrets (passwords, API keys)
├── producer-service.yaml    ← Deployment + Service
├── risk-engine.yaml         ← Deployment + Service (internal)
├── alert-service.yaml       ← Deployment + Service (internal)
├── risk-engine-hpa.yaml     ← Auto-scaling rules
└── dependencies.yaml        ← PostgreSQL + RabbitMQ
```

---

## Step-by-Step: Deploy Your App

### Prerequisites
```bash
# Install Minikube (local Kubernetes)
minikube start          # Starts local Kubernetes cluster on your machine
kubectl get nodes       # Verify cluster is running
```

### Step 1: Start Kubernetes
```bash
minikube start
```

**What it does:**
- Starts a local Kubernetes cluster on your computer
- You can now manage containers with `kubectl` commands
- All data stored locally (no cloud costs)

### Step 2: Deploy Dependencies (PostgreSQL + RabbitMQ)
```bash
kubectl apply -f k8s/dependencies.yaml
```

**What it does:**
- Reads `dependencies.yaml` file
- Creates ConfigMap (non-secret settings)
- Creates Secret (passwords, API keys)
- Starts PostgreSQL container (database)
- Starts RabbitMQ container (message broker)
- Waits for them to be ready

**Check if they're running:**
```bash
kubectl get pods
```

**Expected output:**
```
NAME                    READY   STATUS    RESTARTS
postgres-654db44848*    1/1     Running   0
rabbitmq-6b5645f6*      1/1     Running   0
```

If NOT ready yet:
```bash
# Wait 30 seconds and check again
kubectl get pods
```

### Step 3: Deploy Your Microservices
```bash
# Deploy producer-service (entry point, NodePort exposed on port 30080)
kubectl apply -f k8s/producer-service.yaml

# Deploy risk-engine (internal, ClusterIP only)
kubectl apply -f k8s/risk-engine.yaml

# Deploy alert-service (internal, ClusterIP only)
kubectl apply -f k8s/alert-service.yaml

# Deploy auto-scaling rules (HPA)
kubectl apply -f k8s/risk-engine-hpa.yaml
```

**What it does:**
- Creates deployments (tells Kubernetes to keep pods running)
- Creates services (makes pods reachable)
- Sets up auto-scaling (add/remove pods based on CPU)
- Starts downloading Docker images

**Wait for them to be ready (takes 1-2 minutes):**
```bash
kubectl get pods
```

### Step 4: Verify Everything is Running
```bash
kubectl get pods
kubectl get services
kubectl get deployments
```

**Expected output:**
```
PODS:
NAME                              READY   STATUS
producer-service-7759b84*         1/1     Running
risk-engine-f6b47f867*            1/1     Running
alert-service-bf78cd4b*           1/1     Running
postgres-654db44848*              1/1     Running
rabbitmq-6b5645f6*                1/1     Running

SERVICES:
NAME               TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)
producer-service   NodePort    10.111.253.*    <none>        8080:30080/TCP ← External access
risk-engine        ClusterIP   10.101.70.*     <none>        8082/TCP       ← Internal only
alert-service      ClusterIP   10.104.3.*      <none>        8083/TCP       ← Internal only
postgres           ClusterIP   10.110.53.*     <none>        5432/TCP       ← Internal only
rabbitmq           ClusterIP   10.111.99.*     <none>        5672/TCP       ← Internal only

DEPLOYMENTS:
NAME               READY   UP-TO-DATE   AVAILABLE
producer-service   1/1     1            1 ✓
risk-engine        1/1     1            1 ✓
alert-service      1/1     1            1 ✓
postgres           1/1     1            1 ✓
rabbitmq           1/1     1            1 ✓
```

---

## Common `kubectl` Commands Explained

### 1. `kubectl get pods`
**What it does:** List all running containers

```bash
kubectl get pods
```

**Output:**
```
NAME                              READY   STATUS    RESTARTS   AGE
producer-service-7759b84*         1/1     Running   0          2m
risk-engine-f6b47f8*              1/1     Running   0          2m
alert-service-bf78cd4*            0/1     Running   2          2m
```

**What each column means:**
- `NAME`: Container name (auto-generated)
- `READY`: 1/1 = ready, 0/1 = not ready yet
- `STATUS`: Running = working, CrashLoopBackOff = error
- `RESTARTS`: Number of times restarted (0 = never crashed)
- `AGE`: How long it's been running

---

### 2. `kubectl get services`
**What it does:** List all services (phone numbers to reach pods)

```bash
kubectl get services
```

**Output:**
```
NAME               TYPE        CLUSTER-IP      PORT(S)
producer-service   NodePort    10.111.253.*    8080:30080
risk-engine        ClusterIP   10.101.70.*     8082
alert-service      ClusterIP   10.104.3.*      8083
```

**What each column means:**
- `NAME`: Service name
- `TYPE`: NodePort = external access, ClusterIP = internal only
- `CLUSTER-IP`: Internal IP (only reachable from inside cluster)
- `PORT(S)`: 8080:30080 means internal:external (call 30080 outside, reaches 8080 inside)

---

### 3. `kubectl describe pod <pod-name>`
**What it does:** Show detailed info about a pod

```bash
kubectl describe pod producer-service-7759b84bf9-2pgjn
```

**Shows:**
- Pod status and health
- When it was created
- Which image it's using
- Environment variables
- Recent events (errors, warnings)

**Useful for debugging why a pod isn't starting.**

---

### 4. `kubectl logs <pod-name>`
**What it does:** Show what the app inside the pod printed to console

```bash
kubectl logs producer-service-7759b84bf9-2pgjn
```

**Example output:**
```
2026-04-10 19:27:36.762 [INFO] Starting ProducerServiceApplication
2026-04-10 19:27:38.567 [INFO] Tomcat initialized with port 8080
2026-04-10 19:28:01.234 [INFO] Application started successfully
```

**Shows errors if something went wrong:**
```
2026-04-10 19:40:31.890 [ERROR] Failed to connect to database
org.postgresql.util.PSQLException: Connection refused
```

---

### 5. `kubectl apply -f k8s/`
**What it does:** Deploy everything in the k8s/ folder

```bash
kubectl apply -f k8s/
```

**Steps:**
1. Reads all `.yaml` files in `k8s/` folder
2. Creates ConfigMaps (settings)
3. Creates Secrets (passwords)
4. Creates Deployments (starts pods)
5. Creates Services (makes them reachable)

**Safe to run multiple times** — only updates things that changed.

---

### 6. `kubectl delete -f k8s/`
**What it does:** Remove everything

```bash
kubectl delete -f k8s/
```

**Warning:** Deletes all pods, services, deployments. Data in database is lost.

---

### 7. `kubectl get hpa`
**What it does:** Show auto-scaling status

```bash
kubectl get hpa
```

**Output:**
```
NAME              REFERENCE             TARGETS      MINPODS   MAXPODS   REPLICAS
risk-engine-hpa   Deployment/risk-engine cpu: 5%/70%  1         3         1
```

**What it means:**
- `TARGETS`: Current CPU 5%, trigger at 70%
- `MINPODS`: Always keep at least 1 pod
- `MAXPODS`: Never go above 3 pods
- `REPLICAS`: Currently running 1 pod

---

### 8. `kubectl port-forward svc/producer-service 8080:8080`
**What it does:** Access service from your machine (like a tunnel)

```bash
kubectl port-forward svc/producer-service 8080:8080
```

**Then access:**
```bash
curl http://localhost:8080/api/transaction
```

**Use case:** Debug or test a service that's not exposed via NodePort.

---

## Accessing Your App

### From Inside Minikube Network (No Extra Steps)
```bash
# These work automatically from any pod
http://producer-service:8080        # Via service name
http://postgres:5432               # Database from pods
http://rabbitmq:5672               # Message broker from pods
```

### From Your Machine (NodePort)
```bash
# producer-service is exposed on port 30080
http://localhost:30080

# OR use Minikube IP (if not localhost)
minikube ip                         # Shows IP (e.g., 192.168.49.2)
curl http://192.168.49.2:30080
```

### Test External Access
```bash
# Submit a transaction
curl -X POST http://localhost:30080/api/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "amount": 5000,
    "location": "NYC"
  }'

# Expected response:
# {"transactionId": "txn_abc123", "status": "SUBMITTED"}
```

---

## Troubleshooting

### Problem 1: Pod Not Starting (READY: 0/1)

**Step 1: Check status**
```bash
kubectl get pods
# Shows: alert-service-bf78cd4b   0/1   CrashLoopBackOff
```

**Step 2: Get details**
```bash
kubectl describe pod alert-service-bf78cd4b9-tm9zd
```

**Look for:**
```
Status: CrashLoopBackOff
Last State:
  Terminated
    Reason: Error
    Message: Application run failed
    Exit Code: 1
```

**Step 3: Check logs**
```bash
kubectl logs alert-service-bf78cd4b9-tm9zd
```

**Common causes & fixes:**

| Error | Cause | Fix |
|-------|-------|-----|
| `CrashLoopBackOff` | App crashed | Check logs with `kubectl logs` |
| `ImagePullBackOff` | Wrong Docker image | Make sure image exists: `docker images` |
| `Pending` (never starts) | Not enough resources | Check: `kubectl describe node` |
| `CreateContainerConfigError` | ConfigMap/Secret missing | Create them: `kubectl apply -f k8s/configmap.yaml` |

---

### Problem 2: Service Not Reachable

**Symptom:** Can't access `http://localhost:30080`

**Step 1: Check service exists**
```bash
kubectl get services
```

**Look for:**
```
NAME               TYPE        PORT(S)
producer-service   NodePort    8080:30080
```

**Step 2: Check if pod is ready**
```bash
kubectl get pods
# Risk Engine should show: 1/1 Running
```

**If not ready:**
```bash
kubectl describe pod producer-service-7759b84bf9-2pgjn
```

**Step 3: Check if port is correct**
```bash
# External port is 30080
curl http://localhost:30080

# NOT this (wrong port):
curl http://localhost:8080    ← This won't work
```

**Step 4: Try port-forward as workaround**
```bash
kubectl port-forward svc/producer-service 8080:8080
# Then access: http://localhost:8080
```

**Step 5: Check pod logs**
```bash
kubectl logs producer-service-7759b84bf9-2pgjn
# Look for ERROR messages
```

---

### Problem 3: Database Connection Failed

**Symptom:** Pod starts but app crashes with "Connection refused"

**Error in logs:**
```
org.postgresql.util.PSQLException: Connection refused
Could not connect to database at postgres:5432
```

**Fixes:**

1. **Check if PostgreSQL is running:**
```bash
kubectl get pods | grep postgres
# Should show: postgres-654db44848*   1/1   Running
```

2. **If not running, deploy it:**
```bash
kubectl apply -f k8s/dependencies.yaml
kubectl get pods                      # Wait for PostgreSQL to be ready
```

3. **Test connection from inside cluster:**
```bash
# Connect to a pod and test PostgreSQL
kubectl exec -it producer-service-7759b84bf9-2pgjn -- bash
# Inside pod:
nc -zv postgres 5432   # Should say "Connection successful"
```

---

### Problem 4: HPA Not Scaling

**Check HPA status:**
```bash
kubectl get hpa
```

**Look for:**
```
NAME              TARGETS          MINPODS   MAXPODS   REPLICAS
risk-engine-hpa   cpu: <unknown>/70%   1       3         1
```

**If `TARGETS` shows `<unknown>`:**
- Metrics server not ready yet (wait 1 minute)
- Or pod CPU usage not detected yet
- **Normal** on first deploy — scales automatically once metrics available

---

## Real-World Workflow

### Deploy Initial Version
```bash
kubectl apply -f k8s/
kubectl get pods              # Wait for all "Running"
```

### Test It
```bash
curl -X POST http://localhost:30080/api/transaction \
  -H "Content-Type: application/json" \
  -d '{"userId": "test1", "amount": 5000, "location": "NYC"}'
```

### Monitor It
```bash
kubectl get pods               # Check status
kubectl logs <pod-name>        # Check for errors
kubectl get hpa                # Check auto-scaling
```

### Update Configuration (No Pod Restart)
```bash
# Edit configmap
kubectl edit configmap app-config

# Or:
kubectl patch configmap app-config -p '{"data":{"LOG_LEVEL":"DEBUG"}}'

# Pods see new config automatically (some apps need restart)
```

### Deploy New Version
```bash
# Update Docker image in producer-service.yaml
# Change: image: producer-service:v1.2 (new version)

kubectl apply -f k8s/producer-service.yaml
# Old pods stop, new pods start automatically
```

### Clean Up Everything
```bash
kubectl delete -f k8s/      # Remove all services, pods, deployments
minikube stop               # Stop Kubernetes cluster
```

---

## Key Takeaways

✅ **Pods** = Your app containers (temporary, replaceable)

✅ **Deployments** = Keep N pods running (restart failed pods)

✅ **Services** = Make pods reachable (stable address, load balancing)

✅ **ConfigMap** = Save settings (database host, ports)

✅ **Secret** = Save passwords safely (encrypted)

✅ **HPA** = Scale automatically (add/remove pods based on CPU)

✅ **kubectl** = Commands to manage everything

✅ **Minikube** = Local Kubernetes for testing

---

## Need Help?

| Problem | Command |
|---------|---------|
| Check what's running | `kubectl get pods` |
| Detailed info | `kubectl describe pod <name>` |
| See app output | `kubectl logs <pod-name>` |
| Check services | `kubectl get services` |
| Check scaling | `kubectl get hpa` |
| Deploy/update | `kubectl apply -f k8s/` |
| Remove everything | `kubectl delete -f k8s/` |

**Next Step:** Run `kubectl apply -f k8s/` and watch your app deploy! 🚀
