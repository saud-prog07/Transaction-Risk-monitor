package com.example.riskmonitoring.common.models;

import java.time.Instant;

public record TransactionSubmissionResponse(
        String transactionId,
        RiskDecision decision,
        String message,
        Instant processedAt) {
}