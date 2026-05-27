package za.gov.helpdesk.notification.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String email, String actorName, String rawOtp, long expiryMinutes) {

        PasswordResetEmailNotificationMessage message = PasswordResetEmailNotificationMessage.builder()
                .email(email)
                .actorName(actorName)
                .otp(rawOtp)
                .optExpiryMin(expiryMinutes)
                .build();
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE,
                    RabbitMQConstants.PASSWORD_RESET_EMAIL_ROUTING_KEY,
                    message
            );
            log.info("Password reset email notification queued: email={}", message.getEmail());
        } catch (Exception e) {

            log.error("Failed to queue Password reset email notification: email={} error={}",
                    message.getEmail(), e.getMessage());
        }
    }
}
