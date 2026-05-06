package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.ticket.dto.AssignedTicketsDTO;
import za.gov.helpdesk.ticket.dto.TicketResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

public interface GetTicketService {
    ResponseEntity<List<TicketResponse>> getAllTickets(Pageable pageable);
    ResponseEntity<TicketResponse> getTicketById(Long id);
    ResponseEntity<List<AssignedTicketsDTO>> getAllTicketsByAssignedTechnician(Principal principal);
}
