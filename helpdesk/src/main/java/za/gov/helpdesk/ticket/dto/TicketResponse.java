package za.gov.helpdesk.ticket.dto;

import za.gov.helpdesk.employee.dto.EmployeeResponse;
import za.gov.helpdesk.status.model.Status;
import za.gov.helpdesk.ticketcomment.dto.CommentResponse;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.priority.model.Priority;

import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
        Long id,
        EmployeeResponse assignedTechnician,
        Status status,
        String description,
        String ownerEmail,
        Category category,
        Priority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> comments
) { }
