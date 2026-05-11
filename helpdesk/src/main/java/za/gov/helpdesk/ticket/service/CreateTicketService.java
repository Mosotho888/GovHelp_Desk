package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.ticket.dto.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.TicketResponse;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

public interface CreateTicketService {
    ResponseEntity<TicketResponse> createTicket(CreateTicketRequest createTicketRequest, Principal principal);
}
