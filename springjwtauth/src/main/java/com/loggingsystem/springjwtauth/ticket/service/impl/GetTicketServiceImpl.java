package com.loggingsystem.springjwtauth.ticket.service.impl;

import com.loggingsystem.springjwtauth.common.util.EmployeeUtil;
import com.loggingsystem.springjwtauth.common.util.TicketUtil;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponse;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import com.loggingsystem.springjwtauth.ticket.service.GetTicketService;
import com.loggingsystem.springjwtauth.ticket.service.TicketToTicketResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
@Slf4j
public class GetTicketServiceImpl implements GetTicketService {
    private final TicketsRepository ticketsRepository;
    private final TicketUtil ticketUtil;
    private final EmployeeUtil employeeUtil;
    private final TicketToTicketResponseConverter ticketToTicketResponseConverter;

    public GetTicketServiceImpl(TicketsRepository ticketsRepository, TicketUtil ticketUtil, EmployeeUtil employeeUtil, TicketToTicketResponseConverter ticketToTicketResponseConverter) {
        this.ticketsRepository = ticketsRepository;
        this.ticketUtil = ticketUtil;
        this.employeeUtil = employeeUtil;
        this.ticketToTicketResponseConverter = ticketToTicketResponseConverter;
    }

    @Override
    public ResponseEntity<List<TicketResponse>> getAllTickets(Pageable pageable) {
        log.info("Fetching all tickets with pagination: {}", pageable);
        Page<Tickets> page = ticketsRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<TicketResponse> ticketResponses = ticketUtil.mapToTicketResponse(page.getContent());

        log.info("Fetched {} tickets.", ticketResponses.size());

        return ResponseEntity.ok(ticketResponses);
    }

    @Override
    public ResponseEntity<TicketResponse> getTicketById(Long id) {
        Tickets ticket = ticketUtil.getTicket(id);

        TicketResponse ticketResponse = ticketToTicketResponseConverter.convert(ticket);

        return ResponseEntity.ok(ticketResponse);
    }

    @Override
    public ResponseEntity<List<AssignedTicketsDTO>> getAllTicketsByAssignedTechnician(Principal principal) {
        log.info("Fetching all tickets assigned to technician: {}", principal.getName());
        Employees employee = employeeUtil.getEmployeeByEmail(principal.getName());

        List<AssignedTicketsDTO> ticketsAssigned = ticketUtil.getAssignedTickets(employee);

        log.info("Fetched {} tickets assigned to technician: {}", ticketsAssigned.size(), principal.getName());
        return ResponseEntity.ok(ticketsAssigned);
    }
}
