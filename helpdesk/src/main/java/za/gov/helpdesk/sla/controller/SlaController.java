package za.gov.helpdesk.sla.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.gov.helpdesk.sla.dto.TicketSlaResponse;
import za.gov.helpdesk.sla.service.SlaService;

@RestController
@RequestMapping("/v1/tickets/{ticketId}/sla")
@RequiredArgsConstructor
@Tag(name = "SLA", description = "SLA tracking per ticket")
@SecurityRequirement(name = "bearerAuth")
public class SlaController {

    private final SlaService slaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get SLA status for a ticket (Agent/Admin only)")
    public ResponseEntity<TicketSlaResponse> getSla(@PathVariable Long ticketId) {

        return ResponseEntity.ok(slaService.getSlaStatus(ticketId));
    }
}
