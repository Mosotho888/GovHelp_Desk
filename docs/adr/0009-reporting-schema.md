# ADR 0009: Reporting schema built on materialized views (Phase 1)

## Status

Accepted

## Context

Several features already produce data worth reporting on: ticket volume and priority mix, SLA compliance, agent
workload, category breakdown, asset inventory and warranty status, and knowledge base effectiveness. None of this was
queryable as a dashboard dataset. The closest existing thing, ReportJdbcRepository.getAgentsStats, runs a raw aggregate
query against the live tickets table for one agent at a time on every request, which works for a single agent's profile
page but does not scale to a dashboard that wants the same shape of aggregation across every agent, every category, or
every day in a trailing thirty day window.

The system needed a reporting layer that: (1) answers dashboard queries quickly without scanning the full tickets,
ticket_sla, assets, or knowledge_articles tables on every request; (2) stays reasonably fresh without demanding real
time accuracy, since a dashboard is read by a person, not a downstream process reacting to it; (3) is simple enough to
build and operate as a first phase, with room to grow into an event driven fact table later if reporting needs outgrow
it.

## Decision

Introduce a reporting schema built on Postgres materialized views (V11__create_reporting_views.sql):
mv_ticket_volume_daily, mv_sla_compliance_daily, mv_agent_workload, mv_category_breakdown, mv_asset_summary, and
mv_kb_effectiveness. Each view pre-computes one dashboard's aggregation once, and every subsequent read is a plain
indexed select against a small, already summarised table rather than a live aggregation over the full underlying data.

Each view carries a unique index (on the natural grouping key, such as report_date plus status plus priority, or
agent_id) specifically so it can be refreshed with REFRESH MATERIALIZED VIEW CONCURRENTLY, which rebuilds the view
without holding a lock that would block concurrent reads. Refreshing is handled two ways: a scheduled job,
ReportingRefreshScheduler, runs every fifteen minutes, and an admin only POST /v1/reports/refresh endpoint allows an on
demand refresh (useful right after seeding demo data, or before a live walkthrough). A failure refreshing one view is
logged and skipped rather than aborting the rest, so a single locked or malformed view cannot stop every other dashboard
from getting fresh data.

ReportingRepository reads each view through NamedParameterJdbcTemplate with a typed row mapper per report, rather than
the Map&lt;String, Object&gt; approach ReportJdbcRepository.getAgentsStats already uses. This keeps the same JDBC first
style for aggregate reporting queries (plain SQL, no JPA entity mapping overhead for read only rollups) while giving
callers compile time safety instead of stringly typed map key access.

## Alternatives considered

- **Computing every aggregation on demand with plain JPA or JDBC queries** - rejected as the default approach for
  dashboard scale queries; it was kept only where it already existed (ReportJdbcRepository.getAgentsStats, a single
  agent's own stats page), since re-running that query for one profile view is cheap, but running the system wide
  equivalent for a workload leaderboard on every dashboard load would repeatedly scan the entire tickets table.
- **An event driven fact table fed by a RabbitMQ analytics consumer** - the natural Phase 2 once real time dashboards or
  historical trend analysis beyond what a periodically refreshed view can offer become a real requirement. Deferred
  because it is meaningfully more infrastructure (a new consumer, a fact table schema, backfill tooling) than the
  reporting need currently justifies.
- **A full OLAP or data warehouse export (Parquet/DuckDB)** - considered as a possible Phase 3 for offline analysis at a
  much larger data volume than a single organisation's help desk is likely to reach. Not pursued now for the same
  reason: it solves a scale problem this system does not have yet.
- **Refreshing views inside the request path (compute on first read after staleness)** - rejected in favour of a fixed
  schedule, since it would make an unlucky request pay the cost of a full refresh while everyone else gets stale data
  regardless; a background schedule keeps read latency constant for every caller.

## Consequences

**Easier:**

- Dashboard queries are now simple indexed selects against small, pre aggregated tables, regardless of how large the
  underlying tickets, assets, or knowledge_articles tables grow.
- Adding a new report is a new materialized view plus a thin repository method and DTO, not a redesign of how reporting
  works.
- The unique index every view carries doubles as the natural primary key for that report row, which keeps each RowMapper
  simple and each view's grain unambiguous.

**Harder:**

- Dashboard data is eventually consistent, not live: a ticket created moments ago will not appear in
  mv_ticket_volume_daily until the next refresh (at most fifteen minutes, or immediately after an admin triggers POST
  /v1/reports/refresh). This is an accepted trade-off for a reporting dashboard, which is read by people rather than a
  process that needs the latest row instantly.
- REFRESH MATERIALIZED VIEW CONCURRENTLY requires Postgres specifically (it is not portable SQL), which is an acceptable
  constraint since the project already commits to Postgres elsewhere (native full text search in ADR 0008, generated
  columns, and so on).
- Six views is a reasonable Phase 1 set grounded in what already exists in the schema, but it is not exhaustive; a new
  feature area will need its own materialized view and refresh entry rather than being retrofitted into an existing one.
