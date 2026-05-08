package za.gov.helpdesk.ticket.service.impl;

import za.gov.helpdesk.common.util.EmployeeUtil;
import za.gov.helpdesk.common.util.TicketUtil;
import za.gov.helpdesk.ticket.dto.AssignedTicketsDTO;
import za.gov.helpdesk.ticket.dto.TicketResponse;
import za.gov.helpdesk.ticket.model.Tickets;
import za.gov.helpdesk.ticket.repository.TicketsRepository;
import za.gov.helpdesk.ticket.service.GetTicketService;
import za.gov.helpdesk.ticket.service.TicketToTicketResponseConverter;
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
        za.gov.helpdesk.users.model.User employee = employeeUtil.getEmployeeByEmail(principal.getName());

        List<AssignedTicketsDTO> ticketsAssigned = ticketUtil.getAssignedTickets(employee);

        log.info("Fetched {} tickets assigned to technician: {}", ticketsAssigned.size(), principal.getName());
        return ResponseEntity.ok(ticketsAssigned);
    }
}
