import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8082';

/**
 * Authentication Service - Frontend
 * Handles user login and JWT token management
 */

/**
 * Login user with credentials
 * @param {string} username - User username
 * @param {string} password - User password
 * @returns {Promise} Login response with JWT token
 */
export const login = async (username, password) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/login`, {
      username,
      password,
    });

    const { token, tokenType, expiresIn, role } = response.data;

    if (token) {
      // Store token securely
      localStorage.setItem('authToken', token);
      localStorage.setItem('tokenType', tokenType || 'Bearer');
      localStorage.setItem('tokenExpiresAt', Date.now() + expiresIn * 1000);
      localStorage.setItem('userRole', role);

      // Set default authorization header
      setAuthorizationHeader(token);
    }

    return response.data;
  } catch (error) {
    const errorMessage = 
      error.response?.data?.message || 
      'Login failed. Please check your credentials.';
    throw new Error(errorMessage);
  }
};

/**
 * Logout user
 * Clears stored token and removes authorization header
 */
export const logout = () => {
  localStorage.removeItem('authToken');
  localStorage.removeItem('tokenType');
  localStorage.removeItem('tokenExpiresAt');
  
  // Remove authorization header
  delete axios.defaults.headers.common['Authorization'];
};

/**
 * Get stored authentication token
 * @returns {string|null} JWT token or null
 */
export const getAuthToken = () => {
  return localStorage.getItem('authToken');
};

/**
 * Check if user is authenticated
 * Validates token existence and expiration
 * @returns {boolean} true if user is authenticated
 */
export const isAuthenticated = () => {
  const token = getAuthToken();
  const expiresAt = localStorage.getItem('tokenExpiresAt');

  if (!token || !expiresAt) {
    return false;
  }

  // Check if token has expired
  if (Date.now() > parseInt(expiresAt)) {
    logout();
    return false;
  }

  return true;
};

/**
 * Set authorization header for axios requests
 * @param {string} token - JWT token
 */
export const setAuthorizationHeader = (token) => {
  if (token) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete axios.defaults.headers.common['Authorization'];
  }
};

/**
 * Initialize authentication on app startup
 * Sets authorization header if token exists
 */
export const initializeAuthentication = () => {
  const token = getAuthToken();
  
  if (token && isAuthenticated()) {
    setAuthorizationHeader(token);
  } else {
    logout();
  }
};

/**
 * Get time remaining until token expiration
 * @returns {number} Milliseconds until expiration
 */
export const getTokenExpirationTime = () => {
  const expiresAt = localStorage.getItem('tokenExpiresAt');
  
  if (!expiresAt) {
    return 0;
  }

  const timeRemaining = parseInt(expiresAt) - Date.now();
  return Math.max(0, timeRemaining);
};

/**
 * Check if token is about to expire (within 5 minutes)
 * @returns {boolean} true if token expires soon
 */
export const isTokenExpiringSOon = () => {
  const timeRemaining = getTokenExpirationTime();
  const fiveMinutesInMs = 5 * 60 * 1000;
  return timeRemaining < fiveMinutesInMs && timeRemaining > 0;
};
