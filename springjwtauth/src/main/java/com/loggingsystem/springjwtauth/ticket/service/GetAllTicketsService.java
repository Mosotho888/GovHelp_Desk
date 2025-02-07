package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.common.util.TicketUtils;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GetAllTicketsService {
    private final TicketsRepository ticketsRepository;
    private final TicketUtils ticketUtils;

    public GetAllTicketsService(TicketsRepository ticketsRepository, TicketUtils ticketUtils) {
        this.ticketsRepository = ticketsRepository;
        this.ticketUtils = ticketUtils;
    }

    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(Pageable pageable) {
        log.info("Fetching all tickets with pagination: {}", pageable);
        Page<Tickets> page = ticketsRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<TicketResponseDTO> ticketResponseDTOs = ticketUtils.mapToTicketResponseDTO(page.getContent());

        log.info("Fetched {} tickets.", ticketResponseDTOs.size());

        return ResponseEntity.ok(ticketResponseDTOs);
    }
}
