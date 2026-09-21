package za.gov.helpdesk.reporting.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Current workload for a single agent, across the whole system rather than one ticket at a time.
 */
@Setter
@Getter
@Builder
public class AgentWorkloadReportRow {
    private Long agentId;
    private String agentName;
    private String department;
    private String availability;
    private long openCount;
    private long inProgressCount;
    private long resolvedCount;
    private long closedCount;
    private long escalatedCount;
    private Double avgResolutionHours;
}
