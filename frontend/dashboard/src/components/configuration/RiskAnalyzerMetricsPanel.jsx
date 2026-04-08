import React, { useState, useEffect, useCallback } from 'react';
import './RiskAnalyzerMetricsPanel.css';

/**
 * RiskAnalyzerMetricsPanel Component
 * Displays real-time metrics and performance statistics for risk analyzers
 * 
 * @component
 * @param {Object} props
 * @param {Array} props.analyzers - Array of analyzer configurations
 * @param {Object} props.metrics - Metrics data for analyzers
 * @param {boolean} props.isLoading - Loading state indicator
 * @param {string} props.error - Error message if any
 * @param {Function} props.onRefresh - Callback to refresh metrics
 * @returns {JSX.Element}
 */
const RiskAnalyzerMetricsPanel = ({
  analyzers = [],
  metrics = {},
  isLoading = false,
  error = null,
  onRefresh,
}) => {
  const [selectedTab, setSelectedTab] = useState('overview');
  const [expandedAnalyzers, setExpandedAnalyzers] = useState(new Set());

  /**
   * Format number with decimals
   */
  const formatNumber = (value, decimals = 2) => {
    return typeof value === 'number' ? value.toFixed(decimals) : 'N/A';
  };

  /**
   * Format percentage
   */
  const formatPercentage = (value) => {
    return typeof value === 'number' ? `${value.toFixed(1)}%` : 'N/A';
  };

  /**
   * Get status color based on value
   */
  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'active':
      case 'healthy':
        return '#10b981';
      case 'warning':
        return '#f59e0b';
      case 'error':
      case 'unhealthy':
        return '#ef4444';
      default:
        return '#64748b';
    }
  };

  /**
   * Toggle analyzer expansion
   */
  const toggleAnalyzerExpansion = useCallback((analyzerId) => {
    setExpandedAnalyzers((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(analyzerId)) {
        newSet.delete(analyzerId);
      } else {
        newSet.add(analyzerId);
      }
      return newSet;
    });
  }, []);

  /**
   * Calculate overall statistics
   */
  const calculateOverallStats = useCallback(() => {
    if (!analyzers.length) return null;

    let totalRequests = 0;
    let totalErrors = 0;
    let totalAvgTime = 0;
    let activeAnalyzers = 0;

    analyzers.forEach((analyzer) => {
      const analyzerMetrics = metrics[analyzer.id] || {};
      if (analyzerMetrics.enabled) {
        activeAnalyzers++;
        totalRequests += analyzerMetrics.requestCount || 0;
        totalErrors += analyzerMetrics.errorCount || 0;
        totalAvgTime += analyzerMetrics.avgExecutionTime || 0;
      }
    });

    return {
      totalRequests,
      totalErrors,
      averageErrorRate: totalRequests > 0 ? ((totalErrors / totalRequests) * 100).toFixed(2) : 0,
      averageExecutionTime: activeAnalyzers > 0 ? (totalAvgTime / activeAnalyzers).toFixed(2) : 0,
      activeAnalyzers,
      totalAnalyzers: analyzers.length,
    };
  }, [analyzers, metrics]);

  const overallStats = calculateOverallStats();

  if (isLoading) {
    return (
      <div className="metrics-panel">
        <div className="metrics-loading">Loading metrics...</div>
      </div>
    );
  }

  return (
    <div className="metrics-panel">
      {error && (
        <div className="metrics-error">
          <span className="error-icon">⚠️</span>
          <span className="error-text">{error}</span>
        </div>
      )}

      <div className="metrics-header">
        <h3>Risk Analyzer Metrics</h3>
        <button className="btn-refresh" onClick={onRefresh} title="Refresh metrics">
          🔄
        </button>
      </div>

      {/* Tabs */}
      <div className="metrics-tabs">
        <button
          className={`tab-button ${selectedTab === 'overview' ? 'active' : ''}`}
          onClick={() => setSelectedTab('overview')}
        >
          📊 Overview
        </button>
        <button
          className={`tab-button ${selectedTab === 'analyzers' ? 'active' : ''}`}
          onClick={() => setSelectedTab('analyzers')}
        >
          🔍 Analyzer Details
        </button>
        <button
          className={`tab-button ${selectedTab === 'health' ? 'active' : ''}`}
          onClick={() => setSelectedTab('health')}
        >
          🏥 Health Status
        </button>
      </div>

      {/* Overview Tab */}
      {selectedTab === 'overview' && (
        <div className="tab-content">
          {overallStats && (
            <div className="overview-stats">
              <div className="stat-card">
                <div className="stat-label">Total Requests</div>
                <div className="stat-value">{overallStats.totalRequests}</div>
                <div className="stat-change">
                  {metrics[analyzers[0]?.id]?.requestTrend >= 0 ? '↑' : '↓'} This hour
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Error Rate</div>
                <div className="stat-value" style={{ color: overallStats.averageErrorRate > 5 ? '#ef4444' : '#10b981' }}>
                  {overallStats.averageErrorRate}%
                </div>
                <div className="stat-change">
                  {overallStats.totalErrors} errors
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Avg Execution Time</div>
                <div className="stat-value">{overallStats.averageExecutionTime}ms</div>
                <div className="stat-change">Per request</div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Active Analyzers</div>
                <div className="stat-value" style={{ color: '#10b981' }}>
                  {overallStats.activeAnalyzers}/{overallStats.totalAnalyzers}
                </div>
                <div className="stat-change">
                  {overallStats.totalAnalyzers - overallStats.activeAnalyzers} inactive
                </div>
              </div>
            </div>
          )}

          {/* Charts Section */}
          <div className="metrics-section">
            <h4>Request Volume (Last 24 Hours)</h4>
            <div className="metrics-chart">
              <div className="chart-placeholder">
                📈 Chart visualization would go here
              </div>
            </div>
          </div>

          <div className="metrics-section">
            <h4>Error Rate Trend</h4>
            <div className="metrics-chart">
              <div className="chart-placeholder">
                📉 Trend analysis would go here
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Analyzers Details Tab */}
      {selectedTab === 'analyzers' && (
        <div className="tab-content">
          <div className="analyzers-list">
            {analyzers.map((analyzer) => {
              const analyzerMetrics = metrics[analyzer.id] || {};
              const isExpanded = expandedAnalyzers.has(analyzer.id);

              return (
                <div key={analyzer.id} className="analyzer-metrics-card">
                  <div
                    className="analyzer-metrics-header"
                    onClick={() => toggleAnalyzerExpansion(analyzer.id)}
                  >
                    <div className="analyzer-metrics-title">
                      <span className="expand-icon">{isExpanded ? '▼' : '▶'}</span>
                      <span className="analyzer-icon">{analyzer.icon || '🔍'}</span>
                      <span className="analyzer-name">{analyzer.name}</span>
                      <span
                        className="analyzer-status"
                        style={{
                          backgroundColor: getStatusColor(analyzerMetrics.status),
                        }}
                      >
                        {analyzerMetrics.enabled ? 'Active' : 'Inactive'}
                      </span>
                    </div>

                    <div className="analyzer-quick-stats">
                      <div className="quick-stat">
                        <span className="quick-label">Requests</span>
                        <span className="quick-value">{analyzerMetrics.requestCount || 0}</span>
                      </div>
                      <div className="quick-stat">
                        <span className="quick-label">Errors</span>
                        <span className="quick-value" style={{ color: analyzerMetrics.errorCount > 0 ? '#ef4444' : '#10b981' }}>
                          {analyzerMetrics.errorCount || 0}
                        </span>
                      </div>
                      <div className="quick-stat">
                        <span className="quick-label">Avg Time</span>
                        <span className="quick-value">
                          {formatNumber(analyzerMetrics.avgExecutionTime, 2)}ms
                        </span>
                      </div>
                    </div>
                  </div>

                  {isExpanded && (
                    <div className="analyzer-metrics-details">
                      <div className="details-grid">
                        <div className="detail-item">
                          <span className="detail-label">Total Requests</span>
                          <span className="detail-value">{analyzerMetrics.requestCount || 0}</span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Total Errors</span>
                          <span className="detail-value" style={{ color: analyzerMetrics.errorCount > 0 ? '#ef4444' : '#10b981' }}>
                            {analyzerMetrics.errorCount || 0}
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Error Rate</span>
                          <span className="detail-value">
                            {formatPercentage(
                              analyzerMetrics.requestCount
                                ? (analyzerMetrics.errorCount / analyzerMetrics.requestCount) * 100
                                : 0
                            )}
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Avg Execution Time</span>
                          <span className="detail-value">
                            {formatNumber(analyzerMetrics.avgExecutionTime)}ms
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Min Execution Time</span>
                          <span className="detail-value">
                            {formatNumber(analyzerMetrics.minExecutionTime)}ms
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Max Execution Time</span>
                          <span className="detail-value">
                            {formatNumber(analyzerMetrics.maxExecutionTime)}ms
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">P95 Latency</span>
                          <span className="detail-value">
                            {formatNumber(analyzerMetrics.p95Latency)}ms
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">P99 Latency</span>
                          <span className="detail-value">
                            {formatNumber(analyzerMetrics.p99Latency)}ms
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Last Updated</span>
                          <span className="detail-value">
                            {analyzerMetrics.lastUpdated
                              ? new Date(analyzerMetrics.lastUpdated).toLocaleTimeString()
                              : 'N/A'}
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Uptime</span>
                          <span className="detail-value" style={{ color: '#10b981' }}>
                            {formatPercentage(analyzerMetrics.uptime)}
                          </span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Health Status Tab */}
      {selectedTab === 'health' && (
        <div className="tab-content">
          <div className="health-grid">
            {analyzers.map((analyzer) => {
              const analyzerMetrics = metrics[analyzer.id] || {};
              const healthScore = Math.min(
                100,
                100 -
                  (analyzerMetrics.errorCount || 0) * 10 -
                  (analyzerMetrics.avgExecutionTime || 0) / 10
              );

              return (
                <div key={analyzer.id} className="health-card">
                  <div className="health-header">
                    <span className="analyzer-icon">{analyzer.icon || '🔍'}</span>
                    <span className="analyzer-name">{analyzer.name}</span>
                  </div>

                  <div className="health-score">
                    <div className="health-meter">
                      <div
                        className="health-fill"
                        style={{
                          width: `${healthScore}%`,
                          backgroundColor:
                            healthScore >= 80
                              ? '#10b981'
                              : healthScore >= 60
                              ? '#f59e0b'
                              : '#ef4444',
                        }}
                      />
                    </div>
                    <span className="health-percentage">{healthScore.toFixed(0)}%</span>
                  </div>

                  <div className="health-checks">
                    <div className="check-item">
                      <span className="check-status" style={{ color: analyzerMetrics.enabled ? '#10b981' : '#ef4444' }}>
                        ●
                      </span>
                      <span className="check-label">
                        {analyzerMetrics.enabled ? 'Active' : 'Inactive'}
                      </span>
                    </div>
                    <div className="check-item">
                      <span className="check-status" style={{ color: analyzerMetrics.errorCount === 0 ? '#10b981' : '#ef4444' }}>
                        ●
                      </span>
                      <span className="check-label">
                        {analyzerMetrics.errorCount === 0 ? 'No Errors' : `${analyzerMetrics.errorCount} Errors`}
                      </span>
                    </div>
                    <div className="check-item">
                      <span className="check-status" style={{ color: analyzerMetrics.avgExecutionTime < 100 ? '#10b981' : '#f59e0b' }}>
                        ●
                      </span>
                      <span className="check-label">
                        {analyzerMetrics.avgExecutionTime < 100 ? 'Fast' : 'Slow'} Response
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Info Section */}
      <div className="metrics-info">
        <h4>ℹ️ Metrics Information</h4>
        <ul>
          <li>
            <strong>Request Count:</strong> Total number of analysis requests processed
          </li>
          <li>
            <strong>Error Rate:</strong> Percentage of failed requests
          </li>
          <li>
            <strong>Execution Time:</strong> Time taken per analysis request
          </li>
          <li>
            <strong>P95/P99 Latency:</strong> 95th and 99th percentile response times
          </li>
          <li>
            <strong>Health Score:</strong> Overall analyzer health (based on errors and latency)
          </li>
        </ul>
      </div>
    </div>
  );
};

export default RiskAnalyzerMetricsPanel;
