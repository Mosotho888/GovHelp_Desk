package za.gov.helpdesk.ticket.dto;

import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.priority.model.Priority;
import za.gov.helpdesk.status.model.Status;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmittedTicketsDTO {
    private Long id;
    private za.gov.helpdesk.users.dto.UserResponse assignedTechnician;
    private Category category;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDateTime createdAt;

    public SubmittedTicketsDTO(TicketResponse ticketResponse) {
        this.id = ticketResponse.id();
        this.assignedTechnician = ticketResponse.assignedTechnician();
        this.category = ticketResponse.category();
        this.description = ticketResponse.description();
        this.status = ticketResponse.status();
        this.priority = ticketResponse.priority();
        this.createdAt = ticketResponse.createdAt();
    }
}
