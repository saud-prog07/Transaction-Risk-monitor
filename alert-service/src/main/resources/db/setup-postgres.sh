#!/bin/bash
# PostgreSQL Setup Script for Alert Service
# This script sets up the database and user for the alert-service

set -e

# Configuration
DB_NAME="alert_service_db"
DB_USER="postgres"
DB_PASSWORD="postgres"
DB_HOST="localhost"
DB_PORT="5432"

echo "=========================================="
echo "Alert Service PostgreSQL Setup"
echo "=========================================="

# Check if PostgreSQL is running
echo "Checking PostgreSQL connection..."
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -tc "SELECT 1" > /dev/null 2>&1; then
    echo "Error: Cannot connect to PostgreSQL at $DB_HOST:$DB_PORT"
    echo "Please ensure PostgreSQL is running and accessible"
    exit 1
fi

echo "✓ Connected to PostgreSQL"

# Create database if it doesn't exist
echo "Creating database '$DB_NAME' if not exists..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -tc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" | grep -q 1 || \
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;"

echo "✓ Database '$DB_NAME' ready"

# Connect to the database and run initialization script
echo "Running initialization script..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$(dirname "$0")/init-schema.sql"

echo "✓ Schema initialized"

# Grant privileges (optional, uncomment if needed)
# psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;"
# psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $DB_USER;"

echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "User: $DB_USER"
echo ""
echo "You can now start the alert-service application."
