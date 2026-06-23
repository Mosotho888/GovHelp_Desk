package za.gov.helpdesk.notification.messaging;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;
import za.gov.helpdesk.notification.metrics.NotificationMetrics;
import za.gov.helpdesk.notification.service.sla.SlaEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message consumer responsible for processing asynchronous SLA notification events from RabbitMQ.
 * Reads messages from the dedicated SLA email queue, evaluates whether the event represents an
 * approaching deadline warning or an absolute breach, coordinates outbound email delivery, and
 * handles manual channel-level acknowledgments (ACK/NACK).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaEmailNotificationConsumer {

    private final SlaEmailService slaEmailService;
    private final NotificationMetrics notificationMetrics;

    /**
     * Listens for incoming SLA escalation notification messages from the designated RabbitMQ queue.
     * Evaluates message payload flags to dispatch either warning alert notices or breach reports.
     * Handles manual message acknowledgment thresholds; transient infrastructure connectivity
     * bottlenecks trigger message re-queuing, whereas unrecoverable exceptions reject the payload
     * to force a DLQ route.
     *
     * @param message the parsed asynchronous notification payload containing detailed ticket SLA
     *     properties
     * @param rawMessage the raw AMQP wrapper holding network transport headers and delivery
     *     metadata
     * @param channel the communication channel used to manually signal processing outcomes back to
     *     the broker
     * @throws IOException if a network boundary communication error occurs during channel
     *     acknowledgment
     */
    @RabbitListener(queues = RabbitMQConstants.SLA_EMAIL_QUEUE)
    public void handle(
            final SlaEmailNotificationMessage message,
            final Message rawMessage,
            final Channel channel)
            throws IOException {

        final long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info(
                "Sla message received: ticketId={} agentName={} isWarning={}",
                message.getTicketId(),
                message.getAgentName(),
                message.isWarning());

        try {
            if (message.isWarning()) {
                slaEmailService.sendSlaWarning(
                        message.getAgentEmail(),
                        message.getAgentName(),
                        message.getTicketNumber(),
                        message.getTicketSubject(),
                        message.getDeadlineType(),
                        message.getDueAt());
            } else {
                slaEmailService.sendSlaBreach(
                        message.getAgentEmail(),
                        message.getAgentName(),
                        message.getTicketNumber(),
                        message.getTicketSubject(),
                        message.getDeadlineType());
            }

            channel.basicAck(tag, false);
            notificationMetrics.incrementEmailSent();
            log.info(
                    "sla saved and ACKed: ticketId={} agentName={} isWarning={}",
                    message.getTicketId(),
                    message.getAgentName(),
                    message.isWarning());
        } catch (final DataAccessException e) {
            log.warn(
                    "DB unavailable, re-queuing sla message: isWarning={} error={}",
                    message.isWarning(),
                    e.getMessage());
            notificationMetrics.incrementEmailFailed();
            channel.basicNack(tag, false, true);
        } catch (final Exception e) {
            log.error(
                    "Unrecoverable failure, routing to DLQ: isWarning={} error={}",
                    message.isWarning(),
                    e.getMessage());
            notificationMetrics.incrementEmailDlq();
            channel.basicNack(tag, false, false);
        }
    }
}
