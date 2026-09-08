package za.gov.helpdesk.notification.messaging;

import org.springframework.stereotype.Component;

import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message producer responsible for preparing and staging asynchronous SLA breach and warning
 * notifications. Constructs structural alert payloads and serializes them into the persistent
 * outbox repository using an {@link OutboxWriter} to comply with the Transactional Outbox Pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaEmailNotificationPublisher {

    private final OutboxWriter outboxWriter;

    /**
     * Stages an SLA notification event payload inside the outbox table. Implements transaction
     * atomicity guarantees prior to message broker routing.
     *
     * @param message the structured SLA notification payload record to write, with {@code
     *     isWarning} distinguishing an approaching-deadline warning from an absolute breach
     */
    public void publish(final SlaEmailNotificationMessage message) {

        outboxWriter.write(
                OutboxEvent.EventType.SLA_EMAIL.name(),
                AuditLog.EntityType.TICKET.name(),
                message.getTicketId(),
                message);
    }
}
