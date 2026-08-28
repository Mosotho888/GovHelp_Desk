package za.gov.helpdesk.ticket.dto.response;

import java.time.LocalDateTime;

import za.gov.helpdesk.category.dto.response.CategorySummaryResponse;
import za.gov.helpdesk.ticket.model.Priority;
import za.gov.helpdesk.ticket.model.Status;
import za.gov.helpdesk.users.dto.response.UserResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketResponse {
    private Long id;
    private String subject;
    private String description;
    private Status status;
    private Priority priority;
    private CategorySummaryResponse category;
    private UserResponse requester;
    private UserResponse assignee;
    private boolean escalated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
