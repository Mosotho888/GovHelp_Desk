package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.common.util.TicketUtils;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.service.MessageSender;
import com.loggingsystem.springjwtauth.status.dto.StatusRequestDTO;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.status.service.StatusService;
import com.loggingsystem.springjwtauth.ticket.exception.TechnicianNotAuthorizedToUpdateTicketException;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class UpdatedTicketStatusService {

    private final TicketUtils ticketUtils;
    private final StatusService statusService;
    private final MessageSender messageSender;
    private final TicketsRepository ticketsRepository;

    public UpdatedTicketStatusService(TicketUtils ticketUtils, StatusService statusService, MessageSender messageSender, TicketsRepository ticketsRepository) {
        this.ticketUtils = ticketUtils;
        this.statusService = statusService;
        this.messageSender = messageSender;
        this.ticketsRepository = ticketsRepository;
    }

    public ResponseEntity<Void> updateStatus(Long ticketId, StatusRequestDTO statusId, Principal principal) {
        log.info("Updating status for ticket ID: {} by user: {}", ticketId, principal.getName());
        Tickets ticket = ticketUtils.getTicket(ticketId);
        Status status = statusService.getStatus(statusId.getStatus_id());

        if (!isTicketAssignedToCurrentUser(principal.getName(), ticket.getAssignedTechnician().getEmail())) {
            log.error("User: {} is not authorized to update ticket ID: {}", principal.getName(), ticketId);
            throw new TechnicianNotAuthorizedToUpdateTicketException();
        }

        ticket.setStatus(status);
        ticket.setUpdated_at(LocalDateTime.now());
        ticketsRepository.save(ticket);

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(ticket, null);
        messageSender.sendTicketStatusChangeMessage(emailRequest);

        log.info("Status updated successfully for ticket ID: {}", ticketId);
        return ResponseEntity.ok().build();
    }

    private static boolean isTicketAssignedToCurrentUser(String currentTechnicianEmail, String assignedTechnicianEmail) {
        return assignedTechnicianEmail.equals(currentTechnicianEmail);
    }
}
