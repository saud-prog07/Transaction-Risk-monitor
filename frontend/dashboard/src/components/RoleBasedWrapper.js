import React from 'react';
import { Navigate } from 'react-router-dom';
import { isAdmin } from '../utils/roleUtils';

/**
 * Role-Based Wrapper Component
 * Wraps components that require ADMIN role
 * Redirects to dashboard if user is not ADMIN
 */
const RoleBasedWrapper = ({ children }) => {
  if (!isAdmin()) {
    // Redirect to dashboard if user doesn't have ADMIN role
    return <Navigate to="/dashboard" replace />;
  }

  // Render wrapped component if user has ADMIN role
  return children;
};

export default RoleBasedWrapper;