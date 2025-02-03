package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.dto.*;
import com.loggingsystem.springjwtauth.exception.TechnicianNotAuthorizedToUpdateTicketException;
import com.loggingsystem.springjwtauth.exception.TicketNotFoundException;
import com.loggingsystem.springjwtauth.model.*;
import com.loggingsystem.springjwtauth.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TicketsServices {
    private final TicketsRepository ticketsRepository;
    private final EmployeesServices employeesServices;
    private final PriorityService priorityService;
    private final CategoryService categoryService;
    private final StatusService statusService;
    private final TicketCommentsRepository ticketCommentsRepository;
    private final MessageSender messageSender;

    public TicketsServices(TicketsRepository ticketsRepository, EmployeesServices employeesServices, PriorityService priorityService, CategoryService categoryService, StatusService statusService, TicketCommentsRepository ticketCommentsRepository, MessageSender messageSender) {
        this.ticketsRepository = ticketsRepository;
        this.employeesServices = employeesServices;
        this.priorityService = priorityService;
        this.categoryService = categoryService;
        this.statusService = statusService;
        this.ticketCommentsRepository = ticketCommentsRepository;
        this.messageSender = messageSender;
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

    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(Pageable pageable) {
        log.info("Fetching all tickets with pagination: {}", pageable);
        Page<Tickets> page = ticketsRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<TicketResponseDTO> ticketResponseDTOs = mapToTicketResponseDTO(page.getContent());

        log.info("Fetched {} tickets.", ticketResponseDTOs.size());

        return ResponseEntity.ok(ticketResponseDTOs);
    }

    public ResponseEntity<Tickets> getTicketById(Long id) {
        Tickets ticket = getTicket(id);

        return ResponseEntity.ok(ticket);
    }

    public ResponseEntity<Void> addCommentToTicket(Long ticketId, TicketComments comments, Principal principal) {
        log.info("Adding comment to ticket ID: {} by user: {}", ticketId, principal.getName());
        Tickets ticket = getTicket(ticketId);
        Employees employee = employeesServices.getEmployeeByEmail(principal.getName());

        comments.setTickets(ticket);
        comments.setCommenter(employee);

        ticketCommentsRepository.save(comments);

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(ticket, comments.getComment());
        messageSender.sendTicketCommentMessage(emailRequest);

        log.info("Email notification request sent for comment: {}", comments.getId());

        log.info("Comment added successfully to ticket ID: {}", ticketId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public ResponseEntity<List<CommentResponseDTO>> getAllCommentsByTicketId(Long id) {
        log.info("Fetching all comments for ticket ID: {}", id);
        List<TicketComments> ticketComments = ticketCommentsRepository.findAllByTickets_id(id);
        List<CommentResponseDTO> commentResponseDTOs = ticketComments.stream().map(CommentResponseDTO::new).toList();

        log.info("Fetched {} comments for ticket ID: {}", commentResponseDTOs.size(), id);
        return ResponseEntity.ok(commentResponseDTOs);
    }

    public ResponseEntity<List<TicketResponseDTO>> getAllTicketsByAssignedTechnician(Principal principal) {
        log.info("Fetching all tickets assigned to technician: {}", principal.getName());
        Employees employee = employeesServices.getEmployeeByEmail(principal.getName());

        List<Tickets> tickets = ticketsRepository.findAllByAssignedTechnician(employee);
        List<TicketResponseDTO> ticketResponseDTOs = mapToTicketResponseDTO(tickets);

        log.info("Fetched {} tickets assigned to technician: {}", ticketResponseDTOs.size(), principal.getName());
        return ResponseEntity.ok(ticketResponseDTOs);
    }

    public ResponseEntity<Void> updateStatus(Long ticketId, StatusRequestDTO statusId, Principal principal) {
        log.info("Updating status for ticket ID: {} by user: {}", ticketId, principal.getName());
        Tickets ticket = getTicket(ticketId);
        Status status = statusService.getStatus(statusId.getStatus_id());

        if (!isTicketAssignedToCurrentUser(principal.getName(), ticket.getAssignedTechnician().getEmail())) {
            log.error("User: {} is not authorized to update ticket ID: {}", principal.getName(), ticketId);
            throw new TechnicianNotAuthorizedToUpdateTicketException();
        }

        ticket.setStatus(status);
        ticket.setUpdated_at(LocalDateTime.now());
        ticketsRepository.save(ticket);

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(ticket, null);
        messageSender.sendTicketStatusChangeMessage(emailRequest);

        log.info("Status updated successfully for ticket ID: {}", ticketId);
        return ResponseEntity.ok().build();
    }

    private static boolean isTicketAssignedToCurrentUser(String currentTechnicianEmail, String assignedTechnicianEmail) {
        return assignedTechnicianEmail.equals(currentTechnicianEmail);
    }

    @NotNull
    private static List<TicketResponseDTO> mapToTicketResponseDTO(List<Tickets> page) {
        return page
                .stream()
                .map(TicketResponseDTO::new)
                .toList();
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
