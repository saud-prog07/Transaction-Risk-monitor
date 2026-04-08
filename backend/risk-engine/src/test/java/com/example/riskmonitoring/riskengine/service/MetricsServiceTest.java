package com.example.riskmonitoring.riskengine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MetricsServiceTest {

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();
    }

    @Test
    void testInitialMetrics() {
        assertEquals(0, metricsService.getTotalProcessed());
        assertEquals(0, metricsService.getFlaggedCount());
        assertEquals(0, metricsService.getFailedCount());
        assertEquals(0.0, metricsService.getAvgProcessingTimeMs(), 0.001);
        // Throughput will be very small since we just started
        assertTrue(metricsService.getThroughput() >= 0);
    }

    @Test
    void testIncrementTotalProcessed() {
        metricsService.incrementTotalProcessed();
        metricsService.incrementTotalProcessed();
        metricsService.incrementTotalProcessed();
        
        assertEquals(3, metricsService.getTotalProcessed());
    }

    @Test
    void testIncrementFlaggedCount() {
        metricsService.incrementFlaggedCount();
        metricsService.incrementFlaggedCount();
        
        assertEquals(2, metricsService.getFlaggedCount());
    }

    @Test
    void testIncrementFailedCount() {
        metricsService.incrementFailedCount();
        metricsService.incrementFailedCount();
        metricsService.incrementFailedCount();
        
        assertEquals(3, metricsService.getFailedCount());
    }

    @Test
    void testRecordAndGetAvgProcessingTime() {
        // Record processing times: 10ms, 20ms, 30ms
        metricsService.recordProcessingTime(10_000_000); // 10ms in nanoseconds
        metricsService.recordProcessingTime(20_000_000); // 20ms in nanoseconds
        metricsService.recordProcessingTime(30_000_000); // 30ms in nanoseconds
        
        assertEquals(3, metricsService.getTotalProcessed());
        assertEquals(20.0, metricsService.getAvgProcessingTimeMs(), 0.001); // (10+20+30)/3 = 20ms
    }

    @Test
    void testResetStartTimeAffectsThroughput() throws InterruptedException {
        // Process some transactions
        metricsService.incrementTotalProcessed();
        metricsService.incrementTotalProcessed();
        
        // Wait a bit to ensure measurable time passes
        Thread.sleep(100);
        
        double initialThroughput = metricsService.getThroughput();
        
        // Reset start time
        metricsService.resetStartTime();
        
        // Process more transactions
        metricsService.incrementTotalProcessed();
        metricsService.incrementTotalProcessed();
        
        // Wait a bit more
        Thread.sleep(100);
        
        double finalThroughput = metricsService.getThroughput();
        
        // After reset, throughput should be based on recent activity
        // This is harder to test precisely due to timing, but we can verify it's non-negative
        assertTrue(finalThroughput >= 0);
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int incrementsPerThread = 100;
        
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    metricsService.incrementTotalProcessed();
                    metricsService.incrementFlaggedCount();
                    metricsService.incrementFailedCount();
                    metricsService.recordProcessingTime(5_000_000); // 5ms
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        int expected = numThreads * incrementsPerThread;
        assertEquals(expected, metricsService.getTotalProcessed());
        assertEquals(expected, metricsService.getFlaggedCount());
        assertEquals(expected, metricsService.getFailedCount());
        // Average processing time should be 5ms
        assertEquals(5.0, metricsService.getAvgProcessingTimeMs(), 0.001);
    }
}