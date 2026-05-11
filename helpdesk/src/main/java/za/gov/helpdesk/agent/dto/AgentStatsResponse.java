package za.gov.helpdesk.agent.dto;

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
