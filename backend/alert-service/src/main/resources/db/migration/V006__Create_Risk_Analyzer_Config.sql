-- Flyway Migration: Create risk analyzer configuration table
-- Version: V006 (following V005 audit trail migration)
-- Purpose: Store and manage configurable risk analyzer thresholds

CREATE TABLE IF NOT EXISTS risk_analyzer_config (
    -- Primary key and versioning
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,

    -- Analyzer identification
    analyzer_name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    description TEXT,

    -- Enable/disable toggle
    enabled BOOLEAN NOT NULL DEFAULT true,

    -- Threshold values (all analyzers)
    threshold_primary DECIMAL(10, 2),      -- Primary threshold (multiplier, count, %, etc.)
    threshold_secondary DECIMAL(10, 2),    -- Secondary threshold (minimum amount, etc.)
    threshold_tertiary DECIMAL(10, 2),     -- Tertiary threshold (reserved for future use)

    -- Time window configurations
    time_window_days INTEGER,               -- For baseline/historical analysis (days)
    time_window_minutes INTEGER,            -- For frequency analysis (minutes)

    -- Additional configuration in JSON (optional)
    additional_config TEXT,

    -- Audit trail
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    modified_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_analyzer_name_length CHECK (LENGTH(analyzer_name) > 0),
    CONSTRAINT chk_positive_thresholds CHECK (
        (threshold_primary IS NULL OR threshold_primary >= 0) AND
        (threshold_secondary IS NULL OR threshold_secondary >= 0) AND
        (threshold_tertiary IS NULL OR threshold_tertiary >= 0)
    )
);

-- Indexes for performance
CREATE INDEX idx_analyzer_name ON risk_analyzer_config(analyzer_name);
CREATE INDEX idx_enabled ON risk_analyzer_config(enabled);
CREATE INDEX idx_updated_at ON risk_analyzer_config(updated_at);

-- Table comments for documentation
COMMENT ON TABLE risk_analyzer_config IS 
    'Stores configurable thresholds and settings for risk analyzers. Changes are applied dynamically via caching.';

COMMENT ON COLUMN risk_analyzer_config.analyzer_name IS 
    'Name of analyzer: HighAmountAnalyzer, FrequencyAnalyzer, TimeAnomalyAnalyzer, LocationAnomalyAnalyzer';

COMMENT ON COLUMN risk_analyzer_config.threshold_primary IS 
    'Primary threshold: multiplier (HighAmount), count (Frequency), threshold % (TimeAnomaly), % (Location)';

COMMENT ON COLUMN risk_analyzer_config.threshold_secondary IS 
    'Secondary threshold: minimum amount (HighAmount), unused for others';

COMMENT ON COLUMN risk_analyzer_config.modified_by IS 
    'User or system that last modified this configuration (for audit trail)';

-- Initial configuration data (will be inserted by RiskAnalyzerConfigService.initializeDefaults())
INSERT INTO risk_analyzer_config 
    (analyzer_name, display_name, description, enabled, threshold_primary, threshold_secondary, time_window_days, time_window_minutes, modified_by)
VALUES
    ('HighAmountAnalyzer', 'High Amount Analyzer', 'Detects transactions significantly above user typical spending', true, 2.0, 1000.0, 30, NULL, 'MIGRATION'),
    ('FrequencyAnalyzer', 'Frequency Analyzer', 'Detects abnormally high transaction frequency', true, 5.0, NULL, NULL, 5, 'MIGRATION'),
    ('TimeAnomalyAnalyzer', 'Time Anomaly Analyzer', 'Detects transactions at unusual hours', true, 80.0, NULL, 30, NULL, 'MIGRATION'),
    ('LocationAnomalyAnalyzer', 'Location Anomaly Analyzer', 'Detects transactions from unusual locations', true, 5.0, NULL, 30, NULL, 'MIGRATION')
ON CONFLICT (analyzer_name) DO NOTHING;

-- Audit log note
INSERT INTO audit_logs 
    (alert_id, action, user_id, action_timestamp, details, ip_address, request_id)
SELECT
    NULL,
    'SYSTEM_CONFIG_CREATED',
    'SYSTEM',
    CURRENT_TIMESTAMP,
    'Risk analyzer configuration table created',
    '0.0.0.0',
    'MIGRATION_V006'
WHERE NOT EXISTS (SELECT 1 FROM audit_logs WHERE action = 'SYSTEM_CONFIG_CREATED');
