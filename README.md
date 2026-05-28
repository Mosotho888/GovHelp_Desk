# Government HelpDesk API

A Spring Boot REST API for managing government-style helpdesk support requests. The system covers authentication, users, agents, tickets, comments, attachments, email/audit events, and role-based access control.

The project is structured as a production-style backend portfolio project: it uses PostgreSQL with Flyway migrations, JWT security, RabbitMQ-backed asynchronous workflows, Dockerized infrastructure, OpenAPI documentation, and unit/integration tests.

## What This Project Does

- Authenticates users with JWT access and refresh tokens.
- Enforces role-based access for `USER`, `AGENT`, and `ADMIN`.
- Lets users create and track support tickets.
- Lets agents/admins update ticket status, priority, escalation, and assignment.
- Tracks SLA response/resolution deadlines using business-hour calculations and priority policies.
- Supports threaded ticket comments, internal notes, and resolution comments.
- Supports file uploads/downloads for ticket attachments.
- Tracks audit history for tickets, users, agents, authentication events, and actor activity.
- Publishes/consumes RabbitMQ events for audit and email notification workflows.
- Stores relational data in PostgreSQL and manages schema changes with Flyway.
- Provides Swagger UI/OpenAPI docs for API exploration.
- Includes unit and integration tests using JUnit, Mockito, MockMvc, and Testcontainers.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.5.0 |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL, Spring Data JPA, JDBC |
| Migrations | Flyway |
| Messaging | RabbitMQ |
| Email | Spring Mail / JavaMailSender |
| Templates | Thymeleaf |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers, Rest Assured, JaCoCo |
| Build | Maven Wrapper |
| Deployment | Docker, Docker Compose, Railway config |

## Architecture

```text
Client
  |
  v
Spring MVC Controllers
  |
  v
Service Layer
  |
  +--> Spring Data JPA / JDBC repositories
  +--> JWT security and method-level authorization
  +--> Audit event publishing
  +--> RabbitMQ email notification workflows
  |
  v
PostgreSQL / RabbitMQ / File storage
```

Main modules:

| Module | Purpose |
| --- | --- |
| `auth` | Login, refresh tokens, JWT filtering, user details loading |
| `users` | User CRUD, profile lookup, deactivation, roles |
| `agent` | Agent registration, availability, departments, statistics |
| `ticket` | Ticket creation, filtering, status transitions, assignment, escalation |
| `sla` | SLA policy lookup, ticket SLA deadlines, warnings, breach detection |
| `comment` | Ticket comments, replies, internal notes, edit/delete rules |
| `attachment` | Multipart upload, list, download, delete |
| `auditlog` | Immutable audit history and audit queries |
| `notification` | Email templates, publishing, and consumers |
| `config` | Security, RabbitMQ, mail, database, OpenAPI, rate limiting |
| `exception` | Global exception handling and API error responses |

## Project Structure

```text
GovHelp_Desk/
  README.md
  INTERVIEW_GUIDE.md
  helpdesk/
    Dockerfile
    docker-compose.yml
    pom.xml
    railway.json
    src/main/java/za/gov/helpdesk/
    src/main/resources/
      application.properties
      db/migration/
    src/test/java/za/gov/helpdesk/
    src/test/resources/application-test.properties
```

## Getting Started

### Prerequisites

- Java 17+
- Docker Desktop
- Git

The project uses the Maven Wrapper, so a separate Maven installation is not required.

### Clone

```bash
git clone https://github.com/Mosotho888/GovHelp_Desk.git
cd GovHelp_Desk/helpdesk
```

### Environment Variables

Create a `.env` file inside the `helpdesk` directory for Docker Compose:

```env
PG_HOST=localhost
PG_PORT=5433
POSTGRES_DB=helpdesk_db
PG_USER=helpdesk
PG_PASSWORD=helpdesk

HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect

JWT_SECRET_KEY=replace-with-a-long-random-secret
JWT_VALIDITY=3600000
JWT_REFRESH_VALIDITY=604800000

UPLOAD_PATH=./uploads

MAIL_HOST=localhost
MAIL_PORT=2525
MAIL_USERNAME=test@example.com
MAIL_PASSWORD=test-password

RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest

MIN_CONCURRENCY=2
```

Use strong secrets and real managed-service credentials outside local development.

## Run Locally

### With Docker Compose

From `helpdesk/`:

```bash
docker compose up --build
```

Services:

| Service | URL |
| --- | --- |
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Actuator health | `http://localhost:8080/actuator/health` |
| RabbitMQ management | `http://localhost:15672` |
| PostgreSQL | `localhost:5433` |

### With Maven

Start PostgreSQL and RabbitMQ first, then run:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Testing

The project has unit tests for services, SLA business-hours logic, ticket lifecycle behavior, and notification messaging. Integration tests use MockMvc with Testcontainers-backed PostgreSQL and RabbitMQ.

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

If Docker Desktop is not running, Testcontainers integration tests are skipped cleanly while unit tests still run. To execute the full integration suite, start Docker Desktop first.

Useful focused test commands:

```bash
# Unit tests only
./mvnw test -Dtest="za.gov.helpdesk.unit.**.*Test"

# SLA-focused tests
./mvnw test -Dtest="BusinessHoursCalculatorTest,SlaServiceImplTest"

# Ticket service tests
./mvnw test -Dtest=TicketServiceImplTest
```

For coverage enforcement:

```bash
./mvnw verify
```

JaCoCo is configured with an 80% line coverage threshold during `verify`.

## API Overview

Protected endpoints require:

```http
Authorization: Bearer <access-token>
```

### Authentication

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/v1/auth/login` | Public | Authenticate and receive access/refresh tokens |
| POST | `/v1/auth/refresh` | Public | Exchange a refresh token for a new token pair |

### Users

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/v1/users` | Admin | Create a user |
| GET | `/v1/users` | Admin | List users |
| GET | `/v1/users/me` | Authenticated | Get the current user profile |
| GET | `/v1/users/{id}` | Admin or owner | Get user by ID |
| PUT | `/v1/users/{id}` | Admin or owner | Update user profile |
| DELETE | `/v1/users/{id}` | Admin | Deactivate user |

### Agents

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/v1/agents` | Admin | Register a user as an agent |
| GET | `/v1/agents` | Agent/Admin | List agents |
| GET | `/v1/agents/{id}` | Agent/Admin | Get agent by ID |
| PATCH | `/v1/agents/{id}` | Admin or agent owner | Update department or availability |
| GET | `/v1/agents/{id}/stats` | Admin | Get agent ticket statistics |

### Tickets

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/v1/tickets` | Authenticated | Create a ticket |
| GET | `/v1/tickets` | Authenticated | List tickets with optional `status`, `priority`, and `assigneeId` filters |
| GET | `/v1/tickets/{id}` | Authenticated | Get ticket by ID |
| PATCH | `/v1/tickets/{id}` | Agent/Admin | Update status, priority, escalation, or assignee |
| DELETE | `/v1/tickets/{id}` | Admin | Delete a ticket |

Ticket lifecycle:

```text
OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
OPEN -> IN_PROGRESS -> ESCALATED -> IN_PROGRESS
RESOLVED -> OPEN
```

SLA behavior:

- SLA is initialized when a ticket is created.
- First response is recorded when a ticket moves to `IN_PROGRESS`.
- Resolution is recorded when a ticket moves to `RESOLVED`.
- SLA warnings use each priority policy's `warning_threshold_minutes`.
- SLA breach checks run on a scheduled job.

### SLA

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| GET | `/v1/tickets/{ticketId}/sla` | Agent/Admin | Get SLA status, response deadline, resolution deadline, and breach state |

### Comments

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/v1/tickets/{ticketId}/comments` | Authenticated | Add a comment to a ticket |
| GET | `/v1/tickets/{ticketId}/comments` | Authenticated | List comments on a ticket |
| POST | `/v1/comments/{commentId}/replies` | Authenticated | Reply to a comment |
| GET | `/v1/comments/{commentId}/replies` | Authenticated | List replies |
| PUT | `/v1/comments/{commentId}` | Authenticated | Edit a comment within allowed rules |
| DELETE | `/v1/comments/{commentId}` | Authenticated | Delete a comment within allowed rules |

### Attachments

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/v1/tickets/{ticketId}/attachments` | Authenticated | Upload up to 5 files, 20 MB each |
| GET | `/v1/tickets/{ticketId}/attachments` | Authenticated | List ticket attachments |
| GET | `/v1/attachments/{attachmentId}` | Authenticated | Download attachment |
| DELETE | `/v1/attachments/{attachmentId}` | Authenticated | Delete attachment |

### Audit Logs

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| GET | `/v1/audit/tickets/{id}` | Agent/Admin | Get audit trail for a ticket |
| GET | `/v1/audit/users/{id}` | Admin | Get audit trail for a user |
| GET | `/v1/audit/agents/{id}` | Admin | Get audit trail for an agent |
| GET | `/v1/audit/auth` | Admin | Get authentication audit events |
| GET | `/v1/audit/actor/{actorId}` | Admin | Get actions performed by one actor |
| GET | `/v1/audit/action/{action}` | Admin | Get events by action type |

## Security Features

- Stateless JWT authentication.
- BCrypt password hashing with strength 12.
- Method-level authorization through `@PreAuthorize`.
- Account lockout after repeated failed login attempts.
- Inactive users are blocked from authentication.
- Custom JSON responses for unauthorized and forbidden requests.
- Security headers for HSTS, CSP, and frame protection.
- Rate-limiting filter wired into the security chain.

## Database

Flyway migrations live in:

```text
helpdesk/src/main/resources/db/migration
```

Current migration set:

| Migration | Purpose |
| --- | --- |
| `V1__helpdesk_schema.sql` | Creates users, agents, tickets, comments, attachments, and audit log tables |
| `V2__seed_data.sql` | Inserts local/demo data |
| `V3__refactor_audit_log.sql` | Refactors audit logging into entity-based audit records |
| `V4__create_refresh_tokens.sql` | Adds persistent refresh tokens |
| `V5__create_password_reset_tokens.sql` | Adds password reset OTP storage |
| `V6__create_sla_tables.sql` | Adds SLA policies and ticket SLA tracking |

## Useful Commands

```bash
# Run app locally
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run tests and coverage checks
./mvnw verify

# Build jar
./mvnw clean package

# Start local infrastructure and API
docker compose up --build

# Stop local infrastructure
docker compose down
```

## License

This project is licensed under the MIT License.

## Author

Tebogo Mofokeng
