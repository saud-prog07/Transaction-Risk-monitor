package com.example.riskmonitoring.producer.domain;

import com.example.riskmonitoring.common.models.RiskAssessment;
import com.example.riskmonitoring.common.models.TransactionRequest;
import java.time.Instant;

public class SubmittedTransaction {

    private final TransactionRequest request;
    private RiskAssessment assessment;
    private Instant receivedAt;

    public SubmittedTransaction(TransactionRequest request, Instant receivedAt) {
        this.request = request;
        this.receivedAt = receivedAt;
    }

    public TransactionRequest getRequest() {
        return request;
    }

    public RiskAssessment getAssessment() {
        return assessment;
    }

    public void setAssessment(RiskAssessment assessment) {
        this.assessment = assessment;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}