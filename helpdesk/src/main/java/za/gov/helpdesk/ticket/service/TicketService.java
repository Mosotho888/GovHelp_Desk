package za.gov.helpdesk.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

import java.util.List;

public interface TicketService {

    TicketResponse createTicket(
            CreateTicketRequest request,
            User user);
    TicketResponse getTicketById(
            Long ticketId,
            User user);
    Page<TicketResponse> getTickets(
            Ticket.Status status,
            Ticket.Priority priority,
            Long assigneeId,
            Pageable pageable,
            User user);
    TicketResponse updateTicket(
            Long ticketId,
            UpdateTicketRequest request,
            User user);
    void deleteTicket(
            Long ticketId,
            User user);
}
