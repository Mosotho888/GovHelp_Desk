package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.dto.CommentResponseDTO;
import com.loggingsystem.springjwtauth.dto.TicketRequestDTO;
import com.loggingsystem.springjwtauth.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.model.*;
import com.loggingsystem.springjwtauth.repository.*;
import jakarta.validation.Valid;
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

        URI newLocation = ucb.path("/api/tickets/{id}")
                .buildAndExpand(savedTicket.getId())
                .toUri();

        return ResponseEntity.created(newLocation).build();
    }

    public ResponseEntity<List<TicketResponseDTO>> findAll(Pageable pageable) {
        Page<Tickets> page = ticketsRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<TicketResponseDTO> ticketResponseDTOs = page.getContent()
                .stream()
                .map(TicketResponseDTO::new)
                .toList();

        return ResponseEntity.ok(ticketResponseDTOs);
    }

    public ResponseEntity<Tickets> findById(Long id) {
        Optional<Tickets> ticket = ticketsRepository.findById(id);

        if (ticket.isPresent()) {
            return ResponseEntity.ok(ticket.get());
        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Void> addCommentToTicket(Long ticketId, TicketComments comments, Principal principal) {
        Optional<Tickets> optionalTickets = ticketsRepository.findById(ticketId);
        Optional<Employees> optionalEmployee = employeesRepository.findByEmail(principal.getName());

        if (optionalTickets.isPresent() && optionalEmployee.isPresent()) {
            Tickets tickets = optionalTickets.get();
            Employees employee = optionalEmployee.get();

            comments.setTickets(tickets);
            comments.setCommenter(employee);
            //comments.setCreated_at(LocalDateTime.now());

            ticketCommentsRepository.save(comments);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<CommentResponseDTO>> findAllCommentsByTicketId(Long id) {

        List<TicketComments> ticketComments = ticketCommentsRepository.findAllByTickets_id(id);
        List<CommentResponseDTO> commentResponseDTOs = ticketComments.stream().map(CommentResponseDTO::new).toList();
        return ResponseEntity.ok(commentResponseDTOs);
//        Optional<Tickets> optionalTickets = ticketsRepository.findById(id);
//
//        if (optionalTickets.isPresent()) {
//            Tickets ticket = optionalTickets.get();
//
//            List<CommentResponseDTO> commentResponseDTO = ticket.getComments()
//                    .stream()
//                    .map(CommentResponseDTO::new)
//                    .toList();
//
//            return ResponseEntity.ok(commentResponseDTO);
//        }
//
//        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<TicketResponseDTO>> findAllTicketsByAssignedTechnician(Principal principal) {
        Optional<Employees> optionalEmployee = employeesRepository.findByEmail(principal.getName());

        if (optionalEmployee.isPresent()) {
            Employees employee = optionalEmployee.get();

            List<Tickets> tickets = ticketsRepository.findAllByAssignedTechnician(employee);
            List<TicketResponseDTO> ticketResponseDTOs = tickets.stream().map(TicketResponseDTO::new).toList();

            return ResponseEntity.ok(ticketResponseDTOs);
        }

        return ResponseEntity.notFound().build();
    }
}
