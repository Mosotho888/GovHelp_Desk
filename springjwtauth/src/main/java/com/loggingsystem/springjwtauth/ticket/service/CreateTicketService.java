package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.ticket.dto.TicketRequest;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponse;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

public interface CreateTicketService {
    ResponseEntity<TicketResponse> createTicket(TicketRequest ticketRequest, Principal principal);
}
