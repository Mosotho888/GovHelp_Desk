package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.dto.CommentResponseDTO;
import com.loggingsystem.springjwtauth.dto.StatusRequestDTO;
import com.loggingsystem.springjwtauth.dto.TicketRequestDTO;
import com.loggingsystem.springjwtauth.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.exception.StatusNotFoundException;
import com.loggingsystem.springjwtauth.exception.TechnicianNotAuthorizedToUpdateTicketException;
import com.loggingsystem.springjwtauth.exception.TicketNotFoundException;
import com.loggingsystem.springjwtauth.model.*;
import com.loggingsystem.springjwtauth.repository.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.expression.spel.ast.Assign;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    private final EmployeesRepository employeesRepository;
    private final PriorityRepository priorityRepository;
    private final CategoryRepository categoryRepository;
    private final StatusRepository statusRepository;
    private final TicketCommentsRepository ticketCommentsRepository;

    public TicketsServices(TicketsRepository ticketsRepository, EmployeesRepository employeesRepository,
                           PriorityRepository priorityRepository, CategoryRepository categoryRepository,
                           StatusRepository statusRepository, TicketCommentsRepository ticketCommentsRepository) {
        this.ticketsRepository = ticketsRepository;
        this.employeesRepository = employeesRepository;
        this.priorityRepository = priorityRepository;
        this.categoryRepository = categoryRepository;
        this.statusRepository = statusRepository;
        this.ticketCommentsRepository = ticketCommentsRepository;
    }

    public ResponseEntity<Void> createTicket(@Valid TicketRequestDTO ticketRequest, Principal principal, UriComponentsBuilder ucb) {
        log.info("Creating a new ticket by user: {}", principal.getName());
        Optional<Employees> assignedTechnician = employeesRepository.findById(ticketRequest.getAssigned_user_id());
        Optional<Status> assignedStatus = statusRepository.findById(ticketRequest.getStatus_id());
        Optional<Category> assignedCategory = categoryRepository.findById(ticketRequest.getCategory_id());
        Optional<Priority> assignedPriority = priorityRepository.findById(ticketRequest.getPriority_id());

        Tickets newTicket = new Tickets();
        newTicket.setId(null);

        if (assignedTechnician.isPresent()) {
            newTicket.setAssignedTechnician(assignedTechnician.get());
        }

        if (assignedStatus.isPresent()) {
            newTicket.setStatus(assignedStatus.get());
        }

        newTicket.setDescription(ticketRequest.getDescription());
        newTicket.setResolution(ticketRequest.getResolution());
        newTicket.setAttachments(ticketRequest.getAttachments());
        newTicket.setOwner(principal.getName());

        if (assignedCategory.isPresent()) {
            newTicket.setCategory(assignedCategory.get());
        }

        if (assignedPriority.isPresent()) {
            newTicket.setPriority(assignedPriority.get());
        }
        newTicket.setCreated_at(LocalDateTime.now());

        Tickets savedTicket = ticketsRepository.save(newTicket);
        log.info("Ticket created successfully with ID: {}", savedTicket.getId());

        URI newLocation = ucb.path("/api/tickets/{id}")
                .buildAndExpand(savedTicket.getId())
                .toUri();

        return ResponseEntity.created(newLocation).build();
    }

    public ResponseEntity<List<TicketResponseDTO>> findAllTickets(Pageable pageable) {
        log.info("Fetching all tickets with pagination: {}", pageable);
        Page<Tickets> page = ticketsRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<TicketResponseDTO> ticketResponseDTOs = page.getContent()
                .stream()
                .map(TicketResponseDTO::new)
                .toList();

        log.info("Fetched {} tickets.", ticketResponseDTOs.size());

        return ResponseEntity.ok(ticketResponseDTOs);
    }

    public ResponseEntity<Tickets> findTicketById(Long id) {
        log.info("Fetching ticket with ID: {}", id);
        Optional<Tickets> ticket = ticketsRepository.findById(id);

        if (ticket.isPresent()) {
            log.info("Ticket found with ID: {}", id);
            return ResponseEntity.ok(ticket.get());
        }

        log.warn("Ticket not found with ID: {}", id);
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Void> addCommentToTicket(Long ticketId, TicketComments comments, Principal principal) {
        log.info("Adding comment to ticket ID: {} by user: {}", ticketId, principal.getName());
        Optional<Tickets> optionalTickets = ticketsRepository.findById(ticketId);
        Optional<Employees> optionalEmployee = employeesRepository.findByEmail(principal.getName());

        if (optionalTickets.isPresent() && optionalEmployee.isPresent()) {
            Tickets tickets = optionalTickets.get();
            Employees employee = optionalEmployee.get();

            comments.setTickets(tickets);
            comments.setCommenter(employee);
            //comments.setCreated_at(LocalDateTime.now());

            ticketCommentsRepository.save(comments);

            log.info("Comment added successfully to ticket ID: {}", ticketId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        log.warn("Ticket or employee not found for adding comment. Ticket ID: {}, User: {}", ticketId, principal.getName());
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<CommentResponseDTO>> findAllCommentsByTicketId(Long id) {
        log.info("Fetching all comments for ticket ID: {}", id);
        List<TicketComments> ticketComments = ticketCommentsRepository.findAllByTickets_id(id);
        List<CommentResponseDTO> commentResponseDTOs = ticketComments.stream().map(CommentResponseDTO::new).toList();

        log.info("Fetched {} comments for ticket ID: {}", commentResponseDTOs.size(), id);
        return ResponseEntity.ok(commentResponseDTOs);
    }

    public ResponseEntity<List<TicketResponseDTO>> findAllTicketsByAssignedTechnician(Principal principal) {
        log.info("Fetching all tickets assigned to technician: {}", principal.getName());
        Optional<Employees> optionalEmployee = employeesRepository.findByEmail(principal.getName());

        if (optionalEmployee.isPresent()) {
            Employees employee = optionalEmployee.get();

            List<Tickets> tickets = ticketsRepository.findAllByAssignedTechnician(employee);
            List<TicketResponseDTO> ticketResponseDTOs = tickets.stream().map(TicketResponseDTO::new).toList();

            log.info("Fetched {} tickets assigned to technician: {}", ticketResponseDTOs.size(), principal.getName());
            return ResponseEntity.ok(ticketResponseDTOs);
        }

        log.warn("Technician not found for email: {}", principal.getName());
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Void> updateStatus(Long ticketId, StatusRequestDTO status_id, Principal principal) {
        log.info("Updating status for ticket ID: {} by user: {}", ticketId, principal.getName());
        Optional<Tickets> optionalTickets = ticketsRepository.findById(ticketId);
        Optional<Status> optionalStatus = statusRepository.findById(status_id.getStatus_id());

        if (optionalTickets.isEmpty()) {
            log.error("Ticket not found with ID: {}", ticketId);
            throw new TicketNotFoundException();
        }

        if (optionalStatus.isEmpty()) {
            log.error("Status not found with ID: {}", status_id.getStatus_id());
            throw new StatusNotFoundException();
        }

        Tickets ticket = optionalTickets.get();
        Status status = optionalStatus.get();

        if (!ticket.getAssignedTechnician().getEmail().equals(principal.getName())) {
            log.error("User: {} is not authorized to update ticket ID: {}", principal.getName(), ticketId);
            throw new TechnicianNotAuthorizedToUpdateTicketException();
        }

        ticket.setStatus(status);
        ticket.setUpdated_at(LocalDateTime.now());
        ticketsRepository.save(ticket);

        log.info("Status updated successfully for ticket ID: {}", ticketId);
        return ResponseEntity.ok().build();
    }
}
