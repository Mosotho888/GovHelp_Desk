# Security Policy

GovHelpDesk handles support tickets that may contain personal information about members of the public, so security
issues are taken seriously even though this is a portfolio project.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Instead, report privately via one of:

- GitHub's private vulnerability reporting (Security tab -> "Report a vulnerability"), if enabled on this repository.
- Email the maintainer directly through the contact details on the
  [GitHub profile](https://github.com/Mosotho888).

Please include:

- A description of the vulnerability and its potential impact.
- Steps to reproduce (a minimal request/payload is ideal).
- Any suggested remediation, if you have one.

You should expect an initial response within a few days. This is a single-maintainer project, so turnaround time on
fixes will vary, but all reports are read and taken seriously.

## Supported versions

Only the `main` branch (which tracks the current production deployment) receives security fixes. There is no long-term
support branch at this stage of the project.

## What's already been hardened

For transparency, here's a summary of security work already done in this codebase - see
[`docs/security/security-model.md`](docs/security/security-model.md) for full detail:

- **Authentication**: stateless JWT (access + refresh tokens), BCrypt password hashing (work factor 12), account lockout
  after repeated failed logins.
- **Authorization**: Spring Security method-level `@PreAuthorize` on every non-public endpoint, actor-aware repository
  queries to prevent IDOR (a user cannot fetch or mutate another user's tickets/comments/attachments by guessing an ID).
- **File uploads**: MIME-type allowlist, per-file and per-batch size limits, filename sanitisation, and destination-path
  normalisation/containment checks to prevent path traversal.
- **Transport/headers**: HSTS with `includeSubDomains`, a restrictive Content-Security-Policy
  (`default-src 'self'; frame-ancestors 'none'`), `X-Frame-Options: DENY`, CSRF disabled in favour of stateless
  bearer-token auth (no cookies are used for authentication).
- **Rate limiting**: token-bucket rate limiting (Bucket4j) with distinct capacities for unauthenticated, `USER`,
  `AGENT`, and `ADMIN` callers, keyed by authenticated identity or
  `X-Forwarded-For` IP.
- **Transport encryption in production**: Cloudflare Full (Strict) SSL in front of the OCI VM.

## Out of scope

- Denial-of-service testing against the live production deployment.
- Automated scanning that generates significant load - please use a local instance instead.
- Social engineering, physical security, or third-party services (Cloudflare, GitHub, Oracle Cloud) not directly
  controlled by this codebase.
