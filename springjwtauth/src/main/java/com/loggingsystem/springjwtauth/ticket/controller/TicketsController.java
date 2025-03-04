package com.loggingsystem.springjwtauth.ticket.controller;

import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.service.CommentTicketService;
import com.loggingsystem.springjwtauth.ticket.service.CreateTicketService;
import com.loggingsystem.springjwtauth.ticket.service.GetTicketService;
import com.loggingsystem.springjwtauth.ticket.service.UpdateStatusService;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponseDTO;
import com.loggingsystem.springjwtauth.status.dto.StatusRequestDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketRequestDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
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
    private final UpdateStatusService updateStatusService;
    private final GetTicketService getTicketService;
    private final CreateTicketService createTicketService;
    private final CommentTicketService commentTicketService;

    public TicketsController(UpdateStatusService updateStatusService, GetTicketService getTicketService, CreateTicketService createTicketService, CommentTicketService commentTicketService) {
        this.updateStatusService = updateStatusService;
        this.getTicketService = getTicketService;
        this.createTicketService = createTicketService;
        this.commentTicketService = commentTicketService;
    }

    @PostMapping
    public ResponseEntity<Void> createTicket(@Valid @RequestBody TicketRequestDTO ticketRequest, Principal principal, UriComponentsBuilder ucb) {
        return createTicketService.createTicket(ticketRequest, principal, ucb);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(Pageable pageable) {
        return getTicketService.getAllTickets(pageable);
    }

    @GetMapping("/{ticketId}")
    private ResponseEntity<TicketResponseDTO> getTicketById (@PathVariable Long ticketId) {
        return getTicketService.getTicketById(ticketId);
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<Void> addCommentToTicket(@PathVariable Long ticketId, @RequestBody TicketComments comment, Principal principal) {
        return commentTicketService.addCommentToTicket(ticketId, comment, principal);
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long ticketId, @RequestBody StatusRequestDTO status, Principal principal) {
        return updateStatusService.updateStatus(ticketId, status, principal);
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<CommentResponseDTO>> getAllCommentsByTicketsId(@PathVariable Long ticketId){
        return commentTicketService.getAllCommentsByTicketId(ticketId);
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<AssignedTicketsDTO>> getAllTicketsByAssignedTechnician(Principal principal) {
        return getTicketService.getAllTicketsByAssignedTechnician(principal);
    }

}
