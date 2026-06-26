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
     * @param ticket the active domain {@link Ticket} aggregate root suffering modification
     * @param actor the security {@link User} principal triggering the transactional action change
     * @param action the audited operational action classification marker tracking the phase
     *     mutation
     * @param oldValue the prior state string representation data configuration value before
     *     modification
     * @param newValue the updated state string representation target data configuration value
     * @param description a contextual tracking statement summarizing the intent or trigger of the
     *     change
     * @param comment an optional human-readable remark string or message note attached by the user
     */
    public void publish(
            final Ticket ticket,
            final User actor,
            final AuditLog.AuditAction action,
            final String oldValue,
            final String newValue,
            final String description,
            final String comment) {

        publishAudit(ticket, actor, action, oldValue, newValue, description);
        publishEmail(ticket, action, comment);
    }

    /**
     * Extracts security stakeholder metadata from a given ticket and forwards execution onto the
     * notification layer. Dispatches details targeting the original customer requester along with
     * the currently assigned technical support agent, if one is attached.
     *
     * @param ticket the active domain {@link Ticket} snapshot context
     * @param action the audited operational action type execution phase trigger
     * @param comment an optional remark narrative attached to the operational event notification
     */
    public void publishEmail(
            final Ticket ticket, final AuditLog.AuditAction action, final String comment) {

        final User agentUser = ticket.getAssignee() != null ? ticket.getAssignee().getUser() : null;

        emailPublisher.publish(ticket, ticket.getRequester(), agentUser, action, comment);
    }

    /**
     * Compiles data tracking differentials and passes execution onto the security tracking
     * publisher. Prepares immutable records charting specific field value migrations bound to the
     * ticket entity type.
     *
     * @param ticket the active domain {@link Ticket} instance snapshot
     * @param actor the active {@link User} profile identity executing the business operation
     * @param action the audited operational lifecycle phase action type
     * @param oldValue the baseline historical value pattern string prior to execution
     * @param newValue the targeted outcome value pattern string resulting from execution
     * @param description a descriptive overview logging the background conditions of the change
     */
    public void publishAudit(
            final Ticket ticket,
            final User actor,
            final AuditLog.AuditAction action,
            final String oldValue,
            final String newValue,
            final String description) {

        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticket.getId(),
                actor,
                action,
                oldValue,
                newValue,
                description);
    }
}
