package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.ticket.dto.TicketRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;

public interface CreateTicketService {
    ResponseEntity<Void> createTicket(TicketRequestDTO ticketRequest, Principal principal, UriComponentsBuilder ucb);
}
