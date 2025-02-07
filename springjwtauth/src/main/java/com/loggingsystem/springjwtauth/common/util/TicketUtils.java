package com.loggingsystem.springjwtauth.common.util;

import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.SubmittedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketAssignedDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.ticket.exception.TicketNotFoundException;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TicketUtils {
    private final TicketsRepository ticketsRepository;

    public TicketUtils(TicketsRepository ticketsRepository) {
        this.ticketsRepository = ticketsRepository;
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
    public List<TicketResponseDTO> mapToTicketResponseDTO(List<Tickets> page) {
        return page
                .stream()
                .map(TicketResponseDTO::new)
                .toList();
    }

    @NotNull
    public List<AssignedTicketsDTO> mapToTicketAssignedDTO(List<TicketResponseDTO> page) {
        return page
                .stream()
                .map(AssignedTicketsDTO::new)
                .toList();
    }

    @NotNull
    public List<SubmittedTicketsDTO> mapToSubmittedTicketsDTO(List<TicketResponseDTO> page) {
        return page
                .stream()
                .map(SubmittedTicketsDTO::new)
                .collect(Collectors.toList());
    }

    @NotNull
    public List<AssignedTicketsDTO> getAssignedTickets(Employees employee) {
        List<Tickets> tickets = ticketsRepository.findAllByAssignedTechnician(employee);
        List<TicketResponseDTO> ticketResponse = tickets.stream().map(TicketResponseDTO::new).toList();
        return mapToTicketAssignedDTO(ticketResponse);
    }

    @NotNull
    public List<SubmittedTicketsDTO> getTicketsByOwner(String email) {
        List<Tickets> tickets = ticketsRepository.findAllByOwner(email);
        List<TicketResponseDTO> ticketResponse = tickets.stream().map(TicketResponseDTO::new).toList();
        return mapToSubmittedTicketsDTO(ticketResponse);

    }
}
