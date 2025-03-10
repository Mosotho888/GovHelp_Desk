package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.status.dto.StatusRequestDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponse;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

public interface UpdateStatusService {
    ResponseEntity<TicketResponse> updateStatus(Long ticketId, StatusRequestDTO statusId, Principal principal);
}
