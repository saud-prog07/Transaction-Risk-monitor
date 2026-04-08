package com.example.riskmonitoring.alertservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulates email notifications for alert system without requiring actual email service.
 * In production, this would be replaced with JavaMailSender or third-party email service.
 * Logs all "sent" emails for audit trail and testing.
 */
@Slf4j
@Component
public class EmailNotificationSimulator {

    private final String adminEmail;
    private final String riskTeamEmail;
    private final String notificationPrefix;
    private final Map<String, Integer> emailSendLog;  // For tracking sent emails

    public EmailNotificationSimulator(
            @Value("${notification.email.admin-email:admin@risk-monitoring.local}") String adminEmail,
            @Value("${notification.email.risk-team-email:riskteam@risk-monitoring.local}") String riskTeamEmail,
            @Value("${notification.email.sender-prefix:[RISK-ALERT]}") String notificationPrefix) {
        this.adminEmail = adminEmail;
        this.riskTeamEmail = riskTeamEmail;
        this.notificationPrefix = notificationPrefix;
        this.emailSendLog = new HashMap<>();
    }

    /**
     * Simulates sending a high-risk alert email.
     * In production, this would use JavaMailSender or AWS SES.
     *
     * @param transactionId the transaction ID being alerted on
     * @param subject the email subject
     * @param body the email body
     */
    public void sendHighRiskAlert(String transactionId, String subject, String body) {
        try {
            String[] recipients = {adminEmail, riskTeamEmail};
            sendSimulatedEmail(recipients, subject, body, "HIGH_RISK_ALERT");
            
            // Track email send
            String logKey = "high_risk_alerts";
            emailSendLog.merge(logKey, 1, Integer::sum);

            log.info("SIMULATED_EMAIL_SENT: Recipients={}, TransactionId={}, Subject=\"{}\"",
                    String.join(", ", recipients), transactionId, subject);

        } catch (Exception ex) {
            log.error("Failed to send high-risk alert email: TransactionId={}", transactionId, ex);
        }
    }

    /**
     * Simulates sending a medium-risk alert email (optional).
     *
     * @param transactionId the transaction ID being alerted on
     * @param subject the email subject
     * @param body the email body
     */
    public void sendMediumRiskAlert(String transactionId, String subject, String body) {
        try {
            String[] recipients = {riskTeamEmail};
            sendSimulatedEmail(recipients, subject, body, "MEDIUM_RISK_ALERT");

            String logKey = "medium_risk_alerts";
            emailSendLog.merge(logKey, 1, Integer::sum);

            log.info("SIMULATED_EMAIL_SENT: Recipients={}, TransactionId={}, Subject=\"{}\"",
                    String.join(", ", recipients), transactionId, subject);

        } catch (Exception ex) {
            log.error("Failed to send medium-risk alert email: TransactionId={}", transactionId, ex);
        }
    }

    /**
     * Core method for simulating email sending.
     * Logs comprehensive details that would normally be sent as email.
     */
    private void sendSimulatedEmail(String[] recipients, String subject, String body, String alertType) {
        String timestamp = formatInstant(Instant.now());

        // Create simulated email log entry
        String emailLog = String.format(
                "==================== SIMULATED EMAIL ====================\n" +
                "Timestamp: %s\n" +
                "Alert Type: %s\n" +
                "From: alerts@risk-monitoring.local\n" +
                "To: %s\n" +
                "Subject: %s %s\n" +
                "Message-ID: <alert-%s@risk-monitoring.local>\n" +
                "Priority: high\n" +
                "X-Alert-Type: %s\n" +
                "---\n" +
                "%s\n" +
                "============================================================",
                timestamp,
                alertType,
                String.join("; ", recipients),
                notificationPrefix,
                subject,
                System.currentTimeMillis(),
                alertType,
                body);

        // Log as WARN to ensure visibility in logs
        log.warn("{}", emailLog);

        // Also create a summary log entry for quick reference
        log.info("EMAIL_SIMULATION: AlertType={}, Recipients={}, Timestamp={}",
                alertType, recipients.length, timestamp);
    }

    /**
     * Formats an Instant to a readable timestamp string.
     */
    private String formatInstant(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * Gets the total number of simulated emails sent by type.
     * Useful for monitoring and testing.
     *
     * @return map of email types to count
     */
    public Map<String, Integer> getEmailSendLog() {
        return new HashMap<>(emailSendLog);
    }

    /**
     * Gets count of high-risk alerts sent.
     */
    public int getHighRiskAlertsCount() {
        return emailSendLog.getOrDefault("high_risk_alerts", 0);
    }

    /**
     * Gets count of medium-risk alerts sent.
     */
    public int getMediumRiskAlertsCount() {
        return emailSendLog.getOrDefault("medium_risk_alerts", 0);
    }

    /**
     * Resets email send log (for testing).
     */
    public void resetEmailLog() {
        emailSendLog.clear();
        log.info("Email send log reset");
    }

    /**
     * Gets configured admin email address.
     */
    public String getAdminEmail() {
        return adminEmail;
    }

    /**
     * Gets configured risk team email address.
     */
    public String getRiskTeamEmail() {
        return riskTeamEmail;
    }
}
