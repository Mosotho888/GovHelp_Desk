package za.gov.helpdesk.ticket.repository.jdbc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * High-performance read-only native query repository component utilizing low-level JDBC mechanics.
 * Complements traditional ORM tracking by using a {@link NamedParameterJdbcTemplate} to execute
 * complex analytical aggregations, positional workload calculations, search matches, and bulk batch
 * updates efficiently.
 */
@Repository
@RequiredArgsConstructor
public class TicketJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Aggregates and extracts a totalized structural metric overview of ticket entities categorized
     * by their active lifecycle status states.
     *
     * @return a {@link Map} utilizing insertion order where the key is the stringified status and
     *     the value is the absolute count
     */
    public Map<String, Long> getStatusSummary() {
        final String sql =
                """
                SELECT status, COUNT(*) AS total
                FROM TICKETS
                GROUP BY status
                """;

        final Map<String, Long> result = new LinkedHashMap<>();

        jdbc.query(
                sql,
                new MapSqlParameterSource(),
                rs -> {
                    while (rs.next()) {
                        result.put(rs.getString("status"), rs.getLong("total"));
                    }
                });

        return result;
    }

    /**
     * Calculates analytical support agent workload distribution vectors across the system. Computes
     * the current active queue volume by totaling tickets assigned to individual agents that
     * exclude final resolved or closed termination status markers.
     *
     * @return a {@link List} of unmapped data container {@link Map} lines sorted in descending
     *     order of open ticket backlog
     */
    public List<Map<String, Object>> getAgentWorkload() {
        final String sql =
                """
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

    /**
     * Performs a low-overhead case-insensitive wildcard pattern matching search against string text
     * content fields within ticket records, returning matching primary database long identifiers.
     *
     * @param keyword the un-scrubbed alphanumeric search pattern word input by the operator
     * @param limit the structural ceiling boundary limit constraint applied to page collection
     *     sizing
     * @return a {@link List} containing the primary keys matching the search criteria sorted by
     *     creation date
     */
    public List<Long> searchTicketIds(final String keyword, final int limit) {
        final String sql =
                """
                SELECT id FROM TICKETS
                WHERE LOWER(subject)     LIKE LOWER(:kw)
                   OR LOWER(description) LIKE LOWER(:kw)
                ORDER BY created_at DESC
                LIMIT :limit
                """;
        final MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("kw", "%" + keyword + "%")
                        .addValue("limit", limit);
        return jdbc.queryForList(sql, params, Long.class);
    }

    /**
     * Executes a high-speed batch insert statement to flush collected transactional historical
     * audit tracking logs down to the underlying physical database tables in a single transport
     * payload batch.
     *
     * @param entries a {@link List} containing structured parameterized mapping source states to
     *     loop over
     * @return an array of integer flags indicating row modification update counts per index
     *     statement boundary
     */
    public int[] bulkInsertAuditLogs(final List<MapSqlParameterSource> entries) {
        final String sql =
                """
INSERT INTO AUDIT_LOG (ticket_id, actor_id, action, old_value, new_value, created_at)
VALUES (:ticketId, :actorId, :action, :oldValue, :newValue, CURRENT_TIMESTAMP)""";
        return jdbc.batchUpdate(sql, entries.toArray(new MapSqlParameterSource[0]));
    }
}
