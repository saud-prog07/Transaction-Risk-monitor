package com.example.riskmonitoring.common.retry;

import com.example.riskmonitoring.common.logging.StructuredLogger;

/**
 * Retry executor that handles retrying operations with exponential backoff
 * Usage:
 * RetryExecutor executor = new RetryExecutor(config);
 * String result = executor.execute(() -> riskyOperation(), transactionId);
 */
public class RetryExecutor {
    
    private static final StructuredLogger logger = StructuredLogger.getLogger(RetryExecutor.class);
    
    private final RetryConfig config;
    
    public RetryExecutor(RetryConfig config) {
        this.config = config;
    }
    
    /**
     * Execute an operation with retry logic
     * @param operation The operation to execute
     * @param transactionId Transaction ID for logging
     * @return The result of the operation
     * @throws RetryException if all retries are exhausted
     */
    public <T> T execute(Operation<T> operation, String transactionId) throws RetryException {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt <= config.getMaxRetries()) {
            try {
                attempt++;
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                
                if (!config.shouldRetry(e) || attempt > config.getMaxRetries()) {
                    if (attempt > config.getMaxRetries()) {
                        logger.logRetryExhausted(transactionId, e.getMessage());
                    }
                    throw new RetryException("Operation failed after " + attempt + " attempts", e);
                }
                
                // Calculate backoff delay
                long delayMs = config.getDelayMs(attempt);
                logger.logRetryAttempt(transactionId, attempt, e.getMessage());
                
                // Sleep before retry
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RetryException("Retry interrupted", ie);
                }
            }
        }
        
        // Should not reach here, but just in case
        throw new RetryException("Retry execution failed", lastException);
    }
    
    /**
     * Execute an operation with retry logic (void operation)
     * @param operation The operation to execute
     * @param transactionId Transaction ID for logging
     * @throws RetryException if all retries are exhausted
     */
    public void executeVoid(VoidOperation operation, String transactionId) throws RetryException {
        execute(() -> {
            operation.execute();
            return null;
        }, transactionId);
    }
    
    /**
     * Execute an operation with retry logic and custom result handling
     * @param operation The operation to execute
     * @param resultHandler Handler for successful results
     * @param transactionId Transaction ID for logging
     * @throws RetryException if all retries are exhausted
     */
    public <T> void executeWithHandler(Operation<T> operation, ResultHandler<T> resultHandler, 
                                      String transactionId) throws RetryException {
        T result = execute(operation, transactionId);
        resultHandler.handle(result);
    }
    
    /**
     * Get the retry configuration
     */
    public RetryConfig getConfig() {
        return config;
    }
    
    // Functional interfaces for operations
    
    @FunctionalInterface
    public interface Operation<T> {
        T execute() throws Exception;
    }
    
    @FunctionalInterface
    public interface VoidOperation {
        void execute() throws Exception;
    }
    
    @FunctionalInterface
    public interface ResultHandler<T> {
        void handle(T result);
    }
    
    /**
     * Exception thrown when retry attempts are exhausted
     */
    public static class RetryException extends Exception {
        public RetryException(String message) {
            super(message);
        }
        
        public RetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
