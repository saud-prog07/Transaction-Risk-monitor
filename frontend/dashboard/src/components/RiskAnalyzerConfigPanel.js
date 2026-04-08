import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import RoleBasedWrapper from '../RoleBasedWrapper';
import '../styles/RiskAnalyzerConfig.css';

/**
 * RiskAnalyzerConfigPanel Component
 * 
 * Admin panel for managing risk analyzer configurations
 * Features:
 * - View all risk analyzer configurations
 * - Enable/disable individual analyzers
 * - Update threshold values dynamically
 * - Reset to defaults
 * - View configuration history (modified by, updated at)
 * 
 * Configuration changes are applied immediately without restart
 */
const RiskAnalyzerConfigPanel = () => {
  const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8082';

  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [editValues, setEditValues] = useState({});

  // Load configurations on mount
  useEffect(() => {
    loadConfigurations();
  }, []);

  /**
   * Load all risk analyzer configurations from backend
   */
  const loadConfigurations = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await axios.get(
        `${API_BASE_URL}/api/risk/config`,
        { timeout: 5000 }
      );

      setConfigs(response.data);
    } catch (err) {
      console.error('Error loading configurations:', err);
      setError('Failed to load configurations: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  }, [API_BASE_URL]);

  /**
   * Toggle analyzer enabled/disabled status
   */
  const handleToggleEnabled = useCallback(async (id, analyzerName, currentEnabled) => {
    try {
      setError(null);
      setSuccess(null);

      const response = await axios.patch(
        `${API_BASE_URL}/api/risk/config/${id}/enabled`,
        { enabled: !currentEnabled },
        { timeout: 5000 }
      );

      // Update local state
      setConfigs(configs.map(config =>
        config.id === id ? response.data : config
      ));

      setSuccess(`${analyzerName} ${!currentEnabled ? 'enabled' : 'disabled'} successfully`);
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError('Error toggling analyzer: ' + (err.response?.data?.error || err.message));
    }
  }, [configs, API_BASE_URL]);

  /**
   * Start editing a configuration
   */
  const handleStartEdit = useCallback((config) => {
    setEditingId(config.id);
    setEditValues({
      thresholdPrimary: config.thresholdPrimary || '',
      thresholdSecondary: config.thresholdSecondary || '',
      timeWindowDays: config.timeWindowDays || '',
      timeWindowMinutes: config.timeWindowMinutes || '',
    });
  }, []);

  /**
   * Cancel editing
   */
  const handleCancelEdit = useCallback(() => {
    setEditingId(null);
    setEditValues({});
  }, []);

  /**
   * Save configuration changes
   */
  const handleSaveConfig = useCallback(async (id, config) => {
    try {
      setError(null);
      setSuccess(null);

      const updateData = {
        ...config,
        thresholdPrimary: editValues.thresholdPrimary ? parseFloat(editValues.thresholdPrimary) : null,
        thresholdSecondary: editValues.thresholdSecondary ? parseFloat(editValues.thresholdSecondary) : null,
        timeWindowDays: editValues.timeWindowDays ? parseInt(editValues.timeWindowDays) : null,
        timeWindowMinutes: editValues.timeWindowMinutes ? parseInt(editValues.timeWindowMinutes) : null,
      };

      const response = await axios.put(
        `${API_BASE_URL}/api/risk/config/${id}`,
        updateData,
        { timeout: 5000 }
      );

      // Update local state
      setConfigs(configs.map(c =>
        c.id === id ? response.data : c
      ));

      setEditingId(null);
      setEditValues({});
      setSuccess(`${config.analyzerName} updated successfully`);
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError('Error saving configuration: ' + (err.response?.data?.error || err.message));
    }
  }, [editValues, configs, API_BASE_URL]);

  /**
   * Reset configuration to defaults
   */
  const handleResetDefaults = useCallback(async (id, analyzerName) => {
    if (!window.confirm(`Reset ${analyzerName} to default values?`)) {
      return;
    }

    try {
      setError(null);
      setSuccess(null);

      const response = await axios.post(
        `${API_BASE_URL}/api/risk/config/${id}/reset`,
        {},
        { timeout: 5000 }
      );

      // Update local state
      setConfigs(configs.map(c =>
        c.id === id ? response.data : c
      ));

      setSuccess(`${analyzerName} reset to defaults`);
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError('Error resetting configuration: ' + (err.response?.data?.error || err.message));
    }
  }, [configs, API_BASE_URL]);

  /**
   * Format date for display
   */
  const formatDate = (dateString) => {
    try {
      return new Date(dateString).toLocaleString();
    } catch {
      return dateString;
    }
  };

  /**
   * Get analyzer display info
   */
  const getAnalyzerInfo = (analyzerName) => {
    const info = {
      'HighAmountAnalyzer': {
        icon: '💰',
        description: 'Detects transactions significantly above user typical spending'
      },
      'FrequencyAnalyzer': {
        icon: '⚡',
        description: 'Detects abnormally high transaction frequency'
      },
      'TimeAnomalyAnalyzer': {
        icon: '⏰',
        description: 'Detects transactions at unusual hours'
      },
      'LocationAnomalyAnalyzer': {
        icon: '📍',
        description: 'Detects transactions from unusual locations'
      }
    };
    return info[analyzerName] || { icon: '❓', description: 'Unknown analyzer' };
  };

  if (loading) {
    return <div className="config-loading">Loading risk analyzer configurations...</div>;
  }

  return (
    <div className="risk-analyzer-config">
      <div className="config-header">
        <h2>⚙️ Risk Analyzer Configuration</h2>
        <p className="config-subtitle">Manage and update risk analysis thresholds in real-time</p>
      </div>

      {error && (
        <div className="config-error">
          <span className="error-icon">⚠️</span>
          <span className="error-text">{error}</span>
          <button className="error-close" onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {success && (
        <div className="config-success">
          <span className="success-icon">✓</span>
          <span className="success-text">{success}</span>
        </div>
      )}

      <div className="config-grid">
        {configs.map((config) => {
          const isEditing = editingId === config.id;
          const info = getAnalyzerInfo(config.analyzerName);

          return (
            <div key={config.id} className="config-card">
              <div className="config-card-header">
                <div className="analyzer-title">
                  <span className="analyzer-icon">{info.icon}</span>
                  <div className="analyzer-name-block">
                    <h3>{config.displayName || config.analyzerName}</h3>
                    <code className="analyzer-code">{config.analyzerName}</code>
                  </div>
                </div>
                <div className="analyzer-status">
                  <label className="status-toggle">
                    <input
                      type="checkbox"
                      checked={config.enabled}
                      onChange={() => handleToggleEnabled(config.id, config.analyzerName, config.enabled)}
                      disabled={isEditing}
                    />
                    <span className={`toggle-slider ${config.enabled ? 'enabled' : 'disabled'}`}></span>
                    <span className="toggle-label">{config.enabled ? 'Enabled' : 'Disabled'}</span>
                  </label>
                </div>
              </div>

              <p className="analyzer-description">{info.description}</p>

              {isEditing ? (
                <div className="config-edit-form">
                  {config.thresholdPrimary !== null && (
                    <div className="form-group">
                      <label>Primary Threshold</label>
                      <input
                        type="number"
                        step="0.01"
                        value={editValues.thresholdPrimary}
                        onChange={(e) => setEditValues({ ...editValues, thresholdPrimary: e.target.value })}
                        placeholder="Enter value"
                      />
                      <small>Current: {config.thresholdPrimary}</small>
                    </div>
                  )}

                  {config.thresholdSecondary !== null && (
                    <div className="form-group">
                      <label>Secondary Threshold</label>
                      <input
                        type="number"
                        step="0.01"
                        value={editValues.thresholdSecondary}
                        onChange={(e) => setEditValues({ ...editValues, thresholdSecondary: e.target.value })}
                        placeholder="Enter value"
                      />
                      <small>Current: {config.thresholdSecondary}</small>
                    </div>
                  )}

                  {config.timeWindowDays !== null && (
                    <div className="form-group">
                      <label>Time Window (Days)</label>
                      <input
                        type="number"
                        value={editValues.timeWindowDays}
                        onChange={(e) => setEditValues({ ...editValues, timeWindowDays: e.target.value })}
                        placeholder="Enter days"
                      />
                      <small>Current: {config.timeWindowDays} days</small>
                    </div>
                  )}

                  {config.timeWindowMinutes !== null && (
                    <div className="form-group">
                      <label>Time Window (Minutes)</label>
                      <input
                        type="number"
                        value={editValues.timeWindowMinutes}
                        onChange={(e) => setEditValues({ ...editValues, timeWindowMinutes: e.target.value })}
                        placeholder="Enter minutes"
                      />
                      <small>Current: {config.timeWindowMinutes} minutes</small>
                    </div>
                  )}

                  <div className="form-actions">
                    <button
                      className="btn btn-save"
                      onClick={() => handleSaveConfig(config.id, config)}
                    >
                      💾 Save
                    </button>
                    <button
                      className="btn btn-cancel"
                      onClick={handleCancelEdit}
                    >
                      ✕ Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <div className="config-details">
                  <div className="details-grid">
                    {config.thresholdPrimary !== null && (
                      <div className="detail-row">
                        <span className="detail-label">Primary Threshold</span>
                        <span className="detail-value">{config.thresholdPrimary}</span>
                      </div>
                    )}

                    {config.thresholdSecondary !== null && (
                      <div className="detail-row">
                        <span className="detail-label">Secondary Threshold</span>
                        <span className="detail-value">${config.thresholdSecondary}</span>
                      </div>
                    )}

                    {config.timeWindowDays !== null && (
                      <div className="detail-row">
                        <span className="detail-label">Time Window</span>
                        <span className="detail-value">{config.timeWindowDays} days</span>
                      </div>
                    )}

                    {config.timeWindowMinutes !== null && (
                      <div className="detail-row">
                        <span className="detail-label">Time Window</span>
                        <span className="detail-value">{config.timeWindowMinutes} minutes</span>
                      </div>
                    )}
                  </div>

                  <div className="config-audit">
                    <small>Last modified: {formatDate(config.updatedAt)} by {config.modifiedBy}</small>
                  </div>

                  <div className="config-actions">
                    <button
                      className="btn btn-edit"
                      onClick={() => handleStartEdit(config)}
                    >
                      ✏️ Edit
                    </button>
                    <button
                      className="btn btn-reset"
                      onClick={() => handleResetDefaults(config.id, config.analyzerName)}
                    >
                      🔄 Reset
                    </button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div className="config-info">
        <h4>ℹ️ Configuration Management</h4>
        <ul>
          <li><strong>Enable/Disable:</strong> Toggle analyzers on/off without restarting</li>
          <li><strong>Update Thresholds:</strong> Change detection thresholds in real-time</li>
          <li><strong>Time Windows:</strong> Adjust historical analysis windows</li>
          <li><strong>Reset Defaults:</strong> Restore original configuration values</li>
          <li><strong>Audit Trail:</strong> Track who modified each configuration and when</li>
        </ul>
      </div>
    </div>
  );
};

export default RiskAnalyzerConfigPanel;
