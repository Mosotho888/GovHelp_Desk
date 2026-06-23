package za.gov.helpdesk.auditlog.messaging;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import za.gov.helpdesk.auditlog.dto.messaging.AuditLogMessage;
import za.gov.helpdesk.auditlog.dto.request.AuthAuditContext;
import za.gov.helpdesk.auditlog.mapper.AuditLogMapper;
import za.gov.helpdesk.auditlog.service.AuditService;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.metrics.NotificationMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message consumer responsible for processing asynchronous audit log events from RabbitMQ. Reads
 * messages from the dedicated audit queue, delegates storage logic to the audit service, and
 * handles message acknowledgments (ACK/NACK) based on system transaction outcomes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditService auditService;
    private final NotificationMetrics notificationMetrics;
    private final AuditLogMapper auditLogMapper;

    /**
     * Listens for incoming audit log messages from the designated RabbitMQ queue. Evaluates message
     * context, persists either an authentication record or standard operational record, and
     * performs channel-level manual acknowledgments. Transient database connectivity errors trigger
     * message re-queuing, while unrecoverable processing failures reject the message entirely.
     *
     * @param message the parsed asynchronous payload containing audit details
     * @param rawMessage the raw AMQP wrapper holding transport metadata properties
     * @param channel the communication channel used to manually signal processing success or
     *     failure
     * @throws IOException if a network communication boundary error occurs during channel
     *     acknowledgment
     */
    // @SuppressWarnings("PMD.AvoidCatchingGenericException")
    @RabbitListener(queues = RabbitMQConstants.AUDIT_QUEUE)
    public void handle(
            final AuditLogMessage message, final Message rawMessage, final Channel channel)
            throws IOException {

        final long tag = rawMessage.getMessageProperties().getDeliveryTag();

        log.info(
                "Audit message received: action={} entity={}/{}",
                message.getAction(),
                message.getEntityType(),
                message.getEntityId());

        try {
            if (message.isAuthLog()) {
                auditService.logAuth(
                        AuthAuditContext.builder()
                                .action(message.getAction())
                                .actorId(message.getActorId())
                                .actorName(message.getActorName())
                                .actorRole(message.getActorRole())
                                .ipAddress(message.getIpAddress())
                                .description(message.getDescription())
                                .build());
            } else {
                auditService.log(auditLogMapper.toAuditContext(message));
            }

            channel.basicAck(tag, false);
            notificationMetrics.incrementAuditSaved();
            log.info(
                    "Audit saved and ACKed: action={} entity={}/{}",
                    message.getAction(),
                    message.getEntityType(),
                    message.getEntityId());
        } catch (final DataAccessException e) {
            log.warn(
                    "DB unavailable, re-queuing audit log message payload block: action={}"
                            + " entityId={}",
                    message.getAction(),
                    message.getEntityId(),
                    e);
            notificationMetrics.incrementAuditFailed();
            channel.basicNack(tag, false, true);
        } catch (final RuntimeException e) {
            log.error(
                    "Unrecoverable processing failure, rejecting to DLQ channel: action={}"
                            + " entityId={}",
                    message.getAction(),
                    message.getEntityId(),
                    e);
            notificationMetrics.incrementAuditDlq();
            channel.basicNack(tag, false, false);
        }
    }
}
