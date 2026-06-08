package za.gov.helpdesk.notification.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;
import za.gov.helpdesk.notification.metrics.NotificationMetrics;
import za.gov.helpdesk.notification.service.sla.SlaEmailService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaEmailNotificationConsumer {

    private final SlaEmailService slaEmailService;
    private final NotificationMetrics notificationMetrics;

    @RabbitListener(queues = RabbitMQConstants.SLA_EMAIL_QUEUE)
    public void handle(SlaEmailNotificationMessage message, Message rawMessage, Channel channel) throws IOException {

        long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info("Sla message received: ticketId={} agentName={} isWarning={}", message.getTicketId(), message.getAgentName(), message.isWarning());

        try {
            if (message.isWarning()) {
                slaEmailService.sendSlaWarning(
                        message.getAgentEmail(),
                        message.getAgentName(),
                        message.getTicketNumber(),
                        message.getTicketSubject(),
                        message.getDeadlineType(),
                        message.getDueAt()
                );
            } else {
                slaEmailService.sendSlaBreach(
                        message.getAgentEmail(),
                        message.getAgentName(),
                        message.getTicketNumber(),
                        message.getTicketSubject(),
                        message.getDeadlineType()
                );
            }

            channel.basicAck(tag, false);
            notificationMetrics.incrementEmailSent();
            log.info("sla saved and ACKed: ticketId={} agentName={} isWarning={}",
                    message.getTicketId(), message.getAgentName(), message.isWarning());
        } catch (DataAccessException e) {
            log.warn("DB unavailable, re-queuing sla message: isWarning={} error={}", message.isWarning(), e.getMessage());
            notificationMetrics.incrementEmailFailed();
            channel.basicNack(tag, false,true);
        } catch (Exception e) {
            log.error("Unrecoverable failure, routing to DLQ: isWarning={} error={}",
                    message.isWarning(), e.getMessage());
            notificationMetrics.incrementEmailDlq();
            channel.basicNack(tag, false, false);
        }
    }
}
