package com.example.riskmonitoring.common.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RiskAssessment(
        String transactionId,
        BigDecimal score,
        RiskDecision decision,
        List<String> reasons,
        Instant assessedAt) {
}