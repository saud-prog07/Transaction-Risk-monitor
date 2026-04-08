import React, { useState, useEffect, useCallback } from 'react';
import StatCard from './StatCard';
import FilterBar from './FilterBar';
import AlertsTable from './AlertsTable';
import { fetchAlerts, healthCheck } from '../services/apiService';
import '../styles/Dashboard.css';

/**
 * Dashboard Component - Main dashboard container
 * Displays statistics, filters, and alerts table
 */
const Dashboard = () => {
  const [alerts, setAlerts] = useState([]);
  const [filteredAlerts, setFilteredAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({
    total: 0,
    high: 0,
    medium: 0,
    low: 0,
    new: 0,
  });
  const [apiHealthy, setApiHealthy] = useState(false);
  const [filters, setFilters] = useState({
    riskLevel: '',
    status: '',
    search: '',
  });

  // Check API health on component mount
  useEffect(() => {
    const checkHealth = async () => {
      try {
        await healthCheck();
        setApiHealthy(true);
      } catch (err) {
        console.warn('API health check failed:', err.message);
        setApiHealthy(false);
      }
    };

    checkHealth();
    // Check health every 30 seconds
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  // Fetch alerts
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

      const response = await fetchAlerts(params);
      const data = response.data.content || response.data;
      setAlerts(data);

      // Calculate statistics
      const highRisk = data.filter((a) => a.riskLevel === 'HIGH').length;
      const mediumRisk = data.filter((a) => a.riskLevel === 'MEDIUM').length;
      const lowRisk = data.filter((a) => a.riskLevel === 'LOW').length;
      const newStatus = data.filter((a) => a.status === 'NEW').length;

      setStats({
        total: data.length,
        high: highRisk,
        medium: mediumRisk,
        low: lowRisk,
        new: newStatus,
      });

      setApiHealthy(true);
    } catch (err) {
      console.error('Failed to load alerts:', err);
      setError('Failed to load alerts. Please check your connection.');
      setApiHealthy(false);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  // Load alerts on component mount and when filters change
  useEffect(() => {
    loadAlerts();
  }, [loadAlerts]);

  // Apply search filter
  useEffect(() => {
    if (!filters.search) {
      setFilteredAlerts(alerts);
      return;
    }

    const searchLower = filters.search.toLowerCase();
    const filtered = alerts.filter(
      (alert) =>
        alert.userId.toLowerCase().includes(searchLower) ||
        alert.transactionId.toLowerCase().includes(searchLower)
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
    // Update local state
    setAlerts((prev) =>
      prev.map((alert) =>
        alert.id === alertId ? { ...alert, status: newStatus } : alert
      )
    );

    // Reload stats
    loadAlerts();
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
        <div className="dashboard__health">
          <span
            className={`health-indicator ${
              apiHealthy ? 'health-indicator--healthy' : 'health-indicator--unhealthy'
            }`}
          >
            {apiHealthy ? '● Online' : '● Offline'}
          </span>
        </div>
      </header>

      {/* Statistics Cards */}
      <section className="dashboard__stats">
        <StatCard
          title="Total Alerts"
          value={stats.total}
          icon="📊"
          color="primary"
        />

        <StatCard
          title="High Risk"
          value={stats.high}
          icon="🔴"
          color="danger"
          subtitle={`${((stats.high / Math.max(stats.total, 1)) * 100).toFixed(1)}% fraud`}
        />

        <StatCard
          title="Medium Risk"
          value={stats.medium}
          icon="🟡"
          color="warning"
        />

        <StatCard
          title="Low Risk"
          value={stats.low}
          icon="🟢"
          color="success"
        />

        <StatCard
          title="New Alerts"
          value={stats.new}
          icon="📬"
          color="primary"
        />
      </section>

      {/* Filter Bar */}
      <section className="dashboard__filters">
        <FilterBar
          onFilterChange={handleFilterChange}
          onSearchChange={handleSearchChange}
        />
      </section>

      {/* Alerts Table */}
      <section className="dashboard__table">
        <div className="dashboard__table-header">
          <h2>Recent Flagged Transactions</h2>
          <button
            className="btn btn--secondary"
            onClick={loadAlerts}
            disabled={loading}
          >
            {loading ? 'Refreshing...' : '🔄 Refresh'}
          </button>
        </div>

        <AlertsTable
          alerts={filteredAlerts}
          loading={loading}
          error={error}
          onRefresh={loadAlerts}
          onStatusUpdate={handleStatusUpdate}
        />
      </section>

      {/* Footer */}
      <footer className="dashboard__footer">
        <p>Last updated: {new Date().toLocaleTimeString()}</p>
        <p>Auto-refresh every 30 seconds</p>
      </footer>
    </div>
  );
};

export default Dashboard;
