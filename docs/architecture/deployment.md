# Deployment Architecture

This describes the production topology. For step-by-step operational instructions (how to deploy, roll back, or run
locally), see [`docs/deployment/README.md`](../deployment/deployment-guide.md).

## Production topology

```mermaid
graph TB
    user["Citizen / Agent / Admin"]

    subgraph cf["Cloudflare"]
        dns["DNS: sothoman.com"]
        tls["TLS termination<br/>(Full - Strict mode)"]
    end

    subgraph oci["Oracle Cloud Infrastructure — Free Tier ARM VM (Ubuntu 22.04, Johannesburg)"]
        subgraph compose["Docker Compose stack"]
            app["app<br/>Spring Boot 3.5 (arm64 image)"]
            db[("db<br/>PostgreSQL 18")]
            mq(("rabbitmq<br/>RabbitMQ 3-management"))
            prom["prometheus"]
            graf["grafana:10.4.3"]
        end
        vol[["Named volumes:<br/>postgres_data, upload_data,<br/>rabbitmq_data, prometheus_data,<br/>grafana_data"]]
    end

    gha["GitHub Actions<br/>(CI + CD)"]
    ghcr["Container registry<br/>(image: tebohogiven/spring-boot-app)"]
    user -->|HTTPS| dns --> tls -->|" proxied HTTPS "| app
    app --> db
    app --> mq
    app -.-> vol
    db -.-> vol
    mq -.-> vol
    prom --> app
    graf --> prom
    gha -->|" on push to main:<br/>build, test, static analysis "| gha
    gha -->|" docker build + push (arm64) "| ghcr
    gha -->|" on CI success:<br/>SSH deploy, docker compose pull + up "| app
    classDef ext fill: #999, color: #fff, stroke: #666
    classDef svc fill: #1168bd, color: #fff, stroke: #0b4884
    classDef store fill: #2b6e34, color: #fff, stroke: #1d4c25
    classDef ci fill: #8b6f1f, color: #fff, stroke: #5c4a14
    class user ext
class dns, tls ext
class app,prom, graf svc
class db,mq, vol store
class gha,ghcr ci
```

## Why this topology

- **Single ARM VM, Docker Compose (not Kubernetes)**: GovHelpDesk targets Oracle Cloud's Free Tier ARM (Ampere) compute,
  chosen specifically to keep hosting cost at zero while the project is at portfolio stage. A single Compose stack is
  the right level of complexity for that constraint - see [ADR 0004](../adr/0004-oci-arm-free-tier-deployment.md).
- **Cloudflare in front, Full (Strict) SSL**: Cloudflare terminates public TLS and re-encrypts the hop to the origin VM,
  so the origin also needs a valid certificate rather than a self-signed one. This gives end-to-end encryption without
  managing public DNS or ACME challenges directly on the VM.
- **CD triggers only after CI succeeds on `main`**: the `cd.yml` workflow listens for a
  `workflow_run` completion event from `GovHelpDesk CI`, and only proceeds
  `if: github.event.workflow_run.conclusion == 'success'`. This keeps a broken build from ever reaching production.
- **CD deploys by pulling and restarting only the `app` service**: `docker compose pull app`
  followed by `docker compose up -d --no-deps app` avoids restarting PostgreSQL, RabbitMQ, or the monitoring stack on
  every deploy, minimising downtime and avoiding unnecessary connection churn on stateful services.
- **arm64 image target**: the OCI Free Tier compute shape is Ampere (ARM), so the CI
  `docker-publish` job builds for `arm64` rather than the more common `amd64` default - this was a real mismatch that
  had to be fixed during setup (see
  [`../../CHANGELOG.md`](../../CHANGELOG.md)).

## Observability

- The API exposes `/actuator/health` and `/actuator/prometheus` (both explicitly permitted through Spring Security
  without authentication, restricted to `GET`).
- Prometheus scrapes the API on an interval defined in `monitoring/prometheus/prometheus.yml`.
- Grafana is provisioned with Prometheus as its data source and ships with dashboards covering per-domain metrics
  (`TicketMetrics`, `AuthMetrics`, `AttachmentMetrics`,
  `NotificationMetrics`, `OutboxMetrics`, `SlaMetrics`, `CommentMetrics`, `AgentMetrics`).
- The CD workflow additionally SCPs the `monitoring/` configuration to the VM on every deploy, so Prometheus scrape
  configuration changes ship the same way application changes do.
