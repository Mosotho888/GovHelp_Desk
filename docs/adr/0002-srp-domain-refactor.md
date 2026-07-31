# ADR 0002: SRP-driven domain refactor

## Status

Accepted

## Context

An audit of the early service layer (`TicketServiceImpl`, `CommentServiceImpl`,
`AttachmentServiceImpl`, `AuthServiceImpl`, and others) found services that had accumulated multiple unrelated
responsibilities: request validation, authorization checks, query composition, event dispatch, and persistence
orchestration all living in one class. This made the services hard to unit test in isolation (mocking one collaborator
meant reasoning about all the others) and hard to reason about when making a change, since a single method might mix
business rules with plumbing concerns.

## Decision

Perform a systematic Single Responsibility Principle (SRP) audit and extract roughly 14 distinct responsibilities into
dedicated, narrowly-scoped classes, including:

- `TicketEventDispatcher` - decouples ticket state changes from their audit/notification side effects
- `CommentAccessPolicy` - the author-or-admin, time-windowed edit/delete rule for comments
- `AttachmentValidator` - file count, size, and MIME-type validation
- `LoginLockoutService` - failed-login tracking and account lockout
- `OutboxRelay` - polling and relaying outbox events (see
  [ADR 0001](0001-transactional-outbox-pattern.md))
- `SlaBreachMonitor` - scheduled SLA breach/warning detection
- `TicketStatusTransitionPolicy` - validating legal ticket status transitions
- `TicketUpdateCoordinator` - orchestrating multi-field ticket updates as one operation
- Query helper classes (`TicketQueryHelper`, `CommentQueryHelper`, `AttachmentQueryHelper`,
  `AgentQueryHelper`, `SlaQueryHelper`, `UserQueryHelper`) - isolating read-side query composition from write-side
  service logic

Each domain's `service/` package now typically contains a thin orchestrating service interface plus an `impl/`
implementation that delegates validation, authorization, and side-effect dispatch to these dedicated collaborators
rather than inlining them.

## Alternatives considered

- **Leave services as-is and rely on discipline for new code** - rejected; the existing services were already hard to
  test, and the problem would only compound as the domain grew.
- **Introduce a full CQRS split (separate command/query models per aggregate)** - considered for the read-heavy domains
  (tickets, comments), but judged disproportionate; the query helper classes achieve most of the isolation benefit
  without the added architectural overhead of separate read models.

## Consequences

**Easier:**

- Each extracted collaborator can be unit tested with a narrow, obvious set of mocks.
- Business rules (e.g., "comments can only be edited within 15 minutes by their author") live in one discoverable place
  instead of being buried inside a larger method.
- Adding a new domain can follow the same now-established package shape (`controller/`, `dto/`, `mapper/`, `model/`,
  `repository/`, `service/{,impl/}`, `policy/`,
  `metrics/`, `event/`).

**Harder:**

- More classes and more indirection to navigate for a newcomer to the codebase - mitigated by consistent naming and
  package conventions (see [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md)).
- The refactor temporarily broke the existing test suite, since tests written against the old monolithic services no
  longer matched the new collaborator boundaries. This required a full test suite rewrite (58 broken tests → 94 passing
  tests across 11 files), tracked in
  [`../../CHANGELOG.md`](../../CHANGELOG.md).
