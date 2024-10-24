package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.dto.TicketRequestDTO;
import com.loggingsystem.springjwtauth.model.*;
import com.loggingsystem.springjwtauth.repository.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
public class TicketsServices {
    private final TicketsRepository ticketsRepository;
    private final EmployeesRepository employeesRepository;
    private final PriorityRepository priorityRepository;
    private final CategoryRepository categoryRepository;
    private final StatusRepository statusRepository;

    public TicketsServices(TicketsRepository ticketsRepository, EmployeesRepository employeesRepository,
                           PriorityRepository priorityRepository, CategoryRepository categoryRepository,
                           StatusRepository statusRepository) {
        this.ticketsRepository = ticketsRepository;
        this.employeesRepository = employeesRepository;
        this.priorityRepository = priorityRepository;
        this.categoryRepository = categoryRepository;
        this.statusRepository = statusRepository;
    }

    public ResponseEntity<Void> createTicket(@Valid TicketRequestDTO ticketRequest, Principal principal, UriComponentsBuilder ucb) {
        Optional<Employees> assignedTechnician = employeesRepository.findById(ticketRequest.getAssigned_user_id());
        Optional<Status> assignedStatus = statusRepository.findById(ticketRequest.getStatus_id());
        Optional<Category> assignedCategory = categoryRepository.findById(ticketRequest.getCategory_id());
        Optional<Priority> assignedPriority = priorityRepository.findById(ticketRequest.getPriority_id());

        Tickets newTicket = new Tickets();
        newTicket.setId(null);

        if (assignedTechnician.isPresent()) {
            newTicket.setAssigned_technician(assignedTechnician.get());
        }

        if (assignedStatus.isPresent()) {
            newTicket.setStatus(assignedStatus.get());
        }

        newTicket.setComment(ticketRequest.getComment());
        newTicket.setResolution(ticketRequest.getResolution());
        newTicket.setAttachments(ticketRequest.getAttachments());
        newTicket.setOwner(principal.getName());

        if (assignedCategory.isPresent()) {
            newTicket.setCategory(assignedCategory.get());
        }

        if (assignedPriority.isPresent()) {
            newTicket.setPriority(assignedPriority.get());
        }

        Tickets savedTicket = ticketsRepository.save(newTicket);

        URI newLocation = ucb.path("/api/tickets/{id}")
                .buildAndExpand(savedTicket.getId())
                .toUri();

        return ResponseEntity.created(newLocation).build();
    }

    public ResponseEntity<List<Tickets>> findAll(Pageable pageable) {
        Page<Tickets> page = ticketsRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        return ResponseEntity.ok(page.getContent());
    }
}
