package za.gov.helpdesk.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.model.Ticket;

import java.util.List;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);
    TicketResponse getTicketById(Long ticketId);
    Page<TicketResponse> getTickets(Ticket.Status status, Ticket.Priority priority, Long assigneeId, Pageable pageable);
    TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request);
    void deleteTicket(Long ticketId);
    List<AuditLogResponse> getAuditLog(Long ticketId);
}
