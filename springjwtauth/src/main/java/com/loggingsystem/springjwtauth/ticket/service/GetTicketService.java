package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

public interface GetTicketService {
    ResponseEntity<List<TicketResponseDTO>> getAllTickets(Pageable pageable);
    ResponseEntity<TicketResponseDTO> getTicketById(Long id);
    ResponseEntity<List<AssignedTicketsDTO>> getAllTicketsByAssignedTechnician(Principal principal);
}
