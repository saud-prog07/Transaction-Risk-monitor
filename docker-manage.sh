#!/bin/bash
# Docker Management Script for Risk Monitoring System
# Supports: Linux and macOS

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="risk-monitoring-system"
COMPOSE_FILE="docker-compose.yml"
ENV_FILE=".env"
ENV_EXAMPLE=".env.example"

# Functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi
    print_success "Docker $(docker --version | awk '{print $3}')"
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed"
        exit 1
    fi
    print_success "Docker Compose $(docker-compose --version | awk '{print $3}')"
    
    # Check if Docker daemon is running
    if ! docker ping &> /dev/null 2>&1; then
        print_error "Docker daemon is not running"
        exit 1
    fi
    print_success "Docker daemon is running"
}

setup_env() {
    print_header "Setting Up Environment"
    
    if [ ! -f "$ENV_FILE" ]; then
        if [ -f "$ENV_EXAMPLE" ]; then
            cp "$ENV_EXAMPLE" "$ENV_FILE"
            print_success "Created $ENV_FILE from $ENV_EXAMPLE"
            print_warning "Update $ENV_FILE with your configuration"
        else
            print_error "$ENV_EXAMPLE not found"
            exit 1
        fi
    else
        print_success "$ENV_FILE already exists"
    fi
}

build() {
    print_header "Building Docker Images"
    
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build $1
    
    if [ -z "$1" ]; then
        print_success "All images built successfully"
    else
        print_success "Image '$1' built successfully"
    fi
}

up() {
    print_header "Starting Services"
    
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d $1
    
    if [ -z "$1" ]; then
        print_success "All services started"
        echo ""
        print_header "Service Status"
        docker-compose -f "$COMPOSE_FILE" ps
        echo ""
        print_header "Access Points"
        echo -e "${GREEN}Producer Service:${NC} http://localhost:8080"
        echo -e "${GREEN}Risk Engine:${NC} http://localhost:8081"
        echo -e "${GREEN}Alert Service:${NC} http://localhost:8082"
        echo -e "${GREEN}PostgreSQL:${NC} localhost:5432 (user: riskmonitor)"
        echo -e "${GREEN}IBM MQ Console:${NC} https://localhost:9443/ibmmq/console"
    else
        print_success "Service '$1' started"
    fi
}

down() {
    print_header "Stopping Services"
    
    if [ "$1" == "--volumes" ]; then
        docker-compose -f "$COMPOSE_FILE" down -v
        print_warning "All services and volumes have been removed"
    else
        docker-compose -f "$COMPOSE_FILE" down
        print_success "All services stopped (data preserved)"
    fi
}

status() {
    print_header "Service Status"
    docker-compose -f "$COMPOSE_FILE" ps
}

logs() {
    SERVICE=$1
    if [ -z "$SERVICE" ]; then
        print_header "Showing Logs for All Services"
        docker-compose -f "$COMPOSE_FILE" logs -f --tail=100
    else
        print_header "Showing Logs for $SERVICE"
        docker-compose -f "$COMPOSE_FILE" logs -f --tail=100 "$SERVICE"
    fi
}

health_check() {
    print_header "Health Check"
    
    # Check Producer Service
    if curl -s http://localhost:8080/api/actuator/health | grep -q "UP"; then
        print_success "Producer Service (8080): Healthy"
    else
        print_error "Producer Service (8080): Unhealthy"
    fi
    
    # Check Risk Engine
    if curl -s http://localhost:8081/api/actuator/health | grep -q "UP"; then
        print_success "Risk Engine (8081): Healthy"
    else
        print_error "Risk Engine (8081): Unhealthy"
    fi
    
    # Check Alert Service
    if curl -s http://localhost:8082/api/actuator/health | grep -q "UP"; then
        print_success "Alert Service (8082): Healthy"
    else
        print_error "Alert Service (8082): Unhealthy"
    fi
    
    # Check PostgreSQL
    if docker exec risk-monitoring-postgres pg_isready -U riskmonitor &> /dev/null; then
        print_success "PostgreSQL (5432): Healthy"
    else
        print_error "PostgreSQL (5432): Unhealthy"
    fi
    
    # Check IBM MQ
    if docker exec risk-monitoring-mq runmqsc -w 5 -e QMGR &> /dev/null; then
        print_success "IBM MQ (1414): Healthy"
    else
        print_error "IBM MQ (1414): Unhealthy"
    fi
}

test_transactions() {
    print_header "Testing Transaction Submission"
    
    for i in {1..3}; do
        echo "Submitting transaction $i..."
        curl -X POST http://localhost:8080/api/transaction \
            -H "Content-Type: application/json" \
            -d "{
                \"userId\": \"user$i\",
                \"amount\": $(( 1000 + RANDOM % 5000 )).00,
                \"location\": \"NYC, NY\"
            }" \
            -s | jq '.'
        echo ""
    done
    
    print_success "Transaction tests completed"
}

cleanup() {
    print_header "Cleaning Up"
    
    read -p "Remove all containers and volumes? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose -f "$COMPOSE_FILE" down -v
        print_success "All containers and volumes removed"
    else
        print_warning "Cleanup cancelled"
    fi
}

restart() {
    print_header "Restarting Services"
    
    if [ -z "$1" ]; then
        docker-compose -f "$COMPOSE_FILE" restart
        print_success "All services restarted"
    else
        docker-compose -f "$COMPOSE_FILE" restart "$1"
        print_success "Service '$1' restarted"
    fi
}

# Main menu
show_help() {
    cat << EOF
${BLUE}Risk Monitoring System - Docker Management${NC}

${YELLOW}Usage:${NC}
    $0 <command> [options]

${YELLOW}Commands:${NC}
    check           Check prerequisites (Docker, Docker Compose)
    setup           Setup environment files
    build [service] Build Docker images (all or specific service)
    up [service]    Start services (all or specific service)
    down [--volumes] Stop services (--volumes to delete data)
    restart [svc]   Restart services
    ps              Show service status
    logs [service]  View service logs
    health          Check health of all services
    test            Test transaction submission
    clean           Remove all containers and volumes
    help            Show this help message

${YELLOW}Examples:${NC}
    # Full setup and start
    $0 check
    $0 setup
    $0 build
    $0 up

    # Start specific service
    $0 up alert-service

    # View logs
    $0 logs producer-service

    # Health check all services
    $0 health

    # Stop everything
    $0 down
EOF
}

# Parse commands
case "${1:-help}" in
    check)
        check_prerequisites
        ;;
    setup)
        setup_env
        ;;
    build)
        check_prerequisites
        build $2
        ;;
    up)
        check_prerequisites
        up $2
        ;;
    down)
        down $2
        ;;
    ps|status)
        status
        ;;
    logs)
        logs $2
        ;;
    health)
        health_check
        ;;
    test)
        test_transactions
        ;;
    restart)
        restart $2
        ;;
    clean)
        cleanup
        ;;
    help)
        show_help
        ;;
    *)
        echo "Unknown command: $1"
        show_help
        exit 1
        ;;
esac
