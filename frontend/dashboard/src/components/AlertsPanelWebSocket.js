import React, { useEffect, useState } from 'react';
import { useWebSocketContext } from '../contexts/WebSocketContext';
import '../styles/AlertsPanel.css';

/**
 * AlertsPanelWebSocket Component
 * Example of how to update AlertsPanel to use WebSocket for real-time alerts
 * This replaces the polling-based implementation with WebSocket push notifications
 * 
 * Features:
 * - Real-time alert updates via WebSocket
 * - Alert severity filtering (Critical, Warning, Info)
 * - Connection status indicator
 * - Integration with existing CSS styles
 * - Alert dismissal
 * - Statistics display
 * 
 * MIGRATION STEPS:
 * 1. Import useWebSocketContext instead of using props
 * 2. Remove useEffect that polls /api/alerts
 * 3. Use alerts from WebSocket context
 * 4. Update JSX to display real-time data
 * 5. Replace this file name in imports from AlertsPanel.js to AlertsPanelWebSocket.js
 * 6. Or merge this logic into the existing AlertsPanel.js
 */
function AlertsPanelWebSocket() {
  // Get alerts and WebSocket connection status from context
  const {
    isConnected,
    isConnecting,
    error,
    alerts,
    getLatestAlerts,
    getAlertsBySeverity,
    subscribeToAlerts,
    clearAlerts,
    wsStats
  } = useWebSocketContext();

  // Local state
  const [selectedSeverity, setSelectedSeverity] = useState(null); // null = all
  const [dismissedAlertIds, setDismissedAlertIds] = useState(new Set());
  const [reviewedAlerts, setReviewedAlerts] = useState(new Set());
  const [expandedAlert, setExpandedAlert] = useState(null);

  /**
   * Subscribe to WebSocket alerts when component mounts
   * IMPORTANT: This replaces the polling mechanism
   */
  useEffect(() => {
    if (isConnected) {
      console.log('AlertsPanel: Subscribing to alerts');
      subscribeToAlerts();
    }
  }, [isConnected, subscribeToAlerts]);

  /**
   * Get filtered alerts based on selected severity
   */
  const getFilteredAlerts = () => {
    let filtered = selectedSeverity 
      ? getAlertsBySeverity(selectedSeverity)
      : getLatestAlerts(20); // Show latest 20 alerts

    // Filter out dismissed alerts
    return filtered.filter(alert => !dismissedAlertIds.has(alert.id));
  };

  /**
   * Mark alert as reviewed
   */
  const handleReviewAlert = (alertId) => {
    setReviewedAlerts(prev => {
      const updated = new Set(prev);
      updated.add(alertId);
      return updated;
    });
  };

  /**
   * Dismiss alert from view
   */
  const handleDismissAlert = (alertId) => {
    setDismissedAlertIds(prev => new Set([...prev, alertId]));
  };

  /**
   * Toggle expanded alert details
   */
  const toggleExpanded = (alertId) => {
    setExpandedAlert(prev => (prev === alertId ? null : alertId));
  };

  /**
   * Handle clearing all alerts
   */
  const handleClearAllAlerts = () => {
    clearAlerts();
    setDismissedAlertIds(new Set());
    setReviewedAlerts(new Set());
  };

  /**
   * Get severity statistics
   */
  const getCriticalCount = () => getAlertsBySeverity('CRITICAL').length;
  const getWarningCount = () => getAlertsBySeverity('WARNING').length;
  const getInfoCount = () => getAlertsBySeverity('INFO').length;

  const filteredAlerts = getFilteredAlerts();
  const loading = isConnecting;

  return (
    <div className="alerts-panel">
      {/* Header Section */}
      <div className="alerts-header">
        <div className="header-left">
          <h2>Real-Time Alerts (WebSocket)</h2>
          <span className={`connection-status ${isConnected ? 'connected' : 'disconnected'}`}>
            {isConnecting ? (
              <span className="connecting">⏳ Connecting...</span>
            ) : isConnected ? (
              <span className="connected">🟢 Connected</span>
            ) : (
              <span className="disconnected">🔴 Disconnected</span>
            )}
          </span>
        </div>
        <div className="header-right">
          <button 
            className="btn btn-secondary" 
            onClick={handleClearAllAlerts}
            disabled={loading}
          >
            Clear All
          </button>
        </div>
      </div>

      {/* Loading State */}
      {loading && <div className="loading">Connecting to WebSocket...</div>}

      {/* Error Banner */}
      {error && (
        <div className="error-banner">
          <span className="error-icon">⚠️</span>
          <span className="error-message">
            WebSocket Error: {error.message}
          </span>
        </div>
      )}

      {!isConnected && !isConnecting && (
        <div className="warning-banner">
          <span className="warning-icon">⚠️</span>
          <span className="warning-message">
            WebSocket is disconnected. You will not receive real-time updates.
          </span>
        </div>
      )}

      {/* Statistics Summary */}
      <div className="alerts-statistics">
        <div className="stat-card critical">
          <span className="stat-label">Critical</span>
          <span className="stat-value">{getCriticalCount()}</span>
        </div>
        <div className="stat-card warning">
          <span className="stat-label">Warning</span>
          <span className="stat-value">{getWarningCount()}</span>
        </div>
        <div className="stat-card info">
          <span className="stat-label">Info</span>
          <span className="stat-value">{getInfoCount()}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Total</span>
          <span className="stat-value">{alerts.length}</span>
        </div>
      </div>

      {/* Server Statistics */}
      {wsStats && isConnected && (
        <div className="server-stats">
          <div className="stat-item">
            <span className="label">Active Sessions:</span>
            <span className="value">{wsStats.activeSessions}</span>
          </div>
          <div className="stat-item">
            <span className="label">Alert Subscribers:</span>
            <span className="value">{wsStats.alertSubscribers}</span>
          </div>
        </div>
      )}

      {/* Filter Section */}
      <div className="alerts-filters">
        <span className="filter-label">Filter:</span>
        <div className="filter-buttons">
          <button
            className={`filter-btn ${selectedSeverity === null ? 'active' : ''}`}
            onClick={() => setSelectedSeverity(null)}
          >
            All ({alerts.length})
          </button>
          <button
            className={`filter-btn critical ${selectedSeverity === 'CRITICAL' ? 'active' : ''}`}
            onClick={() => setSelectedSeverity('CRITICAL')}
          >
            Critical ({getCriticalCount()})
          </button>
          <button
            className={`filter-btn warning ${selectedSeverity === 'WARNING' ? 'active' : ''}`}
            onClick={() => setSelectedSeverity('WARNING')}
          >
            Warning ({getWarningCount()})
          </button>
          <button
            className={`filter-btn info ${selectedSeverity === 'INFO' ? 'active' : ''}`}
            onClick={() => setSelectedSeverity('INFO')}
          >
            Info ({getInfoCount()})
          </button>
        </div>
      </div>

      {/* Alerts List */}
      <div className="alerts-list">
        {filteredAlerts.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">📭</span>
            <p>No alerts to display</p>
            {!isConnected && (
              <p className="empty-hint">
                Waiting for WebSocket connection...
              </p>
            )}
          </div>
        ) : (
          filteredAlerts.map(alert => {
            const isExpanded = expandedAlert === alert.id;
            const isReviewed = reviewedAlerts.has(alert.id);
            const isDismissed = dismissedAlertIds.has(alert.id);

            if (isDismissed) return null;

            return (
              <div
                key={alert.id}
                className={`alert-item alert-${alert.severity?.toLowerCase() || 'info'} ${isReviewed ? 'reviewed' : ''}`}
              >
                <div className="alert-row">
                  <div className="alert-left">
                    <div className="alert-checkbox">
                      <input
                        type="checkbox"
                        checked={isReviewed}
                        onChange={() => handleReviewAlert(alert.id)}
                      />
                    </div>
                    <div className="alert-severity-badge">
                      {alert.severity === 'CRITICAL' && '🚨'}
                      {alert.severity === 'WARNING' && '⚠️'}
                      {alert.severity === 'INFO' && 'ℹ️'}
                    </div>
                  </div>

                  <div className="alert-content" onClick={() => toggleExpanded(alert.id)}>
                    <div className="alert-top-row">
                      <h4 className="alert-title">
                        {alert.message || 'Alert'}
                      </h4>
                      <span className="alert-timestamp">
                        {alert.timestamp 
                          ? new Date(alert.timestamp).toLocaleTimeString()
                          : 'Just now'
                        }
                      </span>
                    </div>

                    {alert.description && !isExpanded && (
                      <p className="alert-description">
                        {alert.description.substring(0, 150)}...
                      </p>
                    )}

                    {isExpanded && alert.description && (
                      <p className="alert-description expanded">
                        {alert.description}
                      </p>
                    )}

                    {isExpanded && alert.sourceService && (
                      <div className="alert-meta">
                        <span className="meta-item">
                          <strong>Source:</strong> {alert.sourceService}
                        </span>
                        {alert.additionalData && (
                          <div className="additional-data">
                            {Object.entries(alert.additionalData).map(([key, value]) => (
                              <span key={key} className="data-item">
                                <strong>{key}:</strong> {JSON.stringify(value)}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="alert-right">
                    <button
                      className="btn-dismiss"
                      onClick={() => handleDismissAlert(alert.id)}
                      title="Dismiss"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Footer */}
      <div className="alerts-footer">
        <span className="footer-info">
          {filteredAlerts.length} of {alerts.length} alerts shown
        </span>
        <span className="footer-status">
          {isConnected 
            ? '📡 Real-time (WebSocket)'
            : '⚠️ Offline'
          }
        </span>
      </div>
    </div>
  );
}

export default AlertsPanelWebSocket;
