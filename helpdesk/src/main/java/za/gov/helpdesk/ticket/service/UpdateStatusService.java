package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.status.dto.StatusRequestDTO;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

public interface UpdateStatusService {
    ResponseEntity<Void> updateStatus(Long ticketId, StatusRequestDTO statusId, Principal principal);
}
