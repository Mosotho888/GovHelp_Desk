# Government HelpDesk API

A production-style Spring Boot REST API for managing government support requests, agents, users, ticket comments, attachments, audit history, and role-based access. The project is built as a backend portfolio piece that demonstrates secure API design, relational data modelling, containerized infrastructure, automated database migrations, and integration testing with real PostgreSQL containers.

## Highlights

- JWT authentication with access and refresh tokens.
- Role-based access control for `USER`, `AGENT`, and `ADMIN`.
- Ticket lifecycle management with guarded status transitions.
- Agent assignment, availability tracking, and agent statistics.
- User administration with account deactivation and login lockout.
- Ticket comments, threaded replies, internal notes, and attachments.
- Audit logging for ticket creation, assignment, escalation, deletion, and status changes.
- PostgreSQL schema managed through Flyway migrations.
- Docker Compose setup for the API, PostgreSQL, and RabbitMQ.
- Swagger/OpenAPI documentation.
- Unit and integration test coverage using JUnit, Spring MockMvc, and Testcontainers.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL, Spring Data JPA, JDBC |
| Migrations | Flyway |
| Messaging | RabbitMQ |
| Email | Spring Mail / JavaMailSender |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo |
| Build | Maven Wrapper |
| Deployment | Docker, Docker Compose, Railway config |

## Architecture

The application follows a layered backend structure:

```text
Controller -> Service -> Repository -> PostgreSQL
              |
              -> Security, JWT, validation, audit logging
              -> RabbitMQ/email configuration for async notification workflows
```

Main domains:

- `auth`: login, token refresh, JWT filtering, account lockout.
- `users`: user creation, updates, deactivation, role management.
- `agents`: support agent records, availability, and statistics.
- `ticket`: support ticket lifecycle and audit trails.
- `comment`: ticket comments, replies, and internal notes.
- `attachment`: upload, list, download, and delete ticket files.
- `auditlog`: immutable history for ticket-related activity.

## Project Structure

```text
GovHelp_Desk/
  helpdesk/
    src/main/java/za/gov/helpdesk/
      auth/
      users/
      agent/
      ticket/
      comment/
      attachment/
      auditlog/
      config/
      exception/
    src/main/resources/db/migration/
    src/test/java/za/gov/helpdesk/
    docker-compose.yml
    Dockerfile
    pom.xml
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

Create a `.env` file in the `helpdesk` directory when running with Docker Compose:

```env
PG_HOST=localhost
PG_PORT=5433
POSTGRES_DB=helpdesk_db
PG_USER=helpdesk
PG_PASSWORD=helpdesk

HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect

JWT_SECRET_KEY=5JzoMbk6E5qIqHSuBTgeQCARtUsxAkBiHwdjXOSW8kWdXzYmP3X51C0
JWT_VALIDITY=3600000
JWT_REFRESH_VALIDITY=604800000

MAIL_HOST=localhost
MAIL_PORT=2525
MAIL_USERNAME=test@example.com
MAIL_PASSWORD=test-password

RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest

TECHNICIAN_ASSIGNMENT_QUEUE=technician.assignment.queue
TICKET_STATUS_CHANGE_QUEUE=ticket.status-change.queue
TICKET_COMMENT_QUEUE=ticket.comment.queue
TICKET_CREATION_QUEUE=ticket.creation.queue

TECHNICIAN_ASSIGNMENT_EXCHANGE=technician.assignment.exchange
TICKET_STATUS_CHANGE_EXCHANGE=ticket.status-change.exchange
TICKET_COMMENT_EXCHANGE=ticket.comment.exchange
TICKET_CREATION_EXCHANGE=ticket.creation.exchange

TECHNICIAN_ASSIGNED_ROUTING_KEY=technician.assigned
TICKET_STATUS_CHANGED_ROUTING_KEY=ticket.status.changed
TICKET_COMMENT_ADDED_ROUTING_KEY=ticket.comment.added
TICKET_CREATED_ROUTING_KEY=ticket.created
```

Use stronger secrets and real service credentials outside local development.

## Run Locally

### With Docker Compose

```bash
docker compose up --build
```

The API runs on:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Health endpoint:

```text
http://localhost:8080/actuator/health
```

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

The integration tests use Testcontainers, so Docker Desktop must be running.

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

Current verified result:

```text
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## API Overview

All protected endpoints require:

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
| GET | `/v1/users/{id}` | Admin or owner | Get user by ID |
| PUT | `/v1/users/{id}` | Admin or owner | Update a user profile |
| DELETE | `/v1/users/{id}` | Admin | Deactivate a user |

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
| GET | `/v1/tickets` | Authenticated | List tickets with filters |
| GET | `/v1/tickets/{id}` | Authenticated | Get ticket by ID |
| PATCH | `/v1/tickets/{id}` | Agent/Admin | Update status, priority, escalation, or assignee |
| DELETE | `/v1/tickets/{id}` | Admin | Delete a ticket |
| GET | `/v1/tickets/{id}/audit` | Agent/Admin | View ticket audit history |

### Comments

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/v1/tickets/{ticketId}/comments` | Authenticated | Add a comment to a ticket |
| GET | `/v1/tickets/{ticketId}/comments` | Authenticated | List comments on a ticket |
| POST | `/v1/comments/{commentId}/replies` | Authenticated | Reply to a comment |
| GET | `/v1/comments/{commentId}/replies` | Authenticated | List replies |
| PUT | `/v1/comments/{commentId}` | Author/Admin rules | Edit a comment |
| DELETE | `/v1/comments/{commentId}` | Author/Admin rules | Delete a comment |

### Attachments

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/v1/tickets/{ticketId}/attachments` | Authenticated | Upload one or more files |
| GET | `/v1/tickets/{ticketId}/attachments` | Authenticated | List ticket attachments |
| GET | `/v1/attachments/{attachmentId}` | Authenticated | Download an attachment |
| DELETE | `/v1/attachments/{attachmentId}` | Authenticated | Delete an attachment |

## Security Features

- Stateless JWT authentication.
- BCrypt password hashing.
- Role-based method security using `@PreAuthorize`.
- Account lockout after repeated failed login attempts.
- Inactive users are blocked from authentication.
- JSON error responses for unauthorized and forbidden requests.
- Security headers including content security policy and frame protection.

## Database

Flyway migrations create and seed:

- users
- agents
- tickets
- comments
- attachments
- audit logs

Migrations live in:

```text
helpdesk/src/main/resources/db/migration
```

## Portfolio Notes

This project demonstrates:

- Designing a secure REST API around realistic support-desk workflows.
- Using PostgreSQL constraints and foreign keys to protect domain integrity.
- Applying Testcontainers for database-backed integration tests.
- Handling authentication, authorization, validation, and global exception responses.
- Structuring a Spring Boot codebase into maintainable domain modules.
- Packaging infrastructure with Docker for local development and deployment.

## Future Improvements

- Add CI with GitHub Actions.
- Add SLA tracking and ticket due dates.
- Add notification event consumers for RabbitMQ workflows.
- Add API examples or a Postman collection.
- Add a frontend dashboard for users, agents, and admins.

## License

This project is licensed under the MIT License.

## Author

Tebogo Mofokeng
