# Contributing to GovHelpDesk

Thanks for your interest in GovHelpDesk. This is primarily a solo portfolio project, but issues, suggestions, and pull
requests are welcome.

## Getting set up

1. Fork and clone the repository.
2. Requirements: Java 17, Maven (or use the bundled `./mvnw`), Docker + Docker Compose.
3. Copy the environment variables referenced in `helpdesk/docker-compose.yml` into a local
   `.env` file (database credentials, JWT secret, RabbitMQ credentials, mail settings).
4. From `helpdesk/`, run:
   ```bash
   ./mvnw spring-boot:run
   ```
   or bring up the full stack (API + PostgreSQL + RabbitMQ + Prometheus + Grafana):
   ```bash
   docker compose up --build
   ```
5. Swagger UI is available at `http://localhost:8080/swagger-ui.html` once the app is running.

## Branching

- `main` is the deployable branch; CD deploys to production automatically when CI passes on
  `main`.
- Use short-lived feature branches named `feature/<short-description>`,
  `fix/<short-description>`, or `chore/<short-description>`.
- Rebase on `main` before opening a pull request where practical.

## Code style and static analysis

This project enforces its style through tooling rather than convention alone. Before opening a PR, run:

```bash
./mvnw spotless:apply       # auto-formats code
./mvnw checkstyle:check     # style rules — config/checkstyle/checkstyle.xml
./mvnw pmd:check            # static analysis — config/pmd/pmd-ruleset.xml
./mvnw spotbugs:check       # bug pattern detection — config/spotbugs/
```

All four run as part of the `static-analysis` CI job and must pass before a PR can merge.

A few conventions worth knowing:

- Prefer fixing or excluding a rule globally in the relevant ruleset file over scattering
  `@SuppressWarnings` through the codebase.
- New domains follow the existing package layout: `controller/`, `dto/{request,response}/`,
  `mapper/`, `model/`, `repository/`, `service/{,impl/}`, and (where relevant) `policy/`,
  `metrics/`, `event/`.
- Keep services single-responsibility. If a service is accumulating unrelated concerns (validation, event dispatch,
  query composition), extract a collaborator — see
  `TicketEventDispatcher`, `CommentAccessPolicy`, and `AttachmentValidator` for the pattern.

## Tests

- Unit tests use JUnit 5 and Mockito; integration tests use Testcontainers and Rest-Assured.
- New endpoints or state transitions should come with both a unit test for the service logic and an integration test
  exercising the HTTP layer.
- Run the full suite with:
  ```bash
  ./mvnw test
  ```

## Commit messages

Keep commits scoped and descriptive. Conventional prefixes (`feat:`, `fix:`, `chore:`,
`docs:`, `test:`, `refactor:`) are encouraged but not enforced.

## Pull requests

- Describe what changed and why, not just what.
- Link any relevant issue.
- Confirm CI is green (build-and-test, static-analysis, docker-publish) before requesting review.
- For architecturally significant changes (new external dependency, new messaging queue, a changed authentication flow,
  etc.), consider adding an ADR under `docs/adr/` - see
  `docs/adr/0000-template.md`.

## Reporting bugs or security issues

- Functional bugs: open a GitHub issue using the provided template.
- Security vulnerabilities: do **not** open a public issue - see [SECURITY.md](SECURITY.md)
  for responsible disclosure.

## Code of conduct

Participation in this project is governed by the
[Code of Conduct](./CODE_OF_CONDUCT.md).
