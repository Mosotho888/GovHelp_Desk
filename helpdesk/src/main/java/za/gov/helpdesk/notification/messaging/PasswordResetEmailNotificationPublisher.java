package za.gov.helpdesk.notification.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailNotificationPublisher {

    private final OutboxWriter outboxWriter;

    public void publish(String email, String actorName, String rawOtp, long expiryMinutes) {

        PasswordResetEmailNotificationMessage message = PasswordResetEmailNotificationMessage.builder().email(email)
                .actorName(actorName).otp(rawOtp).optExpiryMin(expiryMinutes).build();
        try {
            outboxWriter.write(OutboxEvent.EventType.PASSWORD_RESET_EMAIL.name(), AuditLog.EntityType.AUTH.name(), null,
                    message);
            log.info("Password reset email notification queued: email={}", message.getEmail());
        } catch (Exception e) {

            log.error("Failed to queue Password reset email notification: email={} error={}", message.getEmail(),
                    e.getMessage());
        }
    }
}
