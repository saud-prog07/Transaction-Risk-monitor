package com.example.riskmonitoring.alertservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal riskScore;

    @Column(nullable = false)
    private String decision;

    @Column(nullable = false, length = 1000)
    private String reasons;

    @Column(nullable = false)
    private Instant flaggedAt;

    protected AlertEntity() {
    }

    public AlertEntity(String id, String transactionId, String accountId, String merchantId, BigDecimal amount,
            String currency, BigDecimal riskScore, String decision, String reasons, Instant flaggedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.riskScore = riskScore;
        this.decision = decision;
        this.reasons = reasons;
        this.flaggedAt = flaggedAt;
    }

    public String getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public String getDecision() {
        return decision;
    }

    public String getReasons() {
        return reasons;
    }

    public Instant getFlaggedAt() {
        return flaggedAt;
    }
}