package za.gov.helpdesk.ticket.event;

import org.springframework.stereotype.Component;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.messaging.TicketEmailNotificationPublisher;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Orchestration dispatcher component responsible for broadcasting ticket lifecycle state mutations.
 * Coordinates simultaneous multi-channel event publishing by delegating to security audit log
 * writers and notification outbox staging engines to ensure architectural decoupling.
 */
@Component
@RequiredArgsConstructor
public class TicketEventDispatcher {

    private final AuditEventPublisher auditPublisher;
    private final TicketEmailNotificationPublisher emailPublisher;

    /**
     * Unified entry point to broadcast a ticket modification event across both the persistent
     * security audit trail and the asynchronous client email communication pipelines.
     *
     * @param event the ticket change details to broadcast
     */
    public void publish(final TicketChangeEvent event) {
        publishAudit(event);
        publishEmail(event);
    }

    /**
     * Extracts security stakeholder metadata from a given ticket and forwards execution onto the
     * notification layer. Dispatches details targeting the original customer requester along with
     * the currently assigned technical support agent, if one is attached.
     *
     * @param event the ticket change details to broadcast
     */
    public void publishEmail(final TicketChangeEvent event) {
        final Ticket ticket = event.ticket();
        final User agentUser = ticket.getAssignee() != null ? ticket.getAssignee().getUser() : null;

        emailPublisher.publish(
                ticket, ticket.getRequester(), agentUser, event.action(), event.comment());
    }

    /**
     * Compiles data tracking differentials and passes execution onto the security tracking
     * publisher. Prepares immutable records charting specific field value migrations bound to the
     * ticket entity type.
     *
     * @param event the ticket change details to broadcast
     */
    public void publishAudit(final TicketChangeEvent event) {
        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                event.ticket().getId(),
                event.actor(),
                event.action(),
                event.oldValue(),
                event.newValue(),
                event.description());
    }
}
