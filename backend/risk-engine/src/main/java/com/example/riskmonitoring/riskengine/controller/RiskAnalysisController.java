package com.example.riskmonitoring.riskengine.controller;

import com.example.riskmonitoring.common.models.RiskResult;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.service.RiskAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for risk analysis endpoints.
 * Provides diagnostic and testing capabilities.
 */
@Slf4j
@RestController
@RequestMapping("/api/risk")
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;

    public RiskAnalysisController(RiskAnalysisService riskAnalysisService) {
        this.riskAnalysisService = riskAnalysisService;
    }

    /**
     * Analyzes a transaction directly via REST API.
     * Useful for testing and manual analysis.
     *
     * @param transaction the transaction to analyze
     * @return RiskResult with analysis output
     */
    @PostMapping("/analyze")
    public ResponseEntity<RiskResult> analyzeTransaction(@RequestBody Transaction transaction) {
        try {
            log.info("REST request to analyze transaction: {}", transaction.getTransactionId());
            RiskResult riskResult = riskAnalysisService.analyzeTransaction(transaction);
            return ResponseEntity.ok(riskResult);
        } catch (Exception ex) {
            log.error("Error analyzing transaction via REST", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets list of configured analyzers.
     * Useful for diagnostics.
     *
     * @return list of analyzer names
     */
    @GetMapping("/analyzers")
    public ResponseEntity<Map<String, Object>> getAnalyzers() {
        Map<String, Object> response = new HashMap<>();
        response.put("analyzers", riskAnalysisService.getConfiguredAnalyzers());
        response.put("count", riskAnalysisService.getConfiguredAnalyzers().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     *
     * @return status message
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "risk-engine");
        return ResponseEntity.ok(response);
    }
}
