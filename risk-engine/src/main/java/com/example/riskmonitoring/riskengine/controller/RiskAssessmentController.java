package com.example.riskmonitoring.riskengine.controller;

import com.example.riskmonitoring.common.models.RiskAssessment;
import com.example.riskmonitoring.common.models.TransactionRequest;
import com.example.riskmonitoring.riskengine.service.TransactionProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskAssessmentController {

    private final TransactionProcessingService processingService;

    public RiskAssessmentController(TransactionProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping("/assess")
    public ResponseEntity<RiskAssessment> assess(@RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(processingService.process(request));
    }
}