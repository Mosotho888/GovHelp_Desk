package za.gov.helpdesk.ticket.dto.request;

import za.gov.helpdesk.ticket.model.Priority;
import za.gov.helpdesk.ticket.model.Status;

import lombok.Data;

@Data
public class UpdateTicketRequest {

    private Status status;
    private Priority priority;
    private Long categoryId;
    private Long assigneeId;
    private Boolean escalated;
    private String escalationReason;
}
