package com.example.riskmonitoring.producer.client;

import com.example.riskmonitoring.common.models.RiskAssessment;
import com.example.riskmonitoring.common.models.TransactionRequest;
import com.example.riskmonitoring.producer.config.RiskEngineProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RiskEngineClient {

    private final RestClient restClient;
    private final RiskEngineProperties properties;

    public RiskEngineClient(RestClient restClient, RiskEngineProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public RiskAssessment assess(TransactionRequest request) {
        return restClient.post()
                .uri(properties.baseUrl() + "/api/risk/assess")
                .body(request)
                .retrieve()
                .body(RiskAssessment.class);
    }
}