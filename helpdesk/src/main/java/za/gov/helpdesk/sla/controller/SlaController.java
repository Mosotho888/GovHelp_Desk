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
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.sla.dto.TicketSlaResponse;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/tickets/{ticketId}/sla")
@RequiredArgsConstructor
@Tag(name = "SLA", description = "SLA tracking per ticket")
@SecurityRequirement(name = "bearerAuth")
public class SlaController {

    private final TicketSlaRepository ticketSlaRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get SLA status for a ticket (Agent/Admin only)")
    public ResponseEntity<TicketSlaResponse> getSla(@PathVariable Long ticketId) {

        return ticketSlaRepository.findByTicketId(ticketId)
                .map(sla -> TicketSlaResponse.builder()
                        .responseDueAt(sla.getResponseDueAt())
                        .resolutionDueAt(sla.getResolutionDueAt())
                        .firstResponseAt(sla.getFirstResponseAt())
                        .resolvedAt(sla.getResolvedAt())
                        .responseBreached(sla.isResponseBreached())
                        .resolutionBreached(sla.isResolutionBreached())
                        .status(resolveStatus(sla))
                        .build())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("SLA", ticketId));
    }

    private String resolveStatus(za.gov.helpdesk.sla.model.TicketSla sla) {
        if (sla.isResolutionBreached()) return "BREACHED";
        if (sla.getResolutionDueAt().minusMinutes(30).isBefore(LocalDateTime.now()))
            return "AT_RISK";
        return "ON_TRACK";
    }
}
