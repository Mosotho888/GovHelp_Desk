package za.gov.helpdesk.agent.mapper;

import java.util.Map;

import org.mapstruct.Mapper;

import za.gov.helpdesk.agent.dto.response.AgentResponse;
import za.gov.helpdesk.agent.dto.response.AgentStatsResponse;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.users.mapper.UserMapper;

/**
 * Data mapping component responsible for converting internal support Agent entities, core user
 * account records, and analytical database aggregation structures into presentation-ready API
 * response payloads.
 */
@Mapper(componentModel = "spring", uses = UserMapper.class)
@FunctionalInterface
public interface AgentMapper {

    /**
     * Translates a persistent domain Agent entity into a standard structured data transfer response
     * representation.
     *
     * @param agent the source persistent domain model to evaluate
     * @return a mapped outbound {@link AgentResponse} presentation payload
     */
    AgentResponse toAgentResponse(Agent agent);

    /**
     * Transforms raw analytical aggregation maps extracted from native JDBC repositories into a
     * strongly-typed metrics dashboard payload wrapper. Handles fallback defaults and safe numeric
     * extraction instances.
     *
     * @param agentId the tracking primary identifier of the target support operative
     * @param agentName the display name profile of the target employee
     * @param raw a key-value data matrix mapping database alias strings to raw numeric values
     * @return a complete and compiled {@link AgentStatsResponse} visualization block
     */
    default AgentStatsResponse toAgentStatsResponse(
            final Long agentId, final String agentName, final Map<String, Object> raw) {
        if (raw == null) {
            return AgentStatsResponse.builder().agentId(agentId).agentName(agentName).build();
        }

        return AgentStatsResponse.builder()
                .agentId(agentId)
                .agentName(agentName)
                .totalAssigned(
                        raw.get("total_assigned") instanceof final Number num
                                ? num.longValue()
                                : 0L)
                .openCount(raw.get("open_count") instanceof final Number num ? num.longValue() : 0L)
                .inProgressCount(
                        raw.get("in_progress_count") instanceof final Number num
                                ? num.longValue()
                                : 0L)
                .resolvedCount(
                        raw.get("resolved_count") instanceof final Number num
                                ? num.longValue()
                                : 0L)
                .closedCount(
                        raw.get("closed_count") instanceof final Number num ? num.longValue() : 0L)
                .escalatedCount(
                        raw.get("escalated_count") instanceof final Number num
                                ? num.longValue()
                                : 0L)
                .avgResolutionHours(
                        raw.get("avg_resolution_hours") instanceof final Number num
                                ? num.doubleValue()
                                : null)
                .build();
    }
}
