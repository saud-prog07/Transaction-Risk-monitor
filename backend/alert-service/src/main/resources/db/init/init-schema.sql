-- Alert Service Database Initialization Script
-- PostgreSQL database setup for flagged_transactions table

-- Create extension for UUID support if not exists
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create flagged_transactions table
CREATE TABLE IF NOT EXISTS flagged_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    risk_level VARCHAR(10) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    investigation_notes VARCHAR(2000),
    
    CONSTRAINT fk_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH'))
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_transaction_id ON flagged_transactions(transaction_id);
CREATE INDEX IF NOT EXISTS idx_risk_level ON flagged_transactions(risk_level);
CREATE INDEX IF NOT EXISTS idx_created_at ON flagged_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_risk_level_created ON flagged_transactions(risk_level, created_at);
CREATE INDEX IF NOT EXISTS idx_reviewed ON flagged_transactions(reviewed);
CREATE INDEX IF NOT EXISTS idx_created_at_desc ON flagged_transactions(created_at DESC);

-- Create view for unreviewed alerts
CREATE OR REPLACE VIEW v_unreviewed_alerts AS
SELECT * FROM flagged_transactions
WHERE reviewed = FALSE
ORDER BY created_at DESC;

-- Create view for high risk alerts
CREATE OR REPLACE VIEW v_high_risk_alerts AS
SELECT * FROM flagged_transactions
WHERE risk_level = 'HIGH'
ORDER BY created_at DESC;

-- Create view for recent alerts (last 7 days)
CREATE OR REPLACE VIEW v_recent_alerts AS
SELECT * FROM flagged_transactions
WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '7 days'
ORDER BY created_at DESC;

-- Grant permissions (adjust as needed for your security model)
-- GRANT SELECT, INSERT, UPDATE ON flagged_transactions TO alert_service_user;
-- GRANT USAGE, SELECT ON flagged_transactions_id_seq TO alert_service_user;

-- Add comments for documentation
COMMENT ON TABLE flagged_transactions IS 'Stores flagged high-risk transactions requiring investigation and review';
COMMENT ON COLUMN flagged_transactions.id IS 'Primary key, auto-generated';
COMMENT ON COLUMN flagged_transactions.transaction_id IS 'Reference to the original transaction, must be unique';
COMMENT ON COLUMN flagged_transactions.risk_level IS 'Risk classification: LOW, MEDIUM, or HIGH';
COMMENT ON COLUMN flagged_transactions.reason IS 'Detailed reason for the risk flag';
COMMENT ON COLUMN flagged_transactions.created_at IS 'Timestamp when alert was created, immutable';
COMMENT ON COLUMN flagged_transactions.updated_at IS 'Timestamp of last update';
COMMENT ON COLUMN flagged_transactions.reviewed IS 'Flag indicating if alert has been reviewed';
COMMENT ON COLUMN flagged_transactions.investigation_notes IS 'Notes from investigation, if any';
