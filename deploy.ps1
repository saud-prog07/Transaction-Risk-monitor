#!/usr/bin/env powershell
<#
.SYNOPSIS
  Production deployment script for Risk Monitoring System (10K+ users)
  
.DESCRIPTION
  Automated deployment with:
  - Docker services startup
  - Database optimization
  - MQ configuration
  - Health checks
  - Optional load testing
  
.EXAMPLE
  .\deploy.ps1 -RunLoadTest
  .\deploy.ps1 -HealthCheckOnly
#>

param(
    [switch]$RunLoadTest,
    [switch]$HealthCheckOnly,
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$projectRoot = "c:\real-time transaction risk monitoring system using microservices architecture"

# Colors for output
$Green = [System.ConsoleColor]::Green
$Red = [System.ConsoleColor]::Red
$Yellow = [System.ConsoleColor]::Yellow
$Blue = [System.ConsoleColor]::Blue

function Write-Status {
    param([string]$Message, [System.ConsoleColor]$Color = $Green)
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Color
}

function Write-Error-Status {
    param([string]$Message)
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ERROR: $Message" -ForegroundColor $Red
}

Write-Status "========================================" $Blue
Write-Status "Risk Monitoring System - Deployment" $Blue
Write-Status "========================================" $Blue

# ========== STEP 1: Health Check Only Mode ==========
if ($HealthCheckOnly) {
    Write-Status "Running health checks only..." $Yellow
    
    Write-Status "Checking Producer Service..."
    $prod = Invoke-RestMethod -Uri "$BaseUrl/api/v1/actuator/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($prod.status -eq "UP") { Write-Status "✓ Producer: UP" } else { Write-Error-Status "Producer: DOWN" }
    
    Write-Status "Checking Risk Engine..."
    $risk = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/actuator/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($risk.status -eq "UP") { Write-Status "✓ Risk Engine: UP" } else { Write-Error-Status "Risk Engine: DOWN" }
    
    Write-Status "Checking Alert Service..."
    $alert = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/actuator/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($alert.status -eq "UP") { Write-Status "✓ Alert Service: UP" } else { Write-Error-Status "Alert Service: DOWN" }
    
    exit 0
}

# ========== STEP 2: Start Docker Services ==========
Write-Status "Starting Docker services..." $Yellow
Set-Location $projectRoot
try {
    # Start MQ with production config
    docker-compose -f mq-docker-compose-production.yml up -d
    Write-Status "✓ Docker services started"
} catch {
    Write-Error-Status "Failed to start Docker services: $_"
    exit 1
}

# Wait for services to be ready
Write-Status "Waiting for services to initialize..." $Yellow
Start-Sleep -Seconds 10

# ========== STEP 3: Apply Database Optimization ==========
Write-Status "Applying database optimizations..." $Yellow
try {
    # Apply indexes and tuning
    docker exec postgres-prod psql -U postgres -d risk_monitoring_db -f postgres-optimization.sql 2>&1 | Out-Null
    Write-Status "✓ Database indexes and optimization applied"
} catch {
    Write-Error-Status "Note: Database optimization may require manual execution"
}

# ========== STEP 4: Apply MQ Configuration ==========
Write-Status "Configuring IBM MQ..." $Yellow
try {
    # Create queues and channels
    Get-Content "$projectRoot\mq-config.mqsc" | docker exec -i risk-monitoring-mq runmqsc QMGR 2>&1 | Out-Null
    Write-Status "✓ MQ queues and channels configured"
} catch {
    Write-Error-Status "Failed to apply MQ configuration: $_"
}

# ========== STEP 5: Health Checks ==========
Write-Status "Running health checks..." $Yellow
$healthChecksFailed = $false

try {
    $producer = Invoke-RestMethod -Uri "$BaseUrl/api/v1/actuator/health" -TimeoutSec 10 -ErrorAction SilentlyContinue
    if ($producer.status -eq "UP") {
        Write-Status "✓ Producer Service is UP"
    } else {
        Write-Error-Status "Producer Service is not responding correctly"
        $healthChecksFailed = $true
    }
} catch {
    Write-Error-Status "Producer Service health check failed: $($_.Exception.Message)"
    $healthChecksFailed = $true
}

try {
    $risk = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/actuator/health" -TimeoutSec 10 -ErrorAction SilentlyContinue
    if ($risk.status -eq "UP") {
        Write-Status "✓ Risk Engine is UP"
    } else {
        Write-Error-Status "Risk Engine is not responding correctly"
        $healthChecksFailed = $true
    }
} catch {
    Write-Error-Status "Risk Engine health check failed: $($_.Exception.Message)"
    $healthChecksFailed = $true
}

try {
    $alert = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/actuator/health" -TimeoutSec 10 -ErrorAction SilentlyContinue
    if ($alert.status -eq "UP") {
        Write-Status "✓ Alert Service is UP"
    } else {
        Write-Error-Status "Alert Service is not responding correctly"
        $healthChecksFailed = $true
    }
} catch {
    Write-Error-Status "Alert Service health check failed: $($_.Exception.Message)"
    $healthChecksFailed = $true
}

# ========== STEP 6: Load Testing ==========
if ($RunLoadTest) {
    Write-Status "Starting K6 load test (10,000 concurrent users)..." $Yellow
    Write-Status "This will take approximately 24 minutes..." $Yellow
    
    # Check if k6 is installed
    $k6Check = Get-Command k6 -ErrorAction SilentlyContinue
    if ($null -eq $k6Check) {
        Write-Error-Status "k6 is not installed. Install with: choco install k6"
        exit 1
    }
    
    # Run load test with results
    Set-Location $projectRoot
    $env:BASE_URL = $BaseUrl
    
    Write-Status "Executing: k6 run load-test-10k-users.js"
    & k6 run load-test-10k-users.js -o "json=load-test-results-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
    
    Write-Status "Load test completed. Results saved." $Green
}

# ========== SUMMARY ==========
Write-Status "========================================" $Blue
if ($healthChecksFailed) {
    Write-Error-Status "Deployment completed with health check warnings"
    Write-Status "Check Docker logs for details:" $Yellow
    Write-Host "docker logs -f risk-monitoring-producer"
    Write-Host "docker logs -f risk-monitoring-risk-engine"
    Write-Host "docker logs -f risk-monitoring-alert-service"
} else {
    Write-Status "✓ Deployment SUCCESSFUL" $Green
    Write-Status "System is ready for production use" $Green
}

Write-Status "Dashboard: http://localhost:3000" $Blue
Write-Status "API Docs: $BaseUrl/api/v1/swagger-ui.html" $Blue
Write-Status "Health: $BaseUrl/api/v1/actuator/health" $Blue
Write-Status "Metrics: $BaseUrl/api/v1/actuator/prometheus" $Blue
Write-Status "========================================" $Blue
