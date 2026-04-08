package com.example.riskmonitoring.riskengine.domain;

import com.example.riskmonitoring.common.models.RiskAssessment;

public class RiskAssessmentRecord {

    private final RiskAssessment assessment;

    public RiskAssessmentRecord(RiskAssessment assessment) {
        this.assessment = assessment;
    }

    public RiskAssessment getAssessment() {
        return assessment;
    }
}