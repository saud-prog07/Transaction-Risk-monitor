package com.example.riskmonitoring.riskengine.service;

import com.example.riskmonitoring.common.models.FlaggedTransactionAlert;
import com.example.riskmonitoring.common.models.RiskAssessment;
import com.example.riskmonitoring.common.models.TransactionRequest;
import com.example.riskmonitoring.riskengine.client.AlertServiceClient;
import com.example.riskmonitoring.riskengine.domain.RiskAssessmentRecord;
import com.example.riskmonitoring.riskengine.repository.RiskAssessmentRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class TransactionProcessingService {

    private final RiskScoringService riskScoringService;
    private final RiskAssessmentRepository assessmentRepository;
    private final AlertServiceClient alertServiceClient;

    public TransactionProcessingService(
            RiskScoringService riskScoringService,
            RiskAssessmentRepository assessmentRepository,
            AlertServiceClient alertServiceClient) {
        this.riskScoringService = riskScoringService;
        this.assessmentRepository = assessmentRepository;
        this.alertServiceClient = alertServiceClient;
    }

    public RiskAssessment process(TransactionRequest request) {
        RiskScoringService.RiskScore riskScore = riskScoringService.evaluate(request);
        RiskAssessment assessment = new RiskAssessment(
                request.transactionId(),
                riskScore.score(),
                riskScore.decision(),
                riskScore.reasons(),
                Instant.now());

        assessmentRepository.save(new RiskAssessmentRecord(assessment));

        if (riskScore.decision() != com.example.riskmonitoring.common.models.RiskDecision.APPROVED) {
            alertServiceClient.createAlert(new FlaggedTransactionAlert(
                    request.transactionId(),
                    request.accountId(),
                    request.merchantId(),
                    request.amount(),
                    request.currency(),
                    riskScore.score(),
                    riskScore.decision(),
                    riskScore.reasons(),
                    assessment.assessedAt()));
        }

        return assessment;
    }
}