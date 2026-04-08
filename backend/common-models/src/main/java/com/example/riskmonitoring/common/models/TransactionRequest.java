package com.example.riskmonitoring.common.models;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(
        String transactionId,
        String accountId,
        String merchantId,
        BigDecimal amount,
        String currency,
        Instant transactionTime) {
}