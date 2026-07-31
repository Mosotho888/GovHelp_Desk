# C4 Model — Level 3: Components (API container)

This zooms into the **API** container from [`c4-container.md`](c4-container.md) to show its
internal module structure. GovHelpDesk is organised as a modular monolith: each business
domain is a self-contained Java package under `za.gov.helpdesk.<domain>` with its own
`controller/`, `dto/`, `mapper/`, `model/`, `repository/`, and `service/` sub-packages.

```mermaid
graph TB
    subgraph edge["Cross-cutting (config package)"]
        sec["SecurityConfig +<br/>JwtAuthenticationFilter"]
        rl["RateLimitingFilter +<br/>RateLimitPolicyProvider"]
        gex["GlobalExceptionHandler"]
    end

    subgraph domains["Domain modules"]
        auth["auth<br/>login, refresh, logout,<br/>password reset, lockout"]
        users["users<br/>user CRUD, admin actions,<br/>password management"]
        agent["agent<br/>agent profile, availability,<br/>stats"]
        ticket["ticket<br/>ticket CRUD, status<br/>transitions, event dispatch"]
        comment["comment<br/>threaded comments,<br/>internal notes"]
        attachment["attachment<br/>upload, download,<br/>validation, storage"]
        sla["sla<br/>SLA policy, breach<br/>monitor (every 5 min)"]
        auditlog["auditlog<br/>audit trail read/write"]
    end

    subgraph messaging["Messaging + reliability"]
        outbox["outbox<br/>OutboxWriter, OutboxRelay<br/>(poll every 5s), OutboxProcessor"]
        notification["notification<br/>email consumers +<br/>Thymeleaf rendering"]
    end

    db[("PostgreSQL")]
    mq(("RabbitMQ"))

    sec --> auth
    sec --> users
    rl --> sec

    ticket --> outbox
    comment --> outbox
    auth --> outbox
    sla --> outbox
    auditlog -.->|"consumes AUDIT queue"| outbox

    outbox -->|"writes PENDING rows"| db
    outbox -->|"relays to"| mq
    mq -->|"AUDIT_QUEUE"| auditlog
    mq -->|"TICKET_EMAIL_QUEUE, SLA_EMAIL_QUEUE,<br/>PASSWORD_RESET_EMAIL_QUEUE"| notification

    ticket --> comment
    ticket --> attachment
    ticket --> sla
    agent --> ticket

    auth --> db
    users --> db
    agent --> db
    ticket --> db
    comment --> db
    attachment --> db
    sla --> db
    auditlog --> db

    gex -.->|"handles exceptions from"| domains

    classDef edgeStyle fill:#8b3a3a,color:#fff,stroke:#5c2626
    classDef domain fill:#1168bd,color:#fff,stroke:#0b4884
    classDef msg fill:#8b6f1f,color:#fff,stroke:#5c4a14
    classDef store fill:#2b6e34,color:#fff,stroke:#1d4c25

    class sec,rl,gex edgeStyle
    class auth,users,agent,ticket,comment,attachment,sla,auditlog domain
    class outbox,notification msg
    class db,mq store
```

## Key components and their single responsibility

| Component | Package | Responsibility |
|---|---|---|
| `JwtAuthenticationFilter` / `JwtService` | `auth.jwt` | Parses and validates bearer tokens once per request; issues access/refresh tokens |
| `LoginLockoutService` | `auth.policy` | Tracks failed login attempts and locks accounts after the configured threshold |
| `TicketEventDispatcher` | `ticket.event` | Decouples ticket state changes from their audit/notification side effects |
| `TicketStatusTransitionPolicy` | `ticket.policy` | Validates that a requested status change is a legal transition |
| `TicketUpdateCoordinator` | `ticket.service.impl` | Orchestrates multi-field ticket updates (status, assignee, priority) as one cohesive operation |
| `CommentAccessPolicy` | `comment.policy` | Enforces the author-or-admin, 15-minute edit window rule for comment mutation |
| `AttachmentValidator` | `attachment.policy` | Enforces file count, size, and MIME-type limits before storage |
| `FileStorageServiceImpl` | `attachment.service.storage` | Normalises and containment-checks the destination path to prevent path traversal |
| `SlaBreachMonitor` | `sla.schedular` | `@Scheduled` job (every 5 minutes) that flags response/resolution breaches |
| `BusinessHoursCalculator` | `sla.service` | Computes SLA due dates against business-hours logic |
| `OutboxWriter` / `OutboxRelay` / `OutboxProcessor` | `outbox` | Implements the transactional outbox pattern — see [ADR 0001](../adr/0001-transactional-outbox-pattern.md) |
| `RateLimitingFilter` / `RateLimitPolicyProvider` | `config.security` | Token-bucket rate limiting, capacity resolved by authenticated role |
| `GlobalExceptionHandler` | `exception.global` | Central `@ControllerAdvice` translating domain exceptions to a consistent `ApiErrorResponse` |

## Design patterns in use

| Pattern | Where | Why |
|---|---|---|
| Transactional Outbox | `OutboxEvent` + `OutboxRelay` | At-least-once delivery to RabbitMQ without distributed transactions |
| Repository per aggregate | `TicketRepository`, `CommentRepository`, etc. | Clean domain boundaries, independently testable |
| DTO separation | `*Request` / `*Response` / `*Message` | Entities never cross the service boundary |
| Domain events | `TicketEventDispatcher` | Decouples state changes from audit/notification side effects |
| Policy objects | `CommentAccessPolicy`, `AttachmentValidator`, `TicketStatusTransitionPolicy` | Authorization/validation rules live in one testable place instead of scattered service `if`s |
| Per-domain metrics | `TicketMetrics`, `AuthMetrics`, etc. | Each domain owns its own observability |
