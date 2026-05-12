package za.gov.helpdesk.ticket.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auditlog.repository.AuditLogRepository;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.TicketService;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.exception.UserNotFoundException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        User requester = getCurrentUser();

        Ticket.TicketBuilder builder = Ticket.builder()
                .subject(request.getSubject())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Ticket.Priority.MEDIUM)
                .category(request.getCategory())
                .requester(requester)
                .status(Ticket.Status.OPEN);

        if (request.getAssigneeId() != null) {
            Agent agent = agentRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent", request.getAssigneeId()));
            builder.assignee(agent);
        }

        Ticket saved = ticketRepository.save(builder.build());

        audit(saved, requester, "TICKET_CREATED", null, Ticket.Status.OPEN.name());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId) {
        return toResponse(findOrThrow(ticketId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTickets(Ticket.Status status, Ticket.Priority priority, Long assigneeId, Pageable pageable) {

        User currentUser = getCurrentUser();

        // End users can only see their own tickets
        if (currentUser.getRole() == User.Role.USER) {
            return ticketRepository.findByRequester(currentUser, pageable)
                    .map(this::toResponse);
        }
        return ticketRepository.findWithFilters(status, priority, assigneeId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = findOrThrow(ticketId);
        User actor = getCurrentUser();

        // Status transition with lifecycle guard
        if (request.getStatus() != null && !ticket.getStatus().equals(request.getStatus())) {
            if (!ticket.canTransitionTo(request.getStatus())) {
                throw new InvalidStatusTransitionException(ticket.getStatus(), request.getStatus());
            }
            audit(ticket, actor, "STATUS_CHANGED", ticket.getStatus().name(), request.getStatus().name());
            ticket.setStatus(request.getStatus());
        }

        // Assignment change
        if (request.getAssigneeId() != null) {
            Agent newAgent = agentRepository.findById(request.getAssigneeId())
                    .orElseThrow(UserNotFoundException::new);
            String oldAssignee = ticket.getAssignee() != null ? ticket.getAssignee().getId().toString() : "unassigned";
            audit(ticket, actor, "ASSIGNED", oldAssignee, request.getAssigneeId().toString());
            ticket.setAssignee(newAgent);
        }

        // Other fields
        if (request.getPriority()  != null) ticket.setPriority(request.getPriority());
        if (request.getCategory()  != null) ticket.setCategory(request.getCategory());
        if (request.getEscalated() != null) {
            if (request.getEscalated() && !ticket.isEscalated()) {
                audit(ticket, actor, "ESCALATED", "false", "true");
            }
            ticket.setEscalated(request.getEscalated());
        }
        return toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public void deleteTicket(Long ticketId) {
        Ticket ticket = findOrThrow(ticketId);
        User actor = getCurrentUser();

        audit(ticket, actor, "TICKET_DELETED", ticket.getStatus().name(), "DELETED");

        ticketRepository.delete(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLog(Long ticketId) {
        findOrThrow(ticketId);

        return auditLogRepository.findByTicketIdOrderByCreatedDateDesc(ticketId)
                .stream().map(this::toAuditResponse).toList();
    }

    private Ticket findOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void audit(Ticket ticket, User actor, String action, String oldVal, String newVal) {
        auditLogRepository.save(AuditLog.builder()
                .ticket(ticket)
                .actor(actor)
                .action(action)
                .oldValue(oldVal)
                .newValue(newVal)
                .build());
    }

    private TicketResponse toResponse(Ticket ticket) {
        UserResponse assigneeResponse = null;

        if (ticket.getAssignee() != null) {
            User agentUser = ticket.getAssignee().getUser();

            assigneeResponse = UserResponse.builder()
                    .id(agentUser.getId())
                    .name(agentUser.getName())
                    .email(agentUser.getEmail())
                    .role(agentUser.getRole())
                    .build();
        }

        return TicketResponse.builder()
                .id(ticket.getId())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .requester(UserResponse.builder()
                        .id(ticket.getRequester().getId())
                        .name(ticket.getRequester().getName())
                        .email(ticket.getRequester().getEmail())
                        .role(ticket.getRequester().getRole())
                        .build())
                .assignee(assigneeResponse)
                .escalated(ticket.isEscalated())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private AuditLogResponse toAuditResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .ticketId(log.getTicket().getId())
                .actor(UserResponse.builder()
                        .id(log.getActor().getId())
                        .name(log.getActor().getName())
                        .email(log.getActor().getEmail())
                        .role(log.getActor().getRole())
                        .build())
                .action(log.getAction())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
