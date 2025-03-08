package com.loggingsystem.springjwtauth.ticket.service.impl;

import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.service.MessageSenderService;
import com.loggingsystem.springjwtauth.ticket.dto.TicketRequest;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponse;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import com.loggingsystem.springjwtauth.ticket.service.CreateTicketService;
import com.loggingsystem.springjwtauth.ticket.service.TicketRequestToTicketConverter;
import com.loggingsystem.springjwtauth.ticket.service.TicketToTicketResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class CreateServiceImpl implements CreateTicketService {
    private final TicketsRepository ticketsRepository;
    private final MessageSenderService messageSenderService;
    private final TicketRequestToTicketConverter ticketRequestToTicketConverter;
    private final TicketToTicketResponseConverter ticketToTicketResponseConverter;

    public CreateServiceImpl(TicketsRepository ticketsRepository, MessageSenderService messageSenderService, TicketRequestToTicketConverter ticketRequestToTicketConverter, TicketToTicketResponseConverter ticketToTicketResponseConverter) {
        this.ticketsRepository = ticketsRepository;
        this.messageSenderService = messageSenderService;
        this.ticketRequestToTicketConverter = ticketRequestToTicketConverter;
        this.ticketToTicketResponseConverter = ticketToTicketResponseConverter;
    }

    @Override
    public ResponseEntity<TicketResponse> createTicket(TicketRequest ticketRequest, Principal principal) {
        log.info("Creating a new ticket by user: {}", principal.getName());
        Tickets ticket = ticketRequestToTicketConverter.convert(ticketRequest);

        ticket.setOwnerEmail(principal.getName());
        ticket.setCreatedAt(LocalDateTime.now());

        Tickets savedTicket = ticketsRepository.save(ticket);
        log.info("Ticket created successfully with ID: {}", savedTicket.getId());

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(savedTicket, null);
        messageSenderService.sendTicketCreationMessage(emailRequest);
        messageSenderService.sendTechnicianAssignmentMessage(emailRequest);

        log.info("Email notification request sent for ticket: {}", savedTicket.getId());

        TicketResponse ticketResponse = ticketToTicketResponseConverter.convert(savedTicket);

        return ResponseEntity.status(HttpStatus.CREATED).body(ticketResponse);
    }
}
