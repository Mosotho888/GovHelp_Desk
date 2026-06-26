package za.gov.helpdesk.notification.messaging;

import org.springframework.stereotype.Component;

import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message producer responsible for staging outbound ticket lifecycle event notifications.
 * Constructs uniform event payloads capturing ticket state changes, assignments, or commentaries,
 * and commits them securely into the transactional outbox database store via an {@link
 * OutboxWriter} to guarantee eventual consistency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEmailNotificationPublisher {

    private final OutboxWriter outboxWriter;

    /**
     * Translates a runtime ticket aggregate operation into a structured notification payload and
     * records it within the outbox boundary. Guarantees that even if the messaging broker
     * encounters a connection breakdown, the core business ticket transaction will not fail.
     *
     * @param ticket the active domain {@link Ticket} state snapshot tracking the change
     * @param customer the domain {@link User} entity representing the underlying client owner
     * @param agentUser the domain {@link User} representing the assigned support agent, or null if
     *     unassigned
     * @param trigger the audited action lifecycle phase identifier executing this notification
     *     build
     * @param comment optional textual commentary details or remark strings attached to the action
     *     context
     */
    public void publish(
            final Ticket ticket,
            final User customer,
            final User agentUser,
            final AuditLog.AuditAction trigger,
            final String comment) {

        final TicketEmailNotificationMessage message =
                TicketEmailNotificationMessage.builder()
                        .trigger(trigger)
                        .ticketId(ticket.getId())
                        .ticketNumber("TKT-" + ticket.getId())
                        .ticketSubject(ticket.getSubject())
                        .ticketStatus(ticket.getStatus().name())
                        .ticketPriority(ticket.getPriority().name())
                        .comment(comment)
                        .customerEmail(customer.getEmail())
                        .customerName(customer.getName())
                        .agentEmail(agentUser != null ? agentUser.getEmail() : null)
                        .agentName(agentUser != null ? agentUser.getName() : null)
                        .build();

        try {

            outboxWriter.write(
                    OutboxEvent.EventType.TICKET_EMAIL.name(),
                    AuditLog.EntityType.TICKET.name(),
                    message.getTicketId(),
                    message);

            log.info("Email notification queued: trigger={} ticket={}", trigger, ticket.getId());
        } catch (final Exception e) {
            // Broker down - log but don't fail the ticket operation
            log.error(
                    "Failed to queue email notification: trigger={} ticket={} error={}",
                    trigger,
                    ticket.getId(),
                    e.getMessage());
        }
    }
}
