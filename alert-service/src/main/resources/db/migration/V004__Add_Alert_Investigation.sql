-- Flyway Migration: Add Alert Investigation Workflow
-- Version: V004
-- Date: 2024
-- Description: Adds audit logging and investigation status tracking to alert system

-- ============================================================================
-- 1. Alter flagged_transactions table to add investigation fields
-- ============================================================================

ALTER TABLE flagged_transactions 
ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'NEW',
ADD COLUMN investigated_at TIMESTAMP NULL,
ADD COLUMN investigated_by VARCHAR(255) NULL,
ADD INDEX idx_status (status);

-- Add comment for clarity
ALTER TABLE flagged_transactions 
MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'NEW' COMMENT 'Investigation status: NEW, REVIEWED, FRAUD, SAFE';

-- ============================================================================
-- 2. Create alert_audit_logs table
-- ============================================================================

CREATE TABLE alert_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    
    -- Foreign keys and relationships
    alert_id BIGINT NOT NULL,
    
    -- Audit action metadata
    action_type VARCHAR(255) NOT NULL COMMENT 'Type of action: CREATED, STATUS_CHANGED, REVIEWED, NOTES_ADDED',
    
    -- Status transition tracking
    previous_status VARCHAR(50) NOT NULL COMMENT 'Status before action',
    new_status VARCHAR(50) NOT NULL COMMENT 'Status after action',
    
    -- Detailed information
    description VARCHAR(500) COMMENT 'Description of action',
    notes VARCHAR(2000) COMMENT 'Investigation notes or findings',
    
    -- Actor information
    performed_by VARCHAR(255) NOT NULL COMMENT 'User ID or system identifier performing the action',
    
    -- Temporal information
    action_timestamp TIMESTAMP NOT NULL COMMENT 'When the action occurred',
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Additional context
    additional_metadata VARCHAR(2000) COMMENT 'JSON metadata for additional context',
    
    -- Indexes for query performance
    CONSTRAINT fk_alert_audit_log_transaction 
        FOREIGN KEY (alert_id) 
        REFERENCES flagged_transactions(id) 
        ON DELETE CASCADE,
    
    INDEX idx_alert_id (alert_id),
    INDEX idx_action_timestamp (action_timestamp),
    INDEX idx_alert_action (alert_id, action_type),
    INDEX idx_performed_by (performed_by),
    INDEX idx_action_type (action_type),
    INDEX idx_status_transition (previous_status, new_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit log for tracking all alert investigation actions';

-- ============================================================================
-- 3. Populate initial audit logs for existing alerts (data migration)
-- ============================================================================

-- Create CREATED audit entries for any existing alerts that don't have audit logs
INSERT INTO alert_audit_logs 
(alert_id, action_type, previous_status, new_status, description, performed_by, action_timestamp, created_timestamp)
SELECT 
    ft.id,
    'CREATED' AS action_type,
    'NEW' AS previous_status,
    'NEW' AS new_status,
    'Alert created by system' AS description,
    'SYSTEM' AS performed_by,
    ft.created_timestamp AS action_timestamp,
    ft.created_timestamp AS created_timestamp
FROM flagged_transactions ft
WHERE NOT EXISTS (
    SELECT 1 FROM alert_audit_logs aal WHERE aal.alert_id = ft.id
);

-- ============================================================================
-- 4. Create view for audit trail summary
-- ============================================================================

CREATE OR REPLACE VIEW v_alert_audit_summary AS
SELECT 
    ft.id AS alert_id,
    ft.transaction_id,
    ft.status,
    ft.risk_level,
    COUNT(aal.id) AS audit_entry_count,
    MAX(aal.action_timestamp) AS last_action_timestamp,
    ft.investigated_by AS current_investigator,
    ft.investigated_at AS investigation_completed_at
FROM flagged_transactions ft
LEFT JOIN alert_audit_logs aal ON ft.id = aal.alert_id
GROUP BY ft.id, ft.transaction_id, ft.status, ft.risk_level, 
         ft.investigated_by, ft.investigated_at;

-- ============================================================================
-- 5. Create stored procedure for audit log cleanup (optional, for maintenance)
-- ============================================================================

DELIMITER $$

CREATE PROCEDURE cleanup_old_audit_logs(IN days_to_keep INT)
BEGIN
    DELETE FROM alert_audit_logs
    WHERE action_timestamp < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
END $$

DELIMITER ;

-- ============================================================================
-- 6. Create index for performance optimization
-- ============================================================================

-- Index for finding investigations by investigator
CREATE INDEX idx_investigated_by ON flagged_transactions(investigated_by);

-- Index for finding recently investigated alerts
CREATE INDEX idx_investigated_at ON flagged_transactions(investigated_at);

-- Composite index for investigation queries
CREATE INDEX idx_status_investigated_at ON flagged_transactions(status, investigated_at);

-- ============================================================================
-- 7. Verify migration success
-- ============================================================================

-- Check that columns were added
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'flagged_transactions' 
AND COLUMN_NAME IN ('status', 'investigated_at', 'investigated_by')
ORDER BY ORDINAL_POSITION;

-- Check that table was created
SELECT TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME = 'alert_audit_logs' 
AND TABLE_SCHEMA = DATABASE();

-- ============================================================================
-- Notes:
-- - All existing alerts default to status 'NEW'
-- - Existing alerts get retroactive CREATED audit log entries
-- - Indexes optimize query performance for common operations
-- - View provides quick summary of alert investigation status
-- - Stored procedure available for audit log archival
-- ============================================================================
