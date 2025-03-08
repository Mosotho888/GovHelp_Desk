package com.loggingsystem.springjwtauth.ticket.controller;

import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.service.CommentTicketService;
import com.loggingsystem.springjwtauth.ticket.service.CreateTicketService;
import com.loggingsystem.springjwtauth.ticket.service.GetTicketService;
import com.loggingsystem.springjwtauth.ticket.service.UpdateStatusService;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponse;
import com.loggingsystem.springjwtauth.status.dto.StatusRequestDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketRequest;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponse;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketRequest ticketRequest, Principal principal) {
        return createTicketService.createTicket(ticketRequest, principal);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets(Pageable pageable) {
        return getTicketService.getAllTickets(pageable);
    }

    @GetMapping("/{ticketId}")
    private ResponseEntity<TicketResponse> getTicketById (@PathVariable Long ticketId) {
        return getTicketService.getTicketById(ticketId);
    }

    // return comment not Void
    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<Void> addCommentToTicket(@PathVariable Long ticketId, @RequestBody TicketComments comment, Principal principal) {
        return commentTicketService.addCommentToTicket(ticketId, comment, principal);
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long ticketId, @RequestBody StatusRequestDTO status, Principal principal) {
        return updateStatusService.updateStatus(ticketId, status, principal);
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<CommentResponse>> getAllCommentsByTicketsId(@PathVariable Long ticketId){
        return commentTicketService.getAllCommentsByTicketId(ticketId);
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<AssignedTicketsDTO>> getAllTicketsByAssignedTechnician(Principal principal) {
        return getTicketService.getAllTicketsByAssignedTechnician(principal);
    }

}
