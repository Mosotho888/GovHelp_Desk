package za.gov.helpdesk.reporting.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.reporting.dto.response.AgentWorkloadReportRow;
import za.gov.helpdesk.reporting.dto.response.AssetSummaryReportRow;
import za.gov.helpdesk.reporting.dto.response.CategoryBreakdownReportRow;
import za.gov.helpdesk.reporting.dto.response.KnowledgeBaseEffectivenessReportRow;
import za.gov.helpdesk.reporting.dto.response.SlaComplianceReportRow;
import za.gov.helpdesk.reporting.dto.response.TicketVolumeReportRow;
import za.gov.helpdesk.reporting.service.ReportingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reporting", description = "Dashboard data drawn from the reporting materialized views")
@SecurityRequirement(name = "bearerAuth")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/ticket-volume")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Daily ticket volume by status and priority")
    public ResponseEntity<List<TicketVolumeReportRow>> getTicketVolume(
            @RequestParam(defaultValue = "30") final int days) {
        return ResponseEntity.ok(reportingService.getTicketVolume(days));
    }

    @GetMapping("/sla-compliance")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Daily SLA compliance rate")
    public ResponseEntity<List<SlaComplianceReportRow>> getSlaCompliance(
            @RequestParam(defaultValue = "30") final int days) {
        return ResponseEntity.ok(reportingService.getSlaCompliance(days));
    }

    @GetMapping("/agent-workload")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Current workload per agent, system wide")
    public ResponseEntity<List<AgentWorkloadReportRow>> getAgentWorkload() {
        return ResponseEntity.ok(reportingService.getAgentWorkload());
    }

    @GetMapping("/category-breakdown")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Ticket volume and average resolution time per category")
    public ResponseEntity<List<CategoryBreakdownReportRow>> getCategoryBreakdown() {
        return ResponseEntity.ok(reportingService.getCategoryBreakdown());
    }

    @GetMapping("/assets")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Asset inventory counts by type and status, with warranty buckets")
    public ResponseEntity<List<AssetSummaryReportRow>> getAssetSummary() {
        return ResponseEntity.ok(reportingService.getAssetSummary());
    }

    @GetMapping("/knowledge-base")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Knowledge base article usage and effectiveness")
    public ResponseEntity<List<KnowledgeBaseEffectivenessReportRow>>
            getKnowledgeBaseEffectiveness() {
        return ResponseEntity.ok(reportingService.getKnowledgeBaseEffectiveness());
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary =
                    "Refresh every reporting view immediately, outside the 15 minute schedule"
                            + " (Admin only)")
    public void refreshNow() {
        reportingService.refreshNow();
    }
}
