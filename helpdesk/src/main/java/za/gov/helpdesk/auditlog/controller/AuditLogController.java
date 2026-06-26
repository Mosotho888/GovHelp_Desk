package za.gov.helpdesk.auditlog.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auditlog.service.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@Tag(
        name = "Audit Log",
        description = "System-wide immutable audit trail - Admin and Agent access only")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get full audit trail for a specific ticket")
    public ResponseEntity<List<AuditLogResponse>> getTicketAuditLogs(@PathVariable final Long id) {

        return ResponseEntity.ok(auditService.getLogsForEntity(AuditLog.EntityType.TICKET, id));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get full audit trail for a specific user (Admin only)")
    public ResponseEntity<List<AuditLogResponse>> getUserAuditLogs(@PathVariable final Long id) {

        return ResponseEntity.ok(auditService.getLogsForEntity(AuditLog.EntityType.USER, id));
    }

    @GetMapping("/agents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get full audit trail for a specific agent (Admin only)")
    public ResponseEntity<List<AuditLogResponse>> getAgentAuditLogs(@PathVariable final Long id) {
        return ResponseEntity.ok(auditService.getLogsForEntity(AuditLog.EntityType.AGENT, id));
    }

    @GetMapping("/auth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all authentication events - logins, failures, lockouts (Admin only)")
    public ResponseEntity<Page<AuditLogResponse>> getAuthLogs(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
                    final Pageable pageable) {

        return ResponseEntity.ok(auditService.getAuthLogs(pageable));
    }

    @GetMapping("/actor/{actorId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all actions performed by a specific user (Admin only)")
    public ResponseEntity<Page<AuditLogResponse>> getByActor(
            @PathVariable final Long actorId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
                    final Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByActor(actorId, pageable));
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all events of a specific action type (Admin only)")
    public ResponseEntity<Page<AuditLogResponse>> getByAction(
            @PathVariable final AuditLog.AuditAction action,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
                    final Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByAction(action, pageable));
    }
}
