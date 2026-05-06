package za.gov.helpdesk.ticket.dto;

import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.priority.model.Priority;
import za.gov.helpdesk.status.model.Status;
import za.gov.helpdesk.ticketcomment.dto.CommentResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignedTicketsDTO {
    private Long id;
    private String ownerEmail;
    private Category category;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDateTime createdAt;
    private List<CommentResponse> comments;

    public AssignedTicketsDTO(TicketResponse ticketResponse) {
        this.id = ticketResponse.id();
        this.ownerEmail = ticketResponse.ownerEmail();
        this.category = ticketResponse.category();
        this.description = ticketResponse.description();
        this.status = ticketResponse.status();
        this.priority = ticketResponse.priority();
        this.createdAt = ticketResponse.createdAt();
        this.comments = ticketResponse.comments();
    }
}
