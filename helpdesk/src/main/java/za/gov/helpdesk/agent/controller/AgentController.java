package za.gov.helpdesk.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.gov.helpdesk.agent.dto.response.AgentResponse;
import za.gov.helpdesk.agent.dto.response.AgentStatsResponse;
import za.gov.helpdesk.agent.dto.request.CreateAgentRequest;
import za.gov.helpdesk.agent.dto.request.UpdateAgentRequest;
import za.gov.helpdesk.agent.service.AgentService;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agents", description = "Agent account management")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a user as an agent (Admin only)")
    public ResponseEntity<AgentResponse> createAgent(@Valid @RequestBody CreateAgentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.createAgent(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('AGENT', 'ADMIN')")
    @Operation(summary = "List of all agents")
    public ResponseEntity<Page<AgentResponse>> getAllAgents(
            @PageableDefault(size = 25, sort = "id") Pageable pageable) {

        return ResponseEntity.ok(agentService.getAllAgents(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get an agent by ID")
    public ResponseEntity<AgentResponse> getAgentById(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.getAgentById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @agentService.getAgentById(#id).user().id == authentication.principal.id")
    @Operation(summary = "update agent availability or department")
    public ResponseEntity<AgentResponse> updateAgent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAgentRequest request) {

        return ResponseEntity.ok(agentService.updateAgent(id, request));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get ticket statistics for an agent (Admin only)")
    public ResponseEntity<AgentStatsResponse> getAgentStats(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.getAgentStats(id));
    }
}
