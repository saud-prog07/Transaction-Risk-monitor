import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/AlertsPanel.css';

/**
 * AlertsPanel Component
 * Displays HIGH-risk alerts with review functionality
 * - Color-coded risk levels (HIGH, MEDIUM, LOW)
 * - Mark alerts as reviewed
 * - Shows alert details (transaction, user, amount, reason)
 * - Paginated display
 */
const AlertsPanel = ({ alerts, loading }) => {
  const navigate = useNavigate();
  const [reviewedAlerts, setReviewedAlerts] = useState(new Set());
  const [expandedAlert, setExpandedAlert] = useState(null);
  const [filter, setFilter] = useState('ALL'); // ALL, HIGH, MEDIUM, LOW, UNREVIEWED

  // Filter alerts based on risk level and review status
  const filteredAlerts = alerts.filter((alert) => {
    if (filter === 'UNREVIEWED') {
      return !reviewedAlerts.has(alert.id);
    }
    if (filter !== 'ALL') {
      return alert.riskLevel === filter;
    }
    return true;
  });

  // Mark alert as reviewed
  const handleReviewAlert = useCallback(
    (alertId) => {
      setReviewedAlerts((prev) => {
        const updated = new Set(prev);
        updated.add(alertId);
        return updated;
      });
    },
    []
  );

  // Toggle expanded view
  const toggleExpanded = useCallback((alertId) => {
    setExpandedAlert((prev) => (prev === alertId ? null : alertId));
  }, []);

  // View alert details in investigation page
  const handleViewDetails = useCallback((alertId) => {
    navigate(`/alerts/${alertId}`, { state: { fromAlerts: true } });
  }, [navigate]);

  // Count unreviewed alerts
  const unreviewed = alerts.filter((a) => !reviewedAlerts.has(a.id)).length;
  const highRiskCount = alerts.filter((a) => a.riskLevel === 'HIGH').length;

  return (
    <div className="alerts-panel-container">
      <div className="alerts-header">
        <h2>⚠️ Risk Alerts</h2>
        <div className="alerts-badges">
          <span className="badge high">
            <strong>{highRiskCount}</strong> HIGH
          </span>
          <span className="badge unreviewed">
            <strong>{unreviewed}</strong> Unreviewed
          </span>
        </div>
      </div>

      {/* Filter Buttons */}
      <div className="alerts-filter">
        <button
          className={`filter-btn ${filter === 'ALL' ? 'active' : ''}`}
          onClick={() => setFilter('ALL')}
        >
          All ({alerts.length})
        </button>
        <button
          className={`filter-btn high ${filter === 'HIGH' ? 'active' : ''}`}
          onClick={() => setFilter('HIGH')}
        >
          High ({highRiskCount})
        </button>
        <button
          className={`filter-btn medium ${filter === 'MEDIUM' ? 'active' : ''}`}
          onClick={() => setFilter('MEDIUM')}
        >
          Medium ({alerts.filter((a) => a.riskLevel === 'MEDIUM').length})
        </button>
        <button
          className={`filter-btn unreviewed ${filter === 'UNREVIEWED' ? 'active' : ''}`}
          onClick={() => setFilter('UNREVIEWED')}
        >
          Unreviewed ({unreviewed})
        </button>
      </div>

      {loading && <div className="alerts-loading">Loading alerts...</div>}

      {/* Alerts List */}
      <div className="alerts-list">
        {filteredAlerts.length === 0 ? (
          <div className="no-alerts">
            <p>
              {filter === 'UNREVIEWED'
                ? '✓ All alerts reviewed!'
                : 'No alerts at this time'}
            </p>
          </div>
        ) : (
          filteredAlerts.map((alert, idx) => (
            <div
              key={`${alert.id}-${idx}`}
              className={`alert-card ${alert.riskLevel.toLowerCase()} ${
                reviewedAlerts.has(alert.id) ? 'reviewed' : ''
              } ${expandedAlert === alert.id ? 'expanded' : ''}`}
              onClick={() => toggleExpanded(alert.id)}
            >
              {/* Alert Header */}
              <div className="alert-header-row">
                {/* Risk Badge */}
                <div className="alert-risk">
                  <span
                    className={`risk-indicator ${alert.riskLevel.toLowerCase()}`}
                  >
                    {alert.riskLevel === 'HIGH' ? '🔴' : alert.riskLevel === 'MEDIUM' ? '🟠' : '🟡'}
                  </span>
                  <span className="risk-level">{alert.riskLevel}</span>
                </div>

                {/* Alert Summary */}
                <div className="alert-summary">
                  <div className="alert-title">{alert.title}</div>
                  <div className="alert-timestamp">
                    {alert.timestamp
                      ? new Date(alert.timestamp).toLocaleTimeString()
                      : 'N/A'}
                  </div>
                </div>

                {/* Review Status */}
                <div className="alert-status">
                  {reviewedAlerts.has(alert.id) ? (
                    <span className="reviewed-badge">✓ Reviewed</span>
                  ) : (
                    <span className="pending-badge">⏳ Pending</span>
                  )}
                </div>
              </div>

              {/* Alert Details (Expanded) */}
              {expandedAlert === alert.id && (
                <div className="alert-details">
                  {alert.userId && (
                    <div className="detail-row">
                      <span className="detail-label">User ID:</span>
                      <span className="detail-value">{alert.userId}</span>
                    </div>
                  )}
                  {alert.amount && (
                    <div className="detail-row">
                      <span className="detail-label">Amount:</span>
                      <span className="detail-value">${alert.amount}</span>
                    </div>
                  )}
                  {alert.location && (
                    <div className="detail-row">
                      <span className="detail-label">Location:</span>
                      <span className="detail-value">📍 {alert.location}</span>
                    </div>
                  )}
                  {alert.reason && (
                    <div className="detail-row">
                      <span className="detail-label">Reason:</span>
                      <span className="detail-value">{alert.reason}</span>
                    </div>
                  )}
                  {alert.description && (
                    <div className="detail-row">
                      <span className="detail-label">Description:</span>
                      <p className="detail-value-text">{alert.description}</p>
                    </div>
                  )}

                  {/* Action Buttons */}
                  <div className="alert-actions">
                    <button
                      className="action-btn primary"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleViewDetails(alert.id);
                      }}
                    >
                      🔍 Investigate
                    </button>
                    {!reviewedAlerts.has(alert.id) && (
                      <button
                        className="action-btn secondary"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleReviewAlert(alert.id);
                        }}
                      >
                        Mark as Reviewed
                      </button>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* Panel Footer */}
      <div className="alerts-footer">
        <div className="footer-info">
          {filteredAlerts.length > 0 && (
            <span>Showing {filteredAlerts.length} alert(s)</span>
          )}
        </div>
      </div>
    </div>
  );
};

export default AlertsPanel;
