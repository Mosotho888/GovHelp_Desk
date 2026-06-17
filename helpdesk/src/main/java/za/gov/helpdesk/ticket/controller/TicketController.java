package za.gov.helpdesk.ticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.service.TicketService;
import za.gov.helpdesk.users.security.CustomUserDetails;

@RestController
@RequestMapping("/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Support ticket management")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "Create a new support ticket")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request, principal.getUser()));
    }

    @GetMapping
    @Operation(summary = "List tickets with optional filters")
    public ResponseEntity<Page<TicketResponse>> getTickets(@RequestParam(required = false) Ticket.Status status,
            @RequestParam(required = false) Ticket.Priority priority, @RequestParam(required = false) Long assigneeId,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ticketService.getTickets(status, priority, assigneeId, pageable, principal.getUser()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a ticket by ID")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ticketService.getTicketById(id, principal.getUser()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Update ticket status, priority, or assignment (Agent/Admin only)")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable Long id, @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request, principal.getUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a ticket (Admin only)")
    public void deleteTicket(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        ticketService.deleteTicket(id, principal.getUser());
    }
}
