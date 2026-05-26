package za.gov.helpdesk.ticket.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.notification.messaging.EmailNotificationPublisher;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.mapper.TicketMapper;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.TicketService;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;


@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final AuditEventPublisher auditPublisher;
    private final EmailNotificationPublisher emailPublisher;

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

        // Status transition with lifecycle guard
        if (request.getStatus() != null && !ticket.getStatus().equals(request.getStatus())) {
            if (!ticket.canTransitionTo(request.getStatus())) {
                throw new InvalidStatusTransitionException(ticket.getStatus(), request.getStatus());
            }

            auditPublisher.publishAudit(
                    AuditLog.EntityType.TICKET,
                    ticket.getId(),
                    user,
                    AuditLog.AuditAction.STATUS_CHANGED,
                    ticket.getStatus().name(),
                    request.getStatus().name(),
                    null
            );
            ticket.setStatus(request.getStatus());

            emailPublisher.publish(
                    ticket,
                    ticket.getRequester(),
                    ticket.getAssignee() != null ? ticket.getAssignee().getUser() : null,
                    AuditLog.AuditAction.STATUS_CHANGED,
                    null);
        }

        // Assignment change
        if (request.getAssigneeId() != null) {
            Agent newAgent = agentRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new  ResourceNotFoundException("Agent", request.getAssigneeId()));

            String oldAssignee = ticket.getAssignee() != null ? ticket.getAssignee().getId().toString() : "Unassigned";

            auditPublisher.publishAudit(
                    AuditLog.EntityType.TICKET,
                    ticket.getId(),
                    user,
                    AuditLog.AuditAction.ASSIGNED_TO_AGENT,
                    oldAssignee,
                    newAgent.getUser().getName(),
                    null
            );

            ticket.setAssignee(newAgent);

            emailPublisher.publish(
                    ticket,
                    ticket.getRequester(),
                    newAgent.getUser(),
                    AuditLog.AuditAction.ASSIGNED_TO_AGENT,
                    null);
        }

        // Other fields
        if (request.getPriority()  != null && !ticket.getPriority().equals(request.getPriority())) {

            auditPublisher.publishAudit(
                    AuditLog.EntityType.TICKET,
                    ticket.getId(),
                    user,
                    AuditLog.AuditAction.PRIORITY_CHANGED,
                    ticket.getPriority().name(),
                    request.getPriority().name(),
                    null
            );
            ticket.setPriority(request.getPriority());

            emailPublisher.publish(
                    ticket,
                    ticket.getRequester(),
                    ticket.getAssignee() != null ? ticket.getAssignee().getUser() : null,
                    AuditLog.AuditAction.PRIORITY_CHANGED,
                    null);
        }
        if (request.getCategory()  != null) ticket.setCategory(request.getCategory());
        if (request.getEscalated() != null && request.getEscalated() && !ticket.isEscalated()) {

            auditPublisher.publishAudit(
                    AuditLog.EntityType.TICKET,
                    ticket.getId(),
                    user,
                    AuditLog.AuditAction.ESCALATED,
                    "false",
                    "true",
                    request.getEscalationReason()
            );
            ticket.setEscalated(true);

            emailPublisher.publish(
                    ticket,
                    ticket.getRequester(),
                    ticket.getAssignee() != null ? ticket.getAssignee().getUser() : null,
                    AuditLog.AuditAction.ESCALATED,
                    null);
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
}
