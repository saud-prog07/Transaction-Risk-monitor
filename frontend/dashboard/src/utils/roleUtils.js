/**
 * Role Utility Functions
 * Handles checking user roles from JWT token
 */

import jwtDecode from 'jwt-decode';

/**
 * Decode JWT token and extract role
 * @returns {string|null} User role or null if no token
 */
export const getUserRole = () => {
  const token = localStorage.getItem('authToken');
  if (!token) return null;

  try {
    const decoded = jwtDecode(token);
    return decoded.role || null;
  } catch (error) {
    console.error('Error decoding JWT token:', error);
    return null;
  }
};

/**
 * Check if user has ADMIN role
 * @returns {boolean} true if user is ADMIN
 */
export const isAdmin = () => {
  return getUserRole() === 'ADMIN';
};

/**
 * Check if user has ANALYST role
 * @returns {boolean} true if user is ANALYST
 */
export const isAnalyst = () => {
  return getUserRole() === 'ANALYST';
};

/**
 * Check if user has any of the specified roles
 * @param {string[]} roles - Array of role strings to check
 * @returns {boolean} true if user has any of the specified roles
 */
export const hasAnyRole = (roles) => {
  const userRole = getUserRole();
  return userRole && roles.includes(userRole);
};