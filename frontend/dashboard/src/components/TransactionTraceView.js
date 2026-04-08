import React, { useState, useEffect } from 'react';
import '../styles/TransactionTraceView.css';

/**
 * TransactionTraceView Component
 * Displays the lifecycle timeline of a selected transaction
 * Shows: Received → Queued → Processed → Flagged → Alerted
 */
const TransactionTraceView = ({ transactionId }) => {
  const [traceData, setTraceData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (transactionId) {
      fetchTransactionTrace();
    }
  }, [transactionId]);

  // Fetch transaction trace data from backend
  const fetchTransactionTrace = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // In a real implementation, this would call a backend API
      // For now, we'll simulate the data based on transactionId
      // The backend would need to store timing information for each step
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // Generate mock trace data - in reality, this would come from backend storage
      const mockTraceData = generateMockTraceData(transactionId);
      setTraceData(mockTraceData);
      setLoading(false);
    } catch (err) {
      console.error('Error fetching transaction trace:', err);
      setError('Failed to load transaction trace');
      setLoading(false);
    }
  };

  // Generate mock trace data for demonstration
  // In production, this would be replaced with actual backend API calls
  const generateMockTraceData = (id) => {
    // Use transactionId as seed for consistent mock data
    const seed = hashCode(id);
    const now = Date.now();
    
    // Simulate realistic timing between steps
    const receivedTime = now - (Math.abs(seed) % 300000); // Within last 5 minutes
    const queuedTime = receivedTime + (Math.abs(seed + 1) % 100); // 0-100ms queue delay
    const processedTime = queuedTime + (Math.abs(seed + 2) % 500); // 0-500ms processing
    const flaggedTime = processedTime + (Math.abs(seed + 3) % 200); // 0-200ms to flagging
    const alertedTime = flaggedTime + (Math.abs(seed + 4) % 100); // 0-100ms to alert generation
    
    // Determine if transaction was flagged based on seed
    const wasFlagged = Math.abs(seed) % 3 === 0; // ~33% flagged rate
    
    return {
      transactionId: id,
      steps: [
        {
          name: 'Received',
          timestamp: receivedTime,
          description: 'Transaction received from message queue',
          icon: '📥',
          color: '#3B82F6' // Blue
        },
        {
          name: 'Queued',
          timestamp: queuedTime,
          description: 'Transaction queued for processing',
          icon: '⏳',
          color: '#F59E0B' // Amber
        },
        {
          name: 'Processed',
          timestamp: processedTime,
          description: 'Transaction processed by risk engine',
          icon: '🔍',
          color: '#10B981' // Green
        },
        {
          name: 'Flagged',
          timestamp: wasFlagged ? flaggedTime : null,
          description: wasFlagged ? 'Transaction flagged for review' : 'Transaction passed risk checks',
          icon: wasFlagged ? '🚩' : '✅',
          color: wasFlagged ? '#EF4444' : '#10B981' // Red if flagged, Green if not
        },
        {
          name: 'Alerted',
          timestamp: wasFlagged ? alertedTime : null,
          description: wasFlagged ? 'Alert generated and sent' : 'No alert required',
          icon: wasFlagged ? '🚨' : '🔇',
          color: wasFlagged ? '#EF4444' : '#6B7280' // Red if alerted, Gray if not
        }
      ].filter(step => step.timestamp !== null), // Remove null timestamps
      wasFlagged,
      totalDuration: wasFlagged ? (alertedTime - receivedTime) : (processedTime - receivedTime)
    };
  };

  // Simple hash function for consistent mock data
  const hashCode = (str) => {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
  };

  if (loading) {
    return (
      <div className="trace-view-container">
        <div className="trace-view-header">
          <h2>🔍 Transaction Trace View</h2>
          {transactionId && <span className="transaction-id">ID: {transactionId}</span>}
        </div>
        <div className="trace-loading">Loading transaction trace...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="trace-view-container">
        <div className="trace-view-header">
          <h2>🔍 Transaction Trace View</h2>
          {transactionId && <span className="transaction-id">ID: {transactionId}</span>}
        </div>
        <div className="trace-error">{error}</div>
      </div>
    );
  }

  if (!traceData) {
    return (
      <div className="trace-view-container">
        <div className="trace-view-header">
          <h2>🔍 Transaction Trace View</h2>
          {transactionId && <span className="transaction-id">ID: {transactionId}</span>}
        </div>
        <div className="trace-placeholder">
          <p>Select a transaction to view its processing timeline</p>
          <p>Transaction trace will appear here when a transaction is selected</p>
        </div>
      </div>
    );
  }

  return (
    <div className="trace-view-container">
      <div className="trace-view-header">
        <h2>🔍 Transaction Trace View</h2>
        <div className="trace-info">
          <span className="transaction-id">Transaction ID: {traceData.transactionId}</span>
          <span className="trace-divider">|</span>
          <span className="trace-status">
            {traceData.wasFlagged ? 'FLAGGED ⚠️' : 'PASSED ✅'}
          </span>
          <span className="trace-divider">|</span>
          <span className="trace-duration">
            Total Processing Time: {traceData.totalDuration.toFixed(0)}ms
          </span>
        </div>
      </div>

      <div className="trace-timeline">
        {traceData.steps.map((step, index) => (
          <div key={index} className="trace-step">
            {/* Step Icon */}
            <div className="trace-step-icon">
              <div className="trace-step-circle" style={{ backgroundColor: step.color }}>
                {step.icon}
              </div>
            </div>
            
            {/* Step Content */}
            <div className="trace-step-content">
              <div className="trace-step-name">{step.name}</div>
              <div className="trace-step-description">{step.description}</div>
              <div className="trace-step-timestamp">
                {new Date(step.timestamp).toLocaleTimeString()}.{new Date(step.timestamp).getMilliseconds()
                  .toString()
                  .padStart(3, '0')}
              </div>
            </div>
            
            {/* Step Connector (except for last step) */}
            {index < traceData.steps.length - 1 && (
              <div className="trace-step-connector">
                <div className="trace-connector-line"></div>
              </div>
            )}
          </div>
        ))}
      </div>
      
      {/* Legend */}
      <div className="trace-legend">
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
            <div className="legend-icon">🚩</div>
            <span>Flagged</span>
          </div>
          <div className="legend-item">
            <div className="legend-icon">🚨</div>
            <span>Alerted</span>
          </div>
          <div className="legend-item">
            <div className="legend-icon">✅</div>
            <span>Passed</span>
          </div>
          <div className="legend-item">
            <div className="legend-icon">🔇</div>
            <span>No Alert</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TransactionTraceView;