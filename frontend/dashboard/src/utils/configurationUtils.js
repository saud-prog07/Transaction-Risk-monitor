/**
 * Configuration Utilities
 * Helper functions for risk configuration management
 */

/**
 * Validator utilities
 */
export const validators = {
  /**
   * Validate analyzer configuration
   */
  validateAnalyzer: (analyzer) => {
    const errors = {};

    if (!analyzer.name || analyzer.name.trim() === '') {
      errors.name = 'Analyzer name is required';
    }

    if (typeof analyzer.weight !== 'number' || analyzer.weight < 0 || analyzer.weight > 1) {
      errors.weight = 'Weight must be a number between 0 and 1';
    }

    if (typeof analyzer.threshold !== 'number' || analyzer.threshold < 0 || analyzer.threshold > 100) {
      errors.threshold = 'Threshold must be a number between 0 and 100';
    }

    return errors;
  },

  /**
   * Validate threshold configuration
   */
  validateThreshold: (threshold) => {
    const errors = [];

    if (!threshold.name || threshold.name.trim() === '') {
      errors.push('Threshold name is required');
    }

    if (threshold.low < 0 || threshold.low > 100) {
      errors.push('Low threshold must be between 0 and 100');
    }

    if (threshold.medium < 0 || threshold.medium > 100) {
      errors.push('Medium threshold must be between 0 and 100');
    }

    if (threshold.high < 0 || threshold.high > 100) {
      errors.push('High threshold must be between 0 and 100');
    }

    if (threshold.critical < 0 || threshold.critical > 100) {
      errors.push('Critical threshold must be between 0 and 100');
    }

    // Validate ordering
    if (threshold.low >= threshold.medium) {
      errors.push('Low threshold must be less than medium');
    }

    if (threshold.medium >= threshold.high) {
      errors.push('Medium threshold must be less than high');
    }

    if (threshold.high >= threshold.critical) {
      errors.push('High threshold must be less than critical');
    }

    return errors;
  },

  /**
   * Validate email
   */
  validateEmail: (email) => {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
  },

  /**
   * Validate URL
   */
  validateUrl: (url) => {
    try {
      new URL(url);
      return true;
    } catch {
      return false;
    }
  },
};

/**
 * Formatter utilities
 */
export const formatters = {
  /**
   * Format number with decimals
   */
  formatNumber: (value, decimals = 2) => {
    if (typeof value !== 'number') return 'N/A';
    return value.toFixed(decimals);
  },

  /**
   * Format percentage
   */
  formatPercentage: (value) => {
    if (typeof value !== 'number') return 'N/A';
    return `${value.toFixed(1)}%`;
  },

  /**
   * Format milliseconds to readable time
   */
  formatTime: (ms) => {
    if (ms < 1000) return `${Math.round(ms)}ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`;
    return `${(ms / 60000).toFixed(2)}m`;
  },

  /**
   * Format date time
   */
  formatDateTime: (date) => {
    if (!(date instanceof Date)) {
      date = new Date(date);
    }
    return date.toLocaleString();
  },

  /**
   * Format date only
   */
  formatDate: (date) => {
    if (!(date instanceof Date)) {
      date = new Date(date);
    }
    return date.toLocaleDateString();
  },

  /**
   * Format time only
   */
  formatTimeOnly: (date) => {
    if (!(date instanceof Date)) {
      date = new Date(date);
    }
    return date.toLocaleTimeString();
  },

  /**
   * Format large numbers with abbreviations
   */
  formatLargeNumber: (num) => {
    if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
    if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
    return num.toString();
  },
};

/**
 * Risk level utilities
 */
export const riskLevels = {
  /**
   * Get risk level from score
   */
  getRiskLevel: (score) => {
    if (score < 20) return 'LOW';
    if (score < 40) return 'MEDIUM';
    if (score < 60) return 'HIGH';
    return 'CRITICAL';
  },

  /**
   * Get risk level label
   */
  getRiskLevelLabel: (score) => {
    const level = riskLevels.getRiskLevel(score);
    return level.charAt(0) + level.slice(1).toLowerCase();
  },

  /**
   * Get risk level color
   */
  getRiskLevelColor: (score) => {
    if (score < 20) return '#10b981'; // Green
    if (score < 40) return '#f59e0b'; // Amber
    if (score < 60) return '#ef4444'; // Red
    return '#8b0000'; // Dark red
  },

  /**
   * Get risk level icon
   */
  getRiskLevelIcon: (score) => {
    if (score < 20) return '✓';
    if (score < 40) return '⚠️';
    if (score < 60) return '🔴';
    return '🚨';
  },

  /**
   * Get background color for risk level
   */
  getRiskLevelBackground: (score, alpha = 0.1) => {
    const color = riskLevels.getRiskLevelColor(score);
    // Convert hex to rgba
    const hex = color.replace('#', '');
    const r = parseInt(hex.substr(0, 2), 16);
    const g = parseInt(hex.substr(2, 2), 16);
    const b = parseInt(hex.substr(4, 2), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  },
};

/**
 * Configuration utilities
 */
export const configUtils = {
  /**
   * Deep clone configuration object
   */
  cloneConfig: (config) => {
    return JSON.parse(JSON.stringify(config));
  },

  /**
   * Merge configurations
   */
  mergeConfigs: (base, override) => {
    return {
      ...base,
      ...override,
      config: {
        ...base.config,
        ...override.config,
      },
    };
  },

  /**
   * Get configuration differences
   */
  getConfigDiff: (original, modified) => {
    const diff = {};

    Object.keys(modified).forEach((key) => {
      if (JSON.stringify(original[key]) !== JSON.stringify(modified[key])) {
        diff[key] = {
          original: original[key],
          modified: modified[key],
        };
      }
    });

    return diff;
  },

  /**
   * Apply configuration defaults
   */
  applyDefaults: (config, defaults) => {
    return {
      ...defaults,
      ...config,
    };
  },

  /**
   * Sanitize configuration (remove sensitive data)
   */
  sanitizeConfig: (config) => {
    const sanitized = configUtils.cloneConfig(config);
    const sensitiveFields = ['password', 'token', 'secret', 'apiKey'];

    sensitiveFields.forEach((field) => {
      if (sanitized[field]) {
        sanitized[field] = '***';
      }
    });

    return sanitized;
  },

  /**
   * Sort analyzers by weight (descending)
   */
  sortAnalyzersByWeight: (analyzers) => {
    return [...analyzers].sort((a, b) => b.weight - a.weight);
  },

  /**
   * Sort analyzers by name
   */
  sortAnalyzersByName: (analyzers) => {
    return [...analyzers].sort((a, b) => a.name.localeCompare(b.name));
  },

  /**
   * Filter analyzers by enabled status
   */
  filterEnabledAnalyzers: (analyzers) => {
    return analyzers.filter((a) => a.enabled);
  },

  /**
   * Calculate analyzer weights sum
   */
  calculateTotalWeight: (analyzers) => {
    return analyzers.reduce((sum, a) => sum + (a.weight || 0), 0);
  },

  /**
   * Normalize analyzer weights
   */
  normalizeWeights: (analyzers) => {
    const total = configUtils.calculateTotalWeight(analyzers);
    if (total === 0) return analyzers;

    return analyzers.map((analyzer) => ({
      ...analyzer,
      weight: analyzer.weight / total,
    }));
  },

  /**
   * Get analyzer by ID
   */
  getAnalyzerById: (analyzers, id) => {
    return analyzers.find((a) => a.id === id);
  },

  /**
   * Find analyzers by type
   */
  getAnalyzersByType: (analyzers, type) => {
    return analyzers.filter((a) => a.type === type);
  },
};

/**
 * Performance utilities
 */
export const performanceUtils = {
  /**
   * Calculate average execution time
   */
  calculateAvgExecutionTime: (metrics) => {
    if (!metrics || metrics.length === 0) return 0;
    const sum = metrics.reduce((acc, m) => acc + m.executionTime, 0);
    return sum / metrics.length;
  },

  /**
   * Calculate percentile
   */
  calculatePercentile: (values, percentile) => {
    if (values.length === 0) return 0;
    const sorted = [...values].sort((a, b) => a - b);
    const index = Math.ceil((percentile / 100) * sorted.length) - 1;
    return sorted[Math.max(0, index)];
  },

  /**
   * Calculate standard deviation
   */
  calculateStdDeviation: (values) => {
    if (values.length < 2) return 0;
    const avg = values.reduce((a, b) => a + b) / values.length;
    const squareDiffs = values.map((v) => Math.pow(v - avg, 2));
    const avgSquareDiff = squareDiffs.reduce((a, b) => a + b) / values.length;
    return Math.sqrt(avgSquareDiff);
  },

  /**
   * Calculate error rate
   */
  calculateErrorRate: (totalRequests, errorCount) => {
    if (totalRequests === 0) return 0;
    return (errorCount / totalRequests) * 100;
  },

  /**
   * Determine health status
   */
  determineHealthStatus: (metrics) => {
    const errorRate = performanceUtils.calculateErrorRate(
      metrics.totalRequests,
      metrics.errorCount
    );

    if (errorRate > 10) return 'UNHEALTHY';
    if (errorRate > 5) return 'WARNING';
    if (metrics.avgExecutionTime > 1000) return 'SLOW';
    return 'HEALTHY';
  },

  /**
   * Calculate health score (0-100)
   */
  calculateHealthScore: (metrics) => {
    if (!metrics) return 0;

    let score = 100;

    // Deduct for error rate
    const errorRate = performanceUtils.calculateErrorRate(
      metrics.totalRequests,
      metrics.errorCount
    );
    score -= Math.min(errorRate, 20);

    // Deduct for slow execution
    if (metrics.avgExecutionTime > 1000) {
      score -= 10;
    } else if (metrics.avgExecutionTime > 500) {
      score -= 5;
    }

    // Deduct for low uptime
    if (metrics.uptime && metrics.uptime < 99) {
      score -= (100 - metrics.uptime) / 10;
    }

    return Math.max(0, Math.min(100, score));
  },
};

/**
 * Data export utilities
 */
export const exportUtils = {
  /**
   * Export data as JSON file
   */
  exportJSON: (data, filename = 'export.json') => {
    const jsonString = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonString], { type: 'application/json' });
    exportUtils.downloadBlob(blob, filename);
  },

  /**
   * Export data as CSV
   */
  exportCSV: (data, filename = 'export.csv') => {
    if (!Array.isArray(data) || data.length === 0) return;

    const headers = Object.keys(data[0]);
    const csvContent = [
      headers.join(','),
      ...data.map((row) =>
        headers.map((header) => {
          const value = row[header];
          if (typeof value === 'string' && value.includes(',')) {
            return `"${value}"`;
          }
          return value;
        }).join(',')
      ),
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    exportUtils.downloadBlob(blob, filename);
  },

  /**
   * Download blob as file
   */
  downloadBlob: (blob, filename) => {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  },

  /**
   * Read file as JSON
   */
  readJSONFile: async (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const data = JSON.parse(e.target.result);
          resolve(data);
        } catch (err) {
          reject(new Error('Invalid JSON file'));
        }
      };
      reader.onerror = () => reject(new Error('File read error'));
      reader.readAsText(file);
    });
  },
};

/**
 * Comparison utilities
 */
export const comparisonUtils = {
  /**
   * Compare two analyzer configurations
   */
  compareAnalyzers: (analyzer1, analyzer2) => {
    const comparison = {
      identical: true,
      differences: [],
    };

    Object.keys(analyzer1).forEach((key) => {
      if (analyzer1[key] !== analyzer2[key]) {
        comparison.identical = false;
        comparison.differences.push({
          field: key,
          before: analyzer1[key],
          after: analyzer2[key],
        });
      }
    });

    return comparison;
  },

  /**
   * Calculate similarity score between two configs
   */
  calculateSimilarity: (config1, config2) => {
    const keys = new Set([...Object.keys(config1), ...Object.keys(config2)]);
    let matches = 0;

    keys.forEach((key) => {
      if (JSON.stringify(config1[key]) === JSON.stringify(config2[key])) {
        matches++;
      }
    });

    return (matches / keys.size) * 100;
  },
};

export default {
  validators,
  formatters,
  riskLevels,
  configUtils,
  performanceUtils,
  exportUtils,
  comparisonUtils,
};
