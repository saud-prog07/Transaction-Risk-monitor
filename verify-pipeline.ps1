# ============================================================================
# Event-Driven Pipeline Verification Script
# ============================================================================
# 
# This script verifies the complete transaction processing pipeline:
# 1. Producer-service receives transaction
# 2. IBM MQ brokers message
# 3. Risk-engine processes and analyzes
# 4. Alert-service stores results
#
# Prerequisites:
# - All services running (producer:8080, risk-engine:8081, alert:8082)
# - PostgreSQL database initialized
#
# Usage: .\verify-pipeline.ps1
# ============================================================================

param(
    [string]$ProducerUrl = "http://localhost:8080",
    [string]$RiskEngineUrl = "http://localhost:8081",
    [string]$AlertServiceUrl = "http://localhost:8082",
    [int]$ProcessingDelaySeconds = 3,
    [bool]$Verbose = $true
)

# Color output constants
$SuccessColor = "Green"
$ErrorColor = "Red"
$WarningColor = "Yellow"
$InfoColor = "Cyan"

# ============================================================================
# Utility Functions
# ============================================================================

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor $SuccessColor
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor $ErrorColor
}

function Write-Warning-Custom {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor $WarningColor
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor $InfoColor
}

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host $Message -ForegroundColor Cyan
    Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
}

# ============================================================================
# Service Health Checks
# ============================================================================

function Test-ServiceHealth {
    param(
        [string]$ServiceName,
        [string]$ServiceUrl
    )

    Write-Info "Checking $ServiceName health..."

    try {
        $response = Invoke-WebRequest -Uri "$ServiceUrl/actuator/health" -Method Get -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Success "$ServiceName is UP"
            return $true
        } else {
            Write-Error-Custom "$ServiceName returned unexpected status: $($response.StatusCode)"
            return $false
        }
    } catch {
        Write-Error-Custom "$ServiceName is DOWN - Connection failed: $($_.Exception.Message)"
        return $false
    }
}

# ============================================================================
# Pipeline Tests
# ============================================================================

function Submit-Transaction {
    param(
        [string]$UserId,
        [decimal]$Amount,
        [string]$Location
    )

    Write-Info "Submitting transaction: UserId=$UserId, Amount=$Amount, Location=$Location"

    try {
        $payload = @{
            userId = $UserId
            amount = $Amount
            location = $Location
        } | ConvertTo-Json

        $response = Invoke-WebRequest -Uri "$ProducerUrl/transaction" `
            -Method Post `
            -ContentType "application/json" `
            -Body $payload `
            -ErrorAction Stop

        if ($response.StatusCode -eq 201) {
            $body = $response.Content | ConvertFrom-Json
            Write-Success "Transaction submitted: $($body.transactionId)"
            return $body.transactionId
        } else {
            Write-Error-Custom "Transaction submission failed: $($response.StatusCode)"
            return $null
        }
    } catch {
        Write-Error-Custom "Failed to submit transaction: $($_.Exception.Message)"
        return $null
    }
}

function Get-Alerts {
    param([int]$PageSize = 20)

    try {
        $response = Invoke-WebRequest -Uri "$AlertServiceUrl/api/alerts?page=0&size=$PageSize" `
            -Method Get `
            -ErrorAction Stop

        if ($response.StatusCode -eq 200) {
            $body = $response.Content | ConvertFrom-Json
            return $body.content
        } else {
            Write-Error-Custom "Failed to fetch alerts: $($response.StatusCode)"
            return $null
        }
    } catch {
        Write-Error-Custom "Failed to fetch alerts: $($_.Exception.Message)"
        return $null
    }
}

function Get-AlertStatistics {
    try {
        $response = Invoke-WebRequest -Uri "$AlertServiceUrl/api/alerts/statistics" `
            -Method Get `
            -ErrorAction Stop

        if ($response.StatusCode -eq 200) {
            return $response.Content | ConvertFrom-Json
        } else {
            Write-Error-Custom "Failed to fetch statistics: $($response.StatusCode)"
            return $null
        }
    } catch {
        Write-Error-Custom "Failed to fetch statistics: $($_.Exception.Message)"
        return $null
    }
}

function Test-HighValueTransactionFlow {
    Write-Header "Test 1: High-Value Transaction Detection"

    $transactionId = Submit-Transaction -UserId "TEST_USER_HIGH" -Amount 15000 -Location "New York, NY"
    if ($null -eq $transactionId) {
        Write-Error-Custom "Failed to submit transaction"
        return $false
    }

    Write-Info "Waiting $ProcessingDelaySeconds seconds for processing..."
    Start-Sleep -Seconds $ProcessingDelaySeconds

    $alerts = Get-Alerts
    if ($null -eq $alerts -or $alerts.Count -eq 0) {
        Write-Warning-Custom "No alerts found (unusual for high-value transaction)"
        return $false
    }

    Write-Success "High-value transaction correctly processed"
    Write-Info "Alert details:"
    foreach ($alert in $alerts | Select-Object -First 1) {
        Write-Host "  - Transaction ID: $($alert.transactionId)"
        Write-Host "  - Risk Level: $($alert.riskLevel)"
        Write-Host "  - Reason: $($alert.reason)"
    }
    return $true
}

function Test-LowRiskTransactionFlow {
    Write-Header "Test 2: Low-Risk Transaction (No Alert)"

    $transactionId = Submit-Transaction -UserId "TEST_USER_LOW" -Amount 50 -Location "Chicago, IL"
    if ($null -eq $transactionId) {
        Write-Error-Custom "Failed to submit transaction"
        return $false
    }

    Write-Info "Waiting $ProcessingDelaySeconds seconds for processing..."
    Start-Sleep -Seconds $ProcessingDelaySeconds

    $alerts = Get-Alerts
    if ($null -eq $alerts -or $alerts.Count -eq 0) {
        Write-Success "Low-risk transaction correctly not flagged"
        return $true
    } else {
        Write-Warning-Custom "Unexpected alert for low-risk transaction"
        return $false
    }
}

function Test-FrequencyAnomalyFlow {
    Write-Header "Test 3: Frequency Anomaly Detection"

    $testUserId = "TEST_USER_FREQ_$(Get-Random -Minimum 1000 -Maximum 9999)"
    
    Write-Info "Submitting 7 rapid transactions from same user..."
    for ($i = 0; $i -lt 7; $i++) {
        $amount = 100 + $i
        $transId = Submit-Transaction -UserId $testUserId -Amount $amount -Location "Los Angeles, CA"
        if ($null -eq $transId) {
            Write-Error-Custom "Failed to submit transaction $($i+1)"
            return $false
        }
        Start-Sleep -Milliseconds 300
    }

    Write-Info "Waiting $ProcessingDelaySeconds seconds for processing..."
    Start-Sleep -Seconds $ProcessingDelaySeconds

    $alerts = Get-Alerts
    if ($null -eq $alerts -or $alerts.Count -eq 0) {
        Write-Warning-Custom "No frequency anomaly alerts detected"
        return $false
    }

    Write-Success "Frequency anomaly correctly detected"
    Write-Info "Alert details:"
    foreach ($alert in $alerts | Select-Object -First 1) {
        Write-Host "  - Risk Level: $($alert.riskLevel)"
        Write-Host "  - Reason: $($alert.reason)"
    }
    return $true
}

function Test-DatabaseConnectivity {
    Write-Header "Test 4: Database Connectivity"

    try {
        $response = Invoke-WebRequest -Uri "$AlertServiceUrl/actuator/health/databaseHealth" `
            -Method Get `
            -ErrorAction Stop

        if ($response.StatusCode -eq 200) {
            $body = $response.Content | ConvertFrom-Json
            Write-Success "Database connection healthy"
            Write-Info "Pool Status:"
            Write-Host "  - Status: $($body.status)"
            Write-Host "  - Pool Size: $($body.pool_size)"
            Write-Host "  - Active Connections: $($body.active_connections)"
            Write-Host "  - Idle Connections: $($body.idle_connections)"
            return $true
        } else {
            Write-Error-Custom "Database health check failed: $($response.StatusCode)"
            return $false
        }
    } catch {
        Write-Error-Custom "Failed to check database health: $($_.Exception.Message)"
        return $false
    }
}

function Test-AlertStatistics {
    Write-Header "Test 5: Alert Statistics"

    $stats = Get-AlertStatistics
    if ($null -eq $stats) {
        Write-Error-Custom "Failed to retrieve statistics"
        return $false
    }

    Write-Success "Alert statistics retrieved successfully"
    Write-Info "Statistics:"
    Write-Host "  - Total Alerts: $($stats.totalAlerts)"
    Write-Host "  - Unreviewed: $($stats.unreviewedCount)"
    Write-Host "  - High Risk: $($stats.highRiskCount)"
    Write-Host "  - Medium Risk: $($stats.mediumRiskCount)"
    return $true
}

# ============================================================================
# Main Pipeline Verification
# ============================================================================

function Verify-Pipeline {
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  Event-Driven Pipeline Verification                        ║" -ForegroundColor Cyan
    Write-Host "║  Comprehensive test suite for complete transaction flow    ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""

    Write-Header "Phase 1: Service Health Checks"
    
    $producerHealthy = Test-ServiceHealth -ServiceName "Producer-Service" -ServiceUrl $ProducerUrl
    $riskEngineHealthy = Test-ServiceHealth -ServiceName "Risk-Engine" -ServiceUrl $RiskEngineUrl
    $alertServiceHealthy = Test-ServiceHealth -ServiceName "Alert-Service" -ServiceUrl $AlertServiceUrl

    if (-not ($producerHealthy -and $riskEngineHealthy -and $alertServiceHealthy)) {
        Write-Error-Custom "Some services are not healthy. Please start all services first."
        Write-Info "Start services with:"
        Write-Host "  cd producer-service && mvn spring-boot:run"
        Write-Host "  cd risk-engine && mvn spring-boot:run"
        Write-Host "  cd alert-service && mvn spring-boot:run"
        return $false
    }

    Write-Success "All services are healthy!"
    Write-Host ""

    # Run pipeline tests
    $testResults = @()
    
    $testResults += @{
        Name = "High-Value Transaction"
        Result = Test-HighValueTransactionFlow
    }

    $testResults += @{
        Name = "Low-Risk Transaction"
        Result = Test-LowRiskTransactionFlow
    }

    $testResults += @{
        Name = "Frequency Anomaly"
        Result = Test-FrequencyAnomalyFlow
    }

    $testResults += @{
        Name = "Database Connectivity"
        Result = Test-DatabaseConnectivity
    }

    $testResults += @{
        Name = "Alert Statistics"
        Result = Test-AlertStatistics
    }

    # Summary
    Write-Header "Test Summary"
    
    $passCount = ($testResults | Where-Object { $_.Result -eq $true }).Count
    $failCount = ($testResults | Where-Object { $_.Result -eq $false }).Count

    foreach ($test in $testResults) {
        if ($test.Result) {
            Write-Success "$($test.Name): PASSED"
        } else {
            Write-Error-Custom "$($test.Name): FAILED"
        }
    }

    Write-Host ""
    Write-Host "Results: $passCount passed, $failCount failed" -ForegroundColor Cyan
    
    if ($failCount -eq 0) {
        Write-Host ""
        Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
        Write-Host "║  All Tests PASSED ✓                                         ║" -ForegroundColor Green
        Write-Host "║  Event-driven pipeline is functioning correctly            ║" -ForegroundColor Green
        Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green
        return $true
    } else {
        Write-Host ""
        Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Red
        Write-Host "║  Some Tests FAILED ✗                                       ║" -ForegroundColor Red
        Write-Host "║  Review errors above for details                          ║" -ForegroundColor Red
        Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Red
        return $false
    }
}

# ============================================================================
# Execute Verification
# ============================================================================

$result = Verify-Pipeline
exit $(if ($result) { 0 } else { 1 })
