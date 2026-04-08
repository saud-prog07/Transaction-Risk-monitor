package com.example.riskmonitoring.producer.service;

import com.example.riskmonitoring.common.models.RiskAssessment;
import com.example.riskmonitoring.common.models.RiskDecision;
import com.example.riskmonitoring.common.models.TransactionRequest;
import com.example.riskmonitoring.common.models.TransactionSubmissionResponse;
import com.example.riskmonitoring.producer.client.RiskEngineClient;
import com.example.riskmonitoring.producer.domain.SubmittedTransaction;
import com.example.riskmonitoring.producer.repository.TransactionRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RiskEngineClient riskEngineClient;

    public TransactionService(TransactionRepository transactionRepository, RiskEngineClient riskEngineClient) {
        this.transactionRepository = transactionRepository;
        this.riskEngineClient = riskEngineClient;
    }

    public TransactionSubmissionResponse submit(TransactionRequest request) {
        TransactionRequest normalizedRequest = normalize(request);
        SubmittedTransaction transaction = new SubmittedTransaction(normalizedRequest, Instant.now());
        transactionRepository.save(transaction);

        try {
            RiskAssessment assessment = riskEngineClient.assess(normalizedRequest);
            transaction.setAssessment(assessment);
            transactionRepository.save(transaction);

            String message = assessment.decision() == RiskDecision.APPROVED
                    ? "Transaction approved"
                    : "Transaction sent for further review";

            return new TransactionSubmissionResponse(
                    assessment.transactionId(),
                    assessment.decision(),
                    message,
                    assessment.assessedAt());
        } catch (RestClientException ex) {
            return new TransactionSubmissionResponse(
                    normalizedRequest.transactionId(),
                    RiskDecision.REVIEW,
                    "Risk engine unavailable; transaction queued for later review",
                    Instant.now());
        }
    }

    private TransactionRequest normalize(TransactionRequest request) {
        String transactionId = Objects.requireNonNullElseGet(request.transactionId(), () -> UUID.randomUUID().toString());
        return new TransactionRequest(
                transactionId,
                request.accountId(),
                request.merchantId(),
                request.amount(),
                request.currency(),
                Objects.requireNonNullElseGet(request.transactionTime(), Instant::now));
    }
}