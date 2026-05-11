package za.gov.helpdesk.ticket.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TicketJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Map<String, Long> getStatusSummary() {
        String sql = """
                SELECT status, COUNT(*) AS total
                FROM TICKETS
                GROUP BY status
                """;

        Map<String, Long> result = new LinkedHashMap<>();

        jdbc.query(sql, new MapSqlParameterSource(), rs -> {
            while (rs.next()) {
                result.put(rs.getString("status"), rs.getLong("total"));
            }
        });

        return result;
    }

    public List<Map<String, Object>> getAgentWorkload() {
        String sql = """
                SELECT a.id          AS agent_id,
                       u.name        AS agent_name,
                       COUNT(t.id)   AS open_tickets
                FROM AGENTS a
                JOIN USERS  u ON u.id = a.user_id
                LEFT JOIN TICKETS t ON t.assignee_id = a.id
                                   AND t.status NOT IN ('RESOLVED', 'CLOSED')
                GROUP BY a.id, u.name
                ORDER BY open_tickets DESC
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource());
    }

    public List<Long> searchTicketIds(String keyword, int limit) {
        String sql = """
                SELECT id FROM TICKETS
                WHERE LOWER(subject)     LIKE LOWER(:kw)
                   OR LOWER(description) LIKE LOWER(:kw)
                ORDER BY created_at DESC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("kw",    "%" + keyword + "%")
                .addValue("limit", limit);
        return jdbc.queryForList(sql, params, Long.class);
    }

    public int[] bulkInsertAuditLogs(List<MapSqlParameterSource> entries) {
        String sql = """
                INSERT INTO AUDIT_LOG (ticket_id, actor_id, action, old_value, new_value, created_at)
                VALUES (:ticketId, :actorId, :action, :oldValue, :newValue, CURRENT_TIMESTAMP)
                """;
        return jdbc.batchUpdate(sql, entries.toArray(new MapSqlParameterSource[0]));
    }
}
