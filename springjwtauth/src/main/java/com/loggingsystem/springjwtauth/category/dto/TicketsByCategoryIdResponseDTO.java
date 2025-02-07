package com.loggingsystem.springjwtauth.category.dto;

import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketsByCategoryResponse;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class TicketsByCategoryIdResponseDTO {
    private Long categoryId;
    private String name;
    private String description;
    private List<TicketsByCategoryResponse> tickets;

    public TicketsByCategoryIdResponseDTO(Category category, List<TicketResponseDTO> ticketsByCategoryResponse) {
        this.categoryId = category.getId();
        this.name = category.getName();
        this.description = category.getDescription();
        this.tickets = ticketsByCategoryResponse.stream().map(TicketsByCategoryResponse::new).collect(Collectors.toList());
    }
}
