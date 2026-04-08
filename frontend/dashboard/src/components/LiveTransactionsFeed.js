import React, { useState, useEffect } from 'react';
import '../styles/LiveTransactionsFeed.css';

/**
 * LiveTransactionsFeed Component
 * Displays real-time streaming transactions
 * - Auto-refreshes with latest transactions
 * - Highlights flagged (HIGH risk) transactions in red
 * - Shows transaction details (amount, user, location, risk level)
 * - Displays processing status
 */
const LiveTransactionsFeed = ({ transactions, loading, stats }) => {
  const [displayTransactions, setDisplayTransactions] = useState([]);
  const [processingCount, setProcessingCount] = useState(0);

  useEffect(() => {
    setDisplayTransactions(transactions);
    // Simulate processing some transactions
    const processing = Math.floor(Math.random() * 5) + 1;
    setProcessingCount(processing);
  }, [transactions]);

  return (
    <div className="live-feed-container">
      <div className="feed-header">
        <h2>📡 Live Transactions Feed</h2>
        <div className="feed-meta">
          <span className="feed-status">
            <span className="pulse-dot"></span> Live
          </span>
          <span className="transaction-count">
            {displayTransactions.length} transactions
          </span>
          <span className="processing-count">
            {processingCount} processing
          </span>
        </div>
      </div>

      {loading && <div className="feed-loading">Loading transactions...</div>}

      <div className="transactions-list">
        {displayTransactions.length === 0 ? (
          <div className="no-transactions">
            <p>No recent transactions</p>
          </div>
        ) : (
          displayTransactions.map((tx, idx) => (
            <div
              key={`${tx.id}-${idx}`}
              className={`transaction-item ${tx.flagged ? 'flagged' : ''} ${
                ['LOW', 'MEDIUM', 'HIGH'].includes(tx.riskLevel) ? tx.riskLevel.toLowerCase() : ''
              }`}
            >
              {/* Flagged Indicator */}
              {tx.flagged && (
                <div className="flagged-indicator">
                  <span className="flag-icon">🚩</span>
                </div>
              )}

              {/* Transaction Icon */}
              <div className="tx-icon">
                {tx.flagged ? '⚠️' : '✓'}
              </div>

              {/* Transaction Content */}
              <div className="tx-content">
                <div className="tx-header">
                  <span className="tx-id">{tx.id}</span>
                  <span className={`risk-badge ${tx.riskLevel.toLowerCase()}`}>
                    {tx.riskLevel}
                  </span>
                </div>
                <div className="tx-details">
                  <div className="detail-group">
                    <span className="detail-label">User:</span>
                    <span className="detail-value">{tx.userId}</span>
                  </div>
                  <div className="detail-group">
                    <span className="detail-label">Amount:</span>
                    <span className="detail-value-amount">${tx.amount}</span>
                  </div>
                  <div className="detail-group">
                    <span className="detail-label">Location:</span>
                    <span className="detail-value">📍 {tx.location}</span>
                  </div>
                  <div className="detail-group">
                    <span className="detail-label">Time:</span>
                    <span className="detail-value">
                      {tx.timestamp.toLocaleTimeString()}
                    </span>
                  </div>
                </div>
              </div>

              {/* Action Button */}
              <div className="tx-action">
                {tx.flagged && (
                  <button className="view-btn">View</button>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Feed Footer with Stats */}
      <div className="feed-footer">
        <div className="footer-stat">
          <span className="stat-label">Processed Today:</span>
          <span className="stat-value">{stats.totalTransactions.toLocaleString()}</span>
        </div>
        <div className="footer-stat">
          <span className="stat-label">Flagged:</span>
          <span className="stat-value error">{stats.highRiskCount}</span>
        </div>
        <div className="footer-stat">
          <span className="stat-label">Fraud Rate:</span>
          <span className="stat-value">{stats.fraudRate.toFixed(2)}%</span>
        </div>
      </div>
    </div>
  );
};

export default LiveTransactionsFeed;
