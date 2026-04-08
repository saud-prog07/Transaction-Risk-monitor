package com.example.riskmonitoring.common.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FlaggedTransactionAlert(
        String transactionId,
        String accountId,
        String merchantId,
        BigDecimal amount,
        String currency,
        BigDecimal riskScore,
        RiskDecision decision,
        List<String> reasons,
        Instant flaggedAt) {
}