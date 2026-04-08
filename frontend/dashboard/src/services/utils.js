/**
 * Format date to readable string
 * @param {string|Date} dateString - ISO date string or Date object
 * @returns {string} Formatted date
 */
export const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

/**
 * Format currency amount
 * @param {number} amount - Amount to format
 * @returns {string} Formatted currency
 */
export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined) return '$0.00';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
};

/**
 * Get color for risk level
 * @param {string} riskLevel - Risk level (LOW, MEDIUM, HIGH)
 * @returns {Object} Color object with bg and text colors
 */
export const getRiskLevelColor = (riskLevel) => {
  const colors = {
    LOW: { background: '#d4edda', text: '#155724', border: '#c3e6cb' },
    MEDIUM: { background: '#fff3cd', text: '#856404', border: '#ffeaa7' },
    HIGH: { background: '#f8d7da', text: '#721c24', border: '#f5c6cb' },
  };
  return colors[riskLevel] || colors.LOW;
};

/**
 * Get status badge color
 * @param {string} status - Status (NEW, REVIEWED, RESOLVED)
 * @returns {Object} Color object
 */
export const getStatusColor = (status) => {
  const colors = {
    NEW: { background: '#cfe2ff', text: '#084298' },
    REVIEWED: { background: '#d1e7dd', text: '#0f5132' },
    RESOLVED: { background: '#e2e3e5', text: '#383d41' },
  };
  return colors[status] || colors.NEW;
};

/**
 * Parse risk score to risk level
 * @param {number} score - Risk score (0-100)
 * @returns {string} Risk level
 */
export const scoreToRiskLevel = (score) => {
  if (score >= 80) return 'HIGH';
  if (score >= 50) return 'MEDIUM';
  return 'LOW';
};

/**
 * Truncate text to specified length
 * @param {string} text - Text to truncate
 * @param {number} length - Max length
 * @returns {string} Truncated text
 */
export const truncateText = (text, length = 50) => {
  if (!text) return '';
  return text.length > length ? text.substring(0, length) + '...' : text;
};
