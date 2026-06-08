package za.gov.helpdesk.notification.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.notification.metrics.NotificationMetrics;
import za.gov.helpdesk.notification.service.ticket.TicketEmailService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEmailNotificationConsumer {

    private final TicketEmailService ticketEmailService;
    private final NotificationMetrics notificationMetrics;

    @RabbitListener(queues = RabbitMQConstants.TICKET_EMAIL_QUEUE)
    public void handle(TicketEmailNotificationMessage message,
                       Message rawMessage,
                       Channel channel) throws IOException {

        long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info("Ticket email notification received: trigger={} ticket={}",
                message.getTrigger(), message.getTicketNumber());

        try {
            switch (message.getTrigger()) {
                case TICKET_CREATED    -> ticketEmailService.sendTicketCreated(message);
                case ASSIGNED_TO_AGENT -> ticketEmailService.sendTicketAssigned(message);
                case STATUS_CHANGED    -> ticketEmailService.sendStatusChanged(message);
                case COMMENT_ADDED     -> ticketEmailService.sendCommentAdded(message);
                case TICKET_CLOSED     -> ticketEmailService.sendTicketClosed(message);
                default -> log.debug("No ticket email handler for trigger={}", message.getTrigger());
            }

            channel.basicAck(tag, false);
            notificationMetrics.incrementEmailSent();
            log.info("Ticket email ACKed: trigger={} ticket={}",
                    message.getTrigger(), message.getTicketNumber());

        } catch (Exception e) {
            // SMTP failed - requeue so it retries
            // After max-attempts it goes to TICKET_EMAIL_DLQ automatically
            log.warn("Ticket email failed, re-queuing: trigger={} ticket={} error={}",
                    message.getTrigger(), message.getTicketNumber(), e.getMessage());
            notificationMetrics.incrementEmailFailed();
            channel.basicNack(tag, false, true);
        }
    }
}
