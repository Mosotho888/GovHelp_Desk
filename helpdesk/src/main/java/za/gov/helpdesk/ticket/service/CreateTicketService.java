package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.ticket.dto.TicketRequest;
import za.gov.helpdesk.ticket.dto.TicketResponse;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

public interface CreateTicketService {
    ResponseEntity<TicketResponse> createTicket(TicketRequest ticketRequest, Principal principal);
}
