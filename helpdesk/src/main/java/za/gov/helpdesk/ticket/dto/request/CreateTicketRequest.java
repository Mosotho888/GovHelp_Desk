package za.gov.helpdesk.ticket.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import za.gov.helpdesk.ticket.model.Priority;

import lombok.Data;

@Data
public class CreateTicketRequest {

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    private Priority priority = Priority.MEDIUM;

    /** Id of the leaf {@code Category} this ticket belongs to. Optional. */
    private Long categoryId;

    private Long assigneeId;

    private List<String> tags;
}
