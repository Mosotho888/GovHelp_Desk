# API Reference

All endpoints are prefixed with `/v1`. This document is generated from the actual controller source
(`za.gov.helpdesk.*.controller`) to stay accurate. For interactive exploration, request/response schemas, and to try
requests directly, use the built-in Swagger UI:

- Local: `http://localhost:8080/swagger-ui.html`
- Production: `https://api.sothoman.com/swagger-ui.html`

## Authentication

Every endpoint except `/v1/auth/**`, `/v1/health`, `/actuator/health`, `/actuator/prometheus`, and the OpenAPI/Swagger
paths requires a JWT bearer token:

```
Authorization: Bearer <access_token>
```

Get a token from `POST /v1/auth/login`, and refresh it from `POST /v1/auth/refresh` before it expires — see [
`docs/security/README.md`](../security/README.md) for token lifetimes and the full authentication model.

## Roles

| Role    | Description                                        |
|---------|----------------------------------------------------|
| `USER`  | A citizen who submits and tracks their own tickets |
| `AGENT` | A support agent working assigned/queued tickets    |
| `ADMIN` | Full administrative access                         |

Authorization on each endpoint below is enforced with Spring Security's `@PreAuthorize`
method security - where a "Role" column says `USER+`, it means any authenticated role (the endpoint has no explicit
`@PreAuthorize`, so any authenticated principal may call it, though the service layer may still scope results to the
caller, e.g. a `USER` only sees their own tickets).

---

## Auth - `/v1/auth`

| Method | Path                      | Role                                  | Description                                                       |
|--------|---------------------------|---------------------------------------|-------------------------------------------------------------------|
| `POST` | `/login`                  | Public                                | Authenticate with email/password, receive access + refresh tokens |
| `POST` | `/refresh`                | Public (valid refresh token required) | Exchange a refresh token for a new access token                   |
| `POST` | `/logout`                 | Authenticated                         | Revoke all of the caller's refresh tokens                         |
| `POST` | `/password-reset/request` | Public                                | Request an OTP be emailed to the given address                    |
| `POST` | `/password-reset/confirm` | Public                                | Confirm the OTP and set a new password                            |

Repeated failed `/login` attempts count toward account lockout - see
[`docs/security/README.md`](../security/README.md#account-lockout).

## Users - `/v1/users`

| Method  | Path           | Role                                                   | Description                      |
|---------|----------------|--------------------------------------------------------|----------------------------------|
| `POST`  | `/`            | `ADMIN`                                                | Create a user                    |
| `GET`   | `/{id}`        | `ADMIN` or self (`#id == authentication.principal.id`) | Get a user by ID                 |
| `GET`   | `/me`          | Authenticated                                          | Get the caller's own profile     |
| `GET`   | `/`            | `ADMIN`                                                | List all users (paginated)       |
| `PUT`   | `/{id}`        | `ADMIN` or self                                        | Update a user's profile          |
| `PATCH` | `/me/password` | Authenticated                                          | Change the caller's own password |

## Admin - `/v1/admin/users`

| Method   | Path               | Role    | Description                                       |
|----------|--------------------|---------|---------------------------------------------------|
| `DELETE` | `/{id}`            | `ADMIN` | Deactivate/delete a user                          |
| `POST`   | `/{id}/reactivate` | `ADMIN` | Reactivate a deactivated/locked user              |
| `PATCH`  | `/{id}/role`       | `ADMIN` | Change a user's role (`USER` / `AGENT` / `ADMIN`) |
| `PATCH`  | `/{id}/password`   | `ADMIN` | Administratively reset a user's password          |

## Agents - `/v1/agents`

| Method  | Path          | Role             | Description                                     |
|---------|---------------|------------------|-------------------------------------------------|
| `POST`  | `/`           | `ADMIN`          | Create an agent (promotes/extends a user)       |
| `GET`   | `/`           | `AGENT`, `ADMIN` | List all agents (paginated)                     |
| `GET`   | `/{id}`       | `AGENT`, `ADMIN` | Get an agent by ID                              |
| `PATCH` | `/{id}`       | `AGENT`, `ADMIN` | Update an agent (e.g. department, availability) |
| `GET`   | `/{id}/stats` | `ADMIN`          | Get an agent's ticket-handling statistics       |

## Tickets - `/v1/tickets`

| Method   | Path    | Role                        | Description                                          |
|----------|---------|-----------------------------|------------------------------------------------------|
| `POST`   | `/`     | Authenticated               | Create a new ticket                                  |
| `GET`    | `/`     | Authenticated               | List tickets (paginated; `USER`s see only their own) |
| `GET`    | `/{id}` | Authenticated (actor-aware) | Get a ticket by ID                                   |
| `PATCH`  | `/{id}` | `AGENT`, `ADMIN`            | Update status, assignee, and/or priority             |
| `DELETE` | `/{id}` | `ADMIN`                     | Delete a ticket                                      |

Ticket status flow: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`, with an `ESCALATED` branch reachable from `OPEN` or
`IN_PROGRESS`. Transitions outside this graph are rejected by
`TicketStatusTransitionPolicy`.

## Comments - `/v1`

| Method   | Path                            | Role                              | Description                                                        |
|----------|---------------------------------|-----------------------------------|--------------------------------------------------------------------|
| `POST`   | `/tickets/{ticketId}/comments`  | Authenticated                     | Add a top-level comment or internal note                           |
| `GET`    | `/tickets/{ticketId}/comments`  | Authenticated                     | List comments (paginated; internal notes filtered out for `USER`s) |
| `POST`   | `/comments/{commentId}/replies` | Authenticated                     | Reply to an existing comment                                       |
| `GET`    | `/comments/{commentId}/replies` | Authenticated                     | List replies to a comment                                          |
| `PUT`    | `/comments/{commentId}`         | Author (within 15 min) or `ADMIN` | Edit a comment                                                     |
| `DELETE` | `/comments/{commentId}`         | Author (within 15 min) or `ADMIN` | Delete a comment                                                   |

Edit/delete is enforced by `CommentAccessPolicy`: the author may mutate their own comment only within a 15-minute window
of creation; an `ADMIN` may always mutate.

## Attachments - `/v1`

| Method   | Path                                 | Role                        | Description                                 |
|----------|--------------------------------------|-----------------------------|---------------------------------------------|
| `POST`   | `/tickets/{ticketId}/attachments`    | Authenticated               | Upload up to 5 files (multipart), 20MB each |
| `GET`    | `/v1/tickets/{ticketId}/attachments` | Authenticated               | List attachments for a ticket               |
| `GET`    | `/v1/attachments/{attachmentId}`     | Authenticated (actor-aware) | Download an attachment                      |
| `DELETE` | `/v1/attachments/{attachmentId}`     | Owner or `ADMIN`            | Delete an attachment                        |

Allowed content types: PNG, JPEG, GIF, PDF, DOC/DOCX, XLS/XLSX, TXT, CSV, ZIP. See
[`docs/security/README.md`](../security/README.md#file-upload-safety) for the full validation and storage-safety model.

## SLA - `/v1/tickets/{ticketId}/sla`

| Method | Path | Role             | Description                                           |
|--------|------|------------------|-------------------------------------------------------|
| `GET`  | `/`  | `AGENT`, `ADMIN` | Get SLA status (due dates, breach flags) for a ticket |

SLA policies are seeded per priority (response/resolution minutes, warning threshold) and evaluated every 5 minutes by
`SlaBreachMonitor` - see
[`docs/database/README.md`](../database/README.md#sla_policies--ticket_sla).

## Audit - `/v1/audit`

| Method | Path               | Role             | Description                                                      |
|--------|--------------------|------------------|------------------------------------------------------------------|
| `GET`  | `/tickets/{id}`    | `AGENT`, `ADMIN` | Audit trail for a ticket                                         |
| `GET`  | `/users/{id}`      | `ADMIN`          | Audit trail for a user                                           |
| `GET`  | `/agents/{id}`     | `ADMIN`          | Audit trail for an agent                                         |
| `GET`  | `/auth`            | `ADMIN`          | Paginated authentication-related audit events (logins, lockouts) |
| `GET`  | `/actor/{actorId}` | `ADMIN`          | Paginated audit events performed by a given actor                |
| `GET`  | `/action/{action}` | `ADMIN`          | Paginated audit events of a given action type                    |

## Health and observability (unauthenticated)

| Method | Path                   | Description                 |
|--------|------------------------|-----------------------------|
| `GET`  | `/v1/health`           | Basic liveness check        |
| `GET`  | `/actuator/health`     | Spring Boot Actuator health |
| `GET`  | `/actuator/prometheus` | Prometheus scrape endpoint  |

## Errors

All errors are returned as a consistent `ApiErrorResponse` via a global
`@ControllerAdvice` (`GlobalExceptionHandler`). Rate-limited requests receive an
`X-Rate-Limit-Remaining` header on every response and a `429` with a descriptive message once the bucket is exhausted -
see [`docs/security/README.md`](../security/README.md#rate-limiting).
