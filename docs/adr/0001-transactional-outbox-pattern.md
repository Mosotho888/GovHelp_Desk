# ADR 0001: Transactional outbox for async messaging

## Status

Accepted

## Context

Several operations in GovHelpDesk need to trigger side effects outside the primary database transaction: writing an
audit log entry, sending a ticket-lifecycle email, sending an SLA warning/breach email, or sending a password-reset OTP
email. These side effects are delivered asynchronously through RabbitMQ (4 queues: `audit.queue`, `ticket.email.queue`,
`password.reset.email.queue`, `sla.email.queue`, each with a matching DLQ).

The problem: if the API publishes directly to RabbitMQ from within the request thread, there is a window where the
database transaction commits but the broker publish fails (or vice versa) - leaving the system in an inconsistent state
(e.g., a ticket is created but no notification is ever sent, or an audit entry is silently lost). True distributed
transactions (XA) across PostgreSQL and RabbitMQ would solve this but add significant operational complexity and latency
for a system of this scale.

## Decision

Use the **transactional outbox pattern**. Every domain event that needs to reach RabbitMQ is first written as a row in
the `outbox_events` table (`event_type`, `aggregate_type`,
`aggregate_id`, `payload` as `JSONB`, `status`, `attempts`, `last_error`) inside the same database transaction as the
business change. A separate `OutboxRelay` component polls for
`PENDING` rows on a fixed schedule (`@Scheduled(fixedDelayString = "${app.outbox.poll-interval:PT5S}")`, i.e. every 5
seconds by default) and relays them to RabbitMQ via `OutboxProcessor`, updating each row's status to `PROCESSED` or
`FAILED` with an error message on failure.

Messages are deserialised into fully-typed DTOs on the consumer side via a `TYPE_MAP`-based mapping keyed by
`event_type`, rather than passing around a generic/untyped payload.

A separate scheduled job (`@Scheduled(cron = "${app.outbox.purge-cron:0 0 3 * * *}")`, default 3am daily) purges old
processed outbox rows to keep the table small.

## Alternatives considered

- **Publish directly to RabbitMQ inside the request thread** - simplest to implement, but loses at-least-once delivery
  guarantees on broker or network failure; rejected.
- **Distributed transactions (XA) across PostgreSQL and RabbitMQ** - technically solves the consistency problem, but
  adds a 2PC coordinator, hurts throughput, and is generally discouraged for this kind of workload; rejected as
  disproportionate for the scale involved.
- **CDC (Change Data Capture) via a tool like Debezium reading the WAL** - a more
  "industrial" version of the same pattern, but adds a whole extra piece of infrastructure (Kafka Connect or equivalent)
  that isn't justified at this project's scale; noted as a possible future direction if throughput ever requires it.

## Consequences

**Easier:**

- At-least-once delivery is guaranteed without distributed transactions - if the app crashes after committing the
  business transaction but before publishing, the relay will pick the row up on its next poll.
- Consumers get typed DTOs, not a generic envelope they have to introspect.
- Failed relay attempts are visible and retryable (`attempts`, `last_error` columns) instead of silently dropped.

**Harder:**

- Notifications are eventually consistent, not immediate - there's up to a 5-second delay (the poll interval) before an
  event reaches RabbitMQ, plus normal consumer processing time.
- The `outbox_events` table needs its own housekeeping (the purge job) to avoid unbounded growth.
- Consumers must be idempotent, since at-least-once delivery means a message could in principle be redelivered after a
  partial failure.
