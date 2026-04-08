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
