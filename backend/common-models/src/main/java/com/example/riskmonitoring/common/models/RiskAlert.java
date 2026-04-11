package com.example.riskmonitoring.common.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RiskAlert - Message payload for risk alerts published to message queue
 */
public class RiskAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    private String alertId;
    private String transactionId;
    private String customerId;
    private String alertType;
    private String severity;
    private String riskLevel;
    private BigDecimal riskScore;
    private String reason;
    private String[] riskFactors;
    private LocalDateTime alertTime;
    private String status;
    private String actionRequired;

    public RiskAlert() {
    }

    public RiskAlert(String alertId, String transactionId, String alertType, String severity) {
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.alertType = alertType;
        this.severity = severity;
    }

    // Getters and Setters
    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String[] getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(String[] riskFactors) {
        this.riskFactors = riskFactors;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(String actionRequired) {
        this.actionRequired = actionRequired;
    }

    @Override
    public String toString() {
        return "RiskAlert{" +
                "alertId='" + alertId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", alertType='" + alertType + '\'' +
                ", severity='" + severity + '\'' +
                ", riskScore=" + riskScore +
                ", alertTime=" + alertTime +
                '}';
    }
}
