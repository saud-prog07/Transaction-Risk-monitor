package com.example.riskmonitoring.riskengine.analyzer;

import com.example.riskmonitoring.common.models.Transaction;

/**
 * Interface for risk analysis strategies.
 * Implementations analyze transactions for specific risk patterns.
 */
public interface RiskAnalyzer {

    /**
     * Analyzes a transaction for risk patterns.
     *
     * @param transaction the transaction to analyze
     * @return the risk detection result
     */
    RiskDetectionResult analyze(Transaction transaction);

    /**
     * Gets the name of this analyzer.
     *
     * @return analyzer name
     */
    String getName();
}
