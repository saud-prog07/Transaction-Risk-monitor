import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, isAuthenticated } from '../services/authService';
import '../styles/Login.css';

/**
 * Login Component
 * Provides user login functionality with JWT authentication
 */
const Login = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  // Redirect to dashboard if already authenticated
  useEffect(() => {
    if (isAuthenticated()) {
      navigate('/dashboard');
    }
  }, [navigate]);

  /**
   * Handle form submission
   * @param {Event} e - Form submit event
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      // Validate inputs
      if (!username.trim() || !password.trim()) {
        setError('Please enter both username and password');
        setLoading(false);
        return;
      }

      // Attempt login
      await login(username, password);

      // Clear form
      setUsername('');
      setPassword('');

      // Redirect to dashboard
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Login failed. Please try again.');
      console.error('Login error:', err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle demo credentials (for testing)
   */
  const handleDemoLogin = () => {
    setUsername('admin');
    setPassword('admin123');
  };

  return (
    <div className="login-container">
      <div className="login-wrapper">
        {/* Header */}
        <div className="login-header">
          <h1 className="login-title">🛡️ Risk Monitor</h1>
          <p className="login-subtitle">
            Real-time Transaction Risk Monitoring System
          </p>
        </div>

        {/* Login Form */}
        <form className="login-form" onSubmit={handleSubmit}>
          <h2 className="login-form-title">Sign In</h2>

          {/* Error Message */}
          {error && (
            <div className="login-error-message">
              <span className="error-icon">⚠️</span>
              <span className="error-text">{error}</span>
            </div>
          )}

          {/* Username Field */}
          <div className="login-form-group">
            <label htmlFor="username" className="login-label">
              Username
            </label>
            <input
              id="username"
              type="text"
              className="login-input"
              placeholder="Enter your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              autoComplete="username"
              required
            />
          </div>

          {/* Password Field */}
          <div className="login-form-group">
            <label htmlFor="password" className="login-label">
              Password
            </label>
            <input
              id="password"
              type="password"
              className="login-input"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
              autoComplete="current-password"
              required
            />
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            className="login-submit-btn"
            disabled={loading}
          >
            {loading ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        {/* Demo Credentials */}
        <div className="login-demo-section">
          <p className="login-demo-text">
            Try demo login with credentials:
          </p>
          <div className="login-demo-credentials">
            <code>Username: admin</code>
            <code>Password: admin123</code>
          </div>
          <button
            type="button"
            className="login-demo-btn"
            onClick={handleDemoLogin}
            disabled={loading}
          >
            Use Demo Credentials
          </button>
        </div>

        {/* Footer */}
        <div className="login-footer">
          <p>
            🔐 Secure authentication with JWT tokens
          </p>
          <p>
            Your credentials are validated securely on the server
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
