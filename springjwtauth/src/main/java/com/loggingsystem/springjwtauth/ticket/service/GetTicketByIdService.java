package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.common.util.TicketUtils;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.ticket.exception.TicketNotFoundException;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GetTicketByIdService {
    private final TicketUtils ticketUtils;

    public GetTicketByIdService(TicketUtils ticketUtils) {
        this.ticketUtils = ticketUtils;
    }

    public ResponseEntity<TicketResponseDTO> getTicketById(Long id) {
        Tickets ticket = ticketUtils.getTicket(id);

        TicketResponseDTO ticketResponse = new TicketResponseDTO(ticket);

        return ResponseEntity.ok(ticketResponse);
    }
}
