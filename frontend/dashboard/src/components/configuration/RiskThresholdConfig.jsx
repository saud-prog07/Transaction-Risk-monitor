import React, { useState, useCallback, useEffect } from 'react';
import './RiskThresholdConfig.css';

/**
 * RiskThresholdConfig Component
 * Manages risk threshold configuration with live preview and validation
 * 
 * @component
 * @param {Object} props
 * @param {Array} props.thresholds - Array of risk threshold configurations
 * @param {Function} props.onSave - Callback when thresholds are saved
 * @param {Function} props.onCancel - Callback when configuration is cancelled
 * @param {boolean} props.isLoading - Loading state indicator
 * @param {string} props.error - Error message if any
 * @returns {JSX.Element}
 */
const RiskThresholdConfig = ({
  thresholds = [],
  onSave,
  onCancel,
  isLoading = false,
  error = null,
}) => {
  const [editingThresholds, setEditingThresholds] = useState(thresholds);
  const [isEditing, setIsEditing] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});

  // Update when props change
  useEffect(() => {
    setEditingThresholds(thresholds);
  }, [thresholds]);

  /**
   * Validates threshold configuration
   */
  const validateThresholds = useCallback(() => {
    const errors = {};

    editingThresholds.forEach((threshold, index) => {
      const thresholdErrors = [];

      if (!threshold.name || threshold.name.trim() === '') {
        thresholdErrors.push('Name is required');
      }

      if (threshold.low < 0 || threshold.low > 100) {
        thresholdErrors.push('Low threshold must be 0-100');
      }

      if (threshold.medium < 0 || threshold.medium > 100) {
        thresholdErrors.push('Medium threshold must be 0-100');
      }

      if (threshold.high < 0 || threshold.high > 100) {
        thresholdErrors.push('High threshold must be 0-100');
      }

      if (threshold.critical < 0 || threshold.critical > 100) {
        thresholdErrors.push('Critical threshold must be 0-100');
      }

      // Check ordering: low < medium < high < critical
      if (threshold.low >= threshold.medium) {
        thresholdErrors.push('Low must be less than medium');
      }

      if (threshold.medium >= threshold.high) {
        thresholdErrors.push('Medium must be less than high');
      }

      if (threshold.high >= threshold.critical) {
        thresholdErrors.push('High must be less than critical');
      }

      if (thresholdErrors.length > 0) {
        errors[index] = thresholdErrors;
      }
    });

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  }, [editingThresholds]);

  /**
   * Handle threshold value change
   */
  const handleThresholdChange = useCallback((index, field, value) => {
    const newThresholds = [...editingThresholds];
    newThresholds[index] = {
      ...newThresholds[index],
      [field]: field === 'name' ? value : parseFloat(value) || 0,
    };
    setEditingThresholds(newThresholds);

    // Clear validation error for this threshold
    if (validationErrors[index]) {
      const newErrors = { ...validationErrors };
      delete newErrors[index];
      setValidationErrors(newErrors);
    }
  }, [editingThresholds, validationErrors]);

  /**
   * Add new threshold
   */
  const handleAddThreshold = useCallback(() => {
    const newThreshold = {
      name: `Threshold ${editingThresholds.length + 1}`,
      low: 20,
      medium: 40,
      high: 60,
      critical: 80,
    };
    setEditingThresholds([...editingThresholds, newThreshold]);
  }, [editingThresholds]);

  /**
   * Remove threshold
   */
  const handleRemoveThreshold = useCallback((index) => {
    const newThresholds = editingThresholds.filter((_, i) => i !== index);
    setEditingThresholds(newThresholds);

    const newErrors = { ...validationErrors };
    delete newErrors[index];
    setValidationErrors(newErrors);
  }, [editingThresholds, validationErrors]);

  /**
   * Handle save
   */
  const handleSave = useCallback(() => {
    if (validateThresholds()) {
      onSave?.(editingThresholds);
      setIsEditing(false);
    }
  }, [editingThresholds, validateThresholds, onSave]);

  /**
   * Handle cancel
   */
  const handleCancel = useCallback(() => {
    setEditingThresholds(thresholds);
    setValidationErrors({});
    setIsEditing(false);
    onCancel?.();
  }, [thresholds, onCancel]);

  /**
   * Get risk level color
   */
  const getRiskLevelColor = (value) => {
    if (value < 20) return '#10b981';
    if (value < 40) return '#f59e0b';
    if (value < 60) return '#ef4444';
    return '#8b0000';
  };

  /**
   * Get risk level label
   */
  const getRiskLevelLabel = (value) => {
    if (value < 20) return 'Low';
    if (value < 40) return 'Medium';
    if (value < 60) return 'High';
    return 'Critical';
  };

  if (isLoading) {
    return (
      <div className="threshold-config">
        <div className="config-loading">Loading thresholds...</div>
      </div>
    );
  }

  return (
    <div className="threshold-config">
      {error && (
        <div className="config-error">
          <span className="error-icon">⚠️</span>
          <span className="error-text">{error}</span>
        </div>
      )}

      <div className="threshold-header">
        <h3>Risk Threshold Configuration</h3>
        <p>Define the score ranges that determine risk levels</p>
      </div>

      {isEditing ? (
        <div className="threshold-edit-form">
          {editingThresholds.map((threshold, index) => (
            <div key={index} className="threshold-edit-item">
              <div className="threshold-name-section">
                <label>Threshold Name</label>
                <input
                  type="text"
                  value={threshold.name}
                  onChange={(e) =>
                    handleThresholdChange(index, 'name', e.target.value)
                  }
                  placeholder="e.g., Default Risk Thresholds"
                />
              </div>

              <div className="threshold-inputs-grid">
                {['low', 'medium', 'high', 'critical'].map((level) => (
                  <div key={level} className="threshold-input-group">
                    <label>
                      {level.charAt(0).toUpperCase() + level.slice(1)} Level
                    </label>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      value={threshold[level]}
                      onChange={(e) =>
                        handleThresholdChange(index, level, e.target.value)
                      }
                    />
                    <small>
                      Risk Level:{' '}
                      <span style={{ color: getRiskLevelColor(threshold[level]) }}>
                        {getRiskLevelLabel(threshold[level])}
                      </span>
                    </small>
                  </div>
                ))}
              </div>

              {validationErrors[index] && (
                <div className="threshold-errors">
                  {validationErrors[index].map((err, i) => (
                    <div key={i} className="error-item">
                      × {err}
                    </div>
                  ))}
                </div>
              )}

              {editingThresholds.length > 1 && (
                <div className="threshold-actions">
                  <button
                    className="btn btn-danger"
                    onClick={() => handleRemoveThreshold(index)}
                  >
                    Remove
                  </button>
                </div>
              )}
            </div>
          ))}

          <div className="threshold-add-section">
            <button className="btn btn-primary" onClick={handleAddThreshold}>
              + Add Threshold
            </button>
          </div>

          <div className="threshold-form-actions">
            <button className="btn btn-save" onClick={handleSave}>
              Save
            </button>
            <button className="btn btn-cancel" onClick={handleCancel}>
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <div className="threshold-display">
          {editingThresholds.map((threshold, index) => (
            <div key={index} className="threshold-card">
              <div className="threshold-card-header">
                <h4>{threshold.name}</h4>
              </div>

              <div className="threshold-visualization">
                {['low', 'medium', 'high', 'critical'].map((level) => {
                  const value = threshold[level];
                  const percentage = (value / 100) * 100;
                  return (
                    <div
                      key={level}
                      className="threshold-bar-container"
                      title={`${level}: ${value}`}
                    >
                      <div className="threshold-bar-label">
                        <span>{level}</span>
                        <span className="threshold-bar-value">{value}</span>
                      </div>
                      <div className="threshold-bar-background">
                        <div
                          className="threshold-bar-fill"
                          style={{
                            width: `${percentage}%`,
                            backgroundColor: getRiskLevelColor(value),
                          }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="threshold-grid">
                <div className="threshold-item">
                  <span className="threshold-level-label">Low</span>
                  <span className="threshold-level-value" style={{ color: getRiskLevelColor(threshold.low) }}>
                    {threshold.low}
                  </span>
                </div>
                <div className="threshold-item">
                  <span className="threshold-level-label">Medium</span>
                  <span className="threshold-level-value" style={{ color: getRiskLevelColor(threshold.medium) }}>
                    {threshold.medium}
                  </span>
                </div>
                <div className="threshold-item">
                  <span className="threshold-level-label">High</span>
                  <span className="threshold-level-value" style={{ color: getRiskLevelColor(threshold.high) }}>
                    {threshold.high}
                  </span>
                </div>
                <div className="threshold-item">
                  <span className="threshold-level-label">Critical</span>
                  <span className="threshold-level-value" style={{ color: getRiskLevelColor(threshold.critical) }}>
                    {threshold.critical}
                  </span>
                </div>
              </div>
            </div>
          ))}

          <div className="threshold-display-actions">
            <button
              className="btn btn-edit"
              onClick={() => setIsEditing(true)}
            >
              Edit Thresholds
            </button>
          </div>
        </div>
      )}

      <div className="threshold-info">
        <h4>ℹ️ Threshold Guide</h4>
        <ul>
          <li>
            <strong>Low Risk:</strong> Acceptable level of transaction risk
          </li>
          <li>
            <strong>Medium Risk:</strong> Monitor required; investigate if needed
          </li>
          <li>
            <strong>High Risk:</strong> Immediate investigation recommended
          </li>
          <li>
            <strong>Critical Risk:</strong> Block transaction and escalate
          </li>
          <li>
            Values must be ordered: Low {'<'} Medium {'<'} High {'<'} Critical
          </li>
        </ul>
      </div>
    </div>
  );
};

export default RiskThresholdConfig;
