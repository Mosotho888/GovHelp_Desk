package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.dto.CommentResponseDTO;
import com.loggingsystem.springjwtauth.dto.StatusRequestDTO;
import com.loggingsystem.springjwtauth.dto.TicketRequestDTO;
import com.loggingsystem.springjwtauth.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.model.TicketComments;
import com.loggingsystem.springjwtauth.model.Tickets;
import com.loggingsystem.springjwtauth.service.TicketsServices;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketsController {
    private final TicketsServices ticketsServices;

    public TicketsController(TicketsServices ticketsServices) {
        this.ticketsServices = ticketsServices;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping
    public ResponseEntity<Void> createTicket(@Valid @RequestBody TicketRequestDTO ticketRequest, Principal principal, UriComponentsBuilder ucb) {
        return ticketsServices.createTicket(ticketRequest, principal, ucb);
    }

    @CrossOrigin(origins = "http://localhost:3000")  // Allow only this origin
    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(Pageable pageable) {
        return ticketsServices.getAllTickets(pageable);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/{id}")
    private ResponseEntity<Tickets> getTicketById (@PathVariable Long id) {
        return ticketsServices.getTicketById(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Void> addCommentToTicket(@PathVariable Long id, @RequestBody TicketComments comment, Principal principal) {
        return ticketsServices.addCommentToTicket(id, comment, principal);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequestDTO status, Principal principal) {
        return ticketsServices.updateStatus(id, status, principal);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponseDTO>> getAllCommentsByTicketsId(@PathVariable Long id){
        return ticketsServices.getAllCommentsByTicketId(id);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/assigned")
    public ResponseEntity<List<TicketResponseDTO>> getAllTicketsByAssignedTechnician(Principal principal) {
        return ticketsServices.getAllTicketsByAssignedTechnician(principal);
    }

}
