import React, { useState } from 'react';
import RiskBadge from './RiskBadge';
import StatusBadge from './StatusBadge';
import { formatDate, formatCurrency, truncateText } from '../services/utils';
import { updateAlertStatus } from '../services/apiService';
import '../styles/AlertsTable.css';

/**
 * AlertsTable Component - Display alerts in a tabular format
 * @param {Object} props
 * @param {Array} props.alerts - Array of alert objects
 * @param {boolean} props.loading - Loading state
 * @param {string} props.error - Error message
 * @param {function} props.onRefresh - Refresh callback
 * @param {function} props.onStatusUpdate - Status update callback
 */
const AlertsTable = ({
  alerts = [],
  loading = false,
  error = null,
  onRefresh,
  onStatusUpdate,
}) => {
  const [updating, setUpdating] = useState(null);

  const handleStatusUpdate = async (alertId, newStatus) => {
    try {
      setUpdating(alertId);
      await updateAlertStatus(alertId, newStatus);
      onStatusUpdate?.(alertId, newStatus);
    } catch (err) {
      console.error('Failed to update alert status:', err);
      alert('Failed to update status. Please try again.');
    } finally {
      setUpdating(null);
    }
  };

  if (loading) {
    return (
      <div className="alerts-table__loading">
        <div className="spinner"></div>
        <p>Loading alerts...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="alerts-table__error">
        <p>⚠️ {error}</p>
        <button onClick={onRefresh} className="btn btn--primary">
          Try Again
        </button>
      </div>
    );
  }

  if (!alerts || alerts.length === 0) {
    return (
      <div className="alerts-table__empty">
        <p>No alerts found</p>
        <small>Adjust your filters or wait for new transactions</small>
      </div>
    );
  }

  return (
    <div className="alerts-table">
      <table className="alerts-table__content">
        <thead>
          <tr>
            <th>Transaction ID</th>
            <th>User ID</th>
            <th>Amount</th>
            <th>Risk Level</th>
            <th>Status</th>
            <th>Created At</th>
            <th>Reason</th>
          </tr>
        </thead>
        <tbody>
          {alerts.map((alert) => (
            <tr key={alert.id} className="alerts-table__row">
              <td className="alerts-table__cell--mono">
                {truncateText(alert.transactionId, 20)}
              </td>
              <td className="alerts-table__cell--mono">
                {truncateText(alert.userId, 20)}
              </td>
              <td className="alerts-table__cell--amount">
                {formatCurrency(alert.amount)}
              </td>
              <td className="alerts-table__cell--risk">
                <RiskBadge
                  level={alert.riskLevel}
                  score={alert.riskScore}
                />
              </td>
              <td>
                <div className="status-cell">
                  <StatusBadge status={alert.status} />
                  {alert.status === 'NEW' && (
                    <select
                      className="status-cell__select"
                      disabled={updating === alert.id}
                      onChange={(e) =>
                        handleStatusUpdate(alert.id, e.target.value)
                      }
                      defaultValue=""
                    >
                      <option value="">Update...</option>
                      <option value="REVIEWED">Mark as Reviewed</option>
                      <option value="RESOLVED">Mark as Resolved</option>
                    </select>
                  )}
                </div>
              </td>
              <td className="alerts-table__cell--date">
                {formatDate(alert.createdAt)}
              </td>
              <td className="alerts-table__cell--reason">
                {truncateText(alert.reason, 40)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AlertsTable;
