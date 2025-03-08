package com.loggingsystem.springjwtauth.category.service;

import com.loggingsystem.springjwtauth.category.dto.TicketsByCategoryResponse;
import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.ticket.dto.TicketsWithoutCategory;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import com.loggingsystem.springjwtauth.ticket.service.TicketsToTicketsWithoutCategoryConverter;
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
        List<Tickets> tickets = ticketsRepository.findAllByCategory(category);

        List<TicketsWithoutCategory> ticketsWithoutCategoryList = new ArrayList<>();

        for (Tickets ticket : tickets) {
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
