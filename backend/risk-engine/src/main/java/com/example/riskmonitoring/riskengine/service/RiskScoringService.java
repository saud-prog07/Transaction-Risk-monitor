package com.example.riskmonitoring.riskengine.service;

import com.example.riskmonitoring.common.models.RiskDecision;
import com.example.riskmonitoring.common.models.TransactionRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {

    public RiskScore evaluate(TransactionRequest request) {
        List<String> reasons = new ArrayList<>();
        BigDecimal score = BigDecimal.TEN;

        if (request.amount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            score = score.add(BigDecimal.valueOf(50));
            reasons.add("Amount is above 10,000");
        } else if (request.amount().compareTo(BigDecimal.valueOf(5000)) > 0) {
            score = score.add(BigDecimal.valueOf(35));
            reasons.add("Amount is above 5,000");
        } else if (request.amount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            score = score.add(BigDecimal.valueOf(15));
            reasons.add("Amount is above 1,000");
        }

        String merchantId = request.merchantId().toLowerCase();
        if (merchantId.contains("crypto") || merchantId.contains("cash")) {
            score = score.add(BigDecimal.valueOf(20));
            reasons.add("Merchant category is higher risk");
        }

        if (request.accountId().endsWith("9")) {
            score = score.add(BigDecimal.valueOf(10));
            reasons.add("Account pattern matched elevated risk rule");
        }

        RiskDecision decision = score.compareTo(BigDecimal.valueOf(75)) >= 0
                ? RiskDecision.BLOCKED
                : score.compareTo(BigDecimal.valueOf(45)) >= 0
                        ? RiskDecision.REVIEW
                        : RiskDecision.APPROVED;

        if (reasons.isEmpty()) {
            reasons.add("No elevated risk indicators matched");
        }

        return new RiskScore(score, decision, reasons);
    }

    public record RiskScore(BigDecimal score, RiskDecision decision, List<String> reasons) {
    }
}