import React, { createContext, useContext, useCallback, useState, useEffect } from 'react';
import useWebSocket from '../hooks/useWebSocket';

/**
 * WebSocket Context
 * Provides centralized WebSocket management for the entire React application
 */
const WebSocketContext = createContext(null);

/**
 * WebSocket Provider Component
 * Wrap your app or specific sections with this provider to enable WebSocket functionality
 * 
 * @param {Object} props - Component props
 * @param {React.ReactNode} props.children - Child components
 * @param {Object} props.options - WebSocket configuration options
 * @returns {JSX.Element} Provider component
 */
export const WebSocketProvider = ({ children, options = {} }) => {
  const [alerts, setAlerts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [wsStats, setWsStats] = useState({
    activeSessions: 0,
    alertSubscribers: 0,
    transactionSubscribers: 0
  });

  // WebSocket message handlers
  const messageCallbacks = {
    'ALERT': (message) => {
      console.log('Received alert:', message.data);
      setAlerts(prev => [message.data, ...prev.slice(0, 99)]); // Keep last 100 alerts
    },
    'TRANSACTION': (message) => {
      console.log('Received transaction:', message.data);
      setTransactions(prev => [message.data, ...prev.slice(0, 99)]); // Keep last 100 transactions
    },
    'CONNECTION_CONFIRMED': (message) => {
      console.log('WebSocket connection confirmed');
    },
    'PONG': (message) => {
      // Handle server pong response (heartbeat)
      console.debug('Heartbeat response received');
    },
    onMessage: (message) => {
      console.debug('WebSocket message received:', message);
    }
  };

  const webSocket = useWebSocket({
    ...options,
    callbacks: messageCallbacks,
    onConnect: () => {
      console.log('WebSocket connected');
      // Subscribe to both alerts and transactions on connect
      webSocket.subscribeToAlerts();
      webSocket.subscribeToTransactions();
    },
    onDisconnect: () => {
      console.log('WebSocket disconnected');
    },
    onError: (error) => {
      console.error('WebSocket error:', error);
    }
  });

  /**
   * Fetch WebSocket statistics from server
   */
  const fetchWebSocketStats = useCallback(async () => {
    try {
      const response = await fetch('/api/websocket/stats');
      if (response.ok) {
        const stats = await response.json();
        setWsStats(stats);
      }
    } catch (err) {
      console.error('Error fetching WebSocket stats:', err);
    }
  }, []);

  /**
   * Refresh stats periodically
   */
  useEffect(() => {
    if (webSocket.isConnected) {
      fetchWebSocketStats();
      const interval = setInterval(fetchWebSocketStats, 10000); // Update every 10 seconds
      return () => clearInterval(interval);
    }
  }, [webSocket.isConnected, fetchWebSocketStats]);

  /**
   * Clear alerts
   */
  const clearAlerts = useCallback(() => {
    setAlerts([]);
  }, []);

  /**
   * Clear transactions
   */
  const clearTransactions = useCallback(() => {
    setTransactions([]);
  }, []);

  /**
   * Get latest alerts
   */
  const getLatestAlerts = useCallback((count = 10) => {
    return alerts.slice(0, count);
  }, [alerts]);

  /**
   * Get latest transactions
   */
  const getLatestTransactions = useCallback((count = 10) => {
    return transactions.slice(0, count);
  }, [transactions]);

  /**
   * Filter alerts by severity
   */
  const getAlertsBySeverity = useCallback((severity) => {
    return alerts.filter(alert => alert.severity === severity);
  }, [alerts]);

  /**
   * Context value
   */
  const contextValue = {
    // WebSocket state and methods
    isConnected: webSocket.isConnected,
    isConnecting: webSocket.isConnecting,
    error: webSocket.error,
    lastMessageTime: webSocket.lastMessageTime,
    connect: webSocket.connect,
    disconnect: webSocket.disconnect,
    
    // Alert methods
    alerts,
    clearAlerts,
    getLatestAlerts,
    getAlertsBySeverity,
    
    // Transaction methods
    transactions,
    clearTransactions,
    getLatestTransactions,
    
    // Statistics and monitoring
    wsStats,
    fetchWebSocketStats,
    subscribeToAlerts: webSocket.subscribeToAlerts,
    subscribeToTransactions: webSocket.subscribeToTransactions,
    unsubscribeFromAlerts: webSocket.unsubscribeFromAlerts,
    unsubscribeFromTransactions: webSocket.unsubscribeFromTransactions,
  };

  return (
    <WebSocketContext.Provider value={contextValue}>
      {children}
    </WebSocketContext.Provider>
  );
};

/**
 * useWebSocketContext - Custom hook to use WebSocket context
 * Use this hook in any component within the WebSocketProvider
 * 
 * @returns {Object} WebSocket context value
 * 
 * @example
 * const { isConnected, alerts, getLatestAlerts } = useWebSocketContext();
 */
export const useWebSocketContext = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocketContext must be used within WebSocketProvider');
  }
  return context;
};

export default WebSocketContext;
