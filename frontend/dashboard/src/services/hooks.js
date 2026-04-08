import { useState, useEffect, useRef, useCallback } from 'react';

/**
 * Custom Hook: Use Polling
 * Periodically executes a callback function
 *
 * @param {Function} callback - Function to execute periodically
 * @param {number} interval - Interval in milliseconds (default: 5000ms)
 * @param {boolean} enabled - Enable/disable polling (default: true)
 * @param {Array} dependencies - Dependencies array for callback
 */
export const usePolling = (callback, interval = 5000, enabled = true, dependencies = []) => {
  const intervalRef = useRef(null);
  const depsRef = useRef(dependencies);

  // Update deps ref whenever dependencies change
  useEffect(() => {
    depsRef.current = dependencies;
  }, [dependencies]);

  const executeCallback = useCallback(async () => {
    try {
      await callback();
    } catch (error) {
      console.error('Polling callback error:', error);
    }
  }, [callback]);

  useEffect(() => {
    if (!enabled) {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      return;
    }

    // Execute immediately on mount
    executeCallback();

    // Set up interval
    intervalRef.current = setInterval(executeCallback, interval);

    // Cleanup
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [enabled, interval, executeCallback]);

  // Manual refresh function
  const refresh = useCallback(async () => {
    await executeCallback();
  }, [executeCallback]);

  return { refresh };
};

/**
 * Custom Hook: Use Async Data
 * Manages loading, data, and error states for async operations
 *
 * @param {Function} asyncFn - Async function to execute
 * @param {boolean} shouldExecute - Whether to execute the function
 * @param {Array} dependencies - Dependencies array
 * @returns {Object} { data, loading, error, execute }
 */
export const useAsyncData = (asyncFn, shouldExecute = true, dependencies = []) => {
  const [state, setState] = useState({
    data: null,
    loading: false,
    error: null,
  });

  const execute = useCallback(async () => {
    setState({ data: null, loading: true, error: null });

    try {
      const result = await asyncFn();
      setState({ data: result, loading: false, error: null });
      return result;
    } catch (error) {
      setState({ data: null, loading: false, error });
      throw error;
    }
  }, [asyncFn]);

  useEffect(() => {
    if (shouldExecute) {
      execute();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shouldExecute, execute, dependencies]);

  return { ...state, execute };
};

/**
 * Custom Hook: Use Debounce
 * Debounces a value with a delay
 *
 * @param {*} value - Value to debounce
 * @param {number} delay - Delay in milliseconds
 * @returns {*} Debounced value
 */
export const useDebounce = (value, delay = 500) => {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => clearTimeout(handler);
  }, [value, delay]);

  return debouncedValue;
};

export default usePolling;
