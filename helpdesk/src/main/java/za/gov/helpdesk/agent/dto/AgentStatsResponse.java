package za.gov.helpdesk.agent.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentStatsResponse {
    private Long agentId;
    private String agentName;
    private Long totalAssigned;
    private Long openCount;
    private Long inProgressCount;
    private Long resolvedCount;
    private Long closedCount;
    private Long escalatedCount;
    private Double avgResolutionHours;
}
