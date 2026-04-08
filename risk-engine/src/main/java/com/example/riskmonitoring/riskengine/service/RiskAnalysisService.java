package com.example.riskmonitoring.riskengine.service;

import com.example.riskmonitoring.common.models.RiskLevel;
import com.example.riskmonitoring.common.models.RiskResult;
import com.example.riskmonitoring.common.models.Transaction;
import com.example.riskmonitoring.riskengine.analyzer.RiskAnalyzer;
import com.example.riskmonitoring.riskengine.analyzer.RiskDetectionResult;
import com.example.riskmonitoring.riskengine.domain.TransactionHistory;
import com.example.riskmonitoring.riskengine.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for analyzing transaction risk.
 * Orchestrates multiple risk analyzers and aggregates their results.
 */
@Slf4j
@Service
public class RiskAnalysisService {

    private final List<RiskAnalyzer> analyzers;
    private final TransactionHistoryRepository transactionRepository;

    public RiskAnalysisService(
            List<RiskAnalyzer> analyzers,
            TransactionHistoryRepository transactionRepository) {
        this.analyzers = analyzers;
        this.transactionRepository = transactionRepository;
        log.info("RiskAnalysisService initialized with {} analyzers: {}",
                analyzers.size(), analyzers.stream()
                        .map(RiskAnalyzer::getName)
                        .collect(Collectors.joining(", ")));
    }

    /**
     * Analyzes a transaction for risk using all configured analyzers.
     *
     * @param transaction the transaction to analyze
     * @return RiskResult with overall risk assessment
     */
    public RiskResult analyzeTransaction(Transaction transaction) {
        try {
            // Record transaction in history
            TransactionHistory history = TransactionHistory.fromTransaction(transaction);
            transactionRepository.save(history);

            log.debug("Analyzing transaction: {}, UserId: {}, Amount: {}",
                    transaction.getTransactionId(), transaction.getUserId(), transaction.getAmount());

            // Run all analyzers
            List<RiskDetectionResult> analysisResults = new ArrayList<>();
            for (RiskAnalyzer analyzer : analyzers) {
                try {
                    RiskDetectionResult result = analyzer.analyze(transaction);
                    analysisResults.add(result);
                    log.debug("Analyzer {} result: flagged={}, riskLevel={}",
                            analyzer.getName(), result.isFlagged(), result.getRiskLevel());
                } catch (Exception ex) {
                    log.error("Error in analyzer {}", analyzer.getName(), ex);
                }
            }

            // Aggregate results
            RiskLevel overallRiskLevel = aggregateRiskLevels(analysisResults);
            String aggregatedReason = generateAggregatedReason(analysisResults);

            RiskResult riskResult = RiskResult.builder()
                    .transactionId(transaction.getTransactionId())
                    .riskLevel(overallRiskLevel)
                    .reason(aggregatedReason)
                    .build();

            log.info("Risk analysis completed: TransactionId={}, RiskLevel={}, FlaggedBy={} analyzers",
                    transaction.getTransactionId(), overallRiskLevel,
                    analysisResults.stream().filter(RiskDetectionResult::isFlagged).count());

            return riskResult;

        } catch (Exception ex) {
            log.error("Error analyzing transaction: {}", transaction.getTransactionId(), ex);
            // Default to MEDIUM risk on analysis error
            return RiskResult.builder()
                    .transactionId(transaction.getTransactionId())
                    .riskLevel(RiskLevel.MEDIUM)
                    .reason("Analysis error: " + ex.getMessage())
                    .build();
        }
    }

    /**
     * Aggregates risk levels from multiple analyzers.
     * Priority: HIGH > MEDIUM > LOW
     *
     * @param results the analysis results from all analyzers
     * @return the overall risk level
     */
    private RiskLevel aggregateRiskLevels(List<RiskDetectionResult> results) {
        // HIGH takes priority
        if (results.stream().anyMatch(r -> r.getRiskLevel() == RiskLevel.HIGH)) {
            return RiskLevel.HIGH;
        }

        // MEDIUM takes priority over LOW
        if (results.stream().anyMatch(r -> r.getRiskLevel() == RiskLevel.MEDIUM)) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    /**
     * Generates an aggregated reason from all analyzer results.
     *
     * @param results the analysis results
     * @return concatenated reason string
     */
    private String generateAggregatedReason(List<RiskDetectionResult> results) {
        // Collect flagged reasons
        List<String> flaggedReasons = results.stream()
                .filter(RiskDetectionResult::isFlagged)
                .map(r -> String.format("[%s] %s", r.getAnalyzerName(), r.getReason()))
                .collect(Collectors.toList());

        if (flaggedReasons.isEmpty()) {
            return "All analyzers indicate low risk";
        }

        return "Risk factors detected: " + String.join("; ", flaggedReasons);
    }

    /**
     * Gets the list of configured analyzers.
     * Useful for monitoring and diagnostics.
     *
     * @return list of analyzer names
     */
    public List<String> getConfiguredAnalyzers() {
        return analyzers.stream()
                .map(RiskAnalyzer::getName)
                .collect(Collectors.toList());
    }
}
