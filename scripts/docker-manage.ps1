# Docker Management Script for Risk Monitoring System
# Windows PowerShell Version

param(
    [Parameter(Position=0)]
    [string]$Command = "help",
    
    [Parameter(Position=1)]
    [string]$Service = ""
)

# Configuration
$projectName = "risk-monitoring-system"
$composeFile = "docker-compose.yml"
$envFile = ".env"
$envExample = ".env.example"

# Color codes
$colors = @{
    'Header' = 'Cyan'
    'Success' = 'Green'
    'Warning' = 'Yellow'
    'Error' = 'Red'
    'Info' = 'White'
}

# Helper functions
function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host ("=" * 50) -ForegroundColor $colors['Header']
    Write-Host $Message -ForegroundColor $colors['Header']
    Write-Host ("=" * 50) -ForegroundColor $colors['Header']
    Write-Host ""
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor $colors['Success']
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor $colors['Warning']
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor $colors['Error']
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor $colors['Info']
}

# Check prerequisites
function Check-Prerequisites {
    Write-Header "Checking Prerequisites"
    
    # Check Docker
    try {
        $dockerVersion = docker --version
        Write-Success "Docker $($dockerVersion.Split()[-1])"
    }
    catch {
        Write-Error "Docker is not installed or not accessible"
        exit 1
    }
    
    # Check Docker Compose
    try {
        $composeVersion = docker-compose --version
        Write-Success "Docker Compose $($composeVersion.Split()[-1])"
    }
    catch {
        Write-Error "Docker Compose is not installed or not accessible"
        exit 1
    }
    
    # Check if Docker daemon is running
    try {
        docker ps | Out-Null
        Write-Success "Docker daemon is running"
    }
    catch {
        Write-Error "Docker daemon is not running or not accessible"
        exit 1
    }
}

# Setup environment
function Setup-Environment {
    Write-Header "Setting Up Environment"
    
    if (-not (Test-Path $envFile)) {
        if (Test-Path $envExample) {
            Copy-Item $envExample $envFile
            Write-Success "Created $envFile from $envExample"
            Write-Warning "Update $envFile with your configuration"
        }
        else {
            Write-Error "$envExample not found"
            exit 1
        }
    }
    else {
        Write-Success "$envFile already exists"
    }
}

# Build Docker images
function Build-Images {
    param([string]$ServiceName = "")
    
    Write-Header "Building Docker Images"
    
    if ([string]::IsNullOrEmpty($ServiceName)) {
        docker-compose -f $composeFile --env-file $envFile build
        Write-Success "All images built successfully"
    }
    else {
        docker-compose -f $composeFile --env-file $envFile build $ServiceName
        Write-Success "Image '$ServiceName' built successfully"
    }
}

# Start services
function Start-Services {
    param([string]$ServiceName = "")
    
    Write-Header "Starting Services"
    
    if ([string]::IsNullOrEmpty($ServiceName)) {
        docker-compose -f $composeFile --env-file $envFile up -d
        Write-Success "All services started"
        Write-Host ""
        Get-ServiceStatus
        Write-Host ""
        Show-AccessPoints
    }
    else {
        docker-compose -f $composeFile --env-file $envFile up -d $ServiceName
        Write-Success "Service '$ServiceName' started"
    }
}

# Stop services
function Stop-Services {
    param([bool]$RemoveVolumes = $false)
    
    Write-Header "Stopping Services"
    
    if ($RemoveVolumes) {
        docker-compose -f $composeFile down -v
        Write-Warning "All services and volumes have been removed"
    }
    else {
        docker-compose -f $composeFile down
        Write-Success "All services stopped (data preserved)"
    }
}

# Get service status
function Get-ServiceStatus {
    Write-Header "Service Status"
    docker-compose -f $composeFile ps
}

# Show logs
function Show-Logs {
    param([string]$ServiceName = "")
    
    if ([string]::IsNullOrEmpty($ServiceName)) {
        Write-Header "Showing Logs for All Services"
        docker-compose -f $composeFile logs -f --tail=100
    }
    else {
        Write-Header "Showing Logs for $ServiceName"
        docker-compose -f $composeFile logs -f --tail=100 $ServiceName
    }
}

# Health check
function Test-Health {
    Write-Header "Health Check"
    
    # Check Producer Service
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" -ErrorAction SilentlyContinue
        if ($response.Content -match '"status":"UP"') {
            Write-Success "Producer Service (8080): Healthy"
        }
        else {
            Write-Error "Producer Service (8080): Unhealthy"
        }
    }
    catch {
        Write-Error "Producer Service (8080): Unreachable"
    }
    
    # Check Risk Engine
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081/api/actuator/health" -ErrorAction SilentlyContinue
        if ($response.Content -match '"status":"UP"') {
            Write-Success "Risk Engine (8081): Healthy"
        }
        else {
            Write-Error "Risk Engine (8081): Unhealthy"
        }
    }
    catch {
        Write-Error "Risk Engine (8081): Unreachable"
    }
    
    # Check Alert Service
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8082/api/actuator/health" -ErrorAction SilentlyContinue
        if ($response.Content -match '"status":"UP"') {
            Write-Success "Alert Service (8082): Healthy"
        }
        else {
            Write-Error "Alert Service (8082): Unhealthy"
        }
    }
    catch {
        Write-Error "Alert Service (8082): Unreachable"
    }
    
    # Check PostgreSQL
    try {
        $result = docker exec risk-monitoring-postgres pg_isready -U riskmonitor 2>$null
        Write-Success "PostgreSQL (5432): Healthy"
    }
    catch {
        Write-Error "PostgreSQL (5432): Unhealthy"
    }
    
    # Check IBM MQ
    try {
        $result = docker exec risk-monitoring-mq runmqsc -w 5 -e QMGR 2>$null
        Write-Success "IBM MQ (1414): Healthy"
    }
    catch {
        Write-Error "IBM MQ (1414): Unhealthy"
    }
}

# Test transactions
function Test-Transactions {
    Write-Header "Testing Transaction Submission"
    
    for ($i = 1; $i -le 3; $i++) {
        Write-Info "Submitting transaction $i..."
        
        $body = @{
            userId = "user$i"
            amount = [decimal]::Parse([string]$(1000 + (Get-Random -Maximum 5000)))
            location = "NYC, NY"
        } | ConvertTo-Json
        
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/transaction" `
                -Method POST `
                -ContentType "application/json" `
                -Body $body `
                -ErrorAction Continue
            
            $response.Content | ConvertFrom-Json | ConvertTo-Json | Write-Host
        }
        catch {
            Write-Error "Failed to submit transaction: $_"
        }
        
        Write-Host ""
    }
    
    Write-Success "Transaction tests completed"
}

# Restart services
function Restart-Services {
    param([string]$ServiceName = "")
    
    Write-Header "Restarting Services"
    
    if ([string]::IsNullOrEmpty($ServiceName)) {
        docker-compose -f $composeFile restart
        Write-Success "All services restarted"
    }
    else {
        docker-compose -f $composeFile restart $ServiceName
        Write-Success "Service '$ServiceName' restarted"
    }
}

# Show access points
function Show-AccessPoints {
    Write-Header "Access Points"
    Write-Host "Producer Service: http://localhost:8080" -ForegroundColor Green
    Write-Host "Risk Engine: http://localhost:8081" -ForegroundColor Green
    Write-Host "Alert Service: http://localhost:8082" -ForegroundColor Green
    Write-Host "PostgreSQL: localhost:5432 (user: riskmonitor)" -ForegroundColor Green
    Write-Host "IBM MQ Console: https://localhost:9443/ibmmq/console" -ForegroundColor Green
}

# Clean up
function Cleanup-All {
    Write-Header "Cleanup Warning"
    Write-Warning "This will remove all containers and volumes!"
    
    $response = Read-Host "Are you sure? (yes/no)"
    
    if ($response -eq "yes") {
        docker-compose -f $composeFile down -v
        Write-Success "All containers and volumes removed"
    }
    else {
        Write-Warning "Cleanup cancelled"
    }
}

# Show help
function Show-Help {
    $helpText = @"
╔════════════════════════════════════════════════════════════╗
║    Risk Monitoring System - Docker Management (Windows)    ║
╚════════════════════════════════════════════════════════════╝

USAGE:
    .\docker-manage.ps1 <command> [service]

COMMANDS:
    check           Check prerequisites (Docker, Docker Compose)
    setup           Setup environment files
    build [svc]     Build Docker images (all or specific service)
    up [svc]        Start services (all or specific service)
    down            Stop services (data preserved)
    down-vol        Stop services and remove volumes
    restart [svc]   Restart services
    ps              Show service status
    logs [svc]      View service logs
    health          Check health of all services
    test            Test transaction submission
    clean           Remove all containers and volumes
    help            Show this help message

EXAMPLES:
    # Full setup and start
    .\docker-manage.ps1 check
    .\docker-manage.ps1 setup
    .\docker-manage.ps1 build
    .\docker-manage.ps1 up

    # Start specific service
    .\docker-manage.ps1 up alert-service

    # View logs
    .\docker-manage.ps1 logs producer-service

    # Health check all services
    .\docker-manage.ps1 health

    # Stop everything
    .\docker-manage.ps1 down

"@
    Write-Host $helpText -ForegroundColor Cyan
}

# Main command processor
switch ($Command.ToLower()) {
    "check" {
        Check-Prerequisites
    }
    "setup" {
        Setup-Environment
    }
    "build" {
        Check-Prerequisites
        Build-Images $Service
    }
    "up" {
        Check-Prerequisites
        Start-Services $Service
    }
    "down" {
        Stop-Services $false
    }
    "down-vol" {
        Stop-Services $true
    }
    "ps" {
        Get-ServiceStatus
    }
    "logs" {
        Show-Logs $Service
    }
    "health" {
        Test-Health
    }
    "test" {
        Test-Transactions
    }
    "restart" {
        Restart-Services $Service
    }
    "clean" {
        Cleanup-All
    }
    "help" {
        Show-Help
    }
    default {
        Write-Error "Unknown command: $Command"
        Show-Help
        exit 1
    }
}
