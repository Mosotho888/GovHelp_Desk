# Changelog

All notable changes to GovHelpDesk are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project follows its own
pragmatic versioning during pre-1.0 development: each entry groups changes by the migration or milestone that introduced
them rather than a strict semver cadence.

## [Unreleased]

### Added

- Line-ending normalisation (`.gitattributes`) to keep LF consistent across contributors.

### Fixed

- Swagger UI CORS/server resolution issue in `OpenApiConfig` by explicitly registering the production and local server
  entries via `addServersItem()`.

---

## [1.0.0] - Production release

### Added

- Full production deployment to an Oracle Cloud Infrastructure Free Tier ARM VM (Ubuntu 22.04, Johannesburg region),
  fronted by Cloudflare DNS with Full (Strict) SSL, serving the API at `api.sothoman.com`.
- Docker Compose stack: Spring Boot API, PostgreSQL 18, RabbitMQ 3 (with management UI), Prometheus, and Grafana, wired
  on a shared bridge network with health-check gated startup.
- GitHub Actions CD workflow (`cd.yml`) that deploys to the OCI VM over SSH via
  `appleboy/ssh-action` once CI succeeds on `main`, pulling the new image and restarting only the `app` service.

### Changed

- Rebuilt the CI Docker image target platform from `amd64` to `arm64` to match the OCI Ampere ARM VM architecture.

---

## [0.6.0] - Test suite rewrite

### Added

- `CommentIntegrationTest`, `AgentIntegrationTest`, and `AuditLogIntegrationTest` integration test classes.
- Full rewrite of the unit and integration test suite following the SRP refactor, bringing coverage from 58 broken tests
  to 94 passing tests across 11 test files.

### Fixed

- Test suite breakage caused by the SRP extraction of collaborator classes (services now depend on
  `TicketEventDispatcher`, `CommentAccessPolicy`, `AttachmentValidator`, etc., which needed to be mocked explicitly
  instead of relying on monolithic service behaviour).

---

## [0.5.0] - Static analysis hardening

### Added

- Checkstyle, PMD 7.x, SpotBugs/FindSecBugs, and Spotless configured with production-grade rulesets
  (`config/checkstyle`, `config/pmd`, `config/spotbugs`).
- `static-analysis` job in the CI pipeline, run after `build-and-test`.

### Changed

- Preferred global ruleset exclusions over scattering `@SuppressWarnings` annotations through the codebase.
- Corrected `SuppressionSingleFilter` placement to be a direct `Checker` child rather than nested inside `TreeWalker`,
  per Checkstyle's XML schema.

### Fixed

- PMD 7 breaking changes and rule category conflicts surfaced by the new ruleset.
- JUnit 5 test method naming violations flagged by the new Checkstyle rules.

---

## [0.4.0] - Security hardening

### Fixed

- **IDOR vulnerabilities**: repository queries across tickets, comments, and attachments made actor-aware so users can
  only read/mutate records they are entitled to.
- **Path traversal** in file uploads: `FileStorageServiceImpl` now normalizes and validates that the resolved
  destination path stays within the configured upload root before writing or deleting a file, and strips unsafe
  characters from filenames.
- Refresh token guard logic corrected so revoked/expired tokens are rejected consistently.

### Added

- Account lockout after `APP_SECURITY_MAX_LOGIN_ATTEMPTS` (default 5) consecutive failed logins, published to the audit
  trail as `ACCOUNT_LOCKED`.
- Per-role rate limiting (`RateLimitingFilter` + `RateLimitPolicyProvider`) with distinct token bucket capacities for
  unauthenticated, `USER`, `AGENT`, and `ADMIN` callers.

---

## [0.3.0] - Messaging and outbox pattern

### Added

- Transactional outbox pattern (`outbox_events` table, `OutboxRelay`, `OutboxWriter`,
  `OutboxProcessor`) guaranteeing at-least-once delivery of audit, ticket email, password-reset email, and SLA email
  events to RabbitMQ without distributed transactions.
- `TYPE_MAP`-based deserialisation so RabbitMQ consumers receive fully-typed DTOs instead of generic payloads.
- Four RabbitMQ queues (`audit.queue`, `ticket.email.queue`, `password.reset.email.queue`,
  `sla.email.queue`) each with a matching dead-letter queue.
- SLA breach monitor (`SlaBreachMonitor`) running every 5 minutes to flag response/resolution breaches and trigger
  warning/breach emails.

### Changed

- `audit_log` table refactored (`V3__refactor_audit_log.sql`) from a ticket-only log into a generic entity audit trail
  (`entity_type` + `entity_id`), with actor name/role and IP address captured at write time.

---

## [0.2.0] - Domain refactor (SRP audit)

### Changed

- Extracted roughly 14 responsibilities out of oversized service classes into dedicated collaborators, including
  `TicketEventDispatcher`, `CommentAccessPolicy`,
  `AttachmentValidator`, `LoginLockoutService`, `OutboxRelay`, and `SlaBreachMonitor`.
- Split ticket read/write concerns between `TicketRepository` (JPA) and
  `TicketJdbcRepository` (JDBC) to isolate query-heavy reporting logic from the transactional aggregate repository.

---

## [0.1.0] - Initial ticketing core

### Added

- Core domain: `users`, `agents`, `tickets`, `comments`, `attachments`, `audit_log` schema (`V1__helpdesk_schema.sql`)
  and seed data (`V2__seed_data.sql`).
- JWT authentication (access + refresh tokens) with `USER` / `AGENT` / `ADMIN` role-based access control enforced via
  Spring Security method security.
- Ticket lifecycle: `OPEN → IN_PROGRESS -> RESOLVED -> CLOSED`, with an `ESCALATED` branch.
- SLA policy tables and per-priority response/resolution deadlines (`V6__create_sla_tables.sql`).
- OTP-based password reset flow (`V5__create_password_reset_tokens.sql`).
- Refresh token store (`V4__create_refresh_tokens.sql`).
