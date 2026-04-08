package com.example.riskmonitoring.riskengine.controller;

import com.example.riskmonitoring.riskengine.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MetricsControllerTest {

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private MetricsController metricsController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetMetrics() {
        // Arrange
        when(metricsService.getTotalProcessed()).thenReturn(100L);
        when(metricsService.getFlaggedCount()).thenReturn(5L);
        when(metricsService.getFailedCount()).thenReturn(2L);
        when(metricsService.getAvgProcessingTimeMs()).thenReturn(15.5);
        when(metricsService.getThroughput()).thenReturn(50.0);

        // Act
        ResponseEntity<Map<String, Object>> response = metricsController.getMetrics();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, Object> metrics = response.getBody();
        assertNotNull(metrics);
        assertEquals(100L, metrics.get("totalProcessed"));
        assertEquals(5L, metrics.get("flaggedCount"));
        assertEquals(2L, metrics.get("failedCount"));
        assertEquals(15.5, metrics.get("avgProcessingTime"));
        assertEquals(50.0, metrics.get("throughput"));
        
        // Verify service methods were called
        verify(metricsService).getTotalProcessed();
        verify(metricsService).getFlaggedCount();
        verify(metricsService).getFailedCount();
        verify(metricsService).getAvgProcessingTimeMs();
        verify(metricsService).getThroughput();
    }
}