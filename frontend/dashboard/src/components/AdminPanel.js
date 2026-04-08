import React, { useState, useEffect } from 'react';
import { fetchConfiguration, updateConfiguration, getDefaultConfiguration } from '../services/configService';
import '../styles/AdminPanel.css';

/**
 * Admin Configuration Panel Component
 * Allows authorized users to configure system settings
 */
const AdminPanel = () => {
  const [config, setConfig] = useState(getDefaultConfiguration());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [hasChanges, setHasChanges] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Load configuration on mount
  useEffect(() => {
    loadConfiguration();
  }, []);

  /**
   * Load current configuration from API
   */
  const loadConfiguration = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchConfiguration();
      setConfig(data);
      setHasChanges(false);
    } catch (err) {
      console.error('Failed to load configuration:', err);
      setError(err.message || 'Failed to load configuration');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle form field change
   */
  const handleChange = (field, value) => {
    setConfig((prev) => ({
      ...prev,
      [field]: value,
    }));
    setHasChanges(true);
    setError(null);
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!hasChanges) {
      setSuccess('No changes to save');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      setSuccess(null);

      await updateConfiguration(config);

      setSuccess('Configuration updated successfully');
      setHasChanges(false);
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      console.error('Failed to update configuration:', err);
      setError(err.message || 'Failed to update configuration');
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * Reset form to last saved state
   */
  const handleReset = () => {
    loadConfiguration();
  };

  if (loading) {
    return (
      <div className="admin-panel">
        <div className="admin-loading">
          Loading configuration...
        </div>
      </div>
    );
  }

  return (
    <div className="admin-panel">
      <div className="admin-header">
        <h1 className="admin-title">⚙️ System Configuration</h1>
        <p className="admin-subtitle">
          Manage risk detection thresholds and system rules
        </p>
      </div>

      {/* Messages */}
      {error && (
        <div className="admin-message admin-error">
          <span className="message-icon">❌</span>
          <span className="message-text">{error}</span>
        </div>
      )}

      {success && (
        <div className="admin-message admin-success">
          <span className="message-icon">✅</span>
          <span className="message-text">{success}</span>
        </div>
      )}

      <form className="admin-form" onSubmit={handleSubmit}>
        {/* Risk Detection Section */}
        <div className="admin-section">
          <h2 className="section-title">🎯 Risk Detection Thresholds</h2>
          <p className="section-description">
            Configure risk score thresholds for transaction classification
          </p>

          <div className="form-group">
            <label htmlFor="highRiskThreshold" className="form-label">
              High Risk Threshold
              <span className="help-text">(Risk score ≥ this value is HIGH)</span>
            </label>
            <div className="input-wrapper">
              <input
                id="highRiskThreshold"
                type="number"
                step="0.1"
                min="0"
                max="100"
                className="form-input"
                value={config.highRiskThreshold}
                onChange={(e) => handleChange('highRiskThreshold', parseFloat(e.target.value))}
                disabled={submitting}
              />
              <span className="input-unit">%</span>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="mediumRiskThreshold" className="form-label">
              Medium Risk Threshold
              <span className="help-text">(Risk score ≥ this value is MEDIUM)</span>
            </label>
            <div className="input-wrapper">
              <input
                id="mediumRiskThreshold"
                type="number"
                step="0.1"
                min="0"
                max="100"
                className="form-input"
                value={config.mediumRiskThreshold}
                onChange={(e) => handleChange('mediumRiskThreshold', parseFloat(e.target.value))}
                disabled={submitting}
              />
              <span className="input-unit">%</span>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="lowRiskThreshold" className="form-label">
              Low Risk Threshold
              <span className="help-text">(Risk score ≥ this value is LOW)</span>
            </label>
            <div className="input-wrapper">
              <input
                id="lowRiskThreshold"
                type="number"
                step="0.1"
                min="0"
                max="100"
                className="form-input"
                value={config.lowRiskThreshold}
                onChange={(e) => handleChange('lowRiskThreshold', parseFloat(e.target.value))}
                disabled={submitting}
              />
              <span className="input-unit">%</span>
            </div>
          </div>
        </div>

        {/* Anomaly Detection Section */}
        <div className="admin-section">
          <h2 className="section-title">📊 Anomaly Detection</h2>
          <p className="section-description">
            Configure anomaly detection parameters
          </p>

          <div className="form-group">
            <label htmlFor="anomalyMultiplier" className="form-label">
              Anomaly Multiplier
              <span className="help-text">(Multiplier for anomaly risk adjustment)</span>
            </label>
            <div className="input-wrapper">
              <input
                id="anomalyMultiplier"
                type="number"
                step="0.1"
                min="0.5"
                max="5"
                className="form-input"
                value={config.anomalyMultiplier}
                onChange={(e) => handleChange('anomalyMultiplier', parseFloat(e.target.value))}
                disabled={submitting}
              />
              <span className="input-unit">x</span>
            </div>
          </div>

          <div className="form-group checkbox-group">
            <label htmlFor="velocityCheckEnabled" className="checkbox-label">
              <input
                id="velocityCheckEnabled"
                type="checkbox"
                className="checkbox-input"
                checked={config.velocityCheckEnabled}
                onChange={(e) => handleChange('velocityCheckEnabled', e.target.checked)}
                disabled={submitting}
              />
              <span className="checkbox-text">Enable Velocity Check</span>
              <span className="help-text">(Monitor rapid sequential transactions)</span>
            </label>
          </div>

          {config.velocityCheckEnabled && (
            <div className="form-group">
              <label htmlFor="velocityThreshold" className="form-label">
                Velocity Threshold
                <span className="help-text">(Maximum transactions per minute)</span>
              </label>
              <div className="input-wrapper">
                <input
                  id="velocityThreshold"
                  type="number"
                  step="1"
                  min="1"
                  max="100"
                  className="form-input"
                  value={config.velocityThreshold}
                  onChange={(e) => handleChange('velocityThreshold', parseInt(e.target.value))}
                  disabled={submitting}
                />
                <span className="input-unit">tx/min</span>
              </div>
            </div>
          )}
        </div>

        {/* Rule Configuration Section */}
        <div className="admin-section">
          <h2 className="section-title">⚡ Detection Rules</h2>
          <p className="section-description">
            Enable or disable specific detection rules
          </p>

          <div className="form-group checkbox-group">
            <label htmlFor="geolocationCheckEnabled" className="checkbox-label">
              <input
                id="geolocationCheckEnabled"
                type="checkbox"
                className="checkbox-input"
                checked={config.geolocationCheckEnabled}
                onChange={(e) => handleChange('geolocationCheckEnabled', e.target.checked)}
                disabled={submitting}
              />
              <span className="checkbox-text">Enable Geolocation Check</span>
              <span className="help-text">(Detect impossible travel scenarios)</span>
            </label>
          </div>

          <div className="form-group checkbox-group">
            <label htmlFor="amountSpikeCheckEnabled" className="checkbox-label">
              <input
                id="amountSpikeCheckEnabled"
                type="checkbox"
                className="checkbox-input"
                checked={config.amountSpikeCheckEnabled}
                onChange={(e) => handleChange('amountSpikeCheckEnabled', e.target.checked)}
                disabled={submitting}
              />
              <span className="checkbox-text">Enable Amount Spike Detection</span>
              <span className="help-text">(Detect unusual transaction amounts)</span>
            </label>
          </div>

          {config.amountSpikeCheckEnabled && (
            <div className="form-group">
              <label htmlFor="amountSpikeMultiplier" className="form-label">
                Amount Spike Multiplier
                <span className="help-text">(Threshold multiplier for spike detection)</span>
              </label>
              <div className="input-wrapper">
                <input
                  id="amountSpikeMultiplier"
                  type="number"
                  step="0.1"
                  min="1"
                  max="10"
                  className="form-input"
                  value={config.amountSpikeMultiplier}
                  onChange={(e) => handleChange('amountSpikeMultiplier', parseFloat(e.target.value))}
                  disabled={submitting}
                />
                <span className="input-unit">x</span>
              </div>
            </div>
          )}
        </div>

        {/* System Settings Section */}
        <div className="admin-section">
          <h2 className="section-title">🔒 System Settings</h2>
          <p className="section-description">
            Configure system-wide behavior
          </p>

          <div className="form-group checkbox-group">
            <label htmlFor="enforceMfaForHighRisk" className="checkbox-label">
              <input
                id="enforceMfaForHighRisk"
                type="checkbox"
                className="checkbox-input"
                checked={config.enforceMfaForHighRisk}
                onChange={(e) => handleChange('enforceMfaForHighRisk', e.target.checked)}
                disabled={submitting}
              />
              <span className="checkbox-text">Enforce MFA for High Risk</span>
              <span className="help-text">(Require MFA for high-risk transactions)</span>
            </label>
          </div>

          <div className="form-group checkbox-group">
            <label htmlFor="autoEscalationEnabled" className="checkbox-label">
              <input
                id="autoEscalationEnabled"
                type="checkbox"
                className="checkbox-input"
                checked={config.autoEscalationEnabled}
                onChange={(e) => handleChange('autoEscalationEnabled', e.target.checked)}
                disabled={submitting}
              />
              <span className="checkbox-text">Enable Auto-Escalation</span>
              <span className="help-text">(Automatically escalate critical alerts)</span>
            </label>
          </div>
        </div>

        {/* Form Actions */}
        <div className="admin-actions">
          <button
            type="submit"
            className="btn btn--primary"
            disabled={!hasChanges || submitting}
            title={hasChanges ? 'Save configuration changes' : 'No changes to save'}
          >
            {submitting ? 'Saving...' : '💾 Save Configuration'}
          </button>
          <button
            type="button"
            className="btn btn--secondary"
            onClick={handleReset}
            disabled={submitting || !hasChanges}
            title="Discard changes and reload from server"
          >
            🔄 Reset
          </button>
        </div>
      </form>

      {/* Info Section */}
      <div className="admin-info">
        <h3>ℹ️ Configuration Notes</h3>
        <ul>
          <li>Risk thresholds must be in ascending order (Low &lt; Medium &lt; High)</li>
          <li>Multipliers control sensitivity of anomaly detection (higher = more sensitive)</li>
          <li>Velocity threshold limits rapid transaction sequences</li>
          <li>Changes take effect immediately for new transactions</li>
          <li>All changes are logged with timestamp and user information</li>
        </ul>
      </div>
    </div>
  );
};

export default AdminPanel;
