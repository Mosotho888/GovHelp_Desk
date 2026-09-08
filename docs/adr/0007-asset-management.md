# ADR 0007: Asset management as an explicit join entity, not a ticket field

## Status

Accepted

## Context

Technicians working a ticket about a specific device had no structured way to see that device's history, warranty
status, or who it's currently assigned to - that information either didn't exist in the system at all, or lived in a
separate spreadsheet the help desk maintained by hand. The system needed a way to (1) register and track IT assets
(laptops, desktops, printers, monitors, networking equipment, and software licenses) with ownership and warranty data,
and (2) associate tickets with the specific asset (s) they concern, so a technician opening a ticket can immediately see
the device's purchase date, warranty status, and every other ticket ever raised against it.

The relationship between tickets and assets isn't strictly one-to-one: a single ticket can concern more than one device
(a network outage affecting several workstations), and a single asset naturally accumulates many tickets over its
lifetime. A plain foreign key on `tickets` (`asset_id`)
would only capture the common case and lose the less common one.

## Decision

Introduce `Asset` as its own aggregate (`/v1/assets`), independent of tickets, and `TicketAsset` as an explicit join
entity (`ticket_assets`) rather than a raw `@ManyToMany` mapping. `TicketAsset`
carries `linked_by_id`/`linked_by_name` and `linked_at`, giving every link its own accountability trail - consistent
with the rest of the codebase's preference for explicit join records over managed collections (see how `Comment` and
`Attachment` each carry their own audit-relevant metadata rather than being bare associations).

An asset's `asset_tag` is auto-generated as `AST-{zero-padded id}` when not supplied, but can be set explicitly to match
an organisation's existing physical barcode/tag scheme - this system is often introduced into an environment that
already has assets tagged, and forcing a specific naming scheme would create migration friction for no benefit.

Ownership updates learned a lesson directly from category management (ADR 0006): `UpdateAssetRequest`
carries an explicit `clearAssignedUser` boolean rather than relying on a `null` `assignedUserId` to mean "unassign",
since `null` and "field omitted" are indistinguishable over JSON and that ambiguity was flagged as a real limitation in
the ticket category feature's frontend.

Warranty status (`ACTIVE` / `EXPIRING_SOON` / `EXPIRED` / `NO_WARRANTY_INFO`) is computed at read time from
`warranty_expiry_date` rather than stored, so it's always correct without a scheduled job to keep it in sync - the same
trade-off already made for SLA breach flags versus a naive "check on read" approach was considered and rejected for
warranties specifically because staleness has no consequence here (nothing needs to *react* to a warranty expiring the
way SLA breaches trigger notifications), so the simpler computed-field approach was preferred.

## Alternatives considered

- **A single `asset_id` foreign key on `tickets`** - rejected; it can't represent a ticket concerning multiple assets,
  and would need a schema migration to fix later if that case ever came up, which is more disruptive than modelling the
  join correctly from the start.
- **A raw `@ManyToMany` between `Ticket` and `Asset`** - rejected in favour of the explicit
  `TicketAsset` join entity, so the link itself can carry who created it and when, matching the codebase's established
  pattern for other relationships.
- **Polymorphic asset subtypes** (a `SoftwareLicense` subclass with license-key/seat-count fields, a `Hardware` subclass
  with different fields) - rejected as disproportionate for the current scope; a single `Asset` table with a `type`
  discriminator and a modest, mostly-nullable field set covers the six asset categories in the initial requirement
  without the mapping complexity of table-per- subclass or single-table inheritance.
- **A scheduled job to flag expiring warranties** (mirroring `SlaBreachMonitor`'s sweep) - deferred; nothing currently
  needs to react to a warranty expiring, so a stored, periodically-recomputed flag would be complexity without a
  consumer. Worth revisiting if/when warranty-expiry notifications become a requirement.

## Consequences

**Easier:**

- A technician opening a ticket can call `GET /v1/tickets/{id}/assets` and immediately see every linked asset's warranty
  status, current owner, and specs, without cross-referencing a spreadsheet.
- `GET /v1/assets/{id}/tickets` gives a complete device history for free, since it's just a read over the same
  `ticket_assets` join table used to create the links.
- Asset reassignment audits cleanly (`ASSET_ASSIGNED`) without the null-vs-omitted ambiguity that affected the category
  feature.

**Harder:**

- Software licenses are modelled with the same field set as physical hardware (no `license_key` or
  `seat_count` fields), so tracking per-seat license utilisation isn't currently possible - only that a license exists
  as an asset and who it's nominally assigned to. Extending `Asset` with a handful of software-specific nullable fields
  is the natural next step if that need arises, rather than introducing subtype tables now for a requirement that
  doesn't yet exist.
- Deleting a ticket cascades to `ticket_assets` (`ON DELETE CASCADE` on `ticket_id`), so an asset's device history is
  incomplete for any ticket that was later hard-deleted - acceptable today since ticket deletion is an `ADMIN`-only,
  rare operation, but worth revisiting if audit completeness through deletion becomes a compliance requirement.
