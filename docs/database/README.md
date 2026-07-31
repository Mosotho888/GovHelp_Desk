# Database

PostgreSQL 18 is the system of record. Schema is managed entirely by Flyway migrations under
`helpdesk/src/main/resources/db/migration` — Hibernate's DDL auto-generation is disabled in
production (`SPRING_JPA_HIBERNATE_DDL_AUTO=validate`), so the migrations are the single
source of truth for schema state.

## Entity-relationship diagram

```mermaid
erDiagram
    users ||--o| agents : "extends (role=AGENT)"
    users ||--o{ tickets : "requests"
    agents ||--o{ tickets : "is assigned"
    tickets ||--o{ comments : "has"
    comments ||--o{ comments : "replies to (parent_id)"
    users ||--o{ comments : "authors"
    tickets ||--o{ attachments : "has"
    users ||--o{ attachments : "uploads"
    tickets ||--o| ticket_sla : "has"
    users ||--o{ audit_log : "acts as"
    users ||--o{ refresh_tokens : "owns"

    users {
        bigint id PK
        varchar name
        varchar email UK
        varchar password_hash
        varchar role "USER|AGENT|ADMIN"
        varchar phone
        varchar timezone
        boolean active
        int login_attempts
        timestamp created_at
        timestamp updated_at
    }

    agents {
        bigint id PK
        bigint user_id FK "UK, 1:1 with users"
        varchar department
        varchar availability "ONLINE|BUSY|AWAY|OFFLINE"
    }

    tickets {
        bigint id PK
        varchar subject
        text description
        varchar status "OPEN|IN_PROGRESS|ESCALATED|RESOLVED|CLOSED"
        varchar priority "LOW|MEDIUM|HIGH|URGENT"
        varchar category
        bigint requester_id FK
        bigint assignee_id FK "references agents.id"
        boolean escalated
        timestamp created_at
        timestamp updated_at
    }

    comments {
        bigint id PK
        bigint ticket_id FK
        bigint author_id FK
        bigint parent_id FK "self-referencing, nullable"
        text body
        boolean internal
        varchar type "REPLY|NOTE|RESOLUTION"
        timestamp created_at
    }

    attachments {
        bigint id PK
        bigint ticket_id FK
        bigint uploader_id FK
        varchar filename
        varchar content_type
        bigint size_bytes
        varchar storage_path
        timestamp created_at
    }

    audit_log {
        bigint id PK
        varchar entity_type
        bigint entity_id
        bigint actor_id FK
        varchar actor_name
        varchar actor_role
        varchar action
        varchar old_value
        varchar new_value
        varchar ip_address
        varchar description
        timestamp created_at
    }

    sla_policies {
        bigint id PK
        varchar priority UK
        int response_minutes
        int resolution_minutes
        int warning_threshold_minutes
    }

    ticket_sla {
        bigint id PK
        bigint ticket_id FK "UK, 1:1 with tickets"
        timestamp response_due_at
        timestamp resolution_due_at
        timestamp first_response_at
        timestamp resolved_at
        boolean response_breached
        boolean resolution_breached
        boolean response_warning_sent
        boolean resolution_warning_sent
        timestamp created_at
    }

    outbox_events {
        bigint id PK
        varchar event_type
        varchar aggregate_type
        bigint aggregate_id
        jsonb payload
        varchar status "PENDING|PROCESSING|PROCESSED|FAILED"
        int attempts
        text last_error
        timestamp created_at
        timestamp processed_at
    }

    refresh_tokens {
        bigint id PK
        varchar token UK
        bigint user_id FK
        timestamp expires_at
        boolean revoked
        timestamp created_at
    }

    password_reset_tokens {
        bigint id PK
        varchar email
        varchar otp_hash
        timestamp expires_at
        boolean used
        int attempts
        timestamp created_at
    }
```

## Table notes

### `users`
System-wide identity table for all three roles. `role` is constrained to
`USER | AGENT | ADMIN` at the database level (`CHECK` constraint), not just in application
code. `login_attempts` backs the account-lockout mechanism — see
[`docs/security/README.md`](../security/README.md#account-lockout).

### `agents`
A strict 1:1 extension of `users` for rows with `role = AGENT` (`user_id` is `UNIQUE`), rather
than a separate identity. `availability` drives ticket assignment/routing decisions.

### `tickets`
The core aggregate. `assignee_id` references `agents.id` (not `users.id`) since only agents
can be assigned. `status` and `priority` are both database-level `CHECK` constrained.
Indexes support the three most common access patterns: status-ordered listing
(`idx_tickets_status`), a requester's own tickets (`idx_tickets_requester`), and an agent's
assigned queue by status (`idx_tickets_assignee`).

### `comments`
Threaded via a self-referencing `parent_id`. `type` distinguishes plain `REPLY`s from
`internal`-only `NOTE`s (visible to agents/admins but not the requesting citizen) and
`RESOLUTION` comments.

### `attachments`
Metadata only — binary content lives on the filesystem (or a mounted volume in production)
under `storage_path`, keyed per ticket. See
[`docs/security/README.md`](../security/README.md#file-upload-safety) for how `storage_path`
is validated to prevent path traversal.

### `audit_log`
Originally a ticket-only log; refactored in `V3__refactor_audit_log.sql` into a generic
entity audit trail (`entity_type` + `entity_id` rather than a hardcoded `ticket_id`), with
`actor_name`/`actor_role` captured at write time (denormalised, so the audit trail remains
accurate even if a user's name or role later changes) plus `ip_address` for security-relevant
events like login and lockout.

### `sla_policies` / `ticket_sla`
`sla_policies` is a small, seeded configuration table (one row per priority). `ticket_sla` is
the per-ticket runtime state, created when a ticket is opened and updated as
`SlaBreachMonitor` runs its 5-minute sweep, flipping `response_breached` /
`resolution_breached` and the corresponding `*_warning_sent` flags to avoid duplicate
notifications.

### `outbox_events`
Backs the transactional outbox pattern — see
[ADR 0001](../adr/0001-transactional-outbox-pattern.md). The partial index
`idx_outbox_status_created` (`WHERE status = 'PENDING'`) keeps the relay's polling query fast
even as processed rows accumulate, since it only indexes the rows the poller actually needs.

### `refresh_tokens` / `password_reset_tokens`
Support the two pieces of server-side auth state described in
[ADR 0003](../adr/0003-jwt-stateless-authentication.md): revocable refresh tokens, and
short-lived OTP hashes (never the raw OTP) for password reset.

## Migration history

| Version | File | Summary |
|---|---|---|
| V1 | `V1__helpdesk_schema.sql` | Core schema: `users`, `agents`, `tickets`, `comments`, `attachments`, original `audit_log` |
| V2 | `V2__seed_data.sql` | Seed data for local/demo environments |
| V3 | `V3__refactor_audit_log.sql` | Generalised `audit_log` from ticket-only to entity-generic, added actor/IP metadata |
| V4 | `V4__create_refresh_tokens.sql` | Added `refresh_tokens` |
| V5 | `V5__create_password_reset_tokens.sql` | Added `password_reset_tokens` |
| V6 | `V6__create_sla_tables.sql` | Added `sla_policies` (seeded) and `ticket_sla` |
| V7 | `V7__create_outbox_events.sql` | Added `outbox_events` for the transactional outbox pattern |

New migrations should always be additive and forward-only (Flyway's model) — never edit a
committed migration file once it has run against any shared environment.
