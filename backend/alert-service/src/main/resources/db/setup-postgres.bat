@echo off
REM PostgreSQL Setup Script for Alert Service (Windows)
REM This script sets up the database and user for the alert-service

setlocal enabledelayedexpansion

REM Configuration
set DB_NAME=alert_service_db
set DB_USER=postgres
set DB_PASSWORD=postgres
set DB_HOST=localhost
set DB_PORT=5432

echo.
echo ==========================================
echo Alert Service PostgreSQL Setup
echo ==========================================
echo.

REM Check if psql is available
where psql >nul 2>nul
if errorlevel 1 (
    echo Error: psql not found in PATH
    echo Please ensure PostgreSQL is installed and added to PATH
    pause
    exit /b 1
)

echo Checking PostgreSQL connection...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -tc "SELECT 1" >nul 2>nul
if errorlevel 1 (
    echo Error: Cannot connect to PostgreSQL at %DB_HOST%:%DB_PORT%
    echo Please ensure PostgreSQL is running and accessible
    pause
    exit /b 1
)

echo OK - Connected to PostgreSQL
echo.

REM Create database if it doesn't exist
echo Creating database '%DB_NAME%' if not exists...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -tc "SELECT 1 FROM pg_database WHERE datname = '%DB_NAME%'" | find "1" >nul
if errorlevel 1 (
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -c "CREATE DATABASE %DB_NAME%;"
    if errorlevel 1 (
        echo Error creating database
        pause
        exit /b 1
    )
)

echo OK - Database '%DB_NAME%' ready
echo.

REM Get the directory of this script
set SCRIPT_DIR=%~dp0

REM Run initialization script
echo Running initialization script...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%SCRIPT_DIR%init\init-schema.sql"
if errorlevel 1 (
    echo Error running initialization script
    pause
    exit /b 1
)

echo OK - Schema initialized
echo.

REM Optional: Grant privileges
REM psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO %DB_USER%;"

echo ==========================================
echo Setup Complete!
echo ==========================================
echo Database: %DB_NAME%
echo Host: %DB_HOST%:%DB_PORT%
echo User: %DB_USER%
echo.
echo You can now start the alert-service application.
echo.
pause
