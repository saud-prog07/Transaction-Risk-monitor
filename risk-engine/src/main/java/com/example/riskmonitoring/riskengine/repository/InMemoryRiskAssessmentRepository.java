package com.example.riskmonitoring.riskengine.repository;

import com.example.riskmonitoring.riskengine.domain.RiskAssessmentRecord;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRiskAssessmentRepository implements RiskAssessmentRepository {

    private final Map<String, RiskAssessmentRecord> assessments = new ConcurrentHashMap<>();

    @Override
    public RiskAssessmentRecord save(RiskAssessmentRecord record) {
        assessments.put(record.getAssessment().transactionId(), record);
        return record;
    }

    @Override
    public Optional<RiskAssessmentRecord> findByTransactionId(String transactionId) {
        return Optional.ofNullable(assessments.get(transactionId));
    }
}