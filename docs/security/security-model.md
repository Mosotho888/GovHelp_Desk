# Security Model

This documents the actual security mechanisms implemented in the codebase, as a reference for reviewers and future
contributors. For how to report a vulnerability, see
[`../../SECURITY.md`](../../SECURITY.md) at the repo root.

## Authentication

JWT-based, stateless (`SessionCreationPolicy.STATELESS`) — see
[ADR 0003](../adr/0003-jwt-stateless-authentication.md) for the full rationale.

- **Access tokens**: short-lived, HMAC-signed (`JwtService`), carry the user's `role` claim, lifetime set by
  `app.jwt.access-token-expiry-ms`.
- **Refresh tokens**: longer-lived, persisted in the `refresh_tokens` table so they can be individually revoked.
  `POST /v1/auth/logout` revokes all of the caller's refresh tokens.
- **Password hashing**: BCrypt with an explicit work factor of 12 (`new BCryptPasswordEncoder(12)`), rather than the
  library default.
- **Password reset**: OTP-based. The OTP itself is never stored, only `otp_hash` and each
  `password_reset_tokens` row tracks `attempts` and `expires_at` to bound brute-force guessing of a short numeric OTP.

## Authorization

Enforced at the method level with Spring Security's `@PreAuthorize` (`@EnableMethodSecurity`)
directly on controller methods - the access control surface is visible by reading the controllers, not buried in a
separate config file. Three patterns are used:

- **Role-only**: `hasRole('ADMIN')`, `hasAnyRole('AGENT', 'ADMIN')`.
- **Self-or-role**: `hasRole('ADMIN') or #id == authentication.principal.id` (e.g. a user can fetch or update their own
  profile without being an admin).
- **No annotation, service-layer scoping**: some endpoints (e.g. `GET /v1/tickets`) are open to any authenticated role
  but scope *results* in the service layer - a `USER` only ever sees tickets they created.

### Preventing IDOR (Insecure Direct Object Reference)

Beyond role checks, repository queries for tickets, comments, and attachments are **actor-aware**: a `USER` cannot fetch
another user's ticket by simply guessing or incrementing an ID, because the query itself is scoped to the requesting
actor rather than relying on a role check alone. `CommentAccessPolicy` additionally enforces that only a comment's
author (within a 15-minute edit window) or an `ADMIN` may mutate it.

## File upload safety

Two independent layers protect the attachment upload path:

1. **`AttachmentValidator`** (business validation): rejects empty uploads, batches over 5 files, individual files over
   20MB, and any MIME type outside an explicit allowlist (PNG, JPEG, GIF, PDF, DOC/DOCX, XLS/XLSX, TXT, CSV, ZIP).
2. **`FileStorageServiceImpl`** (storage-path safety, defends against path traversal):
    - Strips directory components from the original filename and removes any character outside
      `[a-zA-Z0-9._-]`, replacing it with `_`.
    - Prefixes the sanitised name with a random UUID to prevent filename collisions.
    - Resolves the final destination path and **normalises** it, then explicitly checks that the resolved path still
      starts with the configured upload root (`destination.startsWith(root)`) before writing — if a crafted filename
      somehow produced a path escaping the upload root, the write is rejected with a `SecurityException` instead of
      silently succeeding outside the intended directory.
    - The same containment check is applied on delete, so a malicious `storage_path` can't be used to delete arbitrary
      files on the host.

## Rate limiting

Token-bucket rate limiting (Bucket4j) via `RateLimitingFilter`, applied before authentication resolution but after the
request enters the filter chain:

- Buckets are keyed by authenticated identity (`user:<email>`) when available, falling back to the client IP
  (`X-Forwarded-For` if present, else `getRemoteAddr()`).
- Capacity is tiered by role via `RateLimitPolicyProvider`: distinct configurable capacities for unauthenticated,
  `USER`, `AGENT`, and `ADMIN` callers (`rate-limit.capacity.*`
  properties), refilled hourly.
- Every response carries an `X-Rate-Limit-Remaining` header; exceeding the bucket returns a
  `429` via `RateLimitExceededException`.
- Health, Prometheus, and Swagger/OpenAPI paths are explicitly exempted (`shouldNotFilter`), and the filter no-ops
  entirely under the `test` Spring profile to avoid flaky integration tests.

## Account lockout

`LoginLockoutService` tracks consecutive failed logins per user (`users.login_attempts`). After reaching
`app.security.max-login-attempts` (default 5), the account is deactivated (`active = false`) and an `ACCOUNT_LOCKED`
audit event is published. A successful login resets the counter to zero. Locked accounts must be reactivated by an
`ADMIN`
(`POST /v1/admin/users/{id}/reactivate`).

## Transport and headers

Configured in `SecurityConfig`:

- **HSTS**: `includeSubDomains(true)`, `max-age=31536000` (1 year).
- **Content-Security-Policy**: `default-src 'self'; frame-ancestors 'none'` - blocks the API responses from being framed
  and restricts default resource loading to same-origin.
- **X-Frame-Options**: `DENY`.
- **CSRF**: disabled. This is a deliberate choice, not an oversight - the API never authenticates via cookies, so the
  cross-site request forgery threat model CSRF protection addresses doesn't apply to bearer-token-in-header
  authentication.
- **In production**: Cloudflare terminates public TLS and re-encrypts the hop to the origin VM in Full (Strict) mode, so
  the origin also serves a valid certificate rather than HTTP or a self-signed cert.

## Error handling

A single `GlobalExceptionHandler` (`@ControllerAdvice`) maps domain and framework exceptions to a consistent
`ApiErrorResponse` shape, so error responses never leak stack traces, SQL fragments, or other internals to a client - a
common, easily-overlooked information disclosure vector.

## Dependency and code-level security scanning

- **FindSecBugs** (a SpotBugs plugin) runs in CI as part of the `static-analysis` job, specifically targeting
  security-relevant bug patterns (unsafe deserialization, path traversal, hardcoded credentials, etc.) - see
  [ADR 0005](../adr/0005-static-analysis-tooling.md).

## Known limitations / accepted trade-offs

- Access tokens cannot be revoked before expiry (only refresh tokens can) - bounded by keeping the access-token TTL
  short. See
  [ADR 0003](../adr/0003-jwt-stateless-authentication.md#consequences).
- The system runs as a single instance with no WAF or DDoS mitigation beyond what Cloudflare provides at the DNS/edge
  layer.
- There is no dependency-vulnerability scanning (e.g. OWASP Dependency-Check or GitHub Dependabot alerts) wired into CI
  yet - tracked in [`../../ROADMAP.md`](../../ROADMAP.md) as a planned improvement.
