package za.gov.helpdesk.reporting.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.reporting.dto.response.AgentWorkloadReportRow;
import za.gov.helpdesk.reporting.dto.response.AssetSummaryReportRow;
import za.gov.helpdesk.reporting.dto.response.CategoryBreakdownReportRow;
import za.gov.helpdesk.reporting.dto.response.KnowledgeBaseEffectivenessReportRow;
import za.gov.helpdesk.reporting.dto.response.SlaComplianceReportRow;
import za.gov.helpdesk.reporting.dto.response.TicketVolumeReportRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads the reporting materialised views created in V11__create_reporting_views.sql. Each method
 * uses a typed row mapper rather than returning a raw Map, unlike the older
 * ReportJdbcRepository.getAgentsStats, so callers get compile time safety instead of stringly typed
 * map key access.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ReportingRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Reads daily ticket counts by status and priority from {@code mv_ticket_volume_daily}.
     *
     * @param from first report date to include
     * @param to last report date to include
     * @return one row per date, status and priority, oldest first
     */
    public List<TicketVolumeReportRow> findTicketVolume(final LocalDate from, final LocalDate to) {
        final String sql =
                """
                SELECT report_date, status, priority, ticket_count
                FROM mv_ticket_volume_daily
                WHERE report_date BETWEEN :from AND :to
                ORDER BY report_date, status, priority
                """;
        return jdbc.query(
                sql,
                new MapSqlParameterSource().addValue("from", from).addValue("to", to),
                (rs, rowNum) ->
                        TicketVolumeReportRow.builder()
                                .reportDate(rs.getObject("report_date", LocalDate.class))
                                .status(rs.getString("status"))
                                .priority(rs.getString("priority"))
                                .ticketCount(rs.getLong("ticket_count"))
                                .build());
    }

    /**
     * Reads daily SLA compliance figures from {@code mv_sla_compliance_daily}.
     *
     * @param from first report date to include
     * @param to last report date to include
     * @return one row per date, oldest first
     */
    public List<SlaComplianceReportRow> findSlaCompliance(
            final LocalDate from, final LocalDate to) {
        final String sql =
                """
                SELECT report_date, total_count, met_count, breached_count, compliance_rate
                FROM mv_sla_compliance_daily
                WHERE report_date BETWEEN :from AND :to
                ORDER BY report_date
                """;
        return jdbc.query(
                sql,
                new MapSqlParameterSource().addValue("from", from).addValue("to", to),
                (rs, rowNum) ->
                        SlaComplianceReportRow.builder()
                                .reportDate(rs.getObject("report_date", LocalDate.class))
                                .totalCount(rs.getLong("total_count"))
                                .metCount(rs.getLong("met_count"))
                                .breachedCount(rs.getLong("breached_count"))
                                .complianceRate(getNullableDouble(rs, "compliance_rate"))
                                .build());
    }

    /**
     * Reads per-agent ticket workload from {@code mv_agent_workload}.
     *
     * @return one row per agent, ordered by agent name
     */
    public List<AgentWorkloadReportRow> findAgentWorkload() {
        final String sql =
                """
                SELECT agent_id, agent_name, department, availability, open_count,
                       in_progress_count, resolved_count, closed_count, escalated_count,
                       avg_resolution_hours
                FROM mv_agent_workload
                ORDER BY agent_name
                """;
        return jdbc.query(
                sql,
                Map.of(),
                (rs, rowNum) ->
                        AgentWorkloadReportRow.builder()
                                .agentId(rs.getLong("agent_id"))
                                .agentName(rs.getString("agent_name"))
                                .department(rs.getString("department"))
                                .availability(rs.getString("availability"))
                                .openCount(rs.getLong("open_count"))
                                .inProgressCount(rs.getLong("in_progress_count"))
                                .resolvedCount(rs.getLong("resolved_count"))
                                .closedCount(rs.getLong("closed_count"))
                                .escalatedCount(rs.getLong("escalated_count"))
                                .avgResolutionHours(getNullableDouble(rs, "avg_resolution_hours"))
                                .build());
    }

    /**
     * Reads per-category ticket counts and resolution times from {@code mv_category_breakdown}.
     *
     * @return one row per category, busiest category first
     */
    public List<CategoryBreakdownReportRow> findCategoryBreakdown() {
        final String sql =
                """
                SELECT category_id, category_name, ticket_count, avg_resolution_hours
                FROM mv_category_breakdown
                ORDER BY ticket_count DESC
                """;
        return jdbc.query(
                sql,
                Map.of(),
                (rs, rowNum) ->
                        CategoryBreakdownReportRow.builder()
                                .categoryId(rs.getLong("category_id"))
                                .categoryName(rs.getString("category_name"))
                                .ticketCount(rs.getLong("ticket_count"))
                                .avgResolutionHours(getNullableDouble(rs, "avg_resolution_hours"))
                                .build());
    }

    /**
     * Reads asset counts and warranty status by asset type from {@code mv_asset_summary}.
     *
     * @return one row per asset type and status
     */
    public List<AssetSummaryReportRow> findAssetSummary() {
        final String sql =
                """
                SELECT type, status, asset_count, warranty_expiring_soon_count,
                       warranty_expired_count
                FROM mv_asset_summary
                ORDER BY type, status
                """;
        return jdbc.query(
                sql,
                Map.of(),
                (rs, rowNum) ->
                        AssetSummaryReportRow.builder()
                                .type(rs.getString("type"))
                                .status(rs.getString("status"))
                                .assetCount(rs.getLong("asset_count"))
                                .warrantyExpiringSoonCount(
                                        rs.getLong("warranty_expiring_soon_count"))
                                .warrantyExpiredCount(rs.getLong("warranty_expired_count"))
                                .build());
    }

    /**
     * Reads knowledge base article usage and feedback from {@code mv_kb_effectiveness}.
     *
     * @return one row per article, most used article first
     */
    public List<KnowledgeBaseEffectivenessReportRow> findKnowledgeBaseEffectiveness() {
        final String sql =
                """
                SELECT article_id, title, status, view_count, helpful_count, not_helpful_count,
                       usage_count, helpful_ratio
                FROM mv_kb_effectiveness
                ORDER BY usage_count DESC, view_count DESC
                """;
        return jdbc.query(
                sql,
                Map.of(),
                (rs, rowNum) ->
                        KnowledgeBaseEffectivenessReportRow.builder()
                                .articleId(rs.getLong("article_id"))
                                .title(rs.getString("title"))
                                .status(rs.getString("status"))
                                .viewCount(rs.getLong("view_count"))
                                .helpfulCount(rs.getLong("helpful_count"))
                                .notHelpfulCount(rs.getLong("not_helpful_count"))
                                .usageCount(rs.getLong("usage_count"))
                                .helpfulRatio(getNullableDouble(rs, "helpful_ratio"))
                                .build());
    }

    /**
     * Refreshes every reporting materialized view. Uses CONCURRENTLY so dashboard reads are never
     * blocked while a refresh is in progress, at the cost of requiring the unique index each view
     * already carries. Each view is refreshed independently, and a failure on one view is logged
     * and skipped rather than aborting the rest, so a single locked or malformed view cannot stop
     * every other dashboard from getting fresh data.
     */
    public void refreshAll() {
        for (final String viewName :
                List.of(
                        "mv_ticket_volume_daily",
                        "mv_sla_compliance_daily",
                        "mv_agent_workload",
                        "mv_category_breakdown",
                        "mv_asset_summary",
                        "mv_kb_effectiveness")) {
            refreshView(viewName);
        }
    }

    private void refreshView(final String viewName) {
        // View names come only from the fixed list above, never from user input, so this
        // concatenation is not an injection risk despite not being a bound parameter - materialized
        // view names cannot be passed as JDBC bind parameters in a REFRESH statement.
        try {
            jdbc.getJdbcTemplate().execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName);
        } catch (final DataAccessException ex) {
            log.warn(
                    "Failed to refresh materialized view {}. Its data will stay stale until the"
                            + " next scheduled refresh.",
                    viewName,
                    ex);
        }
    }

    private Double getNullableDouble(final ResultSet rs, final String column) throws SQLException {
        final double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
