package com.example.riskmonitoring.producer.service;

import com.example.riskmonitoring.common.logging.StructuredLogger;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RiskEngineClient riskEngineClient;
    private final StructuredLogger structuredLogger;

    public TransactionService(TransactionRepository transactionRepository, RiskEngineClient riskEngineClient) {
        this.transactionRepository = transactionRepository;
        this.riskEngineClient = riskEngineClient;
        this.structuredLogger = StructuredLogger.getLogger(TransactionService.class);
    }

    public TransactionSubmissionResponse submit(TransactionRequest request) {
        long startTime = System.currentTimeMillis();
        
        // Normalize and generate transaction ID if needed
        TransactionRequest normalizedRequest = normalize(request);
        structuredLogger.setTransactionId(normalizedRequest.transactionId());
        
        // Log transaction received
        structuredLogger.logTransactionReceived(normalizedRequest.transactionId(),
                normalizedRequest.accountId(),
                normalizedRequest.amount().doubleValue());
        
        // Create and save transaction record
        SubmittedTransaction transaction = new SubmittedTransaction(normalizedRequest, Instant.now());
        transactionRepository.save(transaction);

        try {
            // Call risk engine for assessment
            RiskAssessment assessment = riskEngineClient.assess(normalizedRequest);
            transaction.setAssessment(assessment);
            transactionRepository.save(transaction);
            
            String message = assessment.decision() == RiskDecision.APPROVED
                    ? "Transaction approved"
                    : "Transaction sent for further review";

            long duration = System.currentTimeMillis() - startTime;
            structuredLogger.logProcessingComplete(assessment.transactionId(), duration);
            
            if (assessment.decision() == RiskDecision.REVIEW) {
                java.util.Map<String, Object> context = new java.util.HashMap<>();
                context.put("riskLevel", "HIGH");
                context.put("reason", "Risk engine assessment indicated suspicious activity");
                structuredLogger.warn("Transaction flagged for review", context);
            }

            return new TransactionSubmissionResponse(
                    assessment.transactionId(),
                    assessment.decision(),
                    message,
                    assessment.assessedAt());
                    
        } catch (RestClientException ex) {
            java.util.Map<String, Object> context = new java.util.HashMap<>();
            context.put("status", "QUEUED_FOR_REVIEW");
            context.put("reason", "Risk engine unavailable");
            structuredLogger.warn("Risk engine unavailable - transaction queued for later review", context);
            
            return new TransactionSubmissionResponse(
                    normalizedRequest.transactionId(),
                    RiskDecision.REVIEW,
                    "Risk engine unavailable; transaction queued for later review",
                    Instant.now());
        } finally {
            structuredLogger.clearContext();
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