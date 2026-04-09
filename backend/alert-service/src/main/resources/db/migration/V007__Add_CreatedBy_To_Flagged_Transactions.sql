-- Flyway Migration: Add created_by field to flagged_transactions table
-- Version: V007
-- Purpose: Track which user created each alert for proper IDOR prevention and ownership verification
-- Security: Enables enforcement of ownership-based access control in the authorization layer

-- Add created_by column to flagged_transactions table
ALTER TABLE flagged_transactions
ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM';

-- Create index on created_by for efficient user-scoped queries
CREATE INDEX idx_created_by ON flagged_transactions(created_by);

-- Create composite index for user-scoped risk level queries
CREATE INDEX idx_created_by_risk_level ON flagged_transactions(created_by, risk_level);

-- Create composite index for user-scoped reviewed status queries
CREATE INDEX idx_created_by_reviewed ON flagged_transactions(created_by, reviewed);

-- Comments
COMMENT ON COLUMN flagged_transactions.created_by IS 'Username of the user who created this alert. Used for ownership verification and access control.';
