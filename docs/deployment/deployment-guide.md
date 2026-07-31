# Deployment Guide

Operational how-to. For the architectural picture and rationale, see
[`docs/architecture/deployment.md`](../architecture/deployment.md) and
[ADR 0004](../adr/0004-oci-arm-free-tier-deployment.md).

## Local development

Requires Java 21, and Docker + Docker Compose for the full stack.

```bash
cd helpdesk
./mvnw spring-boot:run
```

This runs the API alone against whatever datasource is configured in
`application.properties`. To run the full stack (API + PostgreSQL + RabbitMQ + Prometheus + Grafana) exactly as it runs
in production:

```bash
cd helpdesk
cp .env.example .env   # fill in credentials — see "Required environment variables" below
./run.sh
```

`run.sh` creates the shared `spring-boot-api-network` Docker network if it doesn't already exist, then runs
`docker-compose up --build -d`. Once healthy:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- RabbitMQ management: `http://localhost:15672`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

Stop and remove everything with `docker-compose down`.

## Required environment variables

These are consumed by `docker-compose.yml` and must be present in `.env`:

| Variable                                                           | Purpose                                                                                                      |
|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `PG_USER`, `PG_PASSWORD`, `POSTGRES_DB`                            | PostgreSQL credentials and database name                                                                     |
| `JWT_SECRET_KEY`                                                   | HMAC signing secret for JWTs — use a long, random value, never commit it                                     |
| `JWT_VALIDITY`, `JWT_REFRESH_VALIDITY`                             | Access/refresh token TTLs, in milliseconds                                                                   |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`         | SMTP credentials for outbound email                                                                          |
| `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`                   | RabbitMQ broker credentials                                                                                  |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_PORT`          | App-side RabbitMQ connection credentials                                                                     |
| `MIN_CONCURRENCY`                                                  | RabbitMQ listener concurrency (min = max consumers)                                                          |
| `RATE_LIMIT_CAPACITY_UNAUTHENTICATED`, `_USER`, `_AGENT`, `_ADMIN` | Per-role hourly rate-limit bucket capacities                                                                 |
| `GRAFANA_USER`, `GRAFANA_PASSWORD`                                 | Grafana admin credentials (defaults to `admin`/`admin` if unset — **change this in any shared environment**) |

Never commit a populated `.env` file - it is already covered by `../../.gitignore`.

## Docker image

Built via a two-stage `Dockerfile`:

1. **Builder stage** (`eclipse-temurin:21-jdk-jammy`): resolves Maven dependencies offline, then runs
   `./mvnw clean package -DskipTests` with all static-analysis plugins skipped (`spotless`, `checkstyle`, `pmd`,
   `spotbugs`) - those run as their own dedicated CI job instead of gating the image build itself.
2. **Final stage** (`eclipse-temurin:21-jre-jammy`): copies only the built JAR into a slim JRE image, runs as a non-root
   `app` user, and pre-creates the `uploads/` and `logs/`
   directories with correct ownership.

The image is built for **`arm64`** in CI to match the OCI Free Tier Ampere VM - if building locally on an `amd64`
machine for local testing only, that's fine (Docker Compose will use your host architecture), but any image pushed to
the registry for production must target
`arm64`.

## CI pipeline

Defined in `../../.github/workflows/ci.yml`, three sequential jobs on every push:

1. **`build-and-test`** - compiles and runs the full test suite (JUnit 5, Mockito, Testcontainers, Rest-Assured).
2. **`static-analysis`** - Checkstyle, PMD, SpotBugs/FindSecBugs, Spotless check (not auto-fix) -
   see [ADR 0005](../adr/0005-static-analysis-tooling.md).
3. **`docker-publish`** - builds and pushes the `arm64` image to the container registry (`tebohogiven/spring-boot-app`).

## CD pipeline

Defined in `../../.github/workflows/cd.yml`, triggered by a `workflow_run` completion event from the CI workflow, gated
on
`github.event.workflow_run.conclusion == 'success'` and restricted to
`main`:

1. SCPs the current `../../helpdesk/monitoring` directory to the VM (so Prometheus scrape config changes ship alongside
   code changes).
2. SSHes into the OCI VM (`appleboy/ssh-action`) and runs:
   ```bash
   cd ~/helpdesk
   docker compose pull app
   docker compose up -d --no-deps app
   docker image prune -f
   ```

This only restarts the `app` container - PostgreSQL, RabbitMQ, Prometheus, and Grafana keep running, minimising downtime
and avoiding unnecessary connection churn on stateful services.

### Rolling back

There is no automated rollback yet. To roll back manually:

1. Find the previous known-good image tag in the container registry.
2. SSH into the VM.
3. Edit `docker-compose.yml` (or pass an explicit tag) to pin the `app` service to that image.
4. `docker compose up -d --no-deps app`.

Automating this is tracked in [`../../ROADMAP.md`](../../ROADMAP.md).

## DNS and TLS

Production traffic is served under `sothoman.com` via Cloudflare:

- **DNS**: an A/AAAA (or proxied CNAME) record points `sothoman.com` at the OCI VM's public IP, proxied through
  Cloudflare (orange-cloud).
- **SSL/TLS mode**: **Full (Strict)** - Cloudflare requires the origin to present a valid certificate (not self-signed),
  and encrypts the Cloudflare-to-origin hop as well as the client-to-Cloudflare hop. This is stricter than "Full" (which
  accepts a self-signed origin cert) and was chosen deliberately for end-to-end encryption without weakening either hop.

## Alternative target: Railway

A `railway.json` is kept in the repo as a documented fallback deployment target (useful for a quick demo without
provisioning a VM), configured to build from the same `Dockerfile` and health-check against `/v1/health`. It is not the
primary deployment target - see
[ADR 0004](../adr/0004-oci-arm-free-tier-deployment.md#alternatives-considered) for why OCI was chosen instead.

## Health checks

- `GET /v1/health` - application-level liveness, used by Railway's health check config.
- `GET /actuator/health` - Spring Boot Actuator health, used by monitoring.
- PostgreSQL and RabbitMQ both have Docker Compose `healthcheck` blocks; the `app` service
  `depends_on` both with `condition: service_healthy`, so it won't start against a database or broker that isn't ready
  yet.
