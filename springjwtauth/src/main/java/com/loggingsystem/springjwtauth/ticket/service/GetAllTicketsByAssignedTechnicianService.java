package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.common.util.TicketUtils;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.service.EmployeesServices;
import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.security.Principal;
import java.util.List;

@Service
@Slf4j
public class GetAllTicketsByAssignedTechnicianService {
    private final EmployeesServices employeesServices;
    private final TicketUtils ticketUtils;

    public GetAllTicketsByAssignedTechnicianService(EmployeesServices employeesServices, TicketUtils ticketUtils) {
        this.employeesServices = employeesServices;
        this.ticketUtils = ticketUtils;
    }

    public ResponseEntity<List<AssignedTicketsDTO>> getAllTicketsByAssignedTechnician(Principal principal) {
        log.info("Fetching all tickets assigned to technician: {}", principal.getName());
        Employees employee = employeesServices.getEmployeeByEmail(principal.getName());

        List<AssignedTicketsDTO> ticketsAssigned = ticketUtils.getAssignedTickets(employee);

        log.info("Fetched {} tickets assigned to technician: {}", ticketsAssigned.size(), principal.getName());
        return ResponseEntity.ok(ticketsAssigned);
    }


}
