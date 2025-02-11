package com.loggingsystem.springjwtauth.ticket.controller;

import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketAssignedDTO;
import com.loggingsystem.springjwtauth.ticket.service.*;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponseDTO;
import com.loggingsystem.springjwtauth.status.dto.StatusRequestDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketRequestDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
@RestController
@RequestMapping("/api/tickets")
public class TicketsController {
    private final CreateTicketService createTicketService;
    private final GetAllTicketsService getAllTicketsService;
    private final GetTicketByIdService getTicketByIdService;
    private final AddCommentToTicketService addCommentToTicketService;
    private final GetAllCommentsByTicketIdService getAllCommentsByTicketIdService;
    private final GetAllTicketsByAssignedTechnicianService getAllTicketsByAssignedTechnicianService;
    private final UpdatedTicketStatusService updatedTicketStatusService;

    public TicketsController(CreateTicketService createTicketService, GetAllTicketsService getAllTicketsService, GetTicketByIdService getTicketByIdService, AddCommentToTicketService addCommentToTicketService, GetAllCommentsByTicketIdService getAllCommentsByTicketIdService, GetAllTicketsByAssignedTechnicianService getAllTicketsByAssignedTechnicianService, UpdatedTicketStatusService updatedTicketStatusService) {
        this.createTicketService = createTicketService;
        this.getAllTicketsService = getAllTicketsService;
        this.getTicketByIdService = getTicketByIdService;
        this.addCommentToTicketService = addCommentToTicketService;
        this.getAllCommentsByTicketIdService = getAllCommentsByTicketIdService;
        this.getAllTicketsByAssignedTechnicianService = getAllTicketsByAssignedTechnicianService;
        this.updatedTicketStatusService = updatedTicketStatusService;
    }

    @PostMapping
    public ResponseEntity<Void> createTicket(@Valid @RequestBody TicketRequestDTO ticketRequest, Principal principal, UriComponentsBuilder ucb) {
        return createTicketService.createTicket(ticketRequest, principal, ucb);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(Pageable pageable) {
        return getAllTicketsService.getAllTickets(pageable);
    }

    @GetMapping("/{id}")
    private ResponseEntity<TicketResponseDTO> getTicketById (@PathVariable Long id) {
        return getTicketByIdService.getTicketById(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Void> addCommentToTicket(@PathVariable Long id, @RequestBody TicketComments comment, Principal principal) {
        return addCommentToTicketService.addCommentToTicket(id, comment, principal);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequestDTO status, Principal principal) {
        return updatedTicketStatusService.updateStatus(id, status, principal);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponseDTO>> getAllCommentsByTicketsId(@PathVariable Long id){
        return getAllCommentsByTicketIdService.getAllCommentsByTicketId(id);
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<AssignedTicketsDTO>> getAllTicketsByAssignedTechnician(Principal principal) {
        return getAllTicketsByAssignedTechnicianService.getAllTicketsByAssignedTechnician(principal);
    }

}
