package za.gov.helpdesk.ticket.dto;

public record TicketRequest(
        Long assignedTechnicianId,
        Long statusId,
        String description,
        Long categoryId,
        Long priorityId
) { }
