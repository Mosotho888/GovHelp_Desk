package za.gov.helpdesk.notification.messaging;

import java.time.LocalDateTime;

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
     * Stages an SLA approaching deadline warning notification event payload inside the outbox
     * table. Implements transaction atomicity guarantees prior to message broker routing.
     *
     * @param agentEmail the target assigned support agent email address destination
     * @param agentName the alphanumeric handle profile name of the target agent
     * @param ticketNumber the unique human-readable system generation tracking ticket string
     * @param ticketId the database primary unique identifier key of the ticket entity
     * @param ticketSubject the title or short summary of the underlying helpdesk issue
     * @param deadlineType the categorized SLA threshold boundary being checked (e.g., RESPONSE,
     *     RESOLUTION)
     * @param dueAt the absolute future timestamp indicating when the absolute breach window
     *     triggers
     */
    public void publishWarning(
            final String agentEmail,
            final String agentName,
            final String ticketNumber,
            final Long ticketId,
            final String ticketSubject,
            final String deadlineType,
            final LocalDateTime dueAt) {

        final SlaEmailNotificationMessage message =
                SlaEmailNotificationMessage.builder()
                        .agentEmail(agentEmail)
                        .agentName(agentName)
                        .ticketId(ticketId)
                        .ticketNumber(ticketNumber)
                        .ticketSubject(ticketSubject)
                        .deadlineType(deadlineType)
                        .dueAt(dueAt)
                        .isWarning(true)
                        .build();

        publish(message);
    }

    /**
     * Stages an SLA breach escalation event payload inside the outbox table to indicate that a
     * service level agreement threshold has lapsed.
     *
     * @param agentEmail the target assigned support agent email address destination
     * @param agentName the alphanumeric handle profile name of the target agent
     * @param ticketNumber the unique human-readable system generation tracking ticket string
     * @param ticketId the database primary unique identifier key of the ticket entity
     * @param ticketSubject the title or short summary of the underlying helpdesk issue
     * @param deadlineType the categorized SLA threshold boundary that was violated
     */
    public void publishBreach(
            final String agentEmail,
            final String agentName,
            final String ticketNumber,
            final Long ticketId,
            final String ticketSubject,
            final String deadlineType) {

        final SlaEmailNotificationMessage message =
                SlaEmailNotificationMessage.builder()
                        .agentEmail(agentEmail)
                        .agentName(agentName)
                        .ticketId(ticketId)
                        .ticketNumber(ticketNumber)
                        .ticketSubject(ticketSubject)
                        .deadlineType(deadlineType)
                        .isWarning(false)
                        .build();

        publish(message);
    }

    /**
     * Internal encapsulation method handling direct marshalling and persistence into the database
     * outbox layer. Ensures event logs are structurally tied to the target ticket aggregate root.
     *
     * @param message the structured SLA notification payload record to write
     */
    public void publish(final SlaEmailNotificationMessage message) {

        outboxWriter.write(
                OutboxEvent.EventType.SLA_EMAIL.name(),
                AuditLog.EntityType.TICKET.name(),
                message.getTicketId(),
                message);
    }
}
