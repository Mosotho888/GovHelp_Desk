-- =========================
-- REPORTING SCHEMA (Phase 1)
-- =========================
-- Materialised views pre-aggregate common dashboard queries so a reporting request reads a small,
-- already-summarised snapshot instead of scanning the full tickets/sla/asset/knowledge_articles
-- tables on every request. Each view carries a unique index so it can be refreshed with
-- REFRESH MATERIALISED VIEW CONCURRENTLY, which does not block concurrent reads while it runs.
-- Refreshing is handled by ReportingRefreshScheduler (every 15 minutes) plus an on demand
-- POST /v1/reports/refresh endpoint for admins.

-- =========================
-- mv_ticket_volume_daily
-- =========================
-- Daily ticket creation volume, broken down by status and priority, for trend charts.

CREATE MATERIALIZED VIEW mv_ticket_volume_daily AS
SELECT
    date_trunc('day', created_at)::date AS report_date,
    status,
    priority,
    COUNT(*) AS ticket_count
FROM tickets
GROUP BY date_trunc('day', created_at)::date, status, priority
WITH DATA;

CREATE UNIQUE INDEX idx_mv_ticket_volume_daily ON mv_ticket_volume_daily (report_date, status, priority);

-- =========================
-- mv_sla_compliance_daily
-- =========================
-- Daily SLA outcomes: how many tickets met both response and resolution targets versus how many
-- breached at least one, plus a computed compliance rate.

CREATE MATERIALIZED VIEW mv_sla_compliance_daily AS
SELECT
    date_trunc('day', ts.created_at)::date AS report_date,
    COUNT(*) AS total_count,
    COUNT(*) FILTER (WHERE NOT ts.response_breached AND NOT ts.resolution_breached) AS met_count,
    COUNT(*) FILTER (WHERE ts.response_breached OR ts.resolution_breached) AS breached_count,
    ROUND(
            COUNT(*) FILTER (WHERE NOT ts.response_breached AND NOT ts.resolution_breached) * 100.0
            / NULLIF(COUNT(*), 0),
            2
    ) AS compliance_rate
FROM ticket_sla ts
GROUP BY date_trunc('day', ts.created_at)::date
WITH DATA;

CREATE UNIQUE INDEX idx_mv_sla_compliance_daily ON mv_sla_compliance_daily (report_date);

-- =========================
-- mv_agent_workload
-- =========================
-- Current workload per agent: open ticket counts by status plus average resolution time, giving a
-- system wide leaderboard view instead of the per agent query already used by
-- ReportJdbcRepository.getAgentsStats().

CREATE MATERIALIZED VIEW mv_agent_workload AS
SELECT
    a.id AS agent_id,
    u.name AS agent_name,
    a.department,
    a.availability,
    COUNT(t.id) FILTER (WHERE t.status = 'OPEN') AS open_count,
    COUNT(t.id) FILTER (WHERE t.status = 'IN_PROGRESS') AS in_progress_count,
    COUNT(t.id) FILTER (WHERE t.status = 'RESOLVED') AS resolved_count,
    COUNT(t.id) FILTER (WHERE t.status = 'CLOSED') AS closed_count,
    COUNT(t.id) FILTER (WHERE t.status = 'ESCALATED') AS escalated_count,
    ROUND(
            AVG(EXTRACT(EPOCH FROM (t.updated_at - t.created_at)) / 3600)
                FILTER (WHERE t.status IN ('RESOLVED', 'CLOSED')),
            2
    ) AS avg_resolution_hours
FROM agents a
         JOIN users u ON u.id = a.user_id
         LEFT JOIN tickets t ON t.assignee_id = a.id
GROUP BY a.id, u.name, a.department, a.availability
    WITH DATA;

CREATE UNIQUE INDEX idx_mv_agent_workload ON mv_agent_workload (agent_id);

-- =========================
-- mv_category_breakdown
-- =========================
-- Ticket volume and average resolution time per category, for identifying which categories
-- generate the most (or slowest) support requests.

CREATE MATERIALIZED VIEW mv_category_breakdown AS
SELECT
    c.id AS category_id,
    c.name AS category_name,
    COUNT(t.id) AS ticket_count,
    ROUND(
            AVG(EXTRACT(EPOCH FROM (t.updated_at - t.created_at)) / 3600)
                FILTER (WHERE t.status IN ('RESOLVED', 'CLOSED')),
            2
    ) AS avg_resolution_hours
FROM ticket_categories c
         LEFT JOIN tickets t ON t.category_id = c.id
GROUP BY c.id, c.name
    WITH DATA;

CREATE UNIQUE INDEX idx_mv_category_breakdown ON mv_category_breakdown (category_id);

-- =========================
-- mv_asset_summary
-- =========================
-- Inventory counts by type and status, plus warranty expiry buckets for the asset dashboard.

CREATE MATERIALIZED VIEW mv_asset_summary AS
SELECT
    type,
    status,
    COUNT(*) AS asset_count,
    COUNT(*) FILTER (
        WHERE warranty_expiry_date IS NOT NULL
          AND warranty_expiry_date >= CURRENT_DATE
          AND warranty_expiry_date < CURRENT_DATE + INTERVAL '30 days'
    ) AS warranty_expiring_soon_count,
    COUNT(*) FILTER (
        WHERE warranty_expiry_date IS NOT NULL AND warranty_expiry_date < CURRENT_DATE
    ) AS warranty_expired_count
FROM assets
GROUP BY type, status
    WITH DATA;

CREATE UNIQUE INDEX idx_mv_asset_summary ON mv_asset_summary (type, status);

-- =========================
-- mv_kb_effectiveness
-- =========================
-- Per article usage metrics: how often an article is viewed, voted helpful, and linked to resolve
-- a ticket, so the knowledge base's actual impact can be measured rather than assumed.

CREATE MATERIALIZED VIEW mv_kb_effectiveness AS
SELECT
    id AS article_id,
    title,
    status,
    view_count,
    helpful_count,
    not_helpful_count,
    usage_count,
    CASE
        WHEN (helpful_count + not_helpful_count) > 0
            THEN ROUND(helpful_count * 100.0 / (helpful_count + not_helpful_count), 2)
        ELSE NULL
        END AS helpful_ratio
FROM knowledge_articles
    WITH DATA;

CREATE UNIQUE INDEX idx_mv_kb_effectiveness ON mv_kb_effectiveness (article_id);
