package za.gov.helpdesk.notification.messaging;

import org.springframework.stereotype.Component;

import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message producer responsible for staging outbound security password reset email events.
 * Constructs the notification payloads and writes them reliably to the persistent outbox store via
 * an {@link OutboxWriter} to satisfy the requirements of the Transactional Outbox Pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailNotificationPublisher {

    private final OutboxWriter outboxWriter;

    /**
     * Compiles a password reset security token event payload and persists it inside the outbox
     * repository infrastructure. This ensures atomic execution boundaries between the database
     * state changes and downstream email delivery microservice pipelines.
     *
     * @param email the target destination email address where the recovery code must be sent
     * @param actorName the descriptive profile name or identification handle of the user account
     * @param rawOtp the un-hashed, plain-text security verification One-Time Pin string
     * @param expiryMinutes the remaining duration interval in minutes before the verification code
     *     lapses
     */
    public void publish(
            final String email,
            final String actorName,
            final String rawOtp,
            final long expiryMinutes) {

        final PasswordResetEmailNotificationMessage message =
                PasswordResetEmailNotificationMessage.builder()
                        .email(email)
                        .actorName(actorName)
                        .otp(rawOtp)
                        .optExpiryMin(expiryMinutes)
                        .build();
        try {
            outboxWriter.write(
                    OutboxEvent.EventType.PASSWORD_RESET_EMAIL.name(),
                    AuditLog.EntityType.AUTH.name(),
                    null,
                    message);
            log.info("Password reset email notification queued: email={}", message.getEmail());
        } catch (final Exception e) {

            log.error(
                    "Failed to queue Password reset email notification: email={} error={}",
                    message.getEmail(),
                    e.getMessage());
        }
    }
}
