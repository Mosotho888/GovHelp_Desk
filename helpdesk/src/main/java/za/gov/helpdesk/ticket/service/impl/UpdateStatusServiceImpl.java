package za.gov.helpdesk.ticket.service.impl;

import za.gov.helpdesk.common.util.TicketUtil;
import za.gov.helpdesk.emailnotification.dto.EmailNotificationDTO;
import za.gov.helpdesk.emailnotification.service.MessageSenderService;
import za.gov.helpdesk.status.dto.StatusRequestDTO;
import za.gov.helpdesk.status.model.Status;
import za.gov.helpdesk.status.service.StatusService;
import za.gov.helpdesk.ticket.exception.TechnicianNotAuthorizedToUpdateTicketException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.TicketsRepository;
import za.gov.helpdesk.ticket.service.UpdateStatusService;
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
        Ticket ticket = ticketUtil.getTicket(ticketId);
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
