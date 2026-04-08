import { useEffect, useRef, useState, useCallback } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

/**
 * useWebSocket - Custom React Hook for WebSocket Communication
 * 
 * Features:
 * - Automatic connection management
 * - Auto-reconnection with exponential backoff
 * - Subscription/unsubscription management
 * - Message callbacks for different message types
 * - Connection state tracking
 * - Error handling and logging
 * 
 * @param {Object} options - Configuration options
 * @param {string} options.url - WebSocket server URL (default: dynamic based on window.location)
 * @param {boolean} options.autoConnect - Auto-connect on mount (default: true)
 * @param {number} options.reconnectAttempts - Max reconnection attempts (default: 5)
 * @param {number} options.reconnectInterval - Initial reconnect interval in ms (default: 3000)
 * @param {Object} options.callbacks - Message callbacks for different actions
 * @param {Function} options.onConnect - Called when connected
 * @param {Function} options.onDisconnect - Called when disconnected
 * @param {Function} options.onError - Called on error
 * 
 * @returns {Object} WebSocket operations and state
 */
const useWebSocket = (options = {}) => {
  const {
    url = null,
    autoConnect = true,
    reconnectAttempts = 5,
    reconnectInterval = 3000,
    callbacks = {},
    onConnect = null,
    onDisconnect = null,
    onError = null,
  } = options;

  const stompClientRef = useRef(null);
  const sockJsRef = useRef(null);
  const reconnectCountRef = useRef(0);
  const reconnectTimerRef = useRef(null);
  const subscriptionsRef = useRef({});
  const heartbeatIntervalRef = useRef(null);

  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [error, setError] = useState(null);
  const [lastMessageTime, setLastMessageTime] = useState(null);

  /**
   * Get the WebSocket URL dynamically
   */
  const getWebSocketUrl = useCallback(() => {
    if (url) return url;
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${protocol}://${window.location.host}/ws`;
  }, [url]);

  /**
   * Handle incoming messages of specific types
   */
  const handleMessage = useCallback((message) => {
    try {
      const messageData = typeof message.body === 'string' 
        ? JSON.parse(message.body) 
        : message.body;

      setLastMessageTime(new Date());

      // Route message to appropriate callback
      if (callbacks[messageData.action]) {
        callbacks[messageData.action](messageData);
      }

      // General message callback
      if (callbacks.onMessage) {
        callbacks.onMessage(messageData);
      }
    } catch (err) {
      console.error('Error handling WebSocket message:', err);
      if (onError) onError(err);
    }
  }, [callbacks, onError]);

  /**
   * Handle PING messages from server
   */
  const handlePing = useCallback((message) => {
    try {
      if (stompClientRef.current && stompClientRef.current.connected) {
        // Send PONG response
        stompClientRef.current.send('/app/pong', {}, JSON.stringify({
          action: 'PONG',
          timestamp: Date.now()
        }));
      }
    } catch (err) {
      console.error('Error sending PONG:', err);
    }
  }, []);

  /**
   * Connect to WebSocket server
   */
  const connect = useCallback(async () => {
    if (isConnected || isConnecting) {
      console.warn('WebSocket already connected or connecting');
      return;
    }

    setIsConnecting(true);
    setError(null);

    try {
      const wsUrl = getWebSocketUrl();
      console.log('Connecting to WebSocket:', wsUrl);

      // Create SockJS socket with fallback transports
      sockJsRef.current = new SockJS(wsUrl, null, {
        transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
      });

      // Create STOMP client
      stompClientRef.current = Stomp.over(sockJsRef.current);
      stompClientRef.current.debug = process.env.NODE_ENV === 'development';

      // Connect with error handling
      stompClientRef.current.connect(
        {}, // headers
        () => {
          // Success callback
          console.log('WebSocket connected');
          setIsConnected(true);
          setIsConnecting(false);
          reconnectCountRef.current = 0;

          // Set up general subscription for alerts
          if (stompClientRef.current) {
            stompClientRef.current.subscribe('/topic/alerts', handleMessage);
            stompClientRef.current.subscribe('/topic/transactions', handleMessage);
          }

          // Start heartbeat
          startHeartbeat();

          if (onConnect) onConnect();
        },
        (error) => {
          // Error callback
          console.error('WebSocket connection error:', error);
          setIsConnected(false);
          setIsConnecting(false);
          setError(error);

          if (onError) onError(error);

          // Attempt reconnection
          attemptReconnect();
        }
      );
    } catch (err) {
      console.error('Error establishing WebSocket connection:', err);
      setIsConnecting(false);
      setError(err);

      if (onError) onError(err);

      attemptReconnect();
    }
  }, [isConnected, isConnecting, getWebSocketUrl, handleMessage, onConnect, onError]);

  /**
   * Disconnect from WebSocket server
   */
  const disconnect = useCallback(() => {
    if (stompClientRef.current && stompClientRef.current.connected) {
      stompClientRef.current.disconnect(() => {
        console.log('WebSocket disconnected');
        setIsConnected(false);

        if (onDisconnect) onDisconnect();
      });
    }

    // Clear timers
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
    }
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }
  }, [onDisconnect]);

  /**
   * Attempt to reconnect with exponential backoff
   */
  const attemptReconnect = useCallback(() => {
    if (reconnectCountRef.current < reconnectAttempts) {
      reconnectCountRef.current += 1;
      const delay = reconnectInterval * Math.pow(2, reconnectCountRef.current - 1);
      
      console.log(`Attempting reconnection ${reconnectCountRef.current}/${reconnectAttempts} in ${delay}ms`);
      
      reconnectTimerRef.current = setTimeout(() => {
        connect();
      }, delay);
    } else {
      console.error('Max reconnection attempts reached');
      setError(new Error('Max reconnection attempts reached'));
    }
  }, [reconnectAttempts, reconnectInterval, connect]);

  /**
   * Send message to server
   */
  const sendMessage = useCallback((destination, message) => {
    if (!stompClientRef.current || !stompClientRef.current.connected) {
      console.error('WebSocket not connected');
      setError(new Error('WebSocket not connected'));
      return false;
    }

    try {
      stompClientRef.current.send(destination, {}, JSON.stringify(message));
      return true;
    } catch (err) {
      console.error('Error sending WebSocket message:', err);
      setError(err);
      return false;
    }
  }, []);

  /**
   * Subscribe to alerts
   */
  const subscribeToAlerts = useCallback(() => {
    return sendMessage('/app/subscribe', {
      action: 'SUBSCRIBE_ALERTS',
      timestamp: Date.now()
    });
  }, [sendMessage]);

  /**
   * Subscribe to transactions
   */
  const subscribeToTransactions = useCallback(() => {
    return sendMessage('/app/subscribe', {
      action: 'SUBSCRIBE_TRANSACTIONS',
      timestamp: Date.now()
    });
  }, [sendMessage]);

  /**
   * Unsubscribe from alerts
   */
  const unsubscribeFromAlerts = useCallback(() => {
    return sendMessage('/app/unsubscribe', {
      action: 'UNSUBSCRIBE_ALERTS',
      timestamp: Date.now()
    });
  }, [sendMessage]);

  /**
   * Unsubscribe from transactions
   */
  const unsubscribeFromTransactions = useCallback(() => {
    return sendMessage('/app/unsubscribe', {
      action: 'UNSUBSCRIBE_TRANSACTIONS',
      timestamp: Date.now()
    });
  }, [sendMessage]);

  /**
   * Start heartbeat (ping) to server
   */
  const startHeartbeat = useCallback(() => {
    heartbeatIntervalRef.current = setInterval(() => {
      sendMessage('/app/ping', {
        action: 'PING',
        timestamp: Date.now()
      });
    }, 30000); // Send ping every 30 seconds
  }, [sendMessage]);

  /**
   * Effect hook: Auto-connect on mount, cleanup on unmount
   */
  useEffect(() => {
    if (autoConnect) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [autoConnect, connect, disconnect]);

  /**
   * Public API
   */
  return {
    // State
    isConnected,
    isConnecting,
    error,
    lastMessageTime,

    // Methods
    connect,
    disconnect,
    sendMessage,
    subscribeToAlerts,
    subscribeToTransactions,
    unsubscribeFromAlerts,
    unsubscribeFromTransactions,

    // Client reference (for advanced usage)
    client: stompClientRef.current
  };
};

export default useWebSocket;
