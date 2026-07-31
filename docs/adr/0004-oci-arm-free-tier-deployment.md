# ADR 0004: Oracle Cloud Free Tier ARM VM deployment

## Status

Accepted

## Context

GovHelpDesk needed a real, publicly reachable production deployment to be credible as a recruiter-facing portfolio
project - a project that only runs `docker compose up` on
`localhost` doesn't demonstrate operational skills the same way a live deployment does. The constraint driving this
decision was cost: as a self-funded portfolio project, ongoing hosting spend needed to be as close to zero as possible
without compromising on doing the deployment "properly" (TLS, monitoring, CI/CD, containerisation).

## Decision

Deploy to a single **Oracle Cloud Infrastructure Free Tier ARM (Ampere) compute VM**, running Ubuntu 22.04 in the
Johannesburg region (chosen for proximity/latency to the project's South African context), running the full stack via
Docker Compose (Spring Boot API, PostgreSQL 18, RabbitMQ 3, Prometheus, Grafana). Public traffic is fronted by
Cloudflare (DNS + TLS termination, Full Strict SSL mode) under the domain `sothoman.com`, so the origin VM also needs a
valid certificate for the Cloudflare-to-origin hop rather than serving over plain HTTP.

Because OCI's Free Tier ARM shape is Ampere (`arm64`), the CI `docker-publish` job builds and pushes an `arm64` image —
this required correcting an initial `amd64` build target that didn't run on the target VM.

Deployment itself is automated: the `cd.yml` GitHub Actions workflow triggers on successful completion of the CI
workflow against `main`, SCPs the current monitoring configuration to the VM, then SSHes in to
`docker compose pull app && docker compose up -d --no-deps app`, restarting only the application container.

## Alternatives considered

- **A managed PaaS (Railway, Render, Fly.io, Heroku-likes)** — genuinely simpler to operate (no VM/OS management), and
  the project does keep a `railway.json` config as a fallback option. Rejected as the primary target because free tiers
  on these platforms are typically more constrained (sleep on inactivity, limited always-on compute) than OCI's
  genuinely always-free ARM VM, and because managing the VM directly better demonstrates infrastructure skills for a
  portfolio project.
- **AWS/GCP/Azure free tiers** — all viable, but their free tiers are either time-limited (12 months) or
  resource-constrained in ways that don't suit an always-on demo. OCI's Always Free ARM shape (up to 4 OCPUs / 24GB RAM
  across Ampere instances) was the most generous always-free compute available at the time of this decision.
- **Kubernetes (k3s or managed)** — rejected as disproportionate; a single-node Docker Compose stack is the right
  complexity for one API instance with no horizontal scaling requirement yet.

## Consequences

**Easier:**

- Zero ongoing hosting cost while still being a genuine, always-on, publicly reachable deployment with TLS, monitoring,
  and automated CI/CD — a stronger portfolio signal than a local-only demo.
- The `arm64` image also means Apple Silicon developers get a native local build for free.

**Harder:**

- Single point of failure — there is no redundancy or failover; an outage of the one VM is an outage of the whole
  system. Acceptable for a portfolio-stage project, but explicitly called out as a scaling limitation (see [
  `ROADMAP.md`](../../ROADMAP.md)).
- ARM-specific issues (like the initial `amd64`/`arm64` image mismatch) are an ongoing class of bug to watch for in base
  images and native dependencies.
- OS-level maintenance (patching Ubuntu, Docker itself) is now the maintainer's responsibility, unlike on a managed
  PaaS.
