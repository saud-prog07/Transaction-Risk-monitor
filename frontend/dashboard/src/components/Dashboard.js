import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import StatCard from './StatCard';
import FilterBar from './FilterBar';
import AlertsTable from './AlertsTable';
import TransactionLineChart from './TransactionLineChart';
import FlaggedVsNormalBarChart from './FlaggedVsNormalBarChart';
import RiskLevelChart from './RiskLevelChart';
import { fetchAlerts, fetchDashboardStats, healthCheck, getErrorMessage, fetchSystemHealth } from '../services/apiService';
import { usePolling } from '../services/hooks';
import { logout } from '../services/authService';
import SystemHealth from './SystemHealth';
import TransactionTimeline from './TransactionTimeline';
import SystemFlowVisualization from './SystemFlowVisualization';
import BusinessImpactChart from './BusinessImpactChart';
import {
  generateTransactionTimeSeriesData,
  generateFlaggedVsNormalData,
  generateRiskLevelChartData,
} from '../utils/chartDataGenerator';
import '../styles/Dashboard.css';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8082';

/**
 * Dashboard Component - Main dashboard container
 * Displays statistics, filters, and alerts table
 * Auto-refreshes data every 5 seconds
 */
const Dashboard = () => {
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState([]);
  const [filteredAlerts, setFilteredAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [stats, setStats] = useState({
    total: 0,
    high: 0,
    medium: 0,
    low: 0,
    new: 0,
    reviewed: 0,
    resolved: 0,
    flaggedPercentage: 0,
    // Business impact metrics
    totalFraudAmount: '0.00',
    suspiciousPercentage: '0.0',
    averageRiskScore: '0.00',
  });
  const [apiHealthy, setApiHealthy] = useState(false);
  const [systemHealth, setSystemHealth] = useState({
    mqStatus: 'connected',
    databaseStatus: 'connected',
    serviceStatus: 'UP',
    processingRate: 0,
    dlqSize: 0,
    uptime: '99.9%',
    lastUpdated: new Date()
  });
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [filters, setFilters] = useState({
    riskLevel: '',
    status: '',
    search: '',
  });

   // Generate chart data from alerts (memoized for performance)
   const transactionChartData = useMemo(
     () => generateTransactionTimeSeriesData(alerts),
     [alerts]
   );

   const flaggedVsNormalData = useMemo(
     () => generateFlaggedVsNormalData(alerts),
     [alerts]
   );

   const riskLevelData = useMemo(
     () => generateRiskLevelChartData(alerts),
     [alerts]
   );

   // Business impact chart data
   const fraudAmountChartData = useMemo(
     () => {
       if (stats.fraudAmountTrend && stats.fraudAmountTrend.length > 0) {
         return stats.fraudAmountTrend.map((item, index) => ({
           x: item.date,
           y: item.value
         }));
       }
       return [];
     },
     [stats.fraudAmountTrend]
   );

   const riskScoreChartData = useMemo(
     () => {
       if (stats.riskScoreTrend && stats.riskScoreTrend.length > 0) {
         return stats.riskScoreTrend.map((item, index) => ({
           x: item.date,
           y: item.value
         }));
       }
       return [];
     },
     [stats.riskScoreTrend]
   );

  // Check API health on component mount
  useEffect(() => {
    const checkHealth = async () => {
      try {
        await healthCheck();
        setApiHealthy(true);
        
        // Also fetch detailed system health
        const healthData = await fetchSystemHealth();
        setSystemHealth(healthData.data || {
          mqStatus: 'connected',
          databaseStatus: 'connected',
          serviceStatus: 'UP',
          processingRate: 0,
          dlqSize: 0,
          uptime: '99.9%',
          lastUpdated: new Date()
        });
      } catch (err) {
        console.warn('API health check failed:', getErrorMessage(err));
        setApiHealthy(false);
        
        // Set default health status on error
        setSystemHealth({
          mqStatus: 'disconnected',
          databaseStatus: 'disconnected',
          serviceStatus: 'DOWN',
          processingRate: 0,
          dlqSize: 0,
          uptime: '0%',
          lastUpdated: new Date()
        });
      }
    };

    checkHealth();
    // Check health every 30 seconds
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  // Fetch alerts and statistics
  const loadAlerts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const params = {
        page: 0,
        size: 50,
      };

      if (filters.riskLevel) {
        params.riskLevel = filters.riskLevel;
      }

      if (filters.status) {
        params.status = filters.status;
      }

      // Fetch alerts
      const alertsResponse = await fetchAlerts(params);
      const alertsData = alertsResponse.data.content || alertsResponse.data || [];
      setAlerts(alertsData);

      // Fetch statistics
      const statsResponse = await fetchDashboardStats(params);
      setStats(statsResponse.data);

      setLastUpdated(new Date());
      setApiHealthy(true);
    } catch (err) {
      console.error('Failed to load alerts:', err);
      const errorMsg = getErrorMessage(err);
      setError(errorMsg);
      setApiHealthy(false);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  // Set up polling every 5 seconds
  const { refresh } = usePolling(loadAlerts, 5000, true, [filters]);

  // Apply search filter
  useEffect(() => {
    if (!filters.search) {
      setFilteredAlerts(alerts);
      return;
    }

    const searchLower = filters.search.toLowerCase();
    const filtered = alerts.filter(
      (alert) =>
        alert.userId?.toLowerCase().includes(searchLower) ||
        alert.transactionId?.toLowerCase().includes(searchLower)
    );

    setFilteredAlerts(filtered);
  }, [alerts, filters.search]);

  const handleFilterChange = (newFilters) => {
    setFilters((prev) => ({
      ...prev,
      riskLevel: newFilters.riskLevel || '',
      status: newFilters.status || '',
    }));
  };

  const handleSearchChange = (searchTerm) => {
    setFilters((prev) => ({
      ...prev,
      search: searchTerm,
    }));
  };

  const handleStatusUpdate = (alertId, newStatus) => {
    // Update local state immediately for better UX
    setAlerts((prev) =>
      prev.map((alert) =>
        alert.id === alertId ? { ...alert, status: newStatus } : alert
      )
    );

    // Reload data after a short delay
    setTimeout(() => {
      loadAlerts();
    }, 500);
  };

   const handleManualRefresh = async () => {
     await refresh();
   };

   const handleExportCsv = async () => {
     try {
       // Build query parameters based on current filters
       const params = new URLSearchParams();
       if (filters.riskLevel) {
         params.append('riskLevel', filters.riskLevel);
       }
       if (filters.status) {
         params.append('status', filters.status);
       }
       
       const response = await fetch(`${API_BASE_URL}/api/alerts/export/csv?${params.toString()}`, {
         method: 'GET',
         headers: {
           'Accept': 'text/csv',
         },
       });
       
       if (!response.ok) {
         throw new Error(`Failed to export CSV: ${response.statusText}`);
       }
       
       // Get filename from Content-Disposition header or use default
       const contentDisposition = response.headers.get('Content-Disposition');
       let filename = 'flagged-transactions.csv';
       if (contentDisposition && contentDisposition.includes('filename=')) {
         const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
         if (match && match[1]) {
           filename = match[1].replace(/['"]/g, '');
         }
       }
       
       const blob = await response.blob();
       const downloadUrl = window.URL.createObjectURL(blob);
       const link = document.createElement('a');
       link.href = downloadUrl;
       link.setAttribute('download', filename);
       document.body.appendChild(link);
       link.click();
       link.remove();
       window.URL.revokeObjectURL(downloadUrl);
       
       // Show success message
       setSuccess(`Export started: ${filename}`);
       setTimeout(() => setSuccess(null), 5000);
     } catch (err) {
       console.error('Error exporting CSV:', err);
       setError('Failed to export CSV: ' + err.message);
     }
   };

  const handleLogout = () => {
    // Clear authentication
    logout();
    // Redirect to login page
    navigate('/login');
  };

  return (
    <div className="dashboard">
        {/* Header */}
        <header className="dashboard__header">
          <div className="dashboard__header-content">
            <h1 className="dashboard__title">
              🛡️ Transaction Risk Monitor
            </h1>
            <p className="dashboard__subtitle">
              Real-time fraud detection dashboard
            </p>
          </div>
          <div className="dashboard__header-controls">
            <div className="dashboard__health">
              <span
                className={`health-indicator ${
                  apiHealthy ? 'health-indicator--healthy' : 'health-indicator--unhealthy'
                }`}
                title={apiHealthy ? 'API connected' : 'API disconnected'}
              >
                {apiHealthy ? '● Online' : '● Offline'}
              </span>
            </div>
            <button
              className="btn btn--secondary btn--small"
              onClick={() => navigate('/admin')}
              title="Open admin configuration panel"
            >
              ⚙️ Admin
            </button>
            <button
              className="btn btn--success btn--small"
              onClick={handleExportCsv}
              title="Export flagged transactions to CSV"
            >
              📥 Export CSV
            </button>
            <button
              className="btn btn--danger btn--small"
              onClick={handleLogout}
              title="Logout from dashboard"
            >
              🚪 Logout
            </button>
          </div>
        </header>
        
        {/* Success Message */}
        {success && (
          <div className="dashboard__success-banner">
            <span className="success-icon">✅</span>
            <span>{success}</span>
          </div>
        )}
       
       {/* System Health Status */}
       <section className="dashboard__system-health">
         <SystemHealth health={systemHealth} />
       </section>

       {/* System Flow Visualization */}
       <section className="dashboard__system-flow">
         <SystemFlowVisualization />
       </section>

       {/* Transaction Processing Timeline */}
       <section className="dashboard__timeline">
         <TransactionTimeline transactions={filteredAlerts} />
       </section>

       {/* Statistics Cards */}
       <section className="dashboard__stats">
         <StatCard
           title="Total Alerts"
           value={stats.total}
           icon="📊"
           color="primary"
           subtitle={`Monitoring ${stats.total} flagged transactions`}
         />
         <StatCard
           title="High Risk"
           value={stats.high}
           icon="🔴"
           color="danger"
           subtitle={`${stats.flaggedPercentage}% fraud rate`}
         />
         <StatCard
           title="Medium Risk"
           value={stats.medium}
           icon="🟡"
           color="warning"
           subtitle="Requires review"
         />
         <StatCard
           title="Low Risk"
           value={stats.low}
           icon="🟢"
           color="success"
           subtitle="Legitimate"
         />
         <StatCard
           title="New Alerts"
           value={stats.new}
           icon="📬"
           color="primary"
           subtitle="Awaiting action"
         />
         {/* Business Impact Metrics */}
         <StatCard
           title="Fraud Amount Detected"
           value={`$${parseFloat(stats.totalFraudAmount).toLocaleString()}`}
           icon="💰"
           color="danger"
           subtitle="Total amount of fraudulent transactions"
         />
         <StatCard
           title="Suspicious Transactions"
           value={`${stats.suspiciousPercentage}%`}
           icon="⚠️"
           color="warning"
           subtitle="Percentage of non-low risk transactions"
         />
         <StatCard
           title="Average Risk Score"
           value={stats.averageRiskScore}
           icon="📈"
           color="info"
           subtitle="Average risk score across all transactions"
         />
       </section>

      {/* Filter Bar */}
      <section className="dashboard__filters">
        <FilterBar
          onFilterChange={handleFilterChange}
          onSearchChange={handleSearchChange}
        />
      </section>

      {/* Analytics Charts */}
      <section className="dashboard__charts">
        <div className="dashboard__charts-title">
          <h2>Real-Time Analytics</h2>
          <p className="dashboard__charts-subtitle">Charts update every 5 seconds</p>
        </div>

         <div className="dashboard__charts-grid">
           <div className="dashboard__chart-item dashboard__chart-item--full">
             <TransactionLineChart data={transactionChartData} />
           </div>

           <div className="dashboard__chart-item dashboard__chart-item--half">
             <FlaggedVsNormalBarChart data={flaggedVsNormalData} />
           </div>

           <div className="dashboard__chart-item dashboard__chart-item--half">
             <RiskLevelChart data={riskLevelData} />
           </div>
         </div>
         
         {/* Business Impact Charts */}
         <div className="dashboard__charts-title">
           <h2>Business Impact Metrics</h2>
           <p className="dashboard__charts-subtitle">Financial impact and risk trends</p>
         </div>
         <div className="dashboard__charts-grid">
           <div className="dashboard__chart-item dashboard__chart-item--half">
             <BusinessImpactChart
               title="Fraud Amount Detected"
               subtitle="Total USD value of high-risk transactions (7-Day Trend)"
               color="#ef4444"
               data={fraudAmountChartData}
               yLabel="currency"
             />
           </div>

           <div className="dashboard__chart-item dashboard__chart-item--half">
             <BusinessImpactChart
               title="Average Risk Score"
               subtitle="Mean risk score across all transactions (7-Day Trend)"
               color="#3b82f6"
               data={riskScoreChartData}
               yLabel="score"
             />
           </div>
         </div>
         <div className="dashboard__charts-grid">
           <div className="dashboard__chart-item dashboard__chart-item--half">
             {/* Fraud Amount Trend Chart */}
             <div className="business-chart-container">
               <h3 className="chart-title">Fraud Amount Detected (7-Day Trend)</h3>
               <p className="chart-subtitle">Total USD value of high-risk transactions</p>
               {/* Simple line chart for fraud amount trend */}
               <div className="chart-placeholder">
                 <div className="chart-grid">
                   {fraudAmountChartData.map((point, index) => (
                     <div key={index} className="chart-point" style={{ 
                       left: `${(index / (fraudAmountChartData.length - 1 || 1)) * 100}%`,
                       bottom: `${(point.y / Math.max(...fraudAmountChartData.map(p => p.y) || [1])) * 100}%`
                     }}>
                       <div className="chart-dot" style={{ backgroundColor: '#ef4444' }}></div>
                       <div className="chart-label">{point.x}</div>
                       <div className="chart-value">${point.y.toFixed(0)}</div>
                     </div>
                   ))}
                 </div>
               </div>
             </div>
           </div>

           <div className="dashboard__chart-item dashboard__chart-item--half">
             {/* Average Risk Score Trend Chart */}
             <div className="business-chart-container">
               <h3 className="chart-title">Average Risk Score (7-Day Trend)</h3>
               <p className="chart-subtitle">Mean risk score across all transactions</p>
               {/* Simple line chart for risk score trend */}
               <div className="chart-placeholder">
                 <div className="chart-grid">
                   {riskScoreChartData.map((point, index) => (
                     <div key={index} className="chart-point" style={{ 
                       left: `${(index / (riskScoreChartData.length - 1 || 1)) * 100}%`,
                       bottom: `${(point.y / Math.max(...riskScoreChartData.map(p => p.y) || [1])) * 100}%`
                     }}>
                       <div className="chart-dot" style={{ backgroundColor: '#3b82f6' }}></div>
                       <div className="chart-label">{point.x}</div>
                       <div className="chart-value">{point.y.toFixed(2)}</div>
                     </div>
                   ))}
                 </div>
               </div>
             </div>
           </div>
         </div>
      </section>

      {/* Alerts Table */}
      <section className="dashboard__table">
        <div className="dashboard__table-header">
          <h2>Recent Flagged Transactions</h2>
          <div className="dashboard__table-controls">
            <button
              className="btn btn--secondary"
              onClick={handleManualRefresh}
              disabled={loading}
              title="Manually refresh data"
            >
              {loading ? 'Refreshing...' : '🔄 Refresh'}
            </button>
          </div>
        </div>

        {error && (
          <div className="dashboard__error-banner">
            <p>⚠️ {error}</p>
            <button onClick={handleManualRefresh} className="btn btn--primary btn--small">
              Retry
            </button>
          </div>
        )}

        <AlertsTable
          alerts={filteredAlerts}
          loading={loading}
          error={error}
          onRefresh={handleManualRefresh}
          onStatusUpdate={handleStatusUpdate}
        />
      </section>

      {/* Footer */}
      <footer className="dashboard__footer">
        <p>Last updated: {lastUpdated.toLocaleTimeString()}</p>
        <p>Auto-refresh every 5 seconds • {filteredAlerts.length} alerts displayed</p>
      </footer>
    </div>
  );
};

export default Dashboard;
