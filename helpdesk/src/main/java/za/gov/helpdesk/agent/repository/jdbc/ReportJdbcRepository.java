package za.gov.helpdesk.agent.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReportJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Map<String, Object> getAgentsStats(Long agentId) {
        String sql = """
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

        Map<String, Object> stats = jdbc.queryForMap(sql,
                new MapSqlParameterSource("agentId", agentId));

        // Add average resolution time (in hours)
        String avgSql = """
                SELECT ROUND(AVG(
                    EXTRACT(EPOCH FROM (updated_at - created_at)) / 3600
                ), 2) AS avg_resolution_hours
                FROM TICKETS
                WHERE assignee_id = :agentId
                  AND status IN ('RESOLVED', 'CLOSED')
                """;

        try {
            Map<String, Object> avgResult = jdbc.queryForMap(avgSql,
                    new MapSqlParameterSource("agentId", agentId));
            stats.put("avg_resolution_hours", avgResult.get("avg_resolution_hours"));
        } catch (Exception e) {
            stats.put("avg_resolution_hours", null);
        }

        return stats;
    }

    public long countAllCommentsForTicket(Long ticketId) {
        String sql = """
                SELECT COUNT(*) FROM COMMENTS
                WHERE ticket_id = :ticketId
                """;
        Long count = jdbc.queryForObject(sql,
                new MapSqlParameterSource("ticketId", ticketId), Long.class);

        return count != null ? count : 0L;
    }

    public long getTotalAttachmentSize(Long ticketId) {
        String sql = """
                SELECT COALESCE(SUM(size_bytes), 0)
                FROM ATTACHMENTS
                WHERE ticket_id = :ticketId
                """;
        Long total = jdbc.queryForObject(sql,
                new MapSqlParameterSource("ticketId", ticketId), Long.class);
        return total != null ? total : 0L;
    }
}
