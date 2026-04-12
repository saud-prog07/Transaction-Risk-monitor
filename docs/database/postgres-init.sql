-- PostgreSQL Initialization Script for Risk Monitoring System
-- This script is automatically executed by Docker when the PostgreSQL container starts

-- Create schemas
CREATE SCHEMA IF NOT EXISTS public;

-- Create Alert table
CREATE TABLE IF NOT EXISTS alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    risk_level VARCHAR(50) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    risk_score DECIMAL(5,2) NOT NULL,
    location VARCHAR(255),
    alert_message TEXT,
    alert_status VARCHAR(50) NOT NULL DEFAULT 'NEW' CHECK (alert_status IN ('NEW', 'REVIEWED', 'RESOLVED', 'FALSE_POSITIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(255),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(255),
    notes TEXT
);

-- Create indexes on frequently queried columns
CREATE INDEX IF NOT EXISTS idx_alerts_user_id ON alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_alerts_risk_level ON alerts(risk_level);
CREATE INDEX IF NOT EXISTS idx_alerts_alert_status ON alerts(alert_status);
CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON alerts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_transaction_id ON alerts(transaction_id);

-- Create Alert History table for audit trail
CREATE TABLE IF NOT EXISTS alert_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id UUID NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    changed_by VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT
);

-- Create indexes on alert_history
CREATE INDEX IF NOT EXISTS idx_alert_history_alert_id ON alert_history(alert_id);
CREATE INDEX IF NOT EXISTS idx_alert_history_changed_at ON alert_history(changed_at DESC);

-- Create Transaction Cache table for idempotency
CREATE TABLE IF NOT EXISTS transaction_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    location VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    cached_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP + INTERVAL '24 hours'
);

-- Create index on transaction_cache
CREATE INDEX IF NOT EXISTS idx_transaction_cache_transaction_id ON transaction_cache(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_cache_user_id ON transaction_cache(user_id);
CREATE INDEX IF NOT EXISTS idx_transaction_cache_expires_at ON transaction_cache(expires_at);

-- Create Metrics table for monitoring
CREATE TABLE IF NOT EXISTS transaction_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_name VARCHAR(255) NOT NULL,
    metric_value BIGINT NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    service_name VARCHAR(255)
);

-- Create index on metrics
CREATE INDEX IF NOT EXISTS idx_transaction_metrics_metric_name ON transaction_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_transaction_metrics_service_name ON transaction_metrics(service_name);
CREATE INDEX IF NOT EXISTS idx_transaction_metrics_recorded_at ON transaction_metrics(recorded_at DESC);

-- Create comments for documentation
COMMENT ON TABLE alerts IS 'Stores risk alerts generated for transactions';
COMMENT ON TABLE alert_history IS 'Audit trail for all changes to alerts';
COMMENT ON TABLE transaction_cache IS 'Cache for idempotency - prevents duplicate processing';
COMMENT ON TABLE transaction_metrics IS 'Monitoring metrics for transaction processing';

-- ==================================================================================
-- AUTHENTICATION & USER MANAGEMENT TABLES
-- ==================================================================================

-- Create Users table for authentication
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    verification_token VARCHAR(255),
    verification_token_expiry TIMESTAMP,
    reset_password_token VARCHAR(255),
    reset_password_token_expiry TIMESTAMP,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    last_failed_login TIMESTAMP,
    account_locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_email CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Create indexes on users table
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
CREATE INDEX IF NOT EXISTS idx_users_verification_token ON users(verification_token);
CREATE INDEX IF NOT EXISTS idx_users_reset_password_token ON users(reset_password_token);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);

-- Create User Roles table for authorization
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Create index on user_roles
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

-- Create Login Audit Log table
CREATE TABLE IF NOT EXISTS login_audit_log (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    failure_reason VARCHAR(255)
);

-- Create indexes on login_audit_log
CREATE INDEX IF NOT EXISTS idx_login_audit_username ON login_audit_log(username);
CREATE INDEX IF NOT EXISTS idx_login_audit_login_time ON login_audit_log(login_time DESC);
CREATE INDEX IF NOT EXISTS idx_login_audit_success ON login_audit_log(success);

-- Create Password Change Audit table
CREATE TABLE IF NOT EXISTS password_change_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_type VARCHAR(50) NOT NULL CHECK (change_type IN ('PASSWORD_CHANGE', 'PASSWORD_RESET', 'REGISTRATION')),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500)
);

-- Create indexes on password_change_audit
CREATE INDEX IF NOT EXISTS idx_pwd_audit_user_id ON password_change_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_pwd_audit_changed_at ON password_change_audit(changed_at DESC);

COMMENT ON TABLE users IS 'Application users with authentication credentials';
COMMENT ON TABLE user_roles IS 'User roles for authorization (USER, ADMIN, ANALYST)';
COMMENT ON TABLE login_audit_log IS 'Audit trail for all login attempts (success and failure)';
COMMENT ON TABLE password_change_audit IS 'Audit trail for password changes and resets';

-- Create Dead Letter Queue table for failed message handling
CREATE TABLE IF NOT EXISTS dead_letter_messages (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(100),
    original_message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RETRYING', 'RESOLVED', 'DEAD')),
    error_message TEXT,
    error_stacktrace TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_retry_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolution_notes TEXT
);

-- Create indexes on dead_letter_messages for performance
CREATE INDEX IF NOT EXISTS idx_dlq_status ON dead_letter_messages(status);
CREATE INDEX IF NOT EXISTS idx_dlq_created_at ON dead_letter_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dlq_status_created ON dead_letter_messages(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dlq_transaction_id ON dead_letter_messages(transaction_id);

COMMENT ON TABLE dead_letter_messages IS 'Dead Letter Queue - stores failed messages for retry processing';

-- Grant permissions to application user
DO $$
DECLARE
    app_user VARCHAR(255) := 'riskmonitor';
    app_password VARCHAR(255) := 'secure_password';
    database_name VARCHAR(255) := 'risk_monitoring_db';
BEGIN
    -- Create user if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_user WHERE usename = app_user) THEN
        EXECUTE format('CREATE USER %I WITH PASSWORD %L', app_user, app_password);
    END IF;
    
    -- Grant permissions
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I', database_name, app_user);
    EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', app_user);
    EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I', app_user);
    EXECUTE format('GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO %I', app_user);
END $$;

-- Print initialization complete message
SELECT 'Risk Monitoring Database schema initialized successfully' AS message;
