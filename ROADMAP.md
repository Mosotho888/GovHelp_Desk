# Roadmap

GovHelpDesk is a portfolio-quality, recruiter-facing project demonstrating a production-grade full-stack build for South
African government support ticketing. This roadmap tracks where the project has been and where it's headed.

## Shipped

- [x] Core ticketing domain: users, agents, tickets, comments, attachments, audit log
- [x] JWT authentication with access/refresh tokens and RBAC (`USER` / `AGENT` / `ADMIN`)
- [x] OTP-based password reset flow
- [x] SLA policy engine with automated breach/warning detection (`SlaBreachMonitor`, every 5 min)
- [x] Transactional outbox pattern for reliable async messaging (RabbitMQ, 4 queues + DLQs)
- [x] Email notifications via Thymeleaf templates (ticket lifecycle, SLA, password reset)
- [x] SRP-driven domain refactor extracting ~14 single-responsibility collaborators
- [x] Security hardening: IDOR fixes, path traversal prevention, refresh token guard fix, account lockout, per-role rate
  limiting
- [x] Static analysis pipeline: Checkstyle, PMD 7.x, SpotBugs/FindSecBugs, Spotless
- [x] CI pipeline (build-and-test → static-analysis → docker-publish) on GitHub Actions
- [x] CD pipeline deploying to Oracle Cloud Infrastructure via SSH on successful CI
- [x] Full production deployment: OCI Free Tier ARM VM, Cloudflare DNS with Full Strict SSL, Docker Compose stack
  (Spring Boot + PostgreSQL + RabbitMQ + Prometheus + Grafana)
- [x] Observability: per-domain Micrometer metrics, Prometheus scraping, Grafana dashboards
- [x] Test suite rewrite post-refactor: 94 tests across 11 files, including three new integration test classes
- [x] Production React frontend (Vite + TypeScript + TanStack Query/Table + shadcn/ui + React Hook Form + Zod),
  Dockerized and deployed via GitHub Actions CI/CD to [govhelpdesk.sothoman.com](https://govhelpdesk.sothoman.com) —
  see [`Helpdesk_Frontend`](https://github.com/Mosotho888/Helpdesk_Frontend)

## In progress

- [ ] Push the updated README and aligned `docs/` (frontend references, Java 21 correction, API reference fixes) to
  `main`
- [ ] End-to-end validation of the CI/CD pipeline with a real commit against `main`

## Planned

- [ ] API versioning strategy beyond `/v1` (deprecation policy, header-based negotiation)
- [ ] Pagination and filtering consistency audit across all list endpoints
- [ ] WebSocket or SSE channel for real-time ticket/agent status updates
- [ ] Formal load-testing pass (k6 or Gatling) to validate rate-limit tiers under load
- [ ] Multi-tenancy exploration for supporting more than one government department per deployment
- [ ] Structured audit log export (CSV/PDF) for compliance reporting
- [ ] Generated TypeScript client from the OpenAPI spec, to replace hand-written frontend API types with a build-time
  contract check against the backend

## Explicitly out of scope (for now)

- Horizontal scaling / multi-instance deployment - the OCI Free Tier ARM VM is a single-node target chosen for cost
  reasons while the project is portfolio-stage.

Suggestions and feedback are welcome - see [CONTRIBUTING.md](CONTRIBUTING.md).
