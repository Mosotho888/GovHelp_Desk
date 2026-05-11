package za.gov.helpdesk.category.service;

import za.gov.helpdesk.category.dto.TicketsByCategoryResponse;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.ticket.dto.TicketsWithoutCategory;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.TicketsRepository;
import za.gov.helpdesk.ticket.service.TicketsToTicketsWithoutCategoryConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TicketsByCategoryConverter implements Converter<Category, TicketsByCategoryResponse> {
    private final TicketsToTicketsWithoutCategoryConverter ticketsToTicketsWithoutCategoryConverter;
    private final TicketsRepository ticketsRepository;

    public TicketsByCategoryConverter(TicketsToTicketsWithoutCategoryConverter ticketsToTicketsWithoutCategoryConverter, TicketsRepository ticketsRepository) {
        this.ticketsToTicketsWithoutCategoryConverter = ticketsToTicketsWithoutCategoryConverter;
        this.ticketsRepository = ticketsRepository;
    }

    @Override
    public @NotNull TicketsByCategoryResponse convert(@NotNull Category category) {
        List<Ticket> tickets = ticketsRepository.findAllByCategory(category);

        List<TicketsWithoutCategory> ticketsWithoutCategoryList = new ArrayList<>();

        for (Ticket ticket : tickets) {
            ticketsWithoutCategoryList.add(ticketsToTicketsWithoutCategoryConverter.convert(ticket));
        }

        return new TicketsByCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                ticketsWithoutCategoryList
        );
    }
}
