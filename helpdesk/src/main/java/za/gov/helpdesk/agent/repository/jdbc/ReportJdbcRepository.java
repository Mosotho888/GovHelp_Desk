package za.gov.helpdesk.agent.repository.jdbc;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ReportJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Map<String, Object> getAgentsStats(final Long agentId) {
        final String sql =
                """
                SELECT
                    COUNT(*) AS total_assigned,
                    COUNT(CASE WHEN status = 'OPEN' THEN 1 END) AS open_count,
                    COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) AS in_progress_count,
                    COUNT(CASE WHEN status = 'RESOLVED' THEN 1 END) AS resolved_count,
                    COUNT(CASE WHEN status = 'CLOSED' THEN 1 END) AS closed_count,
                    COUNT(CASE WHEN status = 'ESCALATED' THEN 1 END) AS escalated_count
                FROM TICKETS
                WHERE assignee_id = :agentId
                """;

        final Map<String, Object> stats =
                jdbc.queryForMap(sql, new MapSqlParameterSource("agentId", agentId));

        // Add average resolution time (in hours)
        final String avgSql =
                """
                SELECT ROUND(AVG(
                    EXTRACT(EPOCH FROM (updated_at - created_at)) / 3600
                ), 2) AS avg_resolution_hours
                FROM TICKETS
                WHERE assignee_id = :agentId
                  AND status IN ('RESOLVED', 'CLOSED')
                """;

        try {
            final Map<String, Object> avgResult =
                    jdbc.queryForMap(avgSql, new MapSqlParameterSource("agentId", agentId));
            stats.put("avg_resolution_hours", avgResult.get("avg_resolution_hours"));
        } catch (final DataAccessException e) {
            log.warn(
                    "Failed to extract performance metric records for agentId={}. Defaulting to"
                            + " null.",
                    agentId,
                    e);
            stats.put("avg_resolution_hours", null);
        }

        return stats;
    }
}
