package com.loggingsystem.springjwtauth.ticket.service.impl;

import com.loggingsystem.springjwtauth.common.util.TicketUtil;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.service.MessageSenderService;
import com.loggingsystem.springjwtauth.status.dto.StatusRequestDTO;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.status.service.StatusService;
import com.loggingsystem.springjwtauth.ticket.exception.TechnicianNotAuthorizedToUpdateTicketException;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import com.loggingsystem.springjwtauth.ticket.service.UpdateStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class UpdateStatusServiceImpl implements UpdateStatusService {

    private final TicketUtil ticketUtil;
    private final StatusService statusService;
    private final MessageSenderService messageSenderService;
    private final TicketsRepository ticketsRepository;

    public UpdateStatusServiceImpl(TicketUtil ticketUtil, StatusService statusService, MessageSenderService messageSenderService, TicketsRepository ticketsRepository) {
        this.ticketUtil = ticketUtil;
        this.statusService = statusService;
        this.messageSenderService = messageSenderService;
        this.ticketsRepository = ticketsRepository;
    }

    @Override
    public ResponseEntity<Void> updateStatus(Long ticketId, StatusRequestDTO statusId, Principal principal) {
        log.info("Updating status for ticket ID: {} by user: {}", ticketId, principal.getName());
        Tickets ticket = ticketUtil.getTicket(ticketId);
        Status status = statusService.getStatus(statusId.getStatus_id());

        if (!isTicketAssignedToCurrentUser(principal.getName(), ticket.getAssignedTechnician().getEmail())) {
            log.error("User: {} is not authorized to update ticket ID: {}", principal.getName(), ticketId);
            throw new TechnicianNotAuthorizedToUpdateTicketException();
        }

        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketsRepository.save(ticket);

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(ticket, null);
        messageSenderService.sendTicketStatusChangeMessage(emailRequest);

        log.info("Status updated successfully for ticket ID: {}", ticketId);
        return ResponseEntity.ok().build();
    }

    private static boolean isTicketAssignedToCurrentUser(String currentTechnicianEmail, String assignedTechnicianEmail) {
        return assignedTechnicianEmail.equals(currentTechnicianEmail);
    }
}
