/**
 * Configuration Management System - Full Example
 * 
 * This file demonstrates complete usage of the risk analyzer
 * configuration management system with all features integrated.
 */

import React, { useState, useCallback } from 'react';
import { useRiskConfig, useRiskConfigForm } from '../hooks';
import {
  RiskAnalyzerConfigPanel,
  RiskThresholdConfig,
  RiskAnalyzerMetricsPanel,
} from '../components/configuration';
import { configUtils, riskLevels, exportUtils } from '../utils';
import '../styles/RiskAnalyzerConfig.css';
import '../styles/RiskThresholdConfig.css';

/**
 * ConfigurationDashboard Component
 * 
 * Complete example demonstrating:
 * - Loading and displaying configurations
 * - Updating analyzers and thresholds
 * - Real-time metrics monitoring
 * - Form validation
 * - Error handling
 * - Configuration export/import
 */
export default function ConfigurationDashboard() {
  // ============================================
  // State Management
  // ============================================

  const {
    analyzers,
    thresholds,
    metrics,
    loading,
    error,
    lastUpdated,
    updateAnalyzer,
    batchUpdateAnalyzers,
    resetAnalyzer,
    updateThresholds,
    fetchMetrics,
    fetchAll,
    exportConfig,
    importConfig,
    validateConfig,
    clearError,
  } = useRiskConfig({
    autoFetch: true,
    refreshInterval: 30000, // Auto-refresh every 30 seconds
  });

  // UI state
  const [selectedAnalyzerId, setSelectedAnalyzerId] = useState(null);
  const [showExportDialog, setShowExportDialog] = useState(false);
  const [showImportDialog, setShowImportDialog] = useState(false);
  const [successMessage, setSuccessMessage] = useState(null);

  // ============================================
  // Callback Handlers
  // ============================================

  /**
   * Handle single analyzer update
   */
  const handleAnalyzerUpdate = useCallback(
    async (analyzerId, config) => {
      try {
        await updateAnalyzer(analyzerId, config);
        showSuccess('Analyzer configuration updated successfully');
      } catch (err) {
        console.error('Update error:', err);
      }
    },
    [updateAnalyzer]
  );

  /**
   * Handle batch analyzer update
   */
  const handleBatchUpdate = useCallback(
    async (updates) => {
      try {
        await batchUpdateAnalyzers(updates);
        showSuccess('Multiple analyzers updated successfully');
      } catch (err) {
        console.error('Batch update error:', err);
      }
    },
    [batchUpdateAnalyzers]
  );

  /**
   * Handle analyzer reset
   */
  const handleResetAnalyzer = useCallback(
    async (analyzerId) => {
      try {
        const confirmed = window.confirm(
          'Are you sure you want to reset this analyzer to default configuration?'
        );
        if (confirmed) {
          await resetAnalyzer(analyzerId);
          showSuccess('Analyzer reset to default configuration');
        }
      } catch (err) {
        console.error('Reset error:', err);
      }
    },
    [resetAnalyzer]
  );

  /**
   * Handle threshold update
   */
  const handleThresholdUpdate = useCallback(
    async (newThresholds) => {
      try {
        await updateThresholds(newThresholds);
        showSuccess('Risk thresholds updated successfully');
      } catch (err) {
        console.error('Threshold update error:', err);
      }
    },
    [updateThresholds]
  );

  /**
   * Handle configuration export
   */
  const handleExportConfig = useCallback(async () => {
    try {
      await exportConfig();
      showSuccess('Configuration exported successfully');
      setShowExportDialog(false);
    } catch (err) {
      console.error('Export error:', err);
    }
  }, [exportConfig]);

  /**
   * Handle configuration import
   */
  const handleImportConfig = useCallback(
    async (file) => {
      try {
        await importConfig(file);
        showSuccess('Configuration imported successfully');
        setShowImportDialog(false);
      } catch (err) {
        console.error('Import error:', err);
      }
    },
    [importConfig]
  );

  /**
   * Handle configuration validation
   */
  const handleValidateConfig = useCallback(async () => {
    try {
      const currentConfig = {
        analyzers,
        thresholds,
      };

      const valid = await validateConfig(currentConfig);
      if (valid.isValid) {
        showSuccess('Configuration is valid');
      } else {
        alert('Configuration validation errors:\n' + valid.errors.join('\n'));
      }
    } catch (err) {
      console.error('Validation error:', err);
    }
  }, [analyzers, thresholds, validateConfig]);

  /**
   * Show success message
   */
  const showSuccess = (message) => {
    setSuccessMessage(message);
    setTimeout(() => setSuccessMessage(null), 3000);
  };

  /**
   * Handle analyzer selection
   */
  const handleSelectAnalyzer = useCallback((analyzerId) => {
    setSelectedAnalyzerId(analyzerId);
  }, []);

  /**
   * Get selected analyzer
   */
  const selectedAnalyzer = selectedAnalyzerId
    ? configUtils.getAnalyzerById(analyzers, selectedAnalyzerId)
    : null;

  // ============================================
  // Render
  // ============================================

  return (
    <div className="configuration-dashboard">
      {/* Header */}
      <div className="dashboard-header">
        <h1>🎯 Risk Configuration Management</h1>
        <p className="subtitle">
          Manage risk analyzers, thresholds, and monitoring metrics
        </p>
        {lastUpdated && (
          <p className="last-updated">
            Last updated: {new Date(lastUpdated).toLocaleTimeString()}
          </p>
        )}
      </div>

      {/* Error Banner */}
      {error && (
        <div className="error-banner">
          <div className="error-content">
            <span className="error-icon">⚠️</span>
            <p>{error}</p>
          </div>
          <button
            className="error-close"
            onClick={clearError}
            title="Dismiss"
          >
            ✕
          </button>
        </div>
      )}

      {/* Success Message */}
      {successMessage && (
        <div className="success-banner">
          <span className="success-icon">✓</span>
          <p>{successMessage}</p>
        </div>
      )}

      {/* Actions Bar */}
      <div className="actions-bar">
        <div className="action-group">
          <button
            className="btn btn-primary"
            onClick={fetchAll}
            disabled={loading}
          >
            {loading ? '⟳ Refreshing...' : '🔄 Refresh All'}
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => setShowExportDialog(true)}
            disabled={loading}
          >
            📥 Export Configuration
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => setShowImportDialog(true)}
            disabled={loading}
          >
            📤 Import Configuration
          </button>
          <button
            className="btn btn-secondary"
            onClick={handleValidateConfig}
            disabled={loading}
          >
            ✓ Validate Configuration
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="dashboard-content">
        {loading && analyzers.length === 0 ? (
          <div className="loading-state">
            <div className="spinner">⟳</div>
            <p>Loading configuration...</p>
          </div>
        ) : (
          <>
            {/* Analyzer Configuration Panel */}
            <section className="section">
              <h2>📊 Analyzer Configuration</h2>
              <RiskAnalyzerConfigPanel
                analyzers={analyzers}
                thresholds={thresholds}
                metrics={metrics}
                isLoading={loading}
                error={error}
                onUpdate={handleAnalyzerUpdate}
                onRefresh={fetchMetrics}
              />
            </section>

            {/* Threshold Configuration Panel */}
            <section className="section">
              <h2>🎚️ Risk Thresholds</h2>
              <RiskThresholdConfig
                thresholds={thresholds}
                onSave={handleThresholdUpdate}
              />
            </section>

            {/* Metrics Panel */}
            <section className="section">
              <h2>📈 Analyzer Metrics</h2>
              <RiskAnalyzerMetricsPanel
                analyzers={analyzers}
                metrics={metrics}
                isLoading={loading}
                error={error}
                onRefresh={fetchMetrics}
              />
            </section>

            {/* Configuration Summary */}
            <section className="section">
              <h2>📋 Configuration Summary</h2>
              <div className="summary-grid">
                <div className="summary-card">
                  <h3>Total Analyzers</h3>
                  <p className="summary-value">{analyzers.length}</p>
                  <p className="summary-detail">
                    {configUtils.filterEnabledAnalyzers(analyzers).length}{' '}
                    enabled
                  </p>
                </div>

                <div className="summary-card">
                  <h3>Total Weight</h3>
                  <p className="summary-value">
                    {configUtils
                      .calculateTotalWeight(analyzers)
                      .toFixed(2)}
                  </p>
                  <p className="summary-detail">
                    {Math.abs(
                      configUtils.calculateTotalWeight(analyzers) - 1
                    ) < 0.01
                      ? '✓ Normalized'
                      : '⚠️ Not normalized'}
                  </p>
                </div>

                <div className="summary-card">
                  <h3>Risk Thresholds</h3>
                  <p className="summary-value">{thresholds.length}</p>
                  <p className="summary-detail">Threshold sets</p>
                </div>

                <div className="summary-card">
                  <h3>Overall Health</h3>
                  <p className="summary-value">
                    {analyzers.filter((a) => a.enabled).length === 0
                      ? 'N/A'
                      : '✓'}
                  </p>
                  <p className="summary-detail">System status</p>
                </div>
              </div>
            </section>
          </>
        )}
      </div>

      {/* Export Dialog */}
      {showExportDialog && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>Export Configuration</h3>
            <p>
              Export your current risk analyzer configuration to a JSON file for
              backup or sharing.
            </p>
            <div className="modal-actions">
              <button
                className="btn btn-primary"
                onClick={handleExportConfig}
              >
                Download JSON
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => setShowExportDialog(false)}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Import Dialog */}
      {showImportDialog && (
        <ImportDialog
          onImport={handleImportConfig}
          onCancel={() => setShowImportDialog(false)}
        />
      )}

      {/* Styles */}
      <style>{`
        .configuration-dashboard {
          max-width: 1400px;
          margin: 0 auto;
          padding: 20px;
        }

        .dashboard-header {
          text-align: center;
          margin-bottom: 30px;
        }

        .dashboard-header h1 {
          margin: 0;
          font-size: 32px;
          color: #f0f9ff;
        }

        .subtitle {
          color: #cbd5e1;
          font-size: 14px;
          margin: 5px 0 0 0;
        }

        .last-updated {
          color: #94a3b8;
          font-size: 12px;
          margin: 5px 0 0 0;
        }

        .error-banner {
          display: flex;
          justify-content: space-between;
          align-items: center;
          background: rgba(239, 68, 68, 0.1);
          border: 1px solid rgba(239, 68, 68, 0.3);
          border-radius: 8px;
          padding: 15px;
          margin-bottom: 20px;
          color: #fca5a5;
        }

        .error-content {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .error-icon {
          font-size: 18px;
        }

        .error-banner p {
          margin: 0;
          font-size: 13px;
        }

        .error-close {
          background: none;
          border: none;
          color: inherit;
          cursor: pointer;
          font-size: 18px;
        }

        .success-banner {
          display: flex;
          align-items: center;
          gap: 12px;
          background: rgba(34, 197, 94, 0.1);
          border: 1px solid rgba(34, 197, 94, 0.3);
          border-radius: 8px;
          padding: 15px;
          margin-bottom: 20px;
          color: #86efac;
          animation: slideDown 0.3s ease;
        }

        .success-icon {
          font-size: 18px;
        }

        .success-banner p {
          margin: 0;
          font-size: 13px;
        }

        .actions-bar {
          display: flex;
          gap: 20px;
          margin-bottom: 30px;
          padding: 20px;
          background: rgba(30, 41, 59, 0.8);
          border-radius: 8px;
          border: 1px solid rgba(139, 92, 246, 0.2);
        }

        .action-group {
          display: flex;
          gap: 10px;
          flex-wrap: wrap;
          flex: 1;
        }

        .btn {
          padding: 10px 16px;
          border: none;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.3s ease;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          white-space: nowrap;
        }

        .btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .btn-primary {
          background: rgba(59, 130, 246, 0.8);
          color: white;
          border: 1px solid rgba(59, 130, 246, 1);
        }

        .btn-primary:hover:not(:disabled) {
          background: rgba(59, 130, 246, 1);
        }

        .btn-secondary {
          background: rgba(100, 116, 139, 0.6);
          color: #e2e8f0;
          border: 1px solid rgba(100, 116, 139, 0.8);
        }

        .btn-secondary:hover:not(:disabled) {
          background: rgba(100, 116, 139, 0.8);
        }

        .dashboard-content {
          display: flex;
          flex-direction: column;
          gap: 30px;
        }

        .section {
          background: linear-gradient(135deg, #0f172a 0%, #1a1f3a 100%);
          border-radius: 8px;
          padding: 20px;
        }

        .section h2 {
          margin: 0 0 20px 0;
          color: #f0f9ff;
          font-size: 20px;
        }

        .summary-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 16px;
        }

        .summary-card {
          background: rgba(30, 41, 59, 0.8);
          border: 1px solid rgba(139, 92, 246, 0.2);
          border-radius: 6px;
          padding: 16px;
          text-align: center;
        }

        .summary-card h3 {
          margin: 0 0 8px 0;
          color: #cbd5e1;
          font-size: 12px;
          text-transform: uppercase;
        }

        .summary-value {
          margin: 0 0 4px 0;
          color: #f0f9ff;
          font-size: 24px;
          font-weight: 700;
        }

        .summary-detail {
          margin: 0;
          color: #94a3b8;
          font-size: 12px;
        }

        .loading-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 60px;
          text-align: center;
        }

        .spinner {
          font-size: 40px;
          animation: spin 1s linear infinite;
          margin-bottom: 16px;
        }

        @keyframes spin {
          from {
            transform: rotate(0deg);
          }
          to {
            transform: rotate(360deg);
          }
        }

        .modal-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.7);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }

        .modal {
          background: linear-gradient(135deg, #0f172a 0%, #1a1f3a 100%);
          border: 1px solid rgba(139, 92, 246, 0.2);
          border-radius: 8px;
          padding: 30px;
          max-width: 500px;
          width: 90%;
        }

        .modal h3 {
          margin: 0 0 16px 0;
          color: #f0f9ff;
          font-size: 20px;
        }

        .modal p {
          margin: 0 0 20px 0;
          color: #cbd5e1;
          font-size: 14px;
        }

        .modal-actions {
          display: flex;
          gap: 12px;
        }

        .modal-actions .btn {
          flex: 1;
        }

        @keyframes slideDown {
          from {
            opacity: 0;
            transform: translateY(-10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </div>
  );
}

/**
 * Import Dialog Component
 */
function ImportDialog({ onImport, onCancel }) {
  const [file, setFile] = useState(null);

  const handleFileSelect = (e) => {
    const selected = e.target.files?.[0];
    if (selected) {
      setFile(selected);
    }
  };

  const handleImport = async () => {
    if (file) {
      try {
        await onImport(file);
      } catch (err) {
        alert('Import failed: ' + err.message);
      }
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h3>Import Configuration</h3>
        <p>
          Select a JSON file containing your risk analyzer configuration to
          import.
        </p>
        <input
          type="file"
          accept=".json"
          onChange={handleFileSelect}
          style={{
            marginBottom: '20px',
            width: '100%',
          }}
        />
        {file && <p style={{ color: '#a78bfa' }}>Selected: {file.name}</p>}
        <div className="modal-actions">
          <button
            className="btn btn-primary"
            onClick={handleImport}
            disabled={!file}
          >
            Import
          </button>
          <button className="btn btn-secondary" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}

export { ConfigurationDashboard, ImportDialog };
