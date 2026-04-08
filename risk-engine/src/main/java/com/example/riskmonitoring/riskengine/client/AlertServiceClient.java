package com.example.riskmonitoring.riskengine.client;

import com.example.riskmonitoring.common.models.FlaggedTransactionAlert;
import com.example.riskmonitoring.riskengine.config.AlertServiceProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AlertServiceClient {

    private final RestClient restClient;
    private final AlertServiceProperties properties;

    public AlertServiceClient(RestClient restClient, AlertServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public void createAlert(FlaggedTransactionAlert alert) {
        restClient.post()
                .uri(properties.baseUrl() + "/api/alerts")
                .body(alert)
                .retrieve()
                .toBodilessEntity();
    }
}