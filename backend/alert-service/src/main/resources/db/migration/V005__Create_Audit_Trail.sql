-- Flyway Migration: Create audit trail system
-- Version: V005
-- Date: 2026-04-08
-- Description: Creates audit logs table and related structures for comprehensive action tracking

-- ============================================================================
-- Create audit_logs table
-- ============================================================================

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    
    -- Foreign key to alerts
    alert_id BIGINT NOT NULL,
    
    -- Action information
    action VARCHAR(50) NOT NULL COMMENT 'Type of action: CREATED, REVIEWED, UPDATED, etc.',
    
    -- User information
    user_id VARCHAR(255) NOT NULL COMMENT 'The user who performed the action',
    
    -- Timing
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the action occurred',
    
    -- Details
    details VARCHAR(2000) COMMENT 'Detailed information about the action',
    
    -- Status tracking (for status changes)
    previous_status VARCHAR(50) COMMENT 'Status before the action',
    new_status VARCHAR(50) COMMENT 'Status after the action',
    
    -- Request context
    ip_address VARCHAR(45) COMMENT 'IP address that made the request',
    request_id VARCHAR(100) COMMENT 'Correlation ID for request tracing',
    
    -- Constraints
    CONSTRAINT fk_audit_alert
        FOREIGN KEY (alert_id)
        REFERENCES alerts(id)
        ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_alert_id (alert_id),
    INDEX idx_action_timestamp (action_timestamp),
    INDEX idx_user_id (user_id),
    INDEX idx_action_type (action),
    INDEX idx_alert_action (alert_id, action),
    INDEX idx_timestamp (timestamp),
    INDEX idx_request_id (request_id)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Immutable audit log for tracking all actions performed on alerts';

-- ============================================================================
-- Create view for audit summary
-- ============================================================================

CREATE OR REPLACE VIEW v_audit_summary AS
SELECT 
    a.id,
    a.alert_id,
    a.action,
    a.user_id,
    a.timestamp,
    a.previous_status,
    a.new_status,
    COUNT(*) OVER (PARTITION BY a.alert_id) AS alert_action_count,
    ROW_NUMBER() OVER (PARTITION BY a.alert_id ORDER BY a.timestamp DESC) AS action_sequence
FROM audit_logs a;

-- ============================================================================
-- Create stored procedure for audit cleanup (optional)
-- ============================================================================

DELIMITER $$

CREATE PROCEDURE cleanup_audit_logs(IN days_to_keep INT)
BEGIN
    DECLARE deleted_count INT;
    
    DELETE FROM audit_logs
    WHERE timestamp < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
    
    SET deleted_count = ROW_COUNT();
    
    INSERT INTO audit_logs (alert_id, action, user_id, details)
    SELECT NULL, 'CLEANUP', 'SYSTEM', 
           CONCAT('Deleted ', deleted_count, ' old audit records')
    WHERE deleted_count > 0;
END $$

DELIMITER ;

-- ============================================================================
-- Create stored procedure for audit statistics
-- ============================================================================

DELIMITER $$

CREATE PROCEDURE get_audit_statistics(IN days INT)
BEGIN
    SELECT 
        action,
        COUNT(*) as total_actions,
        COUNT(DISTINCT user_id) as unique_users,
        COUNT(DISTINCT alert_id) as affected_alerts,
        MIN(timestamp) as first_action,
        MAX(timestamp) as last_action
    FROM audit_logs
    WHERE timestamp >= DATE_SUB(NOW(), INTERVAL days DAY)
    GROUP BY action
    ORDER BY total_actions DESC;
END $$

DELIMITER ;

-- ============================================================================
-- Create trigger for automatic audit logging (optional, can be added later)
-- ============================================================================

-- Note: Triggers would need to be added to the alerts table to automatically
-- create audit logs when alert status changes. Example:
--
-- CREATE TRIGGER tr_alert_status_change
-- AFTER UPDATE ON alerts
-- FOR EACH ROW
-- BEGIN
--     IF OLD.status != NEW.status THEN
--         INSERT INTO audit_logs (alert_id, action, user_id, previous_status, new_status, timestamp)
--         VALUES (NEW.id, 'STATUS_CHANGED', 'SYSTEM', OLD.status, NEW.status, NOW());
--     END IF;
-- END;

-- ============================================================================
-- Verify table creation
-- ============================================================================

SELECT TABLE_NAME, ENGINE, TABLE_ROWS, TABLE_COLLATION
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME = 'audit_logs'
AND TABLE_SCHEMA = DATABASE();
