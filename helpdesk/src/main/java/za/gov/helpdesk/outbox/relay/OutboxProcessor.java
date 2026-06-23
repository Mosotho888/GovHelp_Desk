package za.gov.helpdesk.outbox.relay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import za.gov.helpdesk.auditlog.dto.messaging.AuditLogMessage;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.outbox.metrics.OutboxMetrics;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service component responsible for executing the background polling and dispatching phases of the
 * Transactional Outbox Pattern. Isolates and processes pending transaction logs, unmarshalls
 * structured JSON event text into target domain payloads, coordinates reliable message broker
 * delivery, and manages state machines and resilience retry metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MIN_PAGE = 0;
    private static final int INCREMENT = 1;

    private static final Map<String, String> ROUTING_KEYS =
            Map.of(
                    "AUDIT", RabbitMQConstants.AUDIT_ROUTING_KEY,
                    "TICKET_EMAIL", RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY,
                    "PASSWORD_RESET_EMAIL", RabbitMQConstants.PASSWORD_RESET_EMAIL_ROUTING_KEY,
                    "SLA_EMAIL", RabbitMQConstants.SLA_EMAIL_ROUTING_KEY);

    private static final Map<String, Class<?>> TYPE_MAP =
            Map.of(
                    "AUDIT", AuditLogMessage.class,
                    "TICKET_EMAIL", TicketEmailNotificationMessage.class,
                    "PASSWORD_RESET_EMAIL", PasswordResetEmailNotificationMessage.class,
                    "SLA_EMAIL", SlaEmailNotificationMessage.class);

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxMetrics outboxMetrics;

    /**
     * Identifies and returns a localized page batch collection of unique identifiers matching
     * outbox events currently sitting in a baseline pending state. Run within a standard
     * transactional boundary to allow fast streaming lookups.
     *
     * @param batchSize the maximum number of record identifiers to return in the page request
     * @return a {@link List} containing the primary long keys of candidate pending outbox records
     */
    @Transactional
    public List<Long> fetchNextPendingIdsBatch(final int batchSize) {
        return outboxRepository.findNextPendingIds(PageRequest.of(MIN_PAGE, batchSize));
    }

    /**
     * Isolates, locks, and dispatches an individual outbox log record securely within its own
     * isolated data transaction boundary using {@link Propagation#REQUIRES_NEW}. Guarantees
     * pessimistic isolation states such that processing failure rollbacks or transient message
     * broker bottlenecks do not interfere with adjacent outbox item sweeps. Handles linear retry
     * ceilings, transitioning persistent states into dead-letter markers once threshold retry
     * tolerances exhaust.
     *
     * @param eventId the primary database identifier key of the targeted outbox row to lock
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOneSecurely(final Long eventId) {
        final OutboxEvent locked = outboxRepository.findById(eventId).orElse(null);
        if (locked == null || locked.getStatus() != OutboxEvent.Status.PENDING) {
            return;
        }

        locked.setStatus(OutboxEvent.Status.PROCESSING);
        locked.setAttempts(locked.getAttempts() + INCREMENT);
        outboxRepository.flush();

        final String routingKey = ROUTING_KEYS.get(locked.getEventType());
        final Class<?> targetClass = TYPE_MAP.get(locked.getEventType());
        if (routingKey == null || targetClass == null) {
            fail(
                    locked,
                    "Unknown domain outbox event destination type signature mapping: "
                            + locked.getEventType());
            return;
        }

        try {
            final Object message = objectMapper.readValue(locked.getPayload(), targetClass);
            rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE, routingKey, message);

            locked.setStatus(OutboxEvent.Status.PROCESSED);
            locked.setProcessedAt(LocalDateTime.now());
            outboxRepository.save(locked);

            outboxMetrics.incrementPublished();
            log.info(
                    "Outbox event published cleanly: id={} type={}",
                    locked.getId(),
                    locked.getEventType());

        } catch (final Exception e) {
            log.error(
                    "Outbox execution processing exception caught for record: id={} attempt={}"
                            + " error={}",
                    locked.getId(),
                    locked.getAttempts(),
                    e.getMessage());

            outboxMetrics.incrementFailed();

            if (locked.getAttempts() >= MAX_ATTEMPTS) {
                fail(locked, e.getMessage());
            } else {
                locked.setStatus(OutboxEvent.Status.PENDING);
                locked.setLastError(e.getMessage());
                outboxRepository.save(locked);
            }
        }
    }

    /**
     * Terminates an event's life cycle by shifting its persistent state into an un-retryable final
     * failure condition. Prevents toxic payload data configurations from looping endlessly inside
     * the outbox polling worker pipeline.
     *
     * @param event the active domain {@link OutboxEvent} instance mapping to the bad execution
     *     trace
     * @param error the descriptive message payload containing structural tracking parameters
     */
    private void fail(final OutboxEvent event, final String error) {
        event.setStatus(OutboxEvent.Status.FAILED);
        event.setLastError(error);
        outboxRepository.save(event);

        outboxMetrics.incrementDeadLetter();
        log.error(
                "Outbox transaction entry routed permanently to dead-letter state: attempts={}"
                        + " id={} type={} error={}",
                event.getAttempts(),
                event.getId(),
                event.getEventType(),
                error);
    }
}
