package com.example.riskmonitoring.alertservice.service;

import com.example.riskmonitoring.alertservice.domain.FlaggedTransaction;
import com.example.riskmonitoring.common.logging.StructuredLogger;
import com.example.riskmonitoring.common.models.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for notifying stakeholders about high-risk transactions.
 * Handles multi-channel notifications: logs, email simulation, and alerts.
 */
@Slf4j
@Service
public class NotificationService {

    private final EmailNotificationSimulator emailNotificationSimulator;
    private final boolean emailNotificationsEnabled;
    private final boolean alertLoggingEnabled;
    private final StructuredLogger structuredLogger = StructuredLogger.getLogger(NotificationService.class);

    public NotificationService(
            EmailNotificationSimulator emailNotificationSimulator,
            @Value("${notification.email.enabled:true}") boolean emailNotificationsEnabled,
            @Value("${notification.alert-logging.enabled:true}") boolean alertLoggingEnabled) {
        this.emailNotificationSimulator = emailNotificationSimulator;
        this.emailNotificationsEnabled = emailNotificationsEnabled;
        this.alertLoggingEnabled = alertLoggingEnabled;
    }

    /**
     * Sends notifications for a flagged transaction based on risk level.
     * HIGH risk triggers multi-channel notifications.
     * MEDIUM risk triggers warning logs.
     *
     * @param flaggedTransaction the flagged transaction requiring notification
     */
    public void notifyFlaggedTransaction(FlaggedTransaction flaggedTransaction) {
        if (flaggedTransaction == null) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "NULL_TRANSACTION");
            structuredLogger.warn("Attempted to send notification for null transaction", context);
            return;
        }

        try {
            RiskLevel riskLevel = flaggedTransaction.getRiskLevel();
            structuredLogger.setTransactionId(flaggedTransaction.getTransactionId().toString());

            if (riskLevel == RiskLevel.HIGH) {
                notifyHighRiskTransaction(flaggedTransaction);
            } else if (riskLevel == RiskLevel.MEDIUM) {
                notifyMediumRiskTransaction(flaggedTransaction);
            } else {
                logLowRiskTransaction(flaggedTransaction);
            }

        } catch (Exception ex) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "NOTIFICATION_ERROR");
            context.put("transactionId", flaggedTransaction.getTransactionId());
            structuredLogger.error("Error processing notification", context, ex);
        }
    }

    /**
     * Sends multi-channel notifications for HIGH risk transactions.
     * Includes email simulation, structured logs, and alerts.
     */
    private void notifyHighRiskTransaction(FlaggedTransaction flaggedTransaction) {
        String txId = flaggedTransaction.getTransactionId().toString();
        structuredLogger.setTransactionId(txId);
        
        String formattedTime = formatInstant(flaggedTransaction.getCreatedAt());

        // Create alert message
        String alertMessage = String.format(
                "⚠️  CRITICAL: HIGH RISK TRANSACTION DETECTED\n" +
                "   TransactionId: %s\n" +
                "   RiskLevel: %s\n" +
                "   Timestamp: %s\n" +
                "   Reason: %s\n" +
                "   AlertId: %d",
                flaggedTransaction.getTransactionId(),
                flaggedTransaction.getRiskLevel(),
                formattedTime,
                flaggedTransaction.getReason(),
                flaggedTransaction.getId());

        // Log as error (highest priority for alerting systems)
        if (alertLoggingEnabled) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "HIGH_RISK_TRANSACTION");
            context.put("riskLevel", "HIGH");
            context.put("reason", flaggedTransaction.getReason());
            structuredLogger.info("High-risk transaction flagged", context);
        }

        // Send email notification
        if (emailNotificationsEnabled) {
            String emailSubject = String.format(
                    "[URGENT] High Risk Transaction Alert - %s",
                    flaggedTransaction.getTransactionId());

            String emailBody = buildHighRiskEmailBody(flaggedTransaction, formattedTime);

            try {
                emailNotificationSimulator.sendHighRiskAlert(
                        flaggedTransaction.getTransactionId().toString(),
                        emailSubject,
                        emailBody);
                Map<String, Object> context = new HashMap<>();
                context.put("event", "ALERT_EMAIL_SENT");
                context.put("channel", "EMAIL");
                structuredLogger.info("High-risk alert email sent", context);
            } catch (Exception ex) {
                Map<String, Object> context = new HashMap<>();
                context.put("event", "EMAIL_SEND_FAILED");
                context.put("channel", "EMAIL");
                structuredLogger.error("Failed to send high risk alert email", context, ex);
            }
        }

        // Log structured metric for monitoring
        Map<String, Object> context = new HashMap<>();
        context.put("event", "HIGH_RISK_NOTIFICATION");
        context.put("status", "COMPLETED");
        structuredLogger.info("High-risk transaction notification processed", context);
    }

    /**
     * Sends warning log for MEDIUM risk transactions.
     */
    private void notifyMediumRiskTransaction(FlaggedTransaction flaggedTransaction) {
        String txId = flaggedTransaction.getTransactionId().toString();
        structuredLogger.setTransactionId(txId);
        
        String formattedTime = formatInstant(flaggedTransaction.getCreatedAt());

        if (alertLoggingEnabled) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "MEDIUM_RISK_TRANSACTION");
            context.put("riskLevel", "MEDIUM");
            context.put("reason", flaggedTransaction.getReason());
            structuredLogger.warn("Medium-risk transaction flagged", context);
        }

        Map<String, Object> context = new HashMap<>();
        context.put("event", "MEDIUM_RISK_NOTIFICATION");
        context.put("status", "COMPLETED");
        structuredLogger.info("Medium-risk transaction notification processed", context);
    }

    /**
     * Logs LOW risk transactions (for audit trail).
     */
    private void logLowRiskTransaction(FlaggedTransaction flaggedTransaction) {
        String txId = flaggedTransaction.getTransactionId().toString();
        structuredLogger.setTransactionId(txId);
        
        if (alertLoggingEnabled) {
            Map<String, Object> context = new HashMap<>();
            context.put("event", "LOW_RISK_TRANSACTION");
            context.put("status", "NO_ACTION_REQUIRED");
            structuredLogger.info("Low risk transaction - No notification required", context);
        }
    }

    /**
     * Builds detailed email body for high-risk alerts.
     */
    private String buildHighRiskEmailBody(FlaggedTransaction transaction, String formattedTime) {
        return String.format(
                "URGENT SECURITY ALERT\n" +
                "====================\n\n" +
                "A high-risk transaction has been flagged in the system.\n\n" +
                "TRANSACTION DETAILS\n" +
                "-------------------\n" +
                "Transaction ID: %s\n" +
                "Alert ID: %d\n" +
                "Risk Level: %s\n" +
                "Detected At: %s\n\n" +

                "RISK REASON\n" +
                "-----------\n" +
                "%s\n\n" +

                "REQUIRED ACTION\n" +
                "---------------\n" +
                "1. Log in to the Risk Monitoring Dashboard\n" +
                "2. View transaction details and risk factors\n" +
                "3. Review the transaction reason\n" +
                "4. Approve or block the transaction\n\n" +

                "SYSTEM INFO\n" +
                "-----------\n" +
                "Service: Alert Service\n" +
                "Notification Type: High Risk Alert\n" +
                "Alert Generated: %s\n\n" +

                "---\n" +
                "This is an automated alert. Do not reply to this email.\n" +
                "Contact your administrator for access to the Risk Monitoring Dashboard.",
                transaction.getTransactionId(),
                transaction.getId(),
                transaction.getRiskLevel(),
                formattedTime,
                transaction.getReason(),
                formattedTime);
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
     * Checks if email notifications are enabled.
     */
    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    /**
     * Checks if alert logging is enabled.
     */
    public boolean isAlertLoggingEnabled() {
        return alertLoggingEnabled;
    }
}
