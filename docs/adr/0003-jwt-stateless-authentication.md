# ADR 0003: Stateless JWT authentication

## Status

Accepted

## Context

GovHelpDesk needs to authenticate three distinct roles (`USER`, `AGENT`, `ADMIN`) across a REST API with no
server-rendered frontend. The API is deployed as a single instance today but should not be architecturally locked into
sticky sessions if it's ever scaled horizontally. Session state also adds operational overhead (a session store,
invalidation on deploy, etc.)
that isn't justified for the current scale of the project.

## Decision

Use stateless JWT authentication:

- On login, the API issues a short-lived **access token** and a longer-lived **refresh token**
  (`JwtService.generateAccessToken` / `generateRefreshToken`), both HMAC-signed, with expiry driven by
  `app.jwt.access-token-expiry-ms` / `app.jwt.refresh-token-expiry-ms`.
- The access token embeds the user's `role` claim; authorization decisions are made from that claim plus Spring
  Security's method-level `@PreAuthorize` annotations on each controller method (e.g. `hasRole('ADMIN')`,
  `hasAnyRole('AGENT', 'ADMIN')`, or ownership expressions like `#id == authentication.principal.id`).
- `SecurityConfig` sets `SessionCreationPolicy.STATELESS` - no server-side session is ever created.
- Refresh tokens are persisted (`refresh_tokens` table) so they can be explicitly revoked (logout revokes all of a
  user's refresh tokens) - this is the one piece of state the design keeps server-side, deliberately, because
  refresh-token revocation is not achievable with a pure stateless JWT design.
- CSRF protection is disabled (`csrf(AbstractHttpConfigurer::disable)`) because the API never relies on cookies for
  authentication - the attack CSRF protection defends against doesn't apply to bearer-token-in-header auth.
- Failed authentication attempts are tracked per user (`LoginLockoutService`) and the account is deactivated after
  `app.security.max-login-attempts` (default 5) consecutive failures, independent of the JWT mechanism itself.

## Alternatives considered

- **Server-side sessions (Spring Session + Redis or DB-backed sessions)** - would make revocation trivial and avoid the
  refresh-token-table workaround, but introduces a session store dependency and sticky-session concerns for a system
  whose whole design goal is a stateless, horizontally-scalable API. Rejected for this project's scale and goals.
- **Fully stateless refresh tokens (no server-side revocation list)** - simpler, but means a compromised refresh token
  can't be invalidated before it naturally expires. Rejected given the sensitivity of government ticket data.
- **OAuth2 / OpenID Connect via an external identity provider** - appropriate for a multi-application government
  identity ecosystem, but disproportionate for a single-service portfolio project with only three internal roles. Noted
  as a natural evolution if GovHelpDesk were integrated into a larger department-wide system.

## Consequences

**Easier:**

- The API can be scaled horizontally without sticky sessions or a shared session store.
- Authorization logic stays declarative and close to the endpoint (`@PreAuthorize`), which makes the access control
  surface easy to audit by reading controllers.

**Harder:**

- Access tokens can't be revoked before they expire - this bounds the blast radius of a leaked access token to the
  access-token TTL, which is why the TTL is kept short and refresh tokens (the longer-lived credential) are the ones
  tracked for revocation.
- Refresh token storage reintroduces a small amount of server-side state, which is an intentional, scoped exception to
  full statelessness rather than a stateless design in the purest sense.
