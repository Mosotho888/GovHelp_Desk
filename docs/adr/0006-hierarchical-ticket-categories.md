# ADR 0006: Hierarchical ticket categories with in-memory tree resolution and department-based routing

## Status

Accepted

## Context

Tickets were previously classified with a free-text `category` column (`Ticket.category: String`). This made reporting
inconsistent (`"Hardware"` vs `"hardware"` vs `"HW"` all meant the same thing to a citizen but different things to a
`GROUP BY` query), gave no structure for filtering a broad area like "Hardware" alongside its specifics ("Laptop",
"Printer"), and provided no hook for automated routing - every ticket had to be assigned manually regardless of what it
was about.

The system needed: (1) a bounded hierarchy - Category → Subcategory → Type - rather than either a flat list or unlimited
nesting; (2) filtering that includes descendants (filtering by "Hardware" should also surface "Laptop"
tickets) without requiring the caller to know the tree shape; (3) a way to route a newly created ticket to the right
team automatically, without building a full rules/skills engine disproportionate to the system's scale.

## Decision

Introduce `TicketCategory` as a self-referencing entity (`parent_id`), capped at three levels via a database `CHECK`
constraint (`level BETWEEN 0 AND 2`) and mirrored in application validation. `Ticket.category` becomes a `@ManyToOne`
to `TicketCategory` (nullable) rather than free text.

Rather than recursive SQL (`WITH RECURSIVE`) for tree operations, `TicketCategoryQueryHelper` loads the full category
table and walks it in memory to build breadcrumbs and resolve a category's descendant ids for filtering. This is a
deliberate simplicity trade-off: a municipal help desk's category tree is tens of nodes, not thousands, so an O (n)
in-memory walk is both simpler to read and just as fast as a recursive CTE at this scale, and it avoids one more
database-specific SQL dialect to maintain.

### Category-based routing

Each category carries an optional `default_department`, inherited from its parent at creation time if left blank so
subcategories don't have to repeat it. `CategoryRoutingService` runs after ticket creation, when no explicit
`assigneeId` was given: it looks up `ONLINE` agents in the category's department and assigns the one currently carrying
the fewest open-or-in-progress tickets. If nobody is online, or the category has no department configured, the ticket is
left unassigned in the shared queue rather than erroring - auto-routing is a best-effort convenience, not a hard
requirement for ticket creation to succeed.

## Alternatives considered

- **Unlimited category nesting** - rejected; a employee-facing helpdesk doesn't need more than three levels of
  specificity, and unbounded depth would complicate both the UI (breadcrumbs, tree pickers) and the routing lookup for
  negligible real-world benefit.
- **Recursive SQL (`WITH RECURSIVE`) for descendant resolution** - considered and prototyped, but the in-memory approach
  was simpler to unit test, easier for a reviewer to follow, and the category table's small size means the performance
  difference is immaterial. Worth revisiting only if the category tree ever grows into the hundreds of nodes.
- **A full rules/skills-based routing engine** (matching agent skills to ticket keywords, multiple weighted criteria) -
  rejected as disproportionate; the department + least-loaded-online-agent heuristic solves the actual problem (don't
  let tickets sit unassigned when someone's available) without the configuration surface a rules engine would need.
- **Hard deletion of categories** - rejected in favour of soft deactivation (`active = false`), since historical tickets
  need to keep a meaningful category label even after that category is retired from use for new tickets.

## Consequences

**Easier:**

- Category-based reporting and Grafana dashboards can now `GROUP BY category_id` reliably, with the hierarchy available
  for roll-ups (e.g. all "Hardware" tickets regardless of subcategory).
- Filtering by a parent category transparently includes its subcategories (`includeDescendants=true`), without the
  frontend needing to know or fetch the tree shape itself.
- Tickets in categories with a configured department are assigned immediately on creation when an agent is available,
  reducing time-to-first-response without any per-ticket manual triage step.

**Harder:**

- The free-text `category` column and its historical values could not be preserved exactly;
  `V8__create_ticket_categories.sql`
  does a best-effort backfill onto the closest matching seeded category (see
  [`../database/database.md`](../database/database.md#migration-history)), which is an acceptable loss of fidelity for
  demo/seed data but would need a more careful mapping exercise against real historical data in a production cutover.
- Recategorising a ticket does not re-run auto-routing, by design (moving a ticket to a new category shouldn't rip it
  away from whoever is already working it) - this means a ticket's assignee can end up in a different department from
  its current category after a recategorisation, which is expected and left to the agent/admin to reassign manually if
  needed.
