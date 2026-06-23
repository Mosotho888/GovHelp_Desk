package za.gov.helpdesk.notification.messaging;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.service.auth.AuthEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message consumer responsible for processing asynchronous password reset email notifications from
 * RabbitMQ. Listens on the dedicated password reset queue, coordinates outbound email transmission
 * via the security mail service, and manages manual AMQP broker message channel acknowledgments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailNotificationConsumer {

    private final AuthEmailService authEmailService;

    /**
     * Listens for incoming password reset notification payloads from the designated broker queue.
     * Delegates the composition and dispatch of the One-Time Pin (OTP) email token to the
     * notification service layer. Performs a manual channel acknowledgment (ACK) upon successful
     * delivery, or re-queues (NACK) the message in the event of transient processing failures.
     *
     * @param message the parsed asynchronous notification data transfer payload containing user
     *     target details
     * @param rawMessage the raw AMQP wrapper holding network transmission properties and transport
     *     headers
     * @param channel the active communication channel used to manually signal processing success or
     *     failure back to the broker
     * @throws IOException if a network boundary error occurs during channel acknowledgment
     *     coordination
     */
    @RabbitListener(queues = RabbitMQConstants.PASSWORD_RESET_EMAIL_QUEUE)
    public void handle(
            final PasswordResetEmailNotificationMessage message,
            final Message rawMessage,
            final Channel channel)
            throws IOException {

        final long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info("Password reset email received: email={}", message.getEmail());

        try {
            authEmailService.sendPasswordResetOtp(message);
            channel.basicAck(tag, false);
            log.info("Password reset email ACKed: email={}", message.getEmail());

        } catch (final Exception e) {
            log.warn(
                    "Password reset email failed, re-queuing: email={} error={}",
                    message.getEmail(),
                    e.getMessage());
            channel.basicNack(tag, false, true);
        }
    }
}
