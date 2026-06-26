package za.gov.helpdesk.ticket.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import za.gov.helpdesk.ticket.model.Ticket;

import lombok.Data;

@Data
public class CreateTicketRequest {

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    private Ticket.Priority priority = Ticket.Priority.MEDIUM;

    @Size(max = 100)
    private String category;

    private Long assigneeId;

    private List<String> tags;
}
