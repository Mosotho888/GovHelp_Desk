package za.gov.helpdesk.outbox.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auditlog.dto.messaging.AuditLogMessage;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE   = 50;

    /** Maps event_type column values to RabbitMQ routing keys. */
    private static final Map<String, String> ROUTING_KEYS = Map.of(
            "AUDIT",               RabbitMQConstants.AUDIT_ROUTING_KEY,
            "TICKET_EMAIL",        RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY,
            "PASSWORD_RESET_EMAIL", RabbitMQConstants.PASSWORD_RESET_EMAIL_ROUTING_KEY,
            "SLA_EMAIL",           RabbitMQConstants.SLA_EMAIL_ROUTING_KEY
    );

    // Maps eventType column value -> the exact DTO class the consumer expects.
    // Jackson deserialises the JSON payload into this type, so
    // Jackson2JsonMessageConverter writes the correct __TypeId__ header,
    // and the consumer receives a fully-typed object - no cast needed.
    private static final Map<String, Class<?>> TYPE_MAP = Map.of(
            "AUDIT",               AuditLogMessage.class,
            "TICKET_EMAIL",        TicketEmailNotificationMessage.class,
            "PASSWORD_RESET_EMAIL", PasswordResetEmailNotificationMessage.class,
            "SLA_EMAIL", SlaEmailNotificationMessage.class
    );

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:PT5S}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = fetchPendingBatch();
        if (batch.isEmpty()) return;

        log.debug("Outbox relay: processing {} events", batch.size());
        batch.forEach(this::processOne);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> fetchPendingBatch() {
        return outboxRepository.findNextPendingBatch(PageRequest.of(0, BATCH_SIZE));
    }

    @Transactional
    public void processOne(OutboxEvent event) {

        OutboxEvent locked = outboxRepository.findById(event.getId()).orElse(null);
        if (locked == null || locked.getStatus() != OutboxEvent.Status.PENDING) {
            return;
        }

        locked.setStatus(OutboxEvent.Status.PROCESSING);
        locked.setAttempts(locked.getAttempts() + 1);
        outboxRepository.save(locked);

        String routingKey = ROUTING_KEYS.get(locked.getEventType());
        Class<?> targetClass = TYPE_MAP.get(locked.getEventType());
        if (routingKey == null || targetClass == null) {
            fail(locked, "Unknown event type: " + locked.getEventType());
            return;
        }

        try {
            // Deserialise to the exact DTO type 0 NOT Object, not LinkedHashMap.
            // Jackson2JsonMessageConverter then serialises this typed instance
            // and writes __TypeId__ = "za.gov.helpdesk...AuditLogMessage"
            // (or whichever class) into the AMQP header automatically.
            // The consumer receives a fully-typed object, zero changes needed there.
            Object message = objectMapper.readValue(locked.getPayload(), targetClass);
            rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE, routingKey, message);

            locked.setStatus(OutboxEvent.Status.PROCESSED);
            locked.setProcessedAt(LocalDateTime.now());
            outboxRepository.save(locked);

            log.info("Outbox event published: id={} type={} aggregate={}/{}",
                    locked.getId(), locked.getEventType(),
                    locked.getAggregateType(), locked.getAggregateId());

        } catch (Exception e) {
            log.error("Outbox publish failed: id={} attempt={} error={}",
                    locked.getId(), locked.getAttempts(), e.getMessage());

            if (locked.getAttempts() >= MAX_ATTEMPTS) {
                fail(locked, e.getMessage());
            } else {
                locked.setStatus(OutboxEvent.Status.PENDING); // retry on next poll
                locked.setLastError(e.getMessage());
                outboxRepository.save(locked);
            }
        }
    }

    @Scheduled(cron = "${app.outbox.purge-cron:0 0 3 * * *}")
    @Transactional
    public void purgeProcessed() {
        int deleted = outboxRepository.deleteProcessedBefore(
                LocalDateTime.now().minusDays(7));
        if (deleted > 0) {
            log.info("Outbox purge: deleted {} processed events", deleted);
        }
    }

    private void fail(OutboxEvent event, String error) {
        event.setStatus(OutboxEvent.Status.FAILED);
        event.setLastError(error);
        outboxRepository.save(event);
        log.error("Outbox event permanently failed after {} attempts: id={} type={} error={}",
                event.getAttempts(), event.getId(), event.getEventType(), error);
    }
}
