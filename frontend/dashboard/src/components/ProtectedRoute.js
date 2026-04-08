import React from 'react';
import { Navigate } from 'react-router-dom';
import { isAuthenticated } from '../services/authService';
import { isAdmin } from '../utils/roleUtils';

/**
 * Protected Route Component
 * Wraps routes that require authentication
 * Redirects to login page if user is not authenticated
 * 
 * Usage:
 * <Route element={<ProtectedRoute><Dashboard /></ProtectedRoute>} path="/dashboard" />
 * <Route element={<ProtectedRole role="ADMIN"><AdminPanel /></ProtectedRole>} path="/admin" />
 */
const ProtectedRoute = ({ children, role }) => {
  // Check if user is authenticated
  if (!isAuthenticated()) {
    // Redirect to login page if not authenticated
    return <Navigate to="/login" replace />;
  }

  // If role is specified, check if user has that role
  if (role && !isAdmin()) {
    // Redirect to dashboard if user doesn't have required role
    return <Navigate to="/dashboard" replace />;
  }

  // Render protected component if authenticated and authorized
  return children;
};

/**
 * Role-Based Route Component
 * Specifically checks for ADMIN role
 */
const ProtectedRoleRoute = ({ children }) => {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin()) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
};

export { ProtectedRoute, ProtectedRoleRoute };
export default ProtectedRoute;
