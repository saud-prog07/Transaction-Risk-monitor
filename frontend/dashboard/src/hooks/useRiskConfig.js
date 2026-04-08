import { useState, useEffect, useCallback, useRef } from 'react';
import configService from '../services/ConfigurationService';

/**
 * useRiskConfig Custom Hook
 * Manages risk analyzer configuration state and operations
 * 
 * @param {Object} options - Hook options
 * @param {number} options.refreshInterval - Auto-refresh interval in ms (0 to disable)
 * @param {boolean} options.autoFetch - Automatically fetch on mount
 * @returns {Object} Hook state and methods
 */
export const useRiskConfig = (options = {}) => {
  const {
    refreshInterval = 0,
    autoFetch = true,
  } = options;

  // Analyzer configuration state
  const [analyzers, setAnalyzers] = useState([]);
  const [thresholds, setThresholds] = useState([]);
  const [metrics, setMetrics] = useState({});

  // UI state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  // Refs for cleanup
  const refreshIntervalRef = useRef(null);
  const abortControllerRef = useRef(null);

  /**
   * Fetch analyzer configurations
   */
  const fetchAnalyzers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await configService.getAnalyzers();
      setAnalyzers(Array.isArray(data) ? data : data.data || []);
      setLastUpdated(new Date());
    } catch (err) {
      setError(err.message || 'Failed to fetch analyzers');
      console.error('Fetch analyzers error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Fetch risk thresholds
   */
  const fetchThresholds = useCallback(async () => {
    try {
      setError(null);

      const data = await configService.getThresholds();
      setThresholds(Array.isArray(data) ? data : data.data || []);
      setLastUpdated(new Date());
    } catch (err) {
      setError(err.message || 'Failed to fetch thresholds');
      console.error('Fetch thresholds error:', err);
    }
  }, []);

  /**
   * Fetch analyzer metrics
   */
  const fetchMetrics = useCallback(async () => {
    try {
      setError(null);

      const data = await configService.getMetrics();
      setMetrics(data || {});
      setLastUpdated(new Date());
    } catch (err) {
      setError(err.message || 'Failed to fetch metrics');
      console.error('Fetch metrics error:', err);
    }
  }, []);

  /**
   * Fetch all configurations
   */
  const fetchAll = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      await Promise.all([fetchAnalyzers(), fetchThresholds(), fetchMetrics()]);
    } catch (err) {
      setError(err.message || 'Failed to fetch configurations');
      console.error('Fetch all error:', err);
    } finally {
      setLoading(false);
    }
  }, [fetchAnalyzers, fetchThresholds, fetchMetrics]);

  /**
   * Update single analyzer
   */
  const updateAnalyzer = useCallback(async (analyzerId, config) => {
    try {
      setError(null);
      const updated = await configService.updateAnalyzer(analyzerId, config);

      setAnalyzers((prev) =>
        prev.map((analyzer) =>
          analyzer.id === analyzerId
            ? { ...analyzer, ...updated }
            : analyzer
        )
      );

      return updated;
    } catch (err) {
      const errorMsg = err.message || 'Failed to update analyzer';
      setError(errorMsg);
      throw err;
    }
  }, []);

  /**
   * Batch update analyzers
   */
  const batchUpdateAnalyzers = useCallback(async (updates) => {
    try {
      setError(null);
      const result = await configService.batchUpdateAnalyzers(updates);

      setAnalyzers((prev) => {
        const updateMap = new Map(updates.map((u) => [u.id, u.config]));
        return prev.map((analyzer) =>
          updateMap.has(analyzer.id)
            ? { ...analyzer, ...updateMap.get(analyzer.id) }
            : analyzer
        );
      });

      return result;
    } catch (err) {
      const errorMsg = err.message || 'Failed to batch update analyzers';
      setError(errorMsg);
      throw err;
    }
  }, []);

  /**
   * Reset analyzer to default
   */
  const resetAnalyzer = useCallback(async (analyzerId) => {
    try {
      setError(null);
      const reset = await configService.resetAnalyzer(analyzerId);

      setAnalyzers((prev) =>
        prev.map((analyzer) =>
          analyzer.id === analyzerId
            ? { ...analyzer, ...reset }
            : analyzer
        )
      );

      return reset;
    } catch (err) {
      const errorMsg = err.message || 'Failed to reset analyzer';
      setError(errorMsg);
      throw err;
    }
  }, []);

  /**
   * Update thresholds
   */
  const updateThresholds = useCallback(async (newThresholds) => {
    try {
      setError(null);
      const result = await configService.updateThresholds(newThresholds);

      setThresholds(Array.isArray(result) ? result : result.data || []);
      return result;
    } catch (err) {
      const errorMsg = err.message || 'Failed to update thresholds';
      setError(errorMsg);
      throw err;
    }
  }, []);

  /**
   * Validate configuration
   */
  const validateConfig = useCallback(async (config) => {
    try {
      const result = await configService.validateConfig(config);
      return result;
    } catch (err) {
      console.error('Validation error:', err);
      throw err;
    }
  }, []);

  /**
   * Export configuration
   */
  const exportConfig = useCallback(async () => {
    try {
      const blob = await configService.exportConfig();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `risk-config-${new Date().getTime()}.json`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      const errorMsg = err.message || 'Failed to export configuration';
      setError(errorMsg);
      throw err;
    }
  }, []);

  /**
   * Import configuration
   */
  const importConfig = useCallback(async (file) => {
    try {
      setError(null);
      const result = await configService.importConfig(file);
      await fetchAll();
      return result;
    } catch (err) {
      const errorMsg = err.message || 'Failed to import configuration';
      setError(errorMsg);
      throw err;
    }
  }, [fetchAll]);

  /**
   * Get analyzer by ID
   */
  const getAnalyzer = useCallback(
    (analyzerId) => analyzers.find((a) => a.id === analyzerId),
    [analyzers]
  );

  /**
   * Get analyzer metrics
   */
  const getAnalyzerMetrics = useCallback(
    (analyzerId) => metrics[analyzerId] || {},
    [metrics]
  );

  /**
   * Clear error
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  /**
   * Setup auto-refresh
   */
  useEffect(() => {
    if (autoFetch) {
      fetchAll();
    }

    if (refreshInterval > 0) {
      refreshIntervalRef.current = setInterval(fetchAll, refreshInterval);
    }

    return () => {
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current);
      }
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [autoFetch, refreshInterval, fetchAll]);

  return {
    // State
    analyzers,
    thresholds,
    metrics,
    loading,
    error,
    lastUpdated,

    // Analyzer operations
    fetchAnalyzers,
    updateAnalyzer,
    batchUpdateAnalyzers,
    resetAnalyzer,
    getAnalyzer,

    // Threshold operations
    fetchThresholds,
    updateThresholds,

    // Metrics operations
    fetchMetrics,
    getAnalyzerMetrics,

    // Configuration operations
    fetchAll,
    validateConfig,
    exportConfig,
    importConfig,

    // Utilities
    clearError,
  };
};

export default useRiskConfig;
