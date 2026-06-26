package za.gov.helpdesk.notification.messaging;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.notification.metrics.NotificationMetrics;
import za.gov.helpdesk.notification.service.ticket.TicketEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message consumer responsible for processing asynchronous ticket lifecycle notification events
 * from RabbitMQ. Listens on the dedicated ticket email queue, dispatches specific transactional
 * communications (creation, assignment, status transitions, commentary additions, or closure) via
 * the underlying mail service, and manages explicit manual channel-level acknowledgments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEmailNotificationConsumer {

    private final TicketEmailService ticketEmailService;
    private final NotificationMetrics notificationMetrics;

    /**
     * Listens for incoming ticket lifecycle notification payloads from the designated RabbitMQ
     * queue. Evaluates the specific trigger event type to route execution to the corresponding
     * email composition strategy. Invokes manual AMQP channel delivery adjustments, committing
     * processing success (ACK) or re-queuing (NACK) transient transport disruptions for automatic
     * delivery retries.
     *
     * @param message the parsed asynchronous notification data transfer payload containing targeted
     *     ticket metadata
     * @param rawMessage the raw AMQP wrapper holding network transmission variables and broker
     *     envelope attributes
     * @param channel the communication channel context used to manually signal delivery
     *     confirmation outcomes
     * @throws IOException if a network or data-stream boundary communication failure occurs during
     *     channel interaction
     */
    @RabbitListener(queues = RabbitMQConstants.TICKET_EMAIL_QUEUE)
    public void handle(
            final TicketEmailNotificationMessage message,
            final Message rawMessage,
            final Channel channel)
            throws IOException {

        final long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info(
                "Ticket email notification received: trigger={} ticket={}",
                message.getTrigger(),
                message.getTicketNumber());

        try {
            switch (message.getTrigger()) {
                case TICKET_CREATED -> ticketEmailService.sendTicketCreated(message);
                case ASSIGNED_TO_AGENT -> ticketEmailService.sendTicketAssigned(message);
                case STATUS_CHANGED -> ticketEmailService.sendStatusChanged(message);
                case COMMENT_ADDED -> ticketEmailService.sendCommentAdded(message);
                case TICKET_CLOSED -> ticketEmailService.sendTicketClosed(message);
                default ->
                        log.debug("No ticket email handler for trigger={}", message.getTrigger());
            }

            channel.basicAck(tag, false);
            notificationMetrics.incrementEmailSent();
            log.info(
                    "Ticket email ACKed: trigger={} ticket={}",
                    message.getTrigger(),
                    message.getTicketNumber());

        } catch (final Exception e) {
            // SMTP failed - requeue so it retries
            // After max-attempts it goes to TICKET_EMAIL_DLQ automatically
            log.warn(
                    "Ticket email failed, re-queuing: trigger={} ticket={} error={}",
                    message.getTrigger(),
                    message.getTicketNumber(),
                    e.getMessage());
            notificationMetrics.incrementEmailFailed();
            channel.basicNack(tag, false, true);
        }
    }
}
