package za.gov.helpdesk.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.model.Priority;
import za.gov.helpdesk.ticket.model.Status;
import za.gov.helpdesk.users.model.User;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request, User user);

    TicketResponse getTicketById(Long ticketId, User user);

    Page<TicketResponse> getTickets(
            Status status,
            Priority priority,
            Long assigneeId,
            Long categoryId,
            boolean includeDescendants,
            Pageable pageable,
            User user);

    TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request, User user);

    void deleteTicket(Long ticketId, User user);
}
