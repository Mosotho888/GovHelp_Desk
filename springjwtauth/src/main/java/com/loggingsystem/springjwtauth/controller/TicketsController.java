package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.dto.TicketRequestDTO;
import com.loggingsystem.springjwtauth.model.Employees;
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

    @PostMapping
    public ResponseEntity<Void> createTicket(@Valid @RequestBody TicketRequestDTO ticketRequest, Principal principal, UriComponentsBuilder ucb) {
        return ticketsServices.createTicket(ticketRequest, principal, ucb);
    }

    @GetMapping
    public ResponseEntity<List<Tickets>> findAll(Pageable pageable) {
        return ticketsServices.findAll(pageable);
    }

    @GetMapping("/{id}")
    private ResponseEntity<Tickets> findById (@PathVariable Long id) {
        return ticketsServices.findById(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Void> addCommentToTicket(@PathVariable Long id, @RequestBody TicketComments comment, Principal principal) {
        return ticketsServices.addCommentToTicket(id, comment, principal);
    }

//    @GetMapping("/{id}/comments")
//    @PutMapping("/{id}")
//    @DeleteMapping("/{id}")

}
