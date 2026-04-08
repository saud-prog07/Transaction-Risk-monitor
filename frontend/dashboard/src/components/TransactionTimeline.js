import React, { useState, useEffect } from 'react';
import '../styles/TransactionTimeline.css';

/**
 * TransactionTimeline Component
 * Visualizes the end-to-end processing timeline of transactions
 * Shows: Received → Queued → Processed → Alert Generated
 */
const TransactionTimeline = ({ transactions }) => {
  const [timelineData, setTimelineData] = useState([]);

  useEffect(() => {
    if (transactions && transactions.length > 0) {
      // Process transaction data to extract timing information
      const processedTimeline = transactions.map(tx => {
        // In a real implementation, these would come from backend timing data
        // For now, we'll simulate or extract from available data
        const receivedTime = tx.timestamp || new Date();
        const queuedTime = new Date(receivedTime.getTime() + Math.random() * 100); // 0-100ms queue delay
        const processedTime = new Date(queuedTime.getTime() + Math.random() * 200); // 0-200ms processing
        const alertTime = tx.riskLevel && tx.riskLevel !== 'NONE' 
          ? new Date(processedTime.getTime() + Math.random() * 50) // 0-50ms alert generation
          : null;

        return {
          id: tx.transactionId || tx.id || Math.random().toString(36).substr(2, 9),
          userId: tx.userId || 'unknown',
          amount: tx.amount || 0,
          location: tx.location || 'unknown',
          riskLevel: tx.riskLevel || 'NONE',
          status: tx.status || 'UNKNOWN',
          receivedTime,
          queuedTime,
          processedTime,
          alertTime,
          hasAlert: !!alertTime
        };
      });

      // Sort by received time (newest first)
      processedTimeline.sort((a, b) => b.receivedTime - a.receivedTime);
      setTimelineData(processedTimeline);
    }
  }, [transactions]);

  if (!timelineData || timelineData.length === 0) {
    return (
      <div className="timeline-container">
        <div className="timeline-empty">
          <h3>📊 Transaction Processing Timeline</h3>
          <p>No transaction data available</p>
          <p>Timeline will appear here as transactions are processed</p>
        </div>
      </div>
    );
  }

  return (
    <div className="timeline-container">
      <div className="timeline-header">
        <h2>⏱️ Transaction Processing Timeline</h2>
        <p className="timeline-subtitle">
          View end-to-end processing timing for recent transactions
        </p>
        <div className="timeline-controls">
          <button className="btn btn--primary btn--small" onClick={() => console.log('Refresh timeline')}>
            🔄 Refresh
          </button>
          <span className="timeline-count">
            Showing {timelineData.length} transactions
          </span>
        </div>
      </div>

      <div className="timeline-list">
        {timelineData.map((tx, index) => (
          <div key={tx.id} className="timeline-item">
            <div className="timeline-item-header">
              <div className="timeline-item-info">
                <div className="timeline-user-id">User: {tx.userId}</div>
                <div className="timeline-amount">
                  Amount: ${tx.amount.toLocaleString()}
                </div>
                <div className="timeline-location">
                  Location: {tx.location}
                </div>
              </div>
              <div className="timeline-item-status">
                <span className={`status-badge status-${tx.riskLevel.toLowerCase()}`}>
                  {tx.riskLevel}
                </span>
              </div>
            </div>

            <div className="timeline-steps">
              {/* Received Step */}
              <div className="timeline-step">
                <div className="timeline-step-icon">
                  📥
                </div>
                <div className="timeline-step-content">
                  <div className="timeline-step-label">Received</div>
                  <div className="timeline-step-time">
                    {tx.receivedTime.toLocaleTimeString()}.{tx.receivedTime.getMilliseconds()
                      .toString()
                      .padStart(3, '0')}
                  </div>
                </div>
              </div>

              {/* Queued Step */}
              <div className="timeline-step">
                <div className="timeline-step-icon">
                  ⏳
                </div>
                <div className="timeline-step-content">
                  <div className="timeline-step-label">Queued</div>
                  <div className="timeline-step-time">
                    {tx.queuedTime.toLocaleTimeString()}.{tx.queuedTime.getMilliseconds()
                      .toString()
                      .padStart(3, '0')}
                  </div>
                </div>
              </div>

              {/* Processed Step */}
              <div className="timeline-step">
                <div className="timeline-step-icon">
                  🔍
                </div>
                <div className="timeline-step-content">
                  <div className="timeline-step-label">Processed</div>
                  <div className="timeline-step-time">
                    {tx.processedTime.toLocaleTimeString()}.{tx.processedTime.getMilliseconds()
                      .toString()
                      .padStart(3, '0')}
                  </div>
                </div>
              </div>

              {/* Alert Generated Step (conditional) */}
              {tx.hasAlert && tx.alertTime && (
                <div className="timeline-step">
                  <div className="timeline-step-icon">
                    🚨
                  </div>
                  <div className="timeline-step-content">
                    <div className="timeline-step-label">Alert Generated</div>
                    <div className="timeline-step-time">
                      {tx.alertTime.toLocaleTimeString()}.{tx.alertTime.getMilliseconds()
                        .toString()
                        .padStart(3, '0')}
                    </div>
                  </div>
                </div>
              )}

              {/* Processing Duration */}
              <div className="timeline-step timeline-duration">
                <div className="timeline-step-icon">
                  ⏱️
                </div>
                <div className="timeline-step-content">
                  <div className="timeline-step-label">Total Processing Time</div>
                  <div className="timeline-step-time">
                    {tx.hasAlert ? 
                      ((tx.alertTime - tx.receivedTime) / 1000).toFixed(2) + 's' :
                      ((tx.processedTime - tx.receivedTime) / 1000).toFixed(2) + 's'
                    }
                  </div>
                </div>
              </div>
            </div>

            {/* Timeline Connector (except for last item) */}
            {index < timelineData.length - 1 && (
              <div className="timeline-connector">
                <div className="timeline-connector-line"></div>
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="timeline-legend">
        <div className="legend-title">Legend:</div>
        <div className="legend-items">
          <div className="legend-item">
            <div className="legend-icon">📥</div>
            <span>Received</span>
          </div>
          <div className="legend-item">
            <div className="legend-icon">⏳</div>
            <span>Queued</span>
          </div>
          <div className="legend-item">
            <div className="legend-icon">🔍</div>
            <span>Processed</span>
          </div>
          <div className="legend-item">
            <div className="legend-icon">🚨</div>
            <span>Alert Generated</span>
          </div>
          <div className="legend-item">
            <div className="legend-icon">⏱️</div>
            <span>Processing Time</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TransactionTimeline;