/**
 * ConfigurationService
 * Service for managing risk analyzer and threshold configurations
 * Communicates with backend API and caches configuration data
 */

class ConfigurationService {
  constructor(baseURL = '/api') {
    this.baseURL = baseURL;
    this.configCache = {};
    this.cacheExpiry = 5 * 60 * 1000; // 5 minutes
  }

  /**
   * Fetch all analyzer configurations
   * @returns {Promise<Array>}
   */
  async getAnalyzers() {
    const cacheKey = 'analyzers';
    if (this.isCacheValid(cacheKey)) {
      return this.configCache[cacheKey].data;
    }

    try {
      const response = await fetch(`${this.baseURL}/config/analyzers`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch analyzers: ${response.statusText}`);
      }

      const data = await response.json();
      this.setCache(cacheKey, data);
      return data;
    } catch (error) {
      console.error('Error fetching analyzers:', error);
      throw error;
    }
  }

  /**
   * Fetch specific analyzer configuration
   * @param {string} analyzerId - Analyzer ID
   * @returns {Promise<Object>}
   */
  async getAnalyzer(analyzerId) {
    try {
      const response = await fetch(`${this.baseURL}/config/analyzers/${analyzerId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch analyzer: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`Error fetching analyzer ${analyzerId}:`, error);
      throw error;
    }
  }

  /**
   * Update analyzer configuration
   * @param {string} analyzerId - Analyzer ID
   * @param {Object} config - Configuration object
   * @returns {Promise<Object>}
   */
  async updateAnalyzer(analyzerId, config) {
    try {
      const response = await fetch(`${this.baseURL}/config/analyzers/${analyzerId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify(config),
      });

      if (!response.ok) {
        throw new Error(`Failed to update analyzer: ${response.statusText}`);
      }

      const data = await response.json();
      this.invalidateCache(['analyzers', `analyzer-${analyzerId}`]);
      return data;
    } catch (error) {
      console.error(`Error updating analyzer ${analyzerId}:`, error);
      throw error;
    }
  }

  /**
   * Batch update multiple analyzers
   * @param {Array} updates - Array of { id, config } objects
   * @returns {Promise<Array>}
   */
  async batchUpdateAnalyzers(updates) {
    try {
      const response = await fetch(`${this.baseURL}/config/analyzers/batch`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify({ updates }),
      });

      if (!response.ok) {
        throw new Error(`Failed to batch update analyzers: ${response.statusText}`);
      }

      const data = await response.json();
      this.invalidateCache(['analyzers']);
      return data;
    } catch (error) {
      console.error('Error batch updating analyzers:', error);
      throw error;
    }
  }

  /**
   * Reset analyzer to default configuration
   * @param {string} analyzerId - Analyzer ID
   * @returns {Promise<Object>}
   */
  async resetAnalyzer(analyzerId) {
    try {
      const response = await fetch(
        `${this.baseURL}/config/analyzers/${analyzerId}/reset`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to reset analyzer: ${response.statusText}`);
      }

      const data = await response.json();
      this.invalidateCache(['analyzers', `analyzer-${analyzerId}`]);
      return data;
    } catch (error) {
      console.error(`Error resetting analyzer ${analyzerId}:`, error);
      throw error;
    }
  }

  /**
   * Fetch all risk thresholds
   * @returns {Promise<Array>}
   */
  async getThresholds() {
    const cacheKey = 'thresholds';
    if (this.isCacheValid(cacheKey)) {
      return this.configCache[cacheKey].data;
    }

    try {
      const response = await fetch(`${this.baseURL}/config/thresholds`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch thresholds: ${response.statusText}`);
      }

      const data = await response.json();
      this.setCache(cacheKey, data);
      return data;
    } catch (error) {
      console.error('Error fetching thresholds:', error);
      throw error;
    }
  }

  /**
   * Update risk thresholds
   * @param {Array} thresholds - Array of threshold configurations
   * @returns {Promise<Array>}
   */
  async updateThresholds(thresholds) {
    try {
      const response = await fetch(`${this.baseURL}/config/thresholds`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify({ thresholds }),
      });

      if (!response.ok) {
        throw new Error(`Failed to update thresholds: ${response.statusText}`);
      }

      const data = await response.json();
      this.invalidateCache(['thresholds']);
      return data;
    } catch (error) {
      console.error('Error updating thresholds:', error);
      throw error;
    }
  }

  /**
   * Fetch analyzer metrics
   * @param {string} analyzerId - Analyzer ID (optional)
   * @returns {Promise<Object>}
   */
  async getMetrics(analyzerId = null) {
    try {
      const url = analyzerId
        ? `${this.baseURL}/metrics/analyzers/${analyzerId}`
        : `${this.baseURL}/metrics/analyzers`;

      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch metrics: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching metrics:', error);
      throw error;
    }
  }

  /**
   * Validate analyzer configuration
   * @param {Object} config - Configuration to validate
   * @returns {Promise<Object>}
   */
  async validateConfig(config) {
    try {
      const response = await fetch(`${this.baseURL}/config/validate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify(config),
      });

      if (!response.ok) {
        throw new Error(`Validation failed: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error validating config:', error);
      throw error;
    }
  }

  /**
   * Export current configuration
   * @returns {Promise<Blob>}
   */
  async exportConfig() {
    try {
      const response = await fetch(`${this.baseURL}/config/export`, {
        method: 'GET',
      });

      if (!response.ok) {
        throw new Error(`Failed to export config: ${response.statusText}`);
      }

      return await response.blob();
    } catch (error) {
      console.error('Error exporting config:', error);
      throw error;
    }
  }

  /**
   * Import configuration from file
   * @param {File} file - Configuration file to import
   * @returns {Promise<Object>}
   */
  async importConfig(file) {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`${this.baseURL}/config/import`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Failed to import config: ${response.statusText}`);
      }

      const data = await response.json();
      this.invalidateCache(['analyzers', 'thresholds']);
      return data;
    } catch (error) {
      console.error('Error importing config:', error);
      throw error;
    }
  }

  /**
   * Get configuration audit trail
   * @param {Object} options - Query options (limit, offset, analyzerId, etc.)
   * @returns {Promise<Object>}
   */
  async getAuditTrail(options = {}) {
    try {
      const params = new URLSearchParams(options);
      const response = await fetch(
        `${this.baseURL}/config/audit?${params.toString()}`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch audit trail: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching audit trail:', error);
      throw error;
    }
  }

  /**
   * Revert configuration to previous version
   * @param {string} version - Version ID to revert to
   * @returns {Promise<Object>}
   */
  async revertConfig(version) {
    try {
      const response = await fetch(`${this.baseURL}/config/revert/${version}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to revert config: ${response.statusText}`);
      }

      const data = await response.json();
      this.invalidateCache();
      return data;
    } catch (error) {
      console.error('Error reverting config:', error);
      throw error;
    }
  }

  /**
   * Set cache entry
   * @private
   */
  setCache(key, data) {
    this.configCache[key] = {
      data,
      timestamp: Date.now(),
    };
  }

  /**
   * Check if cache is valid
   * @private
   */
  isCacheValid(key) {
    const entry = this.configCache[key];
    if (!entry) return false;

    const isValid = Date.now() - entry.timestamp < this.cacheExpiry;
    if (!isValid) {
      delete this.configCache[key];
    }
    return isValid;
  }

  /**
   * Invalidate cache
   * @private
   */
  invalidateCache(keys = null) {
    if (!keys) {
      this.configCache = {};
      return;
    }

    keys.forEach((key) => {
      delete this.configCache[key];
    });
  }

  /**
   * Clear all cache
   */
  clearCache() {
    this.configCache = {};
  }

  /**
   * Set cache expiry time
   * @param {number} milliseconds - Expiry time in milliseconds
   */
  setCacheExpiry(milliseconds) {
    this.cacheExpiry = milliseconds;
  }
}

// Create singleton instance
const configService = new ConfigurationService();

export default configService;
