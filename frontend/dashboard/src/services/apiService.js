import axios from 'axios';

// API base URL - configure based on your environment
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8082';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

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
 * Get dashboard statistics
 * @returns {Promise} Statistics data (total alerts, by risk level, etc.)
 */
export const fetchDashboardStats = () => {
  // This endpoint should be created in your backend
  return apiClient.get('/api/alerts/stats');
};

/**
 * Health check for API connectivity
 * @returns {Promise} Health status
 */
export const healthCheck = () => {
  return apiClient.get('/api/actuator/health');
};

export default apiClient;
