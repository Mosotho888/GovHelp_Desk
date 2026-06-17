package za.gov.helpdesk.notification.messaging;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.service.auth.AuthEmailService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailNotificationConsumer {

    private final AuthEmailService authEmailService;

    @RabbitListener(queues = RabbitMQConstants.PASSWORD_RESET_EMAIL_QUEUE)
    public void handle(PasswordResetEmailNotificationMessage message, Message rawMessage, Channel channel)
            throws IOException {

        long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info("Password reset email received: email={}", message.getEmail());

        try {
            authEmailService.sendPasswordResetOtp(message);
            channel.basicAck(tag, false);
            log.info("Password reset email ACKed: email={}", message.getEmail());

        } catch (Exception e) {
            log.warn("Password reset email failed, re-queuing: email={} error={}", message.getEmail(), e.getMessage());
            channel.basicNack(tag, false, true);
        }
    }
}
