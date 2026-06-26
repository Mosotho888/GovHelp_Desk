package za.gov.helpdesk.agent.controller;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.agent.dto.request.CreateAgentRequest;
import za.gov.helpdesk.agent.dto.request.UpdateAgentRequest;
import za.gov.helpdesk.agent.dto.response.AgentResponse;
import za.gov.helpdesk.agent.dto.response.AgentStatsResponse;
import za.gov.helpdesk.agent.service.AgentService;
import za.gov.helpdesk.users.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agents", description = "Agent account management")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a user as an agent (Admin only)")
    public ResponseEntity<AgentResponse> createAgent(
            @Valid @RequestBody final CreateAgentRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agentService.createAgent(request, principal.getUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "List of all agents")
    public ResponseEntity<Page<AgentResponse>> getAllAgents(
            @PageableDefault(size = 25, sort = "id") final Pageable pageable) {

        return ResponseEntity.ok(agentService.getAllAgents(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get an agent by ID")
    public ResponseEntity<AgentResponse> getAgentById(@PathVariable final Long id) {
        return ResponseEntity.ok(agentService.getAgentById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "update agent availability or department")
    public ResponseEntity<AgentResponse> updateAgent(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateAgentRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {

        return ResponseEntity.ok(agentService.updateAgent(id, request, principal.getUser()));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get ticket statistics for an agent (Admin only)")
    public ResponseEntity<AgentStatsResponse> getAgentStats(@PathVariable final Long id) {
        return ResponseEntity.ok(agentService.getAgentStats(id));
    }
}
