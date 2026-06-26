package za.gov.helpdesk.ticket.service.impl;

import org.springframework.stereotype.Component;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.service.AgentQueryHelper;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.sla.service.SlaService;
import za.gov.helpdesk.ticket.event.TicketEventDispatcher;
import za.gov.helpdesk.ticket.metrics.TicketMetrics;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.policy.TicketStatusTransitionPolicy;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Coordinator component handling mutating operational vectors over existing support tickets.
 * Concentrates state rules, compliance events, and infrastructure metric updates to decouple direct
 * fan-out complexity boundaries from primary service implementations.
 */
@Component
@RequiredArgsConstructor
public class TicketUpdateCoordinator {

    private final TicketRepository ticketRepository;
    private final AgentQueryHelper agentQuery;
    private final TicketEventDispatcher eventDispatcher;
    private final TicketStatusTransitionPolicy transitionPolicy;
    private final SlaService slaService;
    private final TicketMetrics ticketMetrics;

    /**
     * Handles downstream infrastructural orchestrations required immediately following a ticket
     * registration, including SLA timeline compilation, creation counters, and baseline audit
     * publishing.
     *
     * @param ticket the newly persisted domain entity instance
     * @param actor the system identity who requested creation
     */
    public void handlePostCreation(final Ticket ticket, final User actor) {
        slaService.initializeSla(ticket);
        ticketMetrics.incrementCreated();

        eventDispatcher.publish(
                ticket,
                actor,
                AuditLog.AuditAction.TICKET_CREATED,
                null,
                Ticket.Status.OPEN.name(),
                "Ticket created: " + ticket.getSubject(),
                null);
    }

    /**
     * Executes chronological deletion tracking events before permanently purging a ticket instance
     * from database storage.
     *
     * @param ticket the domain entity instance targeted for deletion
     * @param actor the administrative user ordering the extraction runtime loop
     */
    public void handleDeletion(final Ticket ticket, final User actor) {
        eventDispatcher.publish(
                ticket,
                actor,
                AuditLog.AuditAction.TICKET_DELETED,
                ticket.getStatus().name(),
                "DELETED",
                "Ticket deleted by " + actor.getName(),
                null);

        ticketRepository.delete(ticket);
    }

    /**
     * Validates and applies state transitions onto a support ticket while managing auxiliary
     * side-effects such as SLA recording triggers, operational counters, and audit logs.
     *
     * @param ticket the mutable domain entity instance to update
     * @param newStatus the targeted operational state matrix to transition towards
     * @param actor the verified system identity authorizing the mutation state change
     */
    public void applyStatusChange(
            final Ticket ticket, final Ticket.Status newStatus, final User actor) {
        final Ticket.Status oldStatus = ticket.getStatus();
        transitionPolicy.assertCanTransition(oldStatus, newStatus);
        ticket.setStatus(newStatus);

        if (newStatus == Ticket.Status.IN_PROGRESS) {
            slaService.recordFirstResponse(ticket.getId());
        }

        if (newStatus == Ticket.Status.RESOLVED) {
            slaService.recordResolution(ticket.getId());
            ticketMetrics.incrementResolved();
            if (ticket.getCreatedAt() != null) {
                ticketMetrics.recordResolutionTime(ticket.getCreatedAt());
            }
        }

        if (newStatus == Ticket.Status.CLOSED) {
            ticketMetrics.incrementClosed();
        }

        if (newStatus == Ticket.Status.ESCALATED) {
            ticket.setEscalated(true);
            ticketMetrics.incrementEscalated();
        }

        final AuditLog.AuditAction action =
                (newStatus == Ticket.Status.CLOSED)
                        ? AuditLog.AuditAction.TICKET_CLOSED
                        : AuditLog.AuditAction.STATUS_CHANGED;

        eventDispatcher.publish(
                ticket, actor, action, oldStatus.name(), newStatus.name(), null, null);
    }

    /**
     * Reassigns operational ownership of a ticket to a distinct support agent entity and dispatches
     * relevant messaging audit payloads.
     *
     * @param ticket the mutable domain entity instance to update
     * @param assigneeId the unique database primary identifier of the targeted helpdesk agent
     * @param actor the verified system identity authorizing the assignment change
     */
    public void applyAssignmentChange(
            final Ticket ticket, final Long assigneeId, final User actor) {
        final Agent newAgent = agentQuery.findOrThrow(assigneeId);
        if (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(newAgent.getId())) {
            return;
        }

        final String oldAssignee =
                (ticket.getAssignee() != null)
                        ? ticket.getAssignee().getUser().getName()
                        : "Unassigned";

        ticket.setAssignee(newAgent);
        eventDispatcher.publish(
                ticket,
                actor,
                AuditLog.AuditAction.ASSIGNED_TO_AGENT,
                oldAssignee,
                newAgent.getUser().getName(),
                null,
                null);
    }

    /**
     * Alters the structural urgency status level of a ticket entity record and publishes
     * chronological logging tracking indicators.
     *
     * @param ticket the mutable domain entity instance to update
     * @param newPriority the target severity level ranking to bind onto the entity profile
     * @param actor the verified system identity authorizing the adjustment change
     */
    public void applyPriorityChange(
            final Ticket ticket, final Ticket.Priority newPriority, final User actor) {
        final Ticket.Priority oldPriority = ticket.getPriority();
        ticket.setPriority(newPriority);

        eventDispatcher.publish(
                ticket,
                actor,
                AuditLog.AuditAction.PRIORITY_CHANGED,
                oldPriority.name(),
                newPriority.name(),
                null,
                null);
    }

    /**
     * Executes breach escalation procedures over an open support ticket, updating its core flag
     * triggers and publishing dedicated compliance alerts.
     *
     * @param ticket the mutable domain entity instance to update
     * @param reason the documented rationale outlining the escalation trigger vector
     * @param actor the verified system identity authorizing the escalation action
     */
    public void applyEscalation(final Ticket ticket, final String reason, final User actor) {
        ticket.setEscalated(true);
        applyStatusChange(ticket, Ticket.Status.ESCALATED, actor);
        eventDispatcher.publish(
                ticket, actor, AuditLog.AuditAction.ESCALATED, "false", "true", reason, reason);
    }
}
