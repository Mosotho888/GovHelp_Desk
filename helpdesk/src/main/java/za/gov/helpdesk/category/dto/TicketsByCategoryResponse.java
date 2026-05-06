package za.gov.helpdesk.category.dto;

import za.gov.helpdesk.ticket.dto.TicketsWithoutCategory;

import java.util.List;

public record TicketsByCategoryResponse (
        Long categoryId,
        String name,
        String description,
        List<TicketsWithoutCategory> tickets
) { }
