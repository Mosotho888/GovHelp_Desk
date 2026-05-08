package za.gov.helpdesk.ticket.dto;

import za.gov.helpdesk.priority.model.Priority;
import za.gov.helpdesk.status.model.Status;
import za.gov.helpdesk.ticketcomment.dto.CommentResponse;

import java.time.LocalDateTime;
import java.util.List;

public record TicketsWithoutCategory (
        Long id,
        String ownerEmail,
        za.gov.helpdesk.users.dto.UserResponse assignedTechnician,
        Status status,
        Priority priority,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> comments
){ }
