# Database

PostgreSQL 18 is the system of record. Schema is managed entirely by Flyway migrations under
`../../helpdesk/src/main/resources/db/migration` - Hibernate's DDL auto-generation is disabled in production
(`SPRING_JPA_HIBERNATE_DDL_AUTO=validate`), so the migrations are the single source of truth for schema state.

## Entity-relationship diagram

```mermaid
erDiagram
    users ||--o| agents: "extends (role=AGENT)"
    users ||--o{ tickets: "requests"
    agents ||--o{ tickets: "is assigned"
    ticket_categories ||--o{ tickets: "classifies"
    ticket_categories ||--o{ ticket_categories: "parent_id (self-referencing)"
    tickets ||--o{ comments: "has"
    comments ||--o{ comments: "replies to (parent_id)"
    users ||--o{ comments: "authors"
    tickets ||--o{ attachments: "has"
    users ||--o{ attachments: "uploads"
    tickets ||--o| ticket_sla: "has"
    users ||--o{ audit_log: "acts as"
    users ||--o{ refresh_tokens: "owns"
    users ||--o{ assets: "is assigned"
    tickets ||--o{ ticket_assets: "concerns"
    assets ||--o{ ticket_assets: "has history"

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

    ticket_categories {
        bigint id PK
        varchar name
        varchar slug UK
        bigint parent_id FK "self-referencing, nullable"
        smallint level "0-2, CHECK constrained"
        varchar default_department
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    tickets {
        bigint id PK
        varchar subject
        text description
        varchar status "OPEN|IN_PROGRESS|ESCALATED|RESOLVED|CLOSED"
        varchar priority "LOW|MEDIUM|HIGH|URGENT"
        bigint category_id FK "references ticket_categories.id, nullable"
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

    assets {
        bigint id PK
        varchar asset_tag UK
        varchar name
        varchar type "LAPTOP|DESKTOP|PRINTER|MONITOR|NETWORKING_EQUIPMENT|SOFTWARE_LICENSE|OTHER"
        varchar status "IN_USE|IN_STORAGE|UNDER_REPAIR|RETIRED|LOST"
        varchar serial_number UK
        varchar manufacturer
        varchar model
        bigint assigned_user_id FK "nullable"
        varchar location
        varchar vendor
        date purchase_date
        numeric purchase_cost
        date warranty_expiry_date
        text notes
        timestamp created_at
        timestamp updated_at
    }

    ticket_assets {
        bigint id PK
        bigint ticket_id FK
        bigint asset_id FK
        bigint linked_by_id FK
        varchar linked_by_name
        timestamp linked_at
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
`USER | AGENT | ADMIN` at the database level (`CHECK` constraint), not just in application code. `login_attempts` backs
the account-lockout mechanism - see
[`docs/security/security-model.md`](../security/security-model.md#account-lockout).

### `agents`

A strict 1:1 extension of `users` for rows with `role = AGENT` (`user_id` is `UNIQUE`), rather than a separate identity.
`availability` drives ticket assignment/routing decisions.

### `ticket_categories`

Self-referencing hierarchy (`parent_id`) capped at three levels (`level` 0-2, database-level `CHECK` constrained) -
Category → Subcategory → Type. `slug` is a unique, URL-safe identifier generated server-side from `name` at creation
time. `default_department` names the team a ticket should auto-route to when filed under that category (inherited from
the parent at creation time if left blank, so subcategories don't all need to repeat it); routing itself is described
in [`docs/architecture/decisions.md`](../architecture/decisions.md). Categories are soft-deleted via
`active = false` rather than removed outright, since historical tickets keep their category label - `PreAuthorize`
and application-level checks prevent deactivating a category that still has active subcategories, and prevent nesting
past the depth cap. `uq_ticket_categories_parent_name` enforces unique sibling names per parent (including among
top-level categories, where `parent_id IS NULL`).

### `tickets`

The core aggregate. `assignee_id` references `agents.id` (not `users.id`) since only agents can be assigned.
`category_id` references `ticket_categories.id` and is nullable - a ticket can be filed without a category and
categorised later. `status` and `priority` are both database-level `CHECK` constrained. Indexes support the four most
common access patterns: status-ordered listing (`idx_tickets_status`), a requester's own tickets
(`idx_tickets_requester`), an agent's assigned queue by status (`idx_tickets_assignee`), and category-based filtering
(`idx_tickets_category`).

### `comments`

Threaded via a self-referencing `parent_id`. `type` distinguishes plain `REPLY`s from
`internal`-only `NOTE`s (visible to agents/admins but not the requesting citizen) and
`RESOLUTION` comments.

### `attachments`

Metadata only - binary content lives on the filesystem (or a mounted volume in production)
under `storage_path`, keyed per ticket. See
[`docs/security/README.md`](../security/security-model.md#file-upload-safety) for how `storage_path`
is validated to prevent path traversal.

### `assets`

IT inventory: laptops, desktops, printers, monitors, networking equipment, and software licenses. `asset_tag` is a
unique human-readable identifier, auto-generated as `AST-{zero-padded id}` when not supplied by the caller (so it can
alternatively be set to match a pre-existing physical barcode scheme). `assigned_user_id` records current ownership for
reporting; `warranty_expiry_date` and `purchase_cost` support the warranty/ownership lookups technicians need when
triaging a ticket - see [ADR 0007](../adr/0007-asset-management.md). Assets are soft-retired via
`status = 'RETIRED'` rather than deleted, so historical ticket links remain meaningful.

### `ticket_assets`

Join table linking a ticket to the asset (s) it concerns, modelled as its own table (rather than a bare many-to-many)
so each link carries `linked_by_id`/`linked_by_name` for accountability, mirroring `audit_log`'s denormalised actor
fields. `uq_ticket_assets_ticket_asset` prevents linking the same asset to the same ticket twice.
`idx_ticket_assets_asset` (on `asset_id, linked_at DESC`) is what makes an asset's device history
(`GET /v1/assets/{id}/tickets`) fast to page through.

### `audit_log`

Originally a ticket-only log; refactored in `V3__refactor_audit_log.sql` into a generic entity audit trail
(`entity_type` + `entity_id` rather than a hardcoded `ticket_id`), with
`actor_name`/`actor_role` captured at write time (denormalised, so the audit trail remains accurate even if a user's
name or role later changes) plus `ip_address` for security-relevant events like login and lockout.

### `sla_policies` / `ticket_sla`

`sla_policies` is a small, seeded configuration table (one row per priority). `ticket_sla` is the per-ticket runtime
state, created when a ticket is opened and updated as
`SlaBreachMonitor` runs its 5-minute sweep, flipping `response_breached` /
`resolution_breached` and the corresponding `*_warning_sent` flags to avoid duplicate notifications.

### `outbox_events`

Backs the transactional outbox pattern — see
[ADR 0001](../adr/0001-transactional-outbox-pattern.md). The partial index
`idx_outbox_status_created` (`WHERE status = 'PENDING'`) keeps the relay's polling query fast even as processed rows
accumulate, since it only indexes the rows the poller actually needs.

### `refresh_tokens` / `password_reset_tokens`

Support the two pieces of server-side auth state described in
[ADR 0003](../adr/0003-jwt-stateless-authentication.md): revocable refresh tokens, and short-lived OTP hashes (never the
raw OTP) for password reset.

## Migration history

| Version | File                                   | Summary                                                                                                                            |
|---------|----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| V1      | `V1__helpdesk_schema.sql`              | Core schema: `users`, `agents`, `tickets`, `comments`, `attachments`, original `audit_log`                                         |
| V2      | `V2__seed_data.sql`                    | Seed data for local/demo environments                                                                                              |
| V3      | `V3__refactor_audit_log.sql`           | Generalised `audit_log` from ticket-only to entity-generic, added actor/IP metadata                                                |
| V4      | `V4__create_refresh_tokens.sql`        | Added `refresh_tokens`                                                                                                             |
| V5      | `V5__create_password_reset_tokens.sql` | Added `password_reset_tokens`                                                                                                      |
| V6      | `V6__create_sla_tables.sql`            | Added `sla_policies` (seeded) and `ticket_sla`                                                                                     |
| V7      | `V7__create_outbox_events.sql`         | Added `outbox_events` for the transactional outbox pattern                                                                         |
| V8      | `V8__create_ticket_categories.sql`     | Added hierarchical `ticket_categories`, seeded default tree, migrated `tickets.category` (free text) to `tickets.category_id` (FK) |
| V9      | `V9__create_assets.sql`                | Added `assets` and `ticket_assets` (join table), seeded demo inventory                                                             |

New migrations should always be additive and forward-only (Flyway's model) - never edit a committed migration file once
it has run against any shared environment.
