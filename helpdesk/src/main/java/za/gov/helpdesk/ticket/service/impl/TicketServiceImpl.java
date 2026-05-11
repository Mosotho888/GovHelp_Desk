package za.gov.helpdesk.ticket.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.agent.repository.AgentRepository;
import za.gov.helpdesk.auditlog.dto.AuditLogResponse;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auditlog.repository.AuditLogRepository;
import za.gov.helpdesk.ticket.dto.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.TicketResponse;
import za.gov.helpdesk.ticket.exception.TicketNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.TicketRepository;
import za.gov.helpdesk.ticket.service.TicketService;
import za.gov.helpdesk.users.dto.UserResponse;
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
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(String ticketId) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTickets(Ticket.Status status, Ticket.Priority priority, Long assigneeId, Pageable pageable) {
        return null;
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long ticketId, CreateTicketRequest request) {
        return null;
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
                .orElseThrow(TicketNotFoundException::new);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
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
