import { useState, useEffect, useRef } from 'react';

/**
 * Custom hook for auto-refresh functionality
 * @param {number} intervalMs - Refresh interval in milliseconds
 * @returns {Object} Object with refresh function to trigger manual refresh
 */
export function useRefresh(intervalMs) {
  const [count, setCount] = useState(0);
  const savedCallback = useRef();

  // Memoize callback to prevent infinite loops
  useEffect(() => {
    savedCallback.current = () => setCount(c => c + 1);
  }, []);

  // Set up interval for auto-refresh
  useEffect(() => {
    const handler = setInterval(() => {
      savedCallback.current();
    }, intervalMs);

    return () => clearInterval(handler);
  }, [intervalMs]);

  // Return function to trigger refresh manually
  const refresh = () => setCount(c => c + 1);

  return { refresh };
}