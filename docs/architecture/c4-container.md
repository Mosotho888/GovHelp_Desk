# C4 Model - Level 2: Containers

This zooms into the GovHelpDesk system boundary from
[`c4-context.md`](c4-context.md) to show the deployable containers and how they communicate. See [
`c4-component.md`](c4-component.md) for the breakdown of the API container itself.

```mermaid
graph TB
    subgraph clients["Clients"]
        browser["Swagger UI / REST client<br/>(Browser or HTTP client)"]
    end

    subgraph govhelpdesk["GovHelpDesk System (Docker Compose stack)"]
        api["API<br/>(Container: Spring Boot 3.5 / Java 17)<br/>REST API, JWT auth, business logic,<br/>outbox writer, SLA scheduler"]
        db[("PostgreSQL 18<br/>(Container: Database)<br/>Tickets, users, audit log,<br/>SLA, outbox, tokens")]
        mq["RabbitMQ 3<br/>(Container: Message Broker)<br/>4 topic queues + DLQs:<br/>audit, ticket email,<br/>password-reset email, SLA email"]
        prom["Prometheus<br/>(Container: Metrics)<br/>Scrapes /actuator/prometheus"]
        grafana["Grafana<br/>(Container: Dashboards)<br/>8 provisioned dashboards"]
        fs[["Local filesystem volume<br/>(File Storage)<br/>Ticket attachment binaries,<br/>keyed by ticket ID"]]
    end

    smtp["SMTP Provider<br/>(External System)"]
    browser -->|" HTTPS, JWT Bearer "| api
    api -->|" JDBC / JPA "| db
    api -->|" AMQP, publish + consume "| mq
    api -->|" Read/write attachment files "| fs
    api -->|" Exposes /actuator/prometheus "| prom
    prom -->|" Data source "| grafana
    mq -->|" Consumers render Thymeleaf<br/>templates, send via SMTP "| smtp
    classDef container fill: #1168bd, color: #fff, stroke: #0b4884
    classDef db fill: #2b6e34, color: #fff, stroke: #1d4c25
    classDef external fill: #999999, color: #fff, stroke: #6b6b6b
    classDef client fill: #666, color: #fff, stroke: #444
    class api container
class db,mq db
class prom,grafana container
class fs db
class smtp external
class browser client
```

## Containers

| Container    | Technology                                        | Responsibility                                                                                                                                                 |
|--------------|---------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| API          | Spring Boot 3.5, Java 17                          | Serves all `/v1/*` REST endpoints, enforces JWT auth + RBAC, runs the SLA breach scheduler and outbox relay                                                    |
| PostgreSQL   | PostgreSQL 18                                     | System of record — users, agents, tickets, comments, attachments metadata, audit log, SLA policies/state, refresh tokens, password-reset tokens, outbox events |
| RabbitMQ     | RabbitMQ 3 (management image)                     | Durable delivery of audit, ticket-email, password-reset-email, and SLA-email messages; each queue has a matching dead-letter queue                             |
| File storage | Docker named volume mounted at `/opt/app/uploads` | Binary storage for ticket attachments, organised per `ticket-{id}` directory                                                                                   |
| Prometheus   | `prom/prometheus`                                 | Scrapes the API's `/actuator/prometheus` endpoint                                                                                                              |
| Grafana      | `grafana/grafana:10.4.3`                          | Dashboards over the Prometheus data source                                                                                                                     |

## Communication

- **Client → API**: HTTPS, JSON, `Authorization: Bearer <JWT>` for all endpoints except
  `/v1/auth/**`, `/v1/health`, and the Swagger/OpenAPI paths.
- **API → PostgreSQL**: Spring Data JPA for transactional aggregate access, plus a JDBC-based reporting repository
  (`TicketJdbcRepository`, `ReportJdbcRepository`) for query-heavy reads that don't map cleanly to the JPA aggregate
  model.
- **API → RabbitMQ**: never a direct synchronous publish from request threads. All cross-cutting side effects (audit
  entries, notification emails) are written to the
  `outbox_events` table in the same transaction as the business change, then relayed to RabbitMQ by a scheduled
  `OutboxRelay` poller - see
  [ADR 0001](../adr/0001-transactional-outbox-pattern.md).
- **API → Filesystem**: attachment binaries are written under a normalised, containment-checked path to prevent path
  traversal - see [`docs/security/README.md`](../security/security-model.md).
- **API → Prometheus**: pull-based scraping, not push.

## Deployment container mapping

In production (`../../helpdesk/docker-compose.yml`), each container above maps 1:1 to a Docker Compose service (`app`,
`db`,
`rabbitmq`, `prometheus`, `grafana`), all attached to the
`spring-boot-api-network` bridge network, running on a single OCI Free Tier ARM VM - see
[`deployment.md`](deployment.md) for the full picture.
