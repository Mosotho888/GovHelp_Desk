package za.gov.helpdesk.ticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import za.gov.helpdesk.category.dto.response.CategorySummaryResponse;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.mapper.UserMapper;

/**
 * Declared as an abstract class rather than an interface so it can hold an injected {@link
 * CategoryQueryHelper}, used to render the category breadcrumb (e.g. "Hardware > Laptop") without
 * duplicating the ancestor-walk logic here. MapStruct generates a Spring bean extending this class;
 * the {@code @Autowired} field is wired the same way as on any other Spring component.
 */
@Mapper(componentModel = "spring", uses = UserMapper.class)
public abstract class TicketMapper {

    @Autowired protected CategoryQueryHelper categoryQueryHelper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "escalated", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Ticket toEntity(CreateTicketRequest request);

    @Mapping(source = "assignee.user", target = "assignee")
    @Mapping(source = "category", target = "category", qualifiedByName = "categoryToSummary")
    public abstract TicketResponse toTicketResponse(Ticket ticket);

    @Named("categoryToSummary")
    protected CategorySummaryResponse mapCategory(final Category category) {
        return categoryQueryHelper.toSummary(category);
    }
}
