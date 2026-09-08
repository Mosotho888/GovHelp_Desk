package za.gov.helpdesk.ticket.event;

import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

/**
 * Immutable container describing a single ticket lifecycle mutation, bundling everything {@link
 * TicketEventDispatcher} needs to broadcast the change across the audit trail and email
 * notification channels.
 *
 * @param ticket the active domain {@link Ticket} aggregate root suffering modification
 * @param actor the security {@link User} principal triggering the transactional action change
 * @param action the audited operational action classification marker tracking the phase mutation
 * @param oldValue the prior state string representation data configuration value before
 *     modification, or {@code null} when not applicable
 * @param newValue the updated state string representation target data configuration value, or
 *     {@code null} when not applicable
 * @param description a contextual tracking statement summarizing the intent or trigger of the
 *     change, or {@code null} when not applicable
 * @param comment an optional human-readable remark string or message note attached by the user
 */
public record TicketChangeEvent(
        Ticket ticket,
        User actor,
        AuditLog.AuditAction action,
        String oldValue,
        String newValue,
        String description,
        String comment) {}
