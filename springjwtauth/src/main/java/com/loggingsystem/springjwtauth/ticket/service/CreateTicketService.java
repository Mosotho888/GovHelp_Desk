package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.category.service.CategoryService;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.service.MessageSender;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.service.EmployeesServices;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.priority.service.PriorityService;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.status.service.StatusService;
import com.loggingsystem.springjwtauth.ticket.dto.TicketRequestDTO;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class CreateTicketService {
    private final TicketsRepository ticketsRepository;
    private final MessageSender messageSender;
    private final EmployeesServices employeesServices;
    private final PriorityService priorityService;
    private final CategoryService categoryService;
    private final StatusService statusService;

    public CreateTicketService(TicketsRepository ticketsRepository, MessageSender messageSender, EmployeesServices employeesServices, PriorityService priorityService, CategoryService categoryService, StatusService statusService) {
        this.ticketsRepository = ticketsRepository;
        this.messageSender = messageSender;
        this.employeesServices = employeesServices;
        this.priorityService = priorityService;
        this.categoryService = categoryService;
        this.statusService = statusService;
    }

    public ResponseEntity<Void> createTicket(TicketRequestDTO ticketRequest, Principal principal, UriComponentsBuilder ucb) {
        log.info("Creating a new ticket by user: {}", principal.getName());
        Employees assignedTechnician = employeesServices.getEmployee(ticketRequest.getAssigned_user_id());
        Status assignedStatus = statusService.getStatus(ticketRequest.getStatus_id());
        Category assignedCategory = categoryService.getCategory(ticketRequest.getCategory_id());
        Priority assignedPriority = priorityService.getPriority(ticketRequest.getPriority_id());

        Tickets newTicket = buildTicket(ticketRequest, principal, assignedTechnician, assignedStatus, assignedCategory, assignedPriority);

        Tickets savedTicket = ticketsRepository.save(newTicket);
        log.info("Ticket created successfully with ID: {}", savedTicket.getId());

        URI newLocation = ucb.path("/api/tickets/{id}")
                .buildAndExpand(savedTicket.getId())
                .toUri();

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(savedTicket, null);
        messageSender.sendTicketCreationMessage(emailRequest);
        messageSender.sendTechnicianAssignmentMessage(emailRequest);

        log.info("Email notification request sent for ticket: {}", savedTicket.getId());


        return ResponseEntity.created(newLocation).build();
    }

    @NotNull
    private static Tickets buildTicket(TicketRequestDTO ticketRequest, Principal principal, Employees assignedTechnician, Status assignedStatus, Category assignedCategory, Priority assignedPriority) {
        Tickets newTicket = new Tickets();
        newTicket.setId(null);

        newTicket.setAssignedTechnician(assignedTechnician);
        newTicket.setStatus(assignedStatus);

        newTicket.setDescription(ticketRequest.getDescription());
        newTicket.setResolution(ticketRequest.getResolution());
        newTicket.setAttachments(ticketRequest.getAttachments());
        newTicket.setOwner(principal.getName());
        newTicket.setCategory(assignedCategory);
        newTicket.setPriority(assignedPriority);

        newTicket.setCreated_at(LocalDateTime.now());
        return newTicket;
    }


}
