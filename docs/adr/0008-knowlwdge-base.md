# ADR 0008: Knowledge base with Postgres full-text search and ticket-linked usage tracking

## Status

Accepted

## Context

The help desk had no way to publish self-service documentation - troubleshooting guides, FAQs, and standard operating
procedures all lived outside the system (if they existed at all), so every citizen with a common problem (forgotten
password, VPN access request, a laptop that won't turn on)
had to raise a ticket rather than find an answer themselves. This drives up ticket volume with repetitive,
low-complexity requests that a documented answer would resolve faster for the citizen and without consuming agent time.

The system needed: (1) a place to author and publish articles with a lightweight editorial lifecycle (draft, published,
archived); (2) genuine search, not just a filtered list, since a citizen typing "wifi not working" needs to find an
article titled "Requesting VPN Access" even though the words don't match exactly; (3) a way to measure whether the KB is
actually helping - both directly (reader feedback) and indirectly (which articles get used to resolve tickets, and which
categories of ticket never seem to have one).

## Decision

Introduce `KnowledgeArticle` as its own aggregate (`/v1/knowledge-base`), with an optional link to the `TicketCategory`
it helps with (reusing the category tree from ADR 0006 rather than inventing a separate taxonomy), a `type`
(troubleshooting guide / FAQ / SOP / general), and a `status` lifecycle (`DRAFT` -&gt; `PUBLISHED` -&gt; `ARCHIVED`,
with republishing from `ARCHIVED` back to `PUBLISHED`
allowed, but never back to `DRAFT` once published - see "Consequences" below).

**Search** is implemented with Postgres full-text search rather than `LIKE`/`ILIKE` matching or an external search
service: a generated `tsvector` column (`GENERATED ALWAYS AS ... STORED`) combines title, summary, and content with
descending weights (A/B/C), indexed with GIN, and queried via
`plainto_tsquery` ranked by `ts_rank`. The generated column means the index can never drift out of sync with the
article's actual text - Postgres recomputes it automatically on every write - and it avoids standing up a separate
search infrastructure (Elasticsearch, etc.) for a corpus that will realistically be dozens to a few hundred articles for
a single organisation's help desk.

**Tags** are a plain `@ElementCollection<String>` mapped to a join table rather than a dedicated
`Tag` entity - tags here are just filterable labels with no attributes of their own (no colour, no description, no
hierarchy), so a full entity would be pure ceremony.

**Feedback** (`KnowledgeArticleFeedback`) is one row per (article, user), upserted rather than appended, so a single
reader's vote can't be counted twice and can be changed without corrupting the totals - the same reasoning as
`KnowledgeArticleFeedback`'s unique constraint on `(article_id,
user_id)`.

**Ticket linking** (`TicketKnowledgeArticle`) mirrors `TicketAsset` from ADR 0007: an explicit join entity carrying
`linked_by`/`linked_at`, rather than a bare `@ManyToMany`. Every link increments the article's `usage_count`, a
monotonic total (not decremented on unlink) that answers "how often has this article actually helped resolve a ticket" -
the clearest signal of which content is worth maintaining, distinct from `view_count` (someone opened it) or the
helpful/not-helpful vote (someone read it and had an opinion, whether or not it solved their problem).

**Visibility** is enforced centrally in `ArticleQueryHelper`: citizens (role `USER`) can only ever see `PUBLISHED`
articles, and a request for a draft or archived article by id returns 404 rather than 403 - a citizen shouldn't be able
to detect that unpublished content exists at all. Viewing which articles resolved *their own* ticket is the one
exception: `GET
/v1/tickets/{ticketId}/knowledge-articles` is open to the ticket's own requester, since seeing "this FAQ is what fixed
your issue" is itself a self-service reinforcement, not an internal detail worth hiding.

## Alternatives considered

- **`LIKE`/`ILIKE` substring search** - rejected; it can't rank results by relevance and would miss an article about
  "VPN access" when a citizen searches "remote access issues", exactly the gap a real search needs to close for
  self-service to actually reduce ticket volume.
- **An external search service (Elasticsearch, Meilisearch, etc.)** - rejected as disproportionate infrastructure for
  the expected corpus size; Postgres full-text search covers the requirement without a new service to deploy, monitor,
  and keep in sync.
- **A dedicated `Tag` entity** with its own table and foreign keys - rejected in favour of a plain string collection,
  for the same reasons a `TicketCategory`-style tree wasn't used for tags: tags need no attributes or hierarchy of their
  own here.
- **Allowing `PUBLISHED` -&gt; `DRAFT`** - rejected; once an article has been visible to citizens, quietly turning it
  back into an internal draft (rather than explicitly archiving it) makes it too easy to accidentally hide content
  without a clear editorial signal of *why*. Archiving is the correct "take this down" action and is explicit in the
  audit trail (`KB_ARTICLE_ARCHIVED`).

## Consequences

**Easier:**

- A citizen's search query returns ranked, relevant articles instead of an exact-match-only list, which is what actually
  gives self-service a chance of deflecting a ticket before it's raised.
- `usage_count` and the helpful/not-helpful split together give a concrete, queryable answer to
  "which KB articles are worth investing more time in" and, by omission, "which ticket categories still have no
  documented answer" - both genuinely useful inputs for whoever maintains the KB.
- Reusing `TicketCategory` for article categorisation means the category and KB features stay in sync automatically - a
  category rename or reorganisation doesn't need a parallel update elsewhere.

**Harder:**

- The `DRAFT` -&gt; `PUBLISHED` -&gt; `ARCHIVED` lifecycle is intentionally rigid (no path back to
  `DRAFT`). If a published article turns out to need substantial rework before it's fit to be public again, the workflow
  is: edit its content via `PATCH` while it stays `PUBLISHED` (or archive it first), rather than a dedicated "unpublish
  to draft" action. This was accepted as the simpler, more auditable default; a more nuanced workflow (e.g. a
  `PENDING_REVIEW` state) can be added later if editorial process outgrows this.
- Full-text search quality is bounded by Postgres's built-in English text search configuration - it won't handle typos,
  synonyms, or non-English content out of the box. This is an acceptable trade-off for the infrastructure savings today,
  but worth revisiting (e.g. `pg_trgm` for fuzzy matching) if search relevance turns out to be a real pain point once
  real usage data exists.
