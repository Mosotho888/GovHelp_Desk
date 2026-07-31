# Architecture Decisions - Index

Significant, hard-to-reverse architectural decisions are recorded as individual Architecture Decision Records (ADRs)
under [`docs/adr/`](../adr), using the lightweight Michael Nygard format. This page is a chronological index with a
one-line summary of each.

| #                                                   | Title                                    | Status   | Summary                                                                                                                                                                               |
|-----------------------------------------------------|------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [0001](../adr/0001-transactional-outbox-pattern.md) | Transactional outbox for async messaging | Accepted | Write side-effect events to a DB table in the same transaction as the business change, relay to RabbitMQ separately, instead of publishing directly or using distributed transactions |
| [0002](../adr/0002-srp-domain-refactor.md)          | SRP-driven domain refactor               | Accepted | Extract ~14 responsibilities out of oversized services into dedicated single-purpose collaborators (policies, dispatchers, validators)                                                |
| [0003](../adr/0003-jwt-stateless-authentication.md) | Stateless JWT authentication             | Accepted | Access + refresh token pair, no server-side session state, method-level `@PreAuthorize` for RBAC                                                                                      |
| [0004](../adr/0004-oci-arm-free-tier-deployment.md) | Oracle Cloud Free Tier ARM VM deployment | Accepted | Single-VM Docker Compose deployment on OCI's Ampere ARM Free Tier, fronted by Cloudflare, chosen for zero hosting cost at portfolio stage                                             |
| [0005](../adr/0005-static-analysis-tooling.md)      | Multi-tool static analysis pipeline      | Accepted | Checkstyle + PMD 7.x + SpotBugs/FindSecBugs + Spotless, enforced in CI as a dedicated `static-analysis` job                                                                           |

## Why ADRs

This project treats architectural decisions as artifacts worth recording, not just code worth writing - mainly because
the reasoning behind a decision (what was rejected, and why) tends to get lost once the code just "is what it is." For a
portfolio project, ADRs also give a reviewer a fast way to see engineering judgment, not just engineering output.

New ADRs should follow [`docs/adr/0000-template.md`](../adr/0000-template.md).
