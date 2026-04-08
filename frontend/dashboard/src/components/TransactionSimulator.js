import React, { useState, useCallback, useEffect, useRef } from 'react';
import axios from 'axios';
import '../styles/TransactionSimulator.css';

/**
 * TransactionSimulator Component
 * 
 * Generates and sends random transactions to the backend for demo/testing.
 * Features:
 * - Two simulation modes: Normal and Fraud
 * - Real-time transaction generation
 * - Batch submission to backend API
 * - Visual feedback and statistics
 * - Customizable transaction parameters
 */
const TransactionSimulator = () => {
  const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8082';

  const [mode, setMode] = useState('normal');
  const [isRunning, setIsRunning] = useState(false);
  const [transactionCount, setTransactionCount] = useState(1);
  const [generatedTransactions, setGeneratedTransactions] = useState([]);
  const [totalSent, setTotalSent] = useState(0);
  const [status, setStatus] = useState('ready');
  const [error, setError] = useState(null);
  const [successCount, setSuccessCount] = useState(0);
  const [failureCount, setFailureCount] = useState(0);

  /**
   * Generate random transaction based on mode
   */
  const generateTransaction = useCallback((index) => {
    const now = new Date();
    const baseTransactionId = `SIM-${Date.now()}-${index}`;

    if (mode === 'normal') {
      // Normal mode: realistic transactions
      return {
        transactionId: baseTransactionId,
        accountId: `ACC-${Math.floor(Math.random() * 10000).toString().padStart(5, '0')}`,
        amount: parseFloat((Math.random() * 5000 + 10).toFixed(2)),
        merchantId: `MERCHANT-${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`,
        merchantName: generateMerchantName(),
        timestamp: new Date(now.getTime() - Math.random() * 60000).toISOString(),
        location: generateLocation(),
        deviceId: `DEVICE-${Math.random().toString(36).substring(7).toUpperCase()}`,
        channel: ['ONLINE', 'ATM', 'POS', 'MOBILE'][Math.floor(Math.random() * 4)],
        status: 'pending',
      };
    } else {
      // Fraud mode: suspicious transactions
      const isBurstTransaction = Math.random() < 0.6; // 60% rapid transactions
      
      return {
        transactionId: baseTransactionId,
        accountId: `ACC-${Math.floor(Math.random() * 100).toString().padStart(5, '0')}`, // Fewer accounts
        amount: parseFloat((Math.random() * 50000 + 5000).toFixed(2)), // Higher amounts
        merchantId: `MERCHANT-${Math.floor(Math.random() * 50).toString().padStart(3, '0')}`, // Fewer merchants
        merchantName: generateMerchantName(),
        timestamp: new Date(now.getTime() - (isBurstTransaction ? Math.random() * 5000 : Math.random() * 300000)).toISOString(),
        location: generateLocation(),
        deviceId: `DEVICE-${Math.random().toString(36).substring(7).toUpperCase()}`,
        channel: ['ONLINE', 'MOBILE'][Math.floor(Math.random() * 2)],
        status: 'pending',
        flags: ['HIGH_AMOUNT', 'RAPID_SEQUENCE', 'UNUSUAL_LOCATION', 'MULTIPLE_DECLINES'].filter(
          () => Math.random() < 0.4
        ),
      };
    }
  }, [mode]);

  /**
   * Generate random merchant name
   */
  const generateMerchantName = () => {
    const merchants = [
      'AMAZON.COM', 'BEST BUY', 'WALMART', 'TARGET', 'CVS PHARMACY',
      'DELTA AIRLINES', 'HILTON HOTELS', 'UBER TRIP', 'DoorDash', 'NETFLIX',
      'APPLE.COM', 'HOME DEPOT', '7-ELEVEN', 'STARBUCKS', 'GAS STATION',
      'CASINO RESORT', 'LUXURY JEWELRY', 'CRYPTO EXCHANGE', 'FOREIGN ATM', 'UNKNOWN MERCHANT'
    ];
    return merchants[Math.floor(Math.random() * merchants.length)];
  };

  /**
   * Generate random location
   */
  const generateLocation = () => {
    const locations = [
      'New York,US', 'Los Angeles,US', 'Chicago,US', 'Houston,US', 'Phoenix,US',
      'San Francisco,CA', 'Miami,FL', 'Seattle,WA', 'Denver,CO', 'Boston,MA',
      'London,UK', 'Tokyo,JP', 'Singapore,SG', 'Dubai,UAE', 'Hong Kong,HK',
      'Moscow,RU', 'Unknown Location'
    ];
    return locations[Math.floor(Math.random() * locations.length)];
  };

  /**
   * Send transaction to backend API
   */
  const sendTransactionToBackend = async (transaction) => {
    try {
      const response = await axios.post(
        `${API_BASE_URL}/api/transactions`,
        {
          transactionId: transaction.transactionId,
          accountId: transaction.accountId,
          amount: transaction.amount,
          merchantId: transaction.merchantId,
          merchantName: transaction.merchantName,
          timestamp: transaction.timestamp,
          location: transaction.location,
          deviceId: transaction.deviceId,
          channel: transaction.channel,
        },
        {
          timeout: 5000,
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );
      
      return { success: true, data: response.data };
    } catch (err) {
      console.error('Error sending transaction:', err);
      return { 
        success: false, 
        error: err.response?.data?.message || err.message 
      };
    }
  };

  /**
   * Generate and send single transaction
   */
  const handleSendSingle = useCallback(async () => {
    try {
      setError(null);
      setStatus('sending');
      
      const transaction = generateTransaction(0);
      const result = await sendTransactionToBackend(transaction);

      if (result.success) {
        setGeneratedTransactions([transaction, ...generatedTransactions.slice(0, 9)]);
        setTotalSent(prev => prev + 1);
        setSuccessCount(prev => prev + 1);
        setStatus('success');
        setTimeout(() => setStatus('ready'), 2000);
      } else {
        setError(`Failed to send transaction: ${result.error}`);
        setFailureCount(prev => prev + 1);
        setStatus('error');
      }
    } catch (err) {
      setError(`Error: ${err.message}`);
      setFailureCount(prev => prev + 1);
      setStatus('error');
    }
  }, [generatedTransactions, generateTransaction]);

  /**
   * Generate and send batch of transactions
   */
  const handleSendBatch = useCallback(async () => {
    try {
      setError(null);
      setStatus('sending');

      const transactions = Array.from(
        { length: transactionCount },
        (_, i) => generateTransaction(i)
      );

      let successCount = 0;
      let failureCount = 0;

      // Send transactions sequentially with delay for demo effect
      for (let i = 0; i < transactions.length; i++) {
        const result = await sendTransactionToBackend(transactions[i]);
        
        if (result.success) {
          successCount++;
        } else {
          failureCount++;
        }

        // Add small delay between requests
        if (i < transactions.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 200));
        }
      }

      setGeneratedTransactions(transactions);
      setTotalSent(prev => prev + successCount);
      setSuccessCount(prev => prev + successCount);
      setFailureCount(prev => prev + failureCount);
      setStatus('complete');
      setTimeout(() => setStatus('ready'), 3000);
    } catch (err) {
      setError(`Batch error: ${err.message}`);
      setStatus('error');
    }
  }, [transactionCount, generateTransaction]);

  /**
   * Start continuous simulation
   */
  const handleStartSimulation = useCallback(async () => {
    setIsRunning(true);
    setStatus('running');
    setError(null);

    const runSimulation = async () => {
      for (let i = 0; i < transactionCount && isRunning; i++) {
        const transaction = generateTransaction(i);
        const result = await sendTransactionToBackend(transaction);

        if (result.success) {
          setSuccessCount(prev => prev + 1);
        } else {
          setFailureCount(prev => prev + 1);
        }

        setGeneratedTransactions(prev => [transaction, ...prev.slice(0, 9)]);
        setTotalSent(prev => prev + 1);

        // Delay based on mode
        const delay = mode === 'normal' ? 1500 : 300; // Fraud mode: faster
        await new Promise(resolve => setTimeout(resolve, delay));
      }

      setIsRunning(false);
      setStatus('ready');
    };

    runSimulation();
  }, [transactionCount, mode, generateTransaction, isRunning]);

  /**
   * Stop simulation
   */
  const handleStopSimulation = useCallback(() => {
    setIsRunning(false);
    setStatus('stopped');
    setTimeout(() => setStatus('ready'), 2000);
  }, []);

  /**
   * Reset statistics
   */
  const handleReset = useCallback(() => {
    setGeneratedTransactions([]);
    setTotalSent(0);
    setSuccessCount(0);
    setFailureCount(0);
    setStatus('ready');
    setError(null);
  }, []);

  const statusClass = `status-${status}`;

  return (
    <div className="transaction-simulator">
      <div className="simulator-container">
        {/* Header */}
        <div className="simulator-header">
          <h2>🎬 Transaction Simulator</h2>
          <p className="simulator-subtitle">Generate and send test transactions for demo/testing</p>
        </div>

        {/* Control Panel */}
        <div className="simulator-panel">
          {/* Mode Selection */}
          <div className="mode-selector">
            <label>Simulation Mode:</label>
            <div className="mode-buttons">
              <button
                className={`mode-btn ${mode === 'normal' ? 'active' : ''}`}
                onClick={() => setMode('normal')}
                disabled={isRunning}
                title="Normal: Realistic transactions with varied amounts and merchants"
              >
                <span className="mode-icon">📊</span>
                <span className="mode-name">Normal</span>
                <span className="mode-desc">Realistic</span>
              </button>
              <button
                className={`mode-btn ${mode === 'fraud' ? 'active' : ''}`}
                onClick={() => setMode('fraud')}
                disabled={isRunning}
                title="Fraud: High amounts, rapid sequences, unusual patterns"
              >
                <span className="mode-icon">⚠️</span>
                <span className="mode-name">Fraud</span>
                <span className="mode-desc">Suspicious</span>
              </button>
            </div>
          </div>

          {/* Transaction Count */}
          <div className="input-group">
            <label>Transactions to send:</label>
            <div className="input-wrapper">
              <input
                type="number"
                min="1"
                max="100"
                value={transactionCount}
                onChange={(e) => setTransactionCount(Math.max(1, parseInt(e.target.value) || 1))}
                disabled={isRunning}
                className="count-input"
              />
              <span className="input-hint">(1-100)</span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="action-buttons">
            {!isRunning ? (
              <>
                <button
                  className="btn btn-primary btn-send-single"
                  onClick={handleSendSingle}
                  title="Send a single transaction immediately"
                >
                  📤 Send One
                </button>
                <button
                  className="btn btn-success btn-send-batch"
                  onClick={handleSendBatch}
                  title="Send batch of transactions with delay"
                >
                  📦 Send Batch
                </button>
                <button
                  className="btn btn-warning btn-start"
                  onClick={handleStartSimulation}
                  disabled={transactionCount === 0}
                  title="Start continuous simulation"
                >
                  ▶️ Start Stream
                </button>
              </>
            ) : (
              <button
                className="btn btn-error btn-stop"
                onClick={handleStopSimulation}
                title="Stop the running simulation"
              >
                ⏹️ Stop Stream
              </button>
            )}
            <button
              className="btn btn-secondary btn-reset"
              onClick={handleReset}
              disabled={isRunning}
              title="Reset all statistics"
            >
              🔄 Reset
            </button>
          </div>

          {/* Status Indicator */}
          <div className={`status-indicator ${statusClass}`}>
            <span className="status-light"></span>
            <span className="status-text">
              {status === 'ready' && 'Ready'}
              {status === 'sending' && 'Sending...'}
              {status === 'running' && 'Streaming...'}
              {status === 'complete' && '✓ Batch sent'}
              {status === 'stopped' && 'Stream stopped'}
              {status === 'success' && '✓ Transaction sent'}
              {status === 'error' && '✗ Error occurred'}
            </span>
          </div>

          {/* Error Message */}
          {error && (
            <div className="error-message">
              <span className="error-icon">⚠️</span>
              <span className="error-text">{error}</span>
              <button className="error-close" onClick={() => setError(null)}>✕</button>
            </div>
          )}
        </div>

        {/* Statistics */}
        <div className="statistics-panel">
          <div className="stat-item total">
            <div className="stat-value">{totalSent}</div>
            <div className="stat-label">Total Sent</div>
          </div>
          <div className="stat-item success">
            <div className="stat-value">{successCount}</div>
            <div className="stat-label">Successful</div>
          </div>
          <div className="stat-item failure">
            <div className="stat-value">{failureCount}</div>
            <div className="stat-label">Failed</div>
          </div>
          <div className="stat-item rate">
            <div className="stat-value">
              {totalSent > 0 ? ((successCount / totalSent) * 100).toFixed(1) : '0'}%
            </div>
            <div className="stat-label">Success Rate</div>
          </div>
        </div>

        {/* Generated Transactions Display */}
        {generatedTransactions.length > 0 && (
          <div className="transactions-display">
            <h3>📋 Last Transactions</h3>
            <div className="transactions-list">
              {generatedTransactions.map((txn, index) => (
                <div key={`${txn.transactionId}-${index}`} className="transaction-item">
                  <div className="txn-header">
                    <span className="txn-id">{txn.transactionId}</span>
                    <span className={`txn-mode ${mode}`}>{mode.toUpperCase()}</span>
                  </div>
                  <div className="txn-details">
                    <div className="txn-detail-row">
                      <span className="detail-label">Amount:</span>
                      <span className="detail-value">${txn.amount.toFixed(2)}</span>
                    </div>
                    <div className="txn-detail-row">
                      <span className="detail-label">Merchant:</span>
                      <span className="detail-value">{txn.merchantName}</span>
                    </div>
                    <div className="txn-detail-row">
                      <span className="detail-label">Location:</span>
                      <span className="detail-value">{txn.location}</span>
                    </div>
                    {txn.flags && txn.flags.length > 0 && (
                      <div className="txn-flags">
                        {txn.flags.map((flag, idx) => (
                          <span key={idx} className="flag">{flag}</span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Help Section */}
        <div className="help-section">
          <h4>ℹ️ How to use:</h4>
          <ul>
            <li><strong>Normal Mode:</strong> Generates realistic transactions for testing the system with normal patterns</li>
            <li><strong>Fraud Mode:</strong> Generates suspicious transactions (high amounts, rapid sequences) for fraud detection testing</li>
            <li><strong>Send One:</strong> Immediately sends a single transaction</li>
            <li><strong>Send Batch:</strong> Sends multiple transactions with a delay between each</li>
            <li><strong>Start Stream:</strong> Continuously generates and sends transactions at a rapid rate</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

// Demo mode methods for guided demo flow
TransactionSimulator.prototype.startDemoMode = function() {
  // Set to fraud mode to generate suspicious transactions that will trigger alerts
  this.setMode('fraud');
  // Send a batch of transactions to generate alerts
  this.handleSendBatch();
  // Start continuous simulation to show ongoing processing
  setTimeout(() => {
    this.handleStartSimulation();
  }, 2000);
};

export default TransactionSimulator;

export default TransactionSimulator;
