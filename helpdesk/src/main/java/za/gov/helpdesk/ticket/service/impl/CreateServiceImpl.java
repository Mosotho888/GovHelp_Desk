package za.gov.helpdesk.ticket.service.impl;

import za.gov.helpdesk.emailnotification.dto.EmailNotificationDTO;
import za.gov.helpdesk.emailnotification.service.MessageSenderService;
import za.gov.helpdesk.ticket.dto.TicketRequest;
import za.gov.helpdesk.ticket.dto.TicketResponse;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.TicketsRepository;
import za.gov.helpdesk.ticket.service.CreateTicketService;
import za.gov.helpdesk.ticket.service.TicketRequestToTicketConverter;
import za.gov.helpdesk.ticket.service.TicketToTicketResponseConverter;
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
        Ticket ticket = ticketRequestToTicketConverter.convert(ticketRequest);

        ticket.setOwnerEmail(principal.getName());
        ticket.setCreatedAt(LocalDateTime.now());

        Ticket savedTicket = ticketsRepository.save(ticket);
        log.info("Ticket created successfully with ID: {}", savedTicket.getId());

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(savedTicket, null);
        messageSenderService.sendTicketCreationMessage(emailRequest);
        messageSenderService.sendTechnicianAssignmentMessage(emailRequest);

        log.info("Email notification request sent for ticket: {}", savedTicket.getId());

        TicketResponse ticketResponse = ticketToTicketResponseConverter.convert(savedTicket);

        return ResponseEntity.status(HttpStatus.CREATED).body(ticketResponse);
    }
}
