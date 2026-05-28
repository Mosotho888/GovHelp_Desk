package za.gov.helpdesk.ticket.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.notification.messaging.TicketEmailNotificationPublisher;
import za.gov.helpdesk.sla.service.SlaService;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.mapper.TicketMapper;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.TicketService;
import za.gov.helpdesk.users.model.User;


@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final AgentRepository agentRepository;
    private final AuditEventPublisher auditPublisher;
    private final TicketEmailNotificationPublisher emailPublisher;
    private final SlaService slaService;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, User actor) {

        Ticket.TicketBuilder builder = Ticket.builder()
                .subject(request.getSubject())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Ticket.Priority.MEDIUM)
                .category(request.getCategory())
                .requester(actor)
                .status(Ticket.Status.OPEN);

        if (request.getAssigneeId() != null) {
            Agent agent = agentRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent", request.getAssigneeId()));
            builder.assignee(agent);
        }

        Ticket savedTicket = ticketRepository.save(builder.build());
        slaService.initializeSla(savedTicket);

        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                savedTicket.getId(),
                actor,
                AuditLog.AuditAction.TICKET_CREATED,
                null,
                Ticket.Status.OPEN.name(),
                "Ticket created: " + savedTicket.getSubject()
        );

        emailPublisher.publish(
                savedTicket, actor,
                savedTicket.getAssignee() != null ? savedTicket.getAssignee().getUser() : null,
                AuditLog.AuditAction.TICKET_CREATED, null);

        if (savedTicket.getAssignee() != null) {
            emailPublisher.publish(
                    savedTicket, savedTicket.getRequester(), savedTicket.getAssignee().getUser(),
                    AuditLog.AuditAction.ASSIGNED_TO_AGENT, null);
        }

        return ticketMapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId, User actor) {
        return ticketMapper.toTicketResponse(findOrThrow(ticketId, actor));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTickets(Ticket.Status status, Ticket.Priority priority, Long assigneeId, Pageable pageable, User actor) {

        // End users can only see their own tickets
        if (actor.getRole() == User.Role.USER) {
            return ticketRepository.findByRequester(actor, pageable)
                    .map(ticketMapper::toTicketResponse);
        }
        return ticketRepository.findWithFilters(status, priority, assigneeId, pageable)
                .map(ticketMapper::toTicketResponse);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request, User user) {

        Ticket ticket = findOrThrow(ticketId, user);

        if (request.getStatus() != null && !ticket.getStatus().equals(request.getStatus())) {
            applyStatusChange(ticket, request.getStatus(), user);
        }

        if (request.getAssigneeId() != null) {
            applyAssignmentChange(ticket, request.getAssigneeId(), user);
        }

        if (request.getPriority() != null && !ticket.getPriority().equals(request.getPriority())) {
            applyPriorityChange(ticket, request.getPriority(), user);
        }

        if (request.getCategory() != null) {
            ticket.setCategory(request.getCategory());
        }

        if (Boolean.TRUE.equals(request.getEscalated()) && !ticket.isEscalated()) {
            applyEscalation(ticket, request.getEscalationReason(), user);
        }

        return ticketMapper.toTicketResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public void deleteTicket(Long ticketId, User user) {

        Ticket ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticket.getId(),
                user,
                AuditLog.AuditAction.TICKET_DELETED,
                ticket.getStatus().name(),
                "DELETED",
                "Ticket deleted by " + user.getName()
        );

        ticketRepository.delete(ticket);
    }

    private Ticket findOrThrow(Long ticketId, User actor) {
        return ticketRepository
                .findByIdAndPrincipal(
                        ticketId,
                        actor.getEmail(),
                        actor.getRole().name()
                ).orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
    }

    private void applyStatusChange(Ticket ticket, Ticket.Status newStatus, User actor) {
        Ticket.Status oldStatus = ticket.getStatus();

        if (!ticket.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(oldStatus, newStatus);
        }

        ticket.setStatus(newStatus);

        if (newStatus == Ticket.Status.IN_PROGRESS) {
            slaService.recordFirstResponse(ticket.getId());
        }

        if (newStatus == Ticket.Status.RESOLVED) {
            slaService.recordResolution(ticket.getId());
        }

        if (newStatus == Ticket.Status.ESCALATED) {
            ticket.setEscalated(true);
        }

        AuditLog.AuditAction action = newStatus == Ticket.Status.CLOSED
                ? AuditLog.AuditAction.TICKET_CLOSED
                : AuditLog.AuditAction.STATUS_CHANGED;

        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticket.getId(),
                actor,
                action,
                oldStatus.name(),
                newStatus.name(),
                null
        );

        publishTicketEmail(ticket, action, null);
    }

    private void applyAssignmentChange(Ticket ticket, Long assigneeId, User actor) {
        Agent newAgent = agentRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", assigneeId));

        if (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(newAgent.getId())) {
            return;
        }

        String oldAssignee = ticket.getAssignee() != null
                ? ticket.getAssignee().getUser().getName()
                : "Unassigned";

        ticket.setAssignee(newAgent);

        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticket.getId(),
                actor,
                AuditLog.AuditAction.ASSIGNED_TO_AGENT,
                oldAssignee,
                newAgent.getUser().getName(),
                null
        );

        publishTicketEmail(ticket, AuditLog.AuditAction.ASSIGNED_TO_AGENT, null);
    }

    private void applyPriorityChange(Ticket ticket, Ticket.Priority newPriority, User actor) {
        Ticket.Priority oldPriority = ticket.getPriority();
        ticket.setPriority(newPriority);

        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticket.getId(),
                actor,
                AuditLog.AuditAction.PRIORITY_CHANGED,
                oldPriority.name(),
                newPriority.name(),
                null
        );

        publishTicketEmail(ticket, AuditLog.AuditAction.PRIORITY_CHANGED, null);
    }

    private void applyEscalation(Ticket ticket, String reason, User actor) {
        ticket.setEscalated(true);

        if (ticket.getStatus() == Ticket.Status.IN_PROGRESS) {
            ticket.setStatus(Ticket.Status.ESCALATED);
        }

        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticket.getId(),
                actor,
                AuditLog.AuditAction.ESCALATED,
                "false",
                "true",
                reason
        );

        publishTicketEmail(ticket, AuditLog.AuditAction.ESCALATED, reason);
    }

    private void publishTicketEmail(Ticket ticket, AuditLog.AuditAction action, String comment) {
        emailPublisher.publish(
                ticket,
                ticket.getRequester(),
                ticket.getAssignee() != null ? ticket.getAssignee().getUser() : null,
                action,
                comment);
    }
}
