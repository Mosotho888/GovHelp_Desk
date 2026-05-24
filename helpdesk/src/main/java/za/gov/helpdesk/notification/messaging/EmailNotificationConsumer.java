package za.gov.helpdesk.notification.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.EmailNotificationMessage;
import za.gov.helpdesk.notification.service.EmailService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConstants.EMAIL_QUEUE)
    public void handle(EmailNotificationMessage message,
                       Message rawMessage,
                       Channel channel) throws IOException {

        long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info("Email notification received: trigger={} ticket={}",
                message.getTrigger(), message.getTicketNumber());

        try {
            switch (message.getTrigger()) {
                case TICKET_CREATED    -> emailService.sendTicketCreated(message);
                case ASSIGNED_TO_AGENT -> emailService.sendTicketAssigned(message);
                case STATUS_CHANGED    -> emailService.sendStatusChanged(message);
                case COMMENT_ADDED     -> emailService.sendCommentAdded(message);
                case TICKET_CLOSED     -> emailService.sendTicketClosed(message);
                default -> log.debug("No email handler for trigger={}", message.getTrigger());
            }

            channel.basicAck(tag, false);
            log.info("Email ACKed: trigger={} ticket={}",
                    message.getTrigger(), message.getTicketNumber());

        } catch (Exception e) {
            // SMTP failed — requeue so it retries
            // After max-attempts it goes to EMAIL_DLQ automatically
            log.warn("Email failed, re-queuing: trigger={} ticket={} error={}",
                    message.getTrigger(), message.getTicketNumber(), e.getMessage());
            channel.basicNack(tag, false, true);
        }
    }
}
