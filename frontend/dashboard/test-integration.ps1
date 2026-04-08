# ======================================================================
# Dashboard Testing Script (Windows PowerShell)
# Tests the dashboard and API integration
# ======================================================================

param(
    [string]$ApiUrl = "http://localhost:8082",
    [string]$DashboardUrl = "http://localhost:3000",
    [string]$ProducerUrl = "http://localhost:8080"
)

function Test-Connection {
    param(
        [string]$Url,
        [string]$Description
    )
    
    try {
        $response = Invoke-WebRequest -Uri "$Url" -Method GET -TimeoutSec 5 -ErrorAction Stop
        Write-Host "✅ $Description" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "❌ $Description failed" -ForegroundColor Red
        return $false
    }
}

function Test-Alerts {
    param([string]$ApiUrl)
    
    try {
        $response = Invoke-WebRequest -Uri "$ApiUrl/api/alerts?page=0&size=5" `
            -Method GET -TimeoutSec 5 -ErrorAction Stop
        $content = $response.Content | ConvertFrom-Json
        $alertCount = $content.content.Count
        
        if ($alertCount -gt 0) {
            Write-Host "✅ Found $alertCount alerts" -ForegroundColor Green
        }
        else {
            Write-Host "ℹ️  No alerts yet (normal for new setup)" -ForegroundColor Yellow
        }
        return $true
    }
    catch {
        Write-Host "❌ Failed to fetch alerts" -ForegroundColor Red
        return $false
    }
}

function Submit-TestTransaction {
    param([string]$ProducerUrl)
    
    try {
        $timestamp = [int]([datetime]::UtcNow.Subtract([datetime]"1970-01-01")).TotalSeconds
        $body = @{
            userId = "test-user-$timestamp"
            amount = 50000
            location = "Singapore, SG"
        } | ConvertTo-Json
        
        $response = Invoke-WebRequest -Uri "$ProducerUrl/api/transaction" `
            -Method POST `
            -ContentType "application/json" `
            -Body $body `
            -TimeoutSec 5 `
            -ErrorAction Stop
        
        $content = $response.Content | ConvertFrom-Json
        if ($content.transactionId) {
            Write-Host "✅ Test transaction submitted successfully" -ForegroundColor Green
            Write-Host "   Transaction ID: $($content.transactionId)" -ForegroundColor Cyan
            Start-Sleep -Seconds 3
            return $true
        }
    }
    catch {
        Write-Host "⚠️  Producer Service not responding (optional)" -ForegroundColor Yellow
        return $false
    }
}

# ======================================================================
# Main Execution
# ======================================================================

Write-Host "" 
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Dashboard & API Integration Test" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: API Health
Write-Host "1️⃣  Testing API Health..."
if (-not (Test-Connection "$ApiUrl/api/actuator/health" "API Health")) {
    Write-Host "   Start Alert Service first:" -ForegroundColor Yellow
    Write-Host "   cd alert-service && mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Test 2: Fetch Alerts
Write-Host "2️⃣  Testing Alerts Endpoint..."
Test-Alerts $ApiUrl
Write-Host ""

# Test 3: Submit Transaction
Write-Host "3️⃣  Submitting Test Transaction..."
if (Test-Connection "$ProducerUrl/api/actuator/health" "Producer Service") {
    Submit-TestTransaction $ProducerUrl
}
Write-Host ""

# Test 4: Dashboard
Write-Host "4️⃣  Checking Dashboard..."
if (Test-Connection "$DashboardUrl" "Dashboard") {
    Write-Host "   Open in browser: $DashboardUrl" -ForegroundColor Cyan
}
else {
    Write-Host "   Start with: npm start" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "✅ Tests Completed!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Open dashboard: $DashboardUrl"
Write-Host "2. Verify alerts are displayed"
Write-Host "3. Test filtering and search"
Write-Host "4. Try updating alert status"
Write-Host ""
