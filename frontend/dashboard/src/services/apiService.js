import axios from 'axios';
import * as authService from './authService';

// API base URL - configure based on your environment
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8082';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Track if token refresh is in progress to prevent multiple refresh attempts
 */
let isRefreshing = false;
let failedQueue = [];

/**
 * Process queued requests after token refresh
 */
const processQueue = (error) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
  isRefreshing = false;
};

/**
 * Request interceptor - attach authorization token to all requests
 */
apiClient.interceptors.request.use(
  (config) => {
    const token = authService.getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor - handle token expiration and refresh
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const originalRequest = error.config;

    // Handle 401 Unauthorized - token expired
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Wait for refresh to complete
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => {
          return apiClient(originalRequest);
        }).catch((err) => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      return new Promise((resolve, reject) => {
        authService.refreshAccessToken()
          .then(() => {
            // Retry original request with new token
            apiClient(originalRequest)
              .then(response => resolve(response))
              .catch(err => reject(err));
          })
          .catch((err) => {
            // Refresh failed - redirect to login
            processQueue(err);
            authService.logout();
            window.location.href = '/login';
            reject(err);
          });
        
        processQueue(null);
      });
    }

    // Log all other errors
    if (error.response?.status !== 404) {
      console.error('API Error:', error.response?.data || error.message);
    }

    return Promise.reject(error);
  }
);

/**
 * Initialize authentication interceptors
 * Call this in App.js on component mount
 */
export const initializeApiClient = () => {
  const token = authService.getAuthToken();
  if (token) {
    authService.setAuthorizationHeader(token);
  }
};

/**
 * Fetch all alerts (flagged transactions)
 * @param {Object} params - Query parameters
 * @param {number} params.page - Page number (0-indexed)
 * @param {number} params.size - Items per page
 * @param {string} params.status - Filter by status (NEW, REVIEWED, RESOLVED)
 * @param {string} params.riskLevel - Filter by risk level (LOW, MEDIUM, HIGH)
 * @param {string} params.userId - Filter by user ID
 * @returns {Promise} Alerts data
 */
export const fetchAlerts = (params = {}) => {
  const queryParams = {
    page: params.page || 0,
    size: params.size || 20,
    ...params,
  };

  return apiClient.get('/api/alerts', { params: queryParams });
};

/**
 * Get alert by ID
 * @param {string} alertId - Alert ID
 * @returns {Promise} Alert data
 */
export const fetchAlertById = (alertId) => {
  return apiClient.get(`/api/alerts/${alertId}`);
};

/**
 * Update alert status
 * @param {string} alertId - Alert ID
 * @param {string} status - New status (NEW, REVIEWED, RESOLVED)
 * @returns {Promise} Updated alert data
 */
export const updateAlertStatus = (alertId, status) => {
  return apiClient.put(`/api/alerts/${alertId}/status`, { status });
};

/**
 * Get dashboard statistics (aggregated from alerts)
 * Calculates stats based on fetched alerts
 * @returns {Promise} Statistics data (total alerts, by risk level, etc.)
 */
export const fetchDashboardStats = async (params = {}) => {
  try {
    const response = await apiClient.get('/api/alerts', {
      params: {
        page: 0,
        size: 1000, // Fetch more to get accurate stats
        ...params,
      },
    });

    const alerts = response.data.content || response.data || [];

    // Calculate business impact metrics
    const totalFraudAmount = alerts
      .filter((alert) => alert.riskLevel === 'HIGH')
      .reduce((sum, alert) => sum + (parseFloat(alert.amount) || 0), 0);
    
    const suspiciousPercentage = alerts.length > 0 
      ? ((alerts.filter((alert) => alert.riskLevel !== 'LOW').length / alerts.length) * 100).toFixed(1)
      : 0;
    
    const totalRiskScore = alerts
      .reduce((sum, alert) => sum + (parseFloat(alert.riskScore) || 0), 0);
    const averageRiskScore = alerts.length > 0 
      ? (totalRiskScore / alerts.length).toFixed(2)
      : '0.00';

    // Calculate statistics
    const stats = {
      total: alerts.length,
      high: alerts.filter((a) => a.riskLevel === 'HIGH').length,
      medium: alerts.filter((a) => a.riskLevel === 'MEDIUM').length,
      low: alerts.filter((a) => a.riskLevel === 'LOW').length,
      new: alerts.filter((a) => a.status === 'NEW').length,
      reviewed: alerts.filter((a) => a.status === 'REVIEWED').length,
      resolved: alerts.filter((a) => a.status === 'RESOLVED').length,
      flaggedPercentage:
        alerts.length > 0
          ? ((alerts.filter((a) => a.riskLevel === 'HIGH').length /
              alerts.length) *
              100).toFixed(1)
          : 0,
      // Business impact metrics
      totalFraudAmount: totalFraudAmount.toFixed(2),
      suspiciousPercentage: suspiciousPercentage,
      averageRiskScore: averageRiskScore,
    };

    // Calculate trends (simulated for demo - in production would come from backend)
    const fraudAmountTrend = Array.from({ length: 7 }, (_, i) => ({
      date: new Date(Date.now() - i * 24 * 60 * 60 * 1000).toLocaleDateString('en-US', { month: '/', day: '/' }),
      value: Math.max(0, totalFraudAmount + (Math.random() - 0.5) * 1000)
    })).reverse();
    
    const riskScoreTrend = Array.from({ length: 7 }, (_, i) => ({
      date: new Date(Date.now() - i * 24 * 60 * 60 * 1000).toLocaleDateString('en-US', { month: '/', day: '/' }),
      value: Math.max(0, parseFloat(averageRiskScore) + (Math.random() - 0.5) * 0.5)
    })).reverse();

    return { 
      data: { 
        ...stats,
        fraudAmountTrend,
        riskScoreTrend
      }, 
      raw: { ...response.data, content: alerts } 
    };
  } catch (error) {
    console.error('Failed to fetch statistics:', error);
    return {
      data: {
        total: 0,
        high: 0,
        medium: 0,
        low: 0,
        new: 0,
        reviewed: 0,
        resolved: 0,
        flaggedPercentage: 0,
        totalFraudAmount: '0.00',
        suspiciousPercentage: '0.0',
        averageRiskScore: '0.00',
        fraudAmountTrend: [],
        riskScoreTrend: []
      },
      raw: { content: [] },
    };
  }
};

/**
 * Health check for API connectivity
 * @returns {Promise} Health status
 */
export const healthCheck = () => {
  return apiClient.get('/api/actuator/health');
};

/**
 * Fetch system health status for dashboard
 * @returns {Promise} System health data including MQ, DB, and service status
 */
export const fetchSystemHealth = () => {
  return apiClient.get('/api/health');
};

/**
 * Submit a single transaction to the backend
 * Used for testing and demo purposes via the TransactionSimulator
 * @param {Object} transactionData - Transaction data
 * @param {string} transactionData.transactionId - Unique transaction ID
 * @param {string} transactionData.accountId - Account ID
 * @param {number} transactionData.amount - Transaction amount
 * @param {string} transactionData.merchantId - Merchant ID
 * @param {string} transactionData.merchantName - Merchant name
 * @param {string} transactionData.timestamp - Transaction timestamp (ISO 8601)
 * @param {string} transactionData.location - Transaction location
 * @param {string} transactionData.deviceId - Device ID
 * @param {string} transactionData.channel - Channel (ONLINE, ATM, POS, MOBILE)
 * @returns {Promise} Response with transaction ID
 */
export const submitTransaction = (transactionData) => {
  return apiClient.post('/api/transactions', transactionData);
};

/**
 * Submit multiple transactions in batch
 * @param {Array<Object>} transactions - Array of transaction objects
 * @returns {Promise} Batch submission response
 */
export const submitBatchTransactions = (transactions) => {
  return apiClient.post('/api/transactions/batch', {
    transactions: transactions,
  });
};

/**
 * Format API error for display
 * @param {Error} error - Axios error
 * @returns {string} Formatted error message
 */
export const getErrorMessage = (error) => {
  if (!error) return 'An unknown error occurred';

  if (error.response) {
    // Server responded with error status
    const status = error.response.status;
    const message = error.response.data?.message;

    if (status === 404) return 'Resource not found';
    if (status === 400) return `Invalid request: ${message || 'Bad request'}`;
    if (status === 401) return 'Unauthorized access';
    if (status === 403) return 'Access forbidden';
    if (status === 500) return 'Server error. Please try again later';

    return message || `Server error (${status})`;
  } else if (error.request) {
    // Request made but no response
    return 'No response from server. Please check your connection.';
  } else {
    // Error in request setup
    return error.message || 'Failed to make request';
  }
};

/**
 * Retry logic for failed API calls
 * @param {Function} fn - Function to retry
 * @param {number} maxRetries - Maximum number of retries
 * @param {number} delayMs - Delay between retries
 * @returns {Promise} Result of function
 */
export const retryApiCall = async (fn, maxRetries = 3, delayMs = 1000) => {
  let lastError;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      console.warn(`API call failed (attempt ${attempt}/${maxRetries}):`, error.message);

      if (attempt < maxRetries) {
        await new Promise((resolve) => setTimeout(resolve, delayMs * attempt));
      }
    }
  }

  throw lastError;
};

export default apiClient;
