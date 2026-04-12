-- Production Database Optimization for 10K+ Concurrent Users
-- PostgreSQL 15 Performance Tuning

-- ============================================================================
-- CRITICAL INDEXES - Must have for performance
-- ============================================================================

-- Alert table indexes (most queried)
CREATE INDEX CONCURRENTLY idx_alert_user_id ON alert_entity(user_id) WHERE status != 'RESOLVED';
CREATE INDEX CONCURRENTLY idx_alert_transaction_id ON alert_entity(transaction_id);
CREATE INDEX CONCURRENTLY idx_alert_created_at ON alert_entity(created_at DESC) WHERE status = 'NEW';
CREATE INDEX CONCURRENTLY idx_alert_status_created ON alert_entity(status, created_at DESC);
CREATE INDEX CONCURRENTLY idx_alert_risk_level ON alert_entity(risk_level) WHERE risk_level IN ('HIGH', 'CRITICAL');

-- Audit log indexes (high volume)
CREATE INDEX CONCURRENTLY idx_audit_alert_id ON audit_log(alert_id);
CREATE INDEX CONCURRENTLY idx_audit_user_id ON audit_log(user_id);
CREATE INDEX CONCURRENTLY idx_audit_timestamp ON audit_log(timestamp DESC);
CREATE INDEX CONCURRENTLY idx_audit_action_timestamp ON audit_log(action, timestamp DESC);

-- User/Account indexes
CREATE INDEX CONCURRENTLY idx_user_email ON app_user(email) WHERE active = true;
CREATE INDEX CONCURRENTLY idx_user_active ON app_user(active, created_at DESC);

-- Permissions/Roles
CREATE INDEX CONCURRENTLY idx_user_role_role_id ON user_role(role_id);
CREATE INDEX CONCURRENTLY idx_permission_name ON permission(name);

-- ============================================================================
-- COMPOSITE INDEXES - For common query patterns
-- ============================================================================

-- Common alert queries
CREATE INDEX CONCURRENTLY idx_alert_user_status ON alert_entity(user_id, status, created_at DESC);
CREATE INDEX CONCURRENTLY idx_alert_user_risk_created ON alert_entity(user_id, risk_level, created_at DESC);

-- DLQ/Failed message queries
CREATE INDEX CONCURRENTLY idx_dlq_message_status ON dlq_message(status) WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX CONCURRENTLY idx_dlq_message_created ON dlq_message(created_at DESC);

-- ============================================================================
-- PARTIAL INDEXES - For filtered queries (smaller, faster)
-- ============================================================================

-- Only active/recent data
CREATE INDEX CONCURRENTLY idx_alert_active ON alert_entity(user_id, created_at DESC) 
  WHERE status IN ('NEW', 'REVIEWED');

CREATE INDEX CONCURRENTLY idx_user_active_recent ON app_user(id, created_at DESC) 
  WHERE active = true;

-- ============================================================================
-- QUERY STATISTICS - Update for optimizer
-- ============================================================================

VACUUM ANALYZE;
ANALYZE alert_entity;
ANALYZE audit_log;
ANALYZE app_user;
ANALYZE dlq_message;
ANALYZE user_role;
ANALYZE permission;

-- ============================================================================
-- TABLE OPTIMIZATION
-- ============================================================================

-- Set table statistics
ALTER TABLE alert_entity SET (autovacuum_vacuum_scale_factor = 0.01, autovacuum_analyze_scale_factor = 0.005);
ALTER TABLE audit_log SET (autovacuum_vacuum_scale_factor = 0.01, autovacuum_analyze_scale_factor = 0.005);

-- Enable parallel queries
ALTER TABLE alert_entity SET (parallel_workers = 4);
ALTER TABLE audit_log SET (parallel_workers = 4);

-- ============================================================================
-- CONNECTION POOLING HINTS
-- ============================================================================

-- For PostgreSQL configuration (postgresql.conf):
-- shared_buffers = 4GB              (25% of system RAM)
-- effective_cache_size = 12GB       (75% of system RAM)
-- work_mem = 20MB                   (shared_buffers / max_connections * 2)
-- maintenance_work_mem = 1GB
-- random_page_cost = 1.1            (for SSD)
-- max_connections = 200             (from HikariCP settings)
-- max_parallel_workers = 8
-- max_parallel_workers_per_gather = 4
-- max_worker_processes = 8

-- ============================================================================
-- REPLICATION & BACKUP SETTINGS (if using replicas)
-- ============================================================================

-- Enable WAL streaming for high availability
-- SET wal_level = replica;
-- SET max_wal_senders = 10;
-- SET wal_keep_size = '1GB';

-- ============================================================================
-- MONITORING QUERIES
-- ============================================================================

-- Check index bloat (run weekly)
-- SELECT schemaname, tablename, indexname, 
--        round(100.0 * (pg_relation_size(indexrelid) - 
--        pg_relation_size(relfilenode)) / pg_relation_size(indexrelid), 2) AS bloat_ratio
-- FROM pg_stat_user_indexes
-- WHERE pg_relation_size(indexrelid) > 1000000
-- ORDER BY bloat_ratio DESC;

-- Check missing indexes
-- SELECT schemaname, tablename, attname
-- FROM pg_stat_user_tables t
-- JOIN pg_attribute a ON t.relid = a.attrelid
-- WHERE seq_scan > idx_scan AND seq_scan > 1000;

-- Check slow queries
-- SELECT query, calls, total_time, mean_time
-- FROM pg_stat_statements
-- WHERE mean_time > 100
-- ORDER BY total_time DESC
-- LIMIT 10;

-- ============================================================================
-- MAINTENANCE SCRIPTS (Run via cron jobs)
-- ============================================================================

-- Weekly: VACUUM and ANALYZE
-- 0 2 * * 0 psql -U postgres -d risk_monitoring_db -c "VACUUM ANALYZE;"

-- Monthly: REINDEX fragmented indexes
-- 0 3 1 * * psql -U postgres -d risk_monitoring_db -c "REINDEX TABLE alert_entity; REINDEX TABLE audit_log;"
