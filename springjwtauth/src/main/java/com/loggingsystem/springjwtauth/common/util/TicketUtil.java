package com.loggingsystem.springjwtauth.common.util;

import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.SubmittedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponse;
import com.loggingsystem.springjwtauth.ticket.dto.TicketsWithoutCategory;
import com.loggingsystem.springjwtauth.ticket.exception.TicketNotFoundException;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import com.loggingsystem.springjwtauth.ticket.service.TicketToTicketResponseConverter;
import com.loggingsystem.springjwtauth.ticket.service.TicketsToTicketsWithoutCategoryConverter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TicketUtil {
    private final TicketsRepository ticketsRepository;
    private final TicketToTicketResponseConverter ticketToTicketResponseConverter;
    private final TicketsToTicketsWithoutCategoryConverter ticketsToTicketsWithoutCategoryConverter;

    public TicketUtil(TicketsRepository ticketsRepository, TicketToTicketResponseConverter ticketToTicketResponseConverter, TicketsToTicketsWithoutCategoryConverter ticketsToTicketsWithoutCategoryConverter) {
        this.ticketsRepository = ticketsRepository;
        this.ticketToTicketResponseConverter = ticketToTicketResponseConverter;
        this.ticketsToTicketsWithoutCategoryConverter = ticketsToTicketsWithoutCategoryConverter;
    }

    @NotNull
    public Tickets getTicket(Long ticketId) {
        log.info("Fetching ticket with ID: {}", ticketId);
        Optional<Tickets> optionalTicket = ticketsRepository.findById(ticketId);

        if (optionalTicket.isPresent()) {
            log.info("Ticket found with ID: {}", ticketId);
            return optionalTicket.get();
        }

        log.warn("Ticket not found with ID: {}", ticketId);
        throw new TicketNotFoundException();
    }

    @NotNull
    public List<TicketResponse> mapToTicketResponse(List<Tickets> tickets) {
        List<TicketResponse> ticketResponse = new ArrayList<>();

        for (Tickets ticket : tickets) {
            ticketResponse.add(ticketToTicketResponseConverter.convert(ticket));
        }
        return ticketResponse;
    }

    @NotNull
    public List<TicketsWithoutCategory> mapToTicketsByCategoryResponse(List<Tickets> tickets) {
        List<TicketsWithoutCategory> ticketsByCategoryResponseList = new ArrayList<>();

        for (Tickets ticket : tickets) {
            ticketsByCategoryResponseList.add(ticketsToTicketsWithoutCategoryConverter.convert(ticket));
        }
        return ticketsByCategoryResponseList;
    }

    @NotNull
    public List<AssignedTicketsDTO> mapToTicketAssignedDTO(List<TicketResponse> page) {
        return page
                .stream()
                .map(AssignedTicketsDTO::new)
                .toList();
    }

    @NotNull
    public List<SubmittedTicketsDTO> mapToSubmittedTicketsDTO(List<TicketResponse> page) {
        return page
                .stream()
                .map(SubmittedTicketsDTO::new)
                .collect(Collectors.toList());
    }

    @NotNull
    public List<AssignedTicketsDTO> getAssignedTickets(Employees employee) {
        List<Tickets> tickets = ticketsRepository.findAllByAssignedTechnician(employee);

        List<TicketResponse> ticketResponse = mapToTicketResponse(tickets);

        return mapToTicketAssignedDTO(ticketResponse);
    }

    @NotNull
    public List<SubmittedTicketsDTO> getTicketsByOwner(String email) {
        List<Tickets> tickets = ticketsRepository.findAllByOwnerEmail(email);

        List<TicketResponse> ticketResponse = mapToTicketResponse(tickets);

        return mapToSubmittedTicketsDTO(ticketResponse);

    }
}
