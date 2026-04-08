import React, { useState, useEffect, useCallback } from 'react';
import LiveTransactionsFeed from './LiveTransactionsFeed';
import AlertsPanel from './AlertsPanel';
import SystemHealth from './SystemHealth';
import TransactionSimulator from './TransactionSimulator';
import { fetchAlerts, fetchDashboardStats } from '../services/apiService';
import { usePolling } from '../services/hooks';
import '../styles/OperationsDashboard.css';

/**
 * OperationsDashboard Component
 * Real-time monitoring dashboard for transaction operations
 * Features:
 * - Live transaction feed with auto-refresh
 * - Alerts panel with review functionality
 * - System health monitoring (MQ, DLQ, message processing)
 * - Key statistics cards
 */
const OperationsDashboard = () => {
  const [stats, setStats] = useState({
    totalTransactions: 0,
    highRiskCount: 0,
    fraudRate: 0,
    processedMessages: 0,
    dlqSize: 0,
  });

  const [alerts, setAlerts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [systemHealth, setSystemHealth] = useState({
    mqStatus: 'connected',
    processingRate: 0,
    dlqSize: 0,
    lastUpdated: new Date(),
  });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Load operations data
  const loadOperationsData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch alerts
      const alertsResponse = await fetchAlerts({ page: 0, size: 10 });
      const alertsData = alertsResponse.data.content || alertsResponse.data || [];
      setAlerts(alertsData);

      // Fetch stats
      const statsResponse = await fetchDashboardStats({ page: 0, size: 50 });
      const statsData = statsResponse.data;
      
      setStats({
        totalTransactions: statsData.total || 0,
        highRiskCount: statsData.high || 0,
        fraudRate: statsData.flaggedPercentage || 0,
        processedMessages: Math.floor(Math.random() * 10000) + 5000, // Mock data
        dlqSize: Math.floor(Math.random() * 100) + 10, // Mock data
      });

      // Generate mock transaction stream
      const mockTransactions = generateMockTransactions(5);
      setTransactions(mockTransactions);

      // Update system health (mock)
      setSystemHealth({
        mqStatus: Math.random() > 0.05 ? 'connected' : 'disconnected',
        processingRate: Math.floor(Math.random() * 500) + 100,
        dlqSize: Math.floor(Math.random() * 150),
        lastUpdated: new Date(),
      });
    } catch (err) {
      console.error('Failed to load operations data:', err);
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  }, []);

  // Set up auto-refresh every 3 seconds
  usePolling(loadOperationsData, 3000, true, []);

  return (
    <div className="operations-dashboard">
      {/* Header */}
      <div className="ops-header">
        <h1>Operations Dashboard</h1>
        <div className="ops-header-meta">
          <span className="last-updated">
            Last updated: {systemHealth.lastUpdated.toLocaleTimeString()}
          </span>
          <span className={`system-status ${systemHealth.mqStatus}`}>
            ● {systemHealth.mqStatus === 'connected' ? 'System Online' : 'System Offline'}
          </span>
        </div>
      </div>

      {error && <div className="ops-error-banner">{error}</div>}

      {/* Stats Cards Row */}
      <div className="stats-cards-row">
        <StatCard
          label="Total Transactions"
          value={stats.totalTransactions.toLocaleString()}
          icon="📊"
          trend={`+${Math.floor(Math.random() * 100)}`}
          trendColor="positive"
        />
        <StatCard
          label="High-Risk Count"
          value={stats.highRiskCount}
          icon="⚠️"
          trend={`+${Math.floor(Math.random() * 10)}`}
          trendColor="negative"
          highlight
        />
        <StatCard
          label="Fraud Rate"
          value={`${stats.fraudRate.toFixed(2)}%`}
          icon="🔴"
          trend={`${Math.random() > 0.5 ? '+' : '-'}${(Math.random() * 0.5).toFixed(2)}%`}
          trendColor={stats.fraudRate > 5 ? 'negative' : 'positive'}
        />
        <StatCard
          label="Processing Rate"
          value={`${systemHealth.processingRate}/sec`}
          icon="⚡"
          trend="Live"
          trendColor="positive"
        />
      </div>

      {/* Main Content Grid */}
      <div className="ops-main-grid">
        {/* Left Column - Transactions Feed */}
        <div className="ops-column left-column">
          <LiveTransactionsFeed
            transactions={transactions}
            loading={loading}
            stats={stats}
          />
        </div>

        {/* Right Column - Alerts & System Health */}
        <div className="ops-column right-column">
          <AlertsPanel alerts={alerts} loading={loading} />
          <SystemHealth health={systemHealth} />
        </div>
      </div>

      {/* Transaction Simulator - For Testing & Demo */}
      <TransactionSimulator />
    </div>
    </div>
  );
};

/**
 * StatCard Component
 * Displays a single statistic
 */
const StatCard = ({ label, value, icon, trend, trendColor, highlight }) => {
  return (
    <div className={`stat-card ${highlight ? 'highlight' : ''}`}>
      <div className="stat-icon">{icon}</div>
      <div className="stat-content">
        <div className="stat-label">{label}</div>
        <div className="stat-value">{value}</div>
        <div className={`stat-trend ${trendColor}`}>{trend}</div>
      </div>
    </div>
  );
};

/**
 * Generate mock transaction data
 */
function generateMockTransactions(count) {
  const transactions = [];
  const riskLevels = ['LOW', 'MEDIUM', 'HIGH'];
  const locations = ['New York', 'San Francisco', 'London', 'Tokyo', 'Singapore'];

  for (let i = 0; i < count; i++) {
    const riskLevel = riskLevels[Math.floor(Math.random() * riskLevels.length)];
    transactions.push({
      id: `TXN-${Math.random().toString(36).substr(2, 9).toUpperCase()}`,
      userId: `USER-${Math.floor(Math.random() * 10000)}`,
      amount: (Math.random() * 10000 + 100).toFixed(2),
      currency: 'USD',
      location: locations[Math.floor(Math.random() * locations.length)],
      timestamp: new Date(Date.now() - Math.random() * 60000),
      riskLevel: riskLevel,
      flagged: riskLevel === 'HIGH',
      merchant: `Merchant-${Math.floor(Math.random() * 1000)}`,
    });
  }

  return transactions.sort((a, b) => b.timestamp - a.timestamp);
}

export default OperationsDashboard;
