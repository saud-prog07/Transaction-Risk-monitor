import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8082';

/**
 * Authentication Service - Frontend
 * Handles user login, registration, and JWT token management
 * 
 * SECURITY NOTES:
 * - Tokens are stored in memory (not localStorage) to prevent XSS attacks
 * - Refresh token should be stored in HTTPOnly cookie (set by backend)
 * - Never store sensitive data in localStorage
 * - Axios interceptors handle token attachment and refresh
 * - Logout clears all stored authentication data
 */

// Store tokens in memory (cleared on page refresh)
let accessToken = null;
let refreshToken = null;
let tokenExpiresAt = null;

/**
 * Login user with credentials
 * @param {string} username - User username
 * @param {string} password - User password
 * @returns {Promise} Login response with tokens
 */
export const login = async (username, password) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/login`, {
      username,
      password,
    });

    const { accessToken: newAccessToken, refreshToken: newRefreshToken, expiresIn } = response.data;

    if (newAccessToken) {
      // Store tokens in memory only (not localStorage)
      accessToken = newAccessToken;
      refreshToken = newRefreshToken;
      tokenExpiresAt = Date.now() + (expiresIn * 1000);

      // Set default authorization header
      setAuthorizationHeader(newAccessToken);
    }

    return response.data;
  } catch (error) {
    const errorMessage = 
      error.response?.data?.error || 
      'Login failed. Please check your credentials.';
    throw new Error(errorMessage);
  }
};

/**
 * Register new user
 * @param {string} username - Username
 * @param {string} email - Email address
 * @param {string} password - Password
 * @param {string} passwordConfirm - Password confirmation
 * @returns {Promise} Registration response
 */
export const register = async (username, email, password, passwordConfirm) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/register`, {
      username,
      email,
      password,
      passwordConfirm,
    });
    return response.data;
  } catch (error) {
    const errorMessage = 
      error.response?.data?.error || 
      'Registration failed. Please try again.';
    throw new Error(errorMessage);
  }
};

/**
 * Request password reset - sends email with reset link
 * @param {string} email - User email address
 * @returns {Promise} Reset request response
 */
export const requestPasswordReset = async (email) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/forgot-password`, {
      email,
    });
    return response.data;
  } catch (error) {
    const errorMessage = 
      error.response?.data?.error || 
      'Password reset request failed.';
    throw new Error(errorMessage);
  }
};

/**
 * Reset password with token
 * @param {string} token - Password reset token from email
 * @param {string} newPassword - New password
 * @param {string} newPasswordConfirm - New password confirmation
 * @returns {Promise} Reset response
 */
export const resetPassword = async (token, newPassword, newPasswordConfirm) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/reset-password`, {
      token,
      newPassword,
      newPasswordConfirm,
    });
    return response.data;
  } catch (error) {
    const errorMessage = 
      error.response?.data?.error || 
      'Password reset failed.';
    throw new Error(errorMessage);
  }
};

/**
 * Verify email with verification token
 * @param {string} token - Email verification token from email
 * @returns {Promise} Verification response
 */
export const verifyEmail = async (token) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/verify-email`, {
      token,
    });
    return response.data;
  } catch (error) {
    const errorMessage = 
      error.response?.data?.error || 
      'Email verification failed.';
    throw new Error(errorMessage);
  }
};

/**
 * Refresh access token using refresh token
 * @returns {Promise} New access token
 */
export const refreshAccessToken = async () => {
  try {
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
      refreshToken,
    });

    const { accessToken: newAccessToken, expiresIn } = response.data;
    
    if (newAccessToken) {
      accessToken = newAccessToken;
      tokenExpiresAt = Date.now() + (expiresIn * 1000);
      setAuthorizationHeader(newAccessToken);
    }

    return response.data;
  } catch (error) {
    // Refresh failed - need to re-login
    logout();
    throw new Error('Session expired. Please login again.');
  }
};

/**
 * Change password for authenticated user
 * @param {string} oldPassword - Current password
 * @param {string} newPassword - New password
 * @param {string} newPasswordConfirm - New password confirmation
 * @returns {Promise} Change password response
 */
export const changePassword = async (oldPassword, newPassword, newPasswordConfirm) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/change-password`, {
      oldPassword,
      newPassword,
      newPasswordConfirm,
    });
    return response.data;
  } catch (error) {
    const errorMessage = 
      error.response?.data?.error || 
      'Password change failed.';
    throw new Error(errorMessage);
  }
};

/**
 * Logout user - clears stored tokens and revokes refresh token
 */
export const logout = () => {
  // Revoke refresh token on backend
  if (refreshToken) {
    try {
      axios.post(`${API_BASE_URL}/api/auth/logout`, {
        refreshToken,
      });
    } catch (error) {
      console.error('Logout error:', error);
    }
  }

  // Clear all stored tokens
  accessToken = null;
  refreshToken = null;
  tokenExpiresAt = null;
  
  // Remove authorization header
  delete axios.defaults.headers.common['Authorization'];
};

/**
 * Get stored authentication token
 * @returns {string|null} JWT access token or null
 */
export const getAuthToken = () => {
  return accessToken;
};

/**
 * Get stored refresh token
 * @returns {string|null} Refresh token or null
 */
export const getRefreshToken = () => {
  return refreshToken;
};

/**
 * Check if user is authenticated
 * Validates token existence and expiration
 * @returns {boolean} true if user is authenticated
 */
export const isAuthenticated = () => {
  if (!accessToken || !tokenExpiresAt) {
    return false;
  }

  // Check if token has expired (with 1 minute buffer)
  const expirationBuffer = 60 * 1000; // 1 minute
  if (Date.now() > (tokenExpiresAt - expirationBuffer)) {
    logout();
    return false;
  }

  return true;
};

/**
 * Check if token will expire soon (within 5 minutes)
 * @returns {boolean} true if token will expire within 5 minutes
 */
export const willTokenExpireSoon = () => {
  if (!tokenExpiresAt) {
    return false;
  }

  const expirationSoonThreshold = 5 * 60 * 1000; // 5 minutes
  return Date.now() > (tokenExpiresAt - expirationSoonThreshold);
};

/**
 * Set authorization header for axios requests
 * @param {string} token - JWT access token
 */
export const setAuthorizationHeader = (token) => {
  if (token) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete axios.defaults.headers.common['Authorization'];
  }
};

/**
 * Initialize authentication interceptors
 * Automatically handles token refresh on 401 responses
 */
export const initializeAuthInterceptors = () => {
  // Response interceptor to handle token expiration
  axios.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config;

      // Handle 401 Unauthorized
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;

        try {
          // Attempt to refresh token
          await refreshAccessToken();
          
          // Retry original request with new token
          return axios(originalRequest);
        } catch (refreshError) {
          // Refresh failed - logout and redirect to login
          logout();
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      }

      return Promise.reject(error);
    }
  );
};

/**
 * Get remaining token expiration time in milliseconds
 * @returns {number} milliseconds until expiration, or 0 if expired
 */
export const getRemainingTokenTime = () => {
  if (!tokenExpiresAt) {
    return 0;
  }
  
  const remaining = tokenExpiresAt - Date.now();
  return Math.max(0, remaining);
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
