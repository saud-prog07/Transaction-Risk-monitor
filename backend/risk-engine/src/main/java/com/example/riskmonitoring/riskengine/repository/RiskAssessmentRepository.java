package com.example.riskmonitoring.riskengine.repository;

import com.example.riskmonitoring.riskengine.domain.RiskAssessmentRecord;
import java.util.Optional;

public interface RiskAssessmentRepository {

    RiskAssessmentRecord save(RiskAssessmentRecord record);

    Optional<RiskAssessmentRecord> findByTransactionId(String transactionId);
}