package za.gov.helpdesk.ticket.dto;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.status.model.Status;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticketcomment.dto.CommentResponse;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.priority.model.Priority;
import za.gov.helpdesk.users.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TicketResponse {
    private Long id;
    private String subject;
    private String description;
    private Ticket.Status status;
    private Ticket.Priority priority;
    private String category;
    private UserResponse requester;
    private UserResponse assignee;
    private boolean escalated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
