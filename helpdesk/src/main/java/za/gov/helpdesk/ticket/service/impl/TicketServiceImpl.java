package za.gov.helpdesk.ticket.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.service.AgentQueryHelper;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.event.TicketEventDispatcher;
import za.gov.helpdesk.ticket.mapper.TicketMapper;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.TicketQueryHelper;
import za.gov.helpdesk.ticket.service.TicketService;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketQueryHelper ticketQuery;
    private final AgentQueryHelper agentQuery;
    private final TicketMapper ticketMapper;
    private final TicketEventDispatcher eventDispatcher;
    private final TicketUpdateCoordinator updateCoordinator;

    @Override
    @Transactional
    public TicketResponse createTicket(final CreateTicketRequest request, final User actor) {

        final Ticket.TicketBuilder builder =
                Ticket.builder()
                        .subject(request.getSubject())
                        .description(request.getDescription())
                        .priority(
                                request.getPriority() != null
                                        ? request.getPriority()
                                        : Ticket.Priority.MEDIUM)
                        .category(request.getCategory())
                        .requester(actor)
                        .status(Ticket.Status.OPEN);

        if (request.getAssigneeId() != null) {
            final Agent agent = agentQuery.findOrThrow(request.getAssigneeId());
            builder.assignee(agent);
        }

        final Ticket savedTicket = ticketRepository.save(builder.build());
        updateCoordinator.handlePostCreation(savedTicket, actor);

        return ticketMapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(final Long ticketId, final User actor) {
        return ticketMapper.toTicketResponse(ticketQuery.findOrThrow(ticketId, actor));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTickets(
            final Ticket.Status status,
            final Ticket.Priority priority,
            final Long assigneeId,
            final Pageable pageable,
            final User actor) {

        return ticketQuery
                .findWithFiltersAndSecurity(status, priority, assigneeId, pageable, actor)
                .map(ticketMapper::toTicketResponse);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(
            final Long ticketId, final UpdateTicketRequest request, final User actor) {

        final Ticket ticket = ticketQuery.findOrThrow(ticketId, actor);

        if (request.getStatus() != null && !ticket.getStatus().equals(request.getStatus())) {
            updateCoordinator.applyStatusChange(ticket, request.getStatus(), actor);
        }

        if (request.getAssigneeId() != null &&
                (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(request.getAssigneeId()))) {
            updateCoordinator.applyAssignmentChange(ticket, request.getAssigneeId(), actor);
        }

        if (request.getPriority() != null && !ticket.getPriority().equals(request.getPriority())) {
            updateCoordinator.applyPriorityChange(ticket, request.getPriority(), actor);
        }

        if (request.getCategory() != null) {
            ticket.setCategory(request.getCategory());
        }

        if (Boolean.TRUE.equals(request.getEscalated()) && !ticket.isEscalated()) {
            updateCoordinator.applyEscalation(ticket, request.getEscalationReason(), actor);
        }

        return ticketMapper.toTicketResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public void deleteTicket(final Long ticketId, final User actor) {

        if (actor.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException(
                    "Administrative privileges are required to purge system tickets");
        }
        final Ticket ticket = ticketQuery.findOrThrow(ticketId, actor);

        updateCoordinator.handleDeletion(ticket, actor);
    }
}
