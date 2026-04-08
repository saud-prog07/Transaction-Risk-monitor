import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Dashboard from './components/Dashboard';
import AdminPanel from './components/AdminPanel';
import Login from './components/Login';
import { ProtectedRoute, ProtectedRoleRoute } from './components/ProtectedRoute';
import MetricsDashboard from './pages/MetricsDashboard';
import DLQDashboard from './pages/DLQDashboard';
import RoleBasedWrapper from './components/RoleBasedWrapper';
import GuidedDemoFlow from './components/GuidedDemoFlow';
import { initializeAuthentication } from './services/authService';
import './styles/index.css';

/**
 * App Component - Root component with routing
 * Sets up authentication and protected routes
 */
function App() {
  useEffect(() => {
    // Initialize authentication on app startup
    initializeAuthentication();
  }, []);

  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/demo" element={<GuidedDemoFlow />} />

        {/* Protected Routes */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin"
          element={
            <ProtectedRoleRoute>
              <AdminPanel />
            </ProtectedRoleRoute>
          }
        />

        {/* Metrics Dashboard */}
        <Route
          path="/metrics"
          element={
            <ProtectedRoute>
              <MetricsDashboard />
            </ProtectedRoute>
          }
        />
        
        {/* DLQ Dashboard */}
        <Route
          path="/dlq"
          element={
            <ProtectedRoute>
              <DLQDashboard />
            </ProtectedRoute>
          }
        />

        {/* Redirects */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
