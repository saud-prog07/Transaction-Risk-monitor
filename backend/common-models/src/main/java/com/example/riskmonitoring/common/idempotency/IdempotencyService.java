package com.example.riskmonitoring.common.idempotency;

import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.util.Optional;

/**
 * Idempotency management service to prevent duplicate transaction processing.
 * Maintains an in-memory cache of processed transactions.
 * 
 * For production use with high volumes, consider replacing with Redis or database-backed store.
 */
public class IdempotencyService {
    
    private static final IdempotencyService INSTANCE = new IdempotencyService();
    private static final ConcurrentHashMap<String, IdempotencyRecord> processedTransactions = 
            new ConcurrentHashMap<>();
    private static final int MAX_RECORDS = 100_000;
    
    private IdempotencyService() {
        // Initialize periodic cleanup
        startCleanupTask();
    }
    
    public static IdempotencyService getInstance() {
        return INSTANCE;
    }
    
    /**
     * Check if a transaction has already been processed
     * @param transactionId The transaction ID to check
     * @return Optional containing the IdempotencyRecord if found and not expired, empty otherwise
     */
    public Optional<IdempotencyRecord> getProcessedTransaction(String transactionId) {
        IdempotencyRecord record = processedTransactions.get(transactionId);
        
        if (record == null) {
            return Optional.empty();
        }
        
        // Check if record has expired
        if (record.isExpired()) {
            processedTransactions.remove(transactionId);
            return Optional.empty();
        }
        
        return Optional.of(record);
    }
    
    /**
     * Record a processed transaction for future idempotency checks
     * @param transactionId The transaction ID
     * @param userId The user ID
     * @param amount The transaction amount
     * @param location The transaction location
     * @param processingResult The result of processing
     */
    public void recordTransaction(String transactionId, String userId, double amount, 
                                 String location, String processingResult) {
        IdempotencyRecord record = new IdempotencyRecord(transactionId, userId, amount, 
                                                         location, processingResult);
        
        // Check if we need to cleanup before adding new record
        if (processedTransactions.size() >= MAX_RECORDS) {
            cleanupExpiredRecords();
        }
        
        processedTransactions.put(transactionId, record);
    }
    
    /**
     * Check if transaction is duplicate and record it
     * @param transactionId The transaction ID
     * @param userId The user ID
     * @param amount The transaction amount
     * @param location The transaction location
     * @return true if this is a duplicate (already processed), false if it's new
     */
    public boolean isDuplicate(String transactionId, String userId, double amount, String location) {
        if (processedTransactions.containsKey(transactionId)) {
            IdempotencyRecord record = processedTransactions.get(transactionId);
            
            // Verify the transaction details match (prevent ID spoofing)
            if (userId.equals(record.getUserId()) && 
                amount == record.getAmount() && 
                location.equals(record.getLocation())) {
                
                if (!record.isExpired()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Clean up expired records
     */
    public void cleanupExpiredRecords() {
        processedTransactions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Get statistics about the idempotency cache
     */
    public IdempotencyCacheStats getStats() {
        long expiredCount = processedTransactions.values().stream()
                .filter(IdempotencyRecord::isExpired)
                .count();
        
        return new IdempotencyCacheStats(
                processedTransactions.size(),
                (int) expiredCount,
                MAX_RECORDS
        );
    }
    
    /**
     * Clear all records (for testing)
     */
    public void clearAll() {
        processedTransactions.clear();
    }
    
    /**
     * Start periodic cleanup task to remove expired records
     */
    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    // Run cleanup every 5 minutes
                    Thread.sleep(300_000);
                    cleanupExpiredRecords();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "IdempotencyCleanupThread");
        
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    /**
     * Statistics about the idempotency cache
     */
    public static class IdempotencyCacheStats {
        public final int totalRecords;
        public final int expiredRecords;
        public final int maxCapacity;
        public final double utilizationPercent;
        
        public IdempotencyCacheStats(int totalRecords, int expiredRecords, int maxCapacity) {
            this.totalRecords = totalRecords;
            this.expiredRecords = expiredRecords;
            this.maxCapacity = maxCapacity;
            this.utilizationPercent = (totalRecords * 100.0) / maxCapacity;
        }
        
        @Override
        public String toString() {
            return "IdempotencyCacheStats{" +
                    "totalRecords=" + totalRecords +
                    ", expiredRecords=" + expiredRecords +
                    ", maxCapacity=" + maxCapacity +
                    ", utilizationPercent=" + String.format("%.2f", utilizationPercent) + "%" +
                    '}';
        }
    }
}
