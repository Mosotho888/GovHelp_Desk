package com.loggingsystem.springjwtauth.category.dto;

import com.loggingsystem.springjwtauth.ticket.dto.TicketsWithoutCategory;

import java.util.List;

public record TicketsByCategoryResponse (
        Long categoryId,
        String name,
        String description,
        List<TicketsWithoutCategory> tickets
) { }
