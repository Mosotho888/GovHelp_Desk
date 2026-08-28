package za.gov.helpdesk.ticket.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.agent.service.AgentQueryHelper;
import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.mapper.TicketMapper;
import za.gov.helpdesk.ticket.model.Priority;
import za.gov.helpdesk.ticket.model.Status;
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
    private final CategoryQueryHelper categoryQuery;
    private final TicketMapper ticketMapper;
    private final TicketUpdateCoordinator updateCoordinator;

    @Override
    @Transactional
    public TicketResponse createTicket(final CreateTicketRequest request, final User actor) {

        final Ticket ticket = ticketMapper.toEntity(request);
        ticket.setRequester(actor);
        ticket.setStatus(Status.OPEN);

        if (ticket.getPriority() == null) {
            ticket.setPriority(Priority.MEDIUM);
        }

        if (request.getCategoryId() != null) {
            ticket.setCategory(categoryQuery.findOrThrow(request.getCategoryId()));
        }

        if (request.getAssigneeId() != null) {
            ticket.setAssignee(agentQuery.findOrThrow(request.getAssigneeId()));
        }

        final Ticket savedTicket = ticketRepository.save(ticket);
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
            final Status status,
            final Priority priority,
            final Long assigneeId,
            final Long categoryId,
            final boolean includeDescendants,
            final Pageable pageable,
            final User actor) {

        return ticketQuery
                .findWithFiltersAndSecurity(
                        status,
                        priority,
                        assigneeId,
                        categoryId,
                        includeDescendants,
                        pageable,
                        actor)
                .map(ticketMapper::toTicketResponse);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(
            final Long ticketId, final UpdateTicketRequest request, final User actor) {

        final Ticket ticket = ticketQuery.findOrThrow(ticketId, actor);

        processStatusUpdate(ticket, request, actor);
        processAssigneeUpdate(ticket, request, actor);
        processPriorityUpdate(ticket, request, actor);
        processCategoryUpdate(ticket, request, actor);
        processEscalationUpdate(ticket, request, actor);

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

    private void processStatusUpdate(
            final Ticket ticket, final UpdateTicketRequest request, final User actor) {
        if (request.getStatus() != null && !ticket.getStatus().equals(request.getStatus())) {
            updateCoordinator.applyStatusChange(ticket, request.getStatus(), actor);
        }
    }

    private void processAssigneeUpdate(
            final Ticket ticket, final UpdateTicketRequest request, final User actor) {
        if (request.getAssigneeId() != null
                && (ticket.getAssignee() == null
                        || !ticket.getAssignee().getId().equals(request.getAssigneeId()))) {
            updateCoordinator.applyAssignmentChange(ticket, request.getAssigneeId(), actor);
        }
    }

    private void processPriorityUpdate(
            final Ticket ticket, final UpdateTicketRequest request, final User actor) {
        if (request.getPriority() != null && !ticket.getPriority().equals(request.getPriority())) {
            updateCoordinator.applyPriorityChange(ticket, request.getPriority(), actor);
        }
    }

    private void processCategoryUpdate(
            final Ticket ticket, final UpdateTicketRequest request, final User actor) {
        if (request.getCategoryId() != null) {
            updateCoordinator.applyCategoryChange(ticket, request.getCategoryId(), actor);
        }
    }

    private void processEscalationUpdate(
            final Ticket ticket, final UpdateTicketRequest request, final User actor) {
        if (Boolean.TRUE.equals(request.getEscalated()) && !ticket.isEscalated()) {
            updateCoordinator.applyEscalation(ticket, request.getEscalationReason(), actor);
        }
    }
}
