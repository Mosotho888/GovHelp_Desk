package za.gov.helpdesk.auditlog.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.auditlog.dto.messaging.AuditLogMessage;
import za.gov.helpdesk.auditlog.service.AuditService;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.metrics.NotificationMetrics;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditService auditService;
    private final NotificationMetrics notificationMetrics;

    @RabbitListener(queues = RabbitMQConstants.AUDIT_QUEUE)
    public void handle(AuditLogMessage message, Message rawMessage, Channel channel) throws IOException {

        long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info("Audit message received: action={} entity={}/{}", message.getAction(), message.getEntityType(), message.getEntityId());

        try {
            if (message.isAuthLog()) {
                auditService.logAuth(
                        message.getAction(),
                        message.getActorId(),
                        message.getActorName(),
                        message.getActorRole(),
                        message.getIpAddress(),
                        message.getDescription());
            } else {
                auditService.log(
                        message.getEntityType(),
                        message.getEntityId(),
                        message.getActorId(),
                        message.getActorName(),
                        message.getActorRole(),
                        message.getIpAddress(),
                        message.getAction(),
                        message.getOldValue(),
                        message.getNewValue(),
                        message.getDescription()
                );
            }

            channel.basicAck(tag, false);
            notificationMetrics.incrementAuditSaved();
            log.info("Audit saved and ACKed: action={} entity={}/{}",
                    message.getAction(), message.getEntityType(), message.getEntityId());
        } catch (DataAccessException e) {
            log.warn("DB unavailable, re-queuing audit log: action={} error={}", message.getAction(), e.getMessage());
            notificationMetrics.incrementAuditFailed();
            channel.basicNack(tag, false,true);
        } catch (Exception e) {
            log.error("Unrecoverable failure, routing to DLQ: action={} error={}",
                    message.getAction(), e.getMessage());
            notificationMetrics.incrementAuditDlq();
            channel.basicNack(tag, false, false);
        }
    }
}
