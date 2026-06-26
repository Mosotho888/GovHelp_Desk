package za.gov.helpdesk.ticket.dto.request;

import za.gov.helpdesk.ticket.model.Ticket;

import lombok.Data;

@Data
public class UpdateTicketRequest {

    private Ticket.Status status;
    private Ticket.Priority priority;
    private String category;
    private Long assigneeId;
    private Boolean escalated;
    private String escalationReason;
}
