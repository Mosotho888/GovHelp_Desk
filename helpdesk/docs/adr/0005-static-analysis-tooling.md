# ADR 0005: Multi-tool static analysis pipeline

## Status

Accepted

## Context

As the codebase grew past the initial ticketing core, style drift and a handful of real bug classes (resource leaks,
missed null checks, security-sensitive patterns) started to appear in review. A single linter doesn't cover every
concern: style consistency, code smells/design issues, and security-relevant bug patterns are genuinely different
problems that different tools specialise in.

## Decision

Adopt four complementary static analysis tools, each configured with a production-grade ruleset under
`helpdesk/config/`:

- **Checkstyle** (`config/checkstyle/checkstyle.xml` +
  `checkstyle-suppressions.xml`) - enforces style and structural conventions (naming, import order, Javadoc presence on
  public APIs, method length).
- **PMD 7.x** (`config/pmd/pmd-ruleset.xml`) - catches design smells and code quality issues (unused code, overly
  complex methods, suboptimal collection usage).
- **SpotBugs + FindSecBugs** (`config/spotbugs/spotbugs-include.xml` +
  `spotbugs-exclude.xml`) - bytecode-level bug pattern detection, with FindSecBugs adding security-specific patterns
  (e.g. path traversal, hardcoded credentials, unsafe deserialisation).
- **Spotless** - auto-formatting, run as `spotless:apply` locally and checked (not auto-fixed) in CI, so formatting is
  never a source of review nitpicking.

All four run in a dedicated `static-analysis` CI job that executes after `build-and-test`
succeeds, and a failing check blocks merge.

Two conventions were adopted alongside the tooling:

- **Prefer global ruleset exclusions over per-file `@SuppressWarnings`.** Scattering suppression annotations through the
  codebase hides *why* a rule doesn't apply and makes the ruleset's intent harder to audit later; an explicit, commented
  exclusion in the shared ruleset file keeps that reasoning in one place.
- **`SuppressionSingleFilter` must be a direct child of `Checker`, not nested inside
  `TreeWalker`**, per Checkstyle's XML module schema - this was a real configuration bug during setup, documented here
  so it doesn't get silently reintroduced.

## Alternatives considered

- **Single tool (e.g. just SpotBugs, or just a formatter)** - faster to configure and faster in CI, but would miss
  either the style-consistency layer (Checkstyle/Spotless) or the design-smell layer (PMD) that the other tools don't
  cover. Rejected as insufficient coverage.
- **SonarQube / SonarCloud as a unified platform** - would consolidate reporting into one dashboard, but adds either a
  hosted dependency (SonarCloud) or infrastructure to run (self-hosted SonarQube) that isn't justified for a
  single-maintainer project. The four-tool CI approach gets equivalent coverage without the extra service.

## Consequences

**Easier:**

- Style, design, and security-pattern issues are caught before review, not during it.
- A new contributor gets fast, specific feedback from CI rather than a manual style critique.

**Harder:**

- Four tools means four configuration surfaces to maintain, and occasional friction when a major version bump (this
  happened with PMD 7) introduces breaking rule-category changes that require the ruleset to be updated before CI goes
  green again.
- Initial setup cost was non-trivial - most of the early hardening work (see
  [`CHANGELOG.md`](../../CHANGELOG.md)) was spent getting all four tools to agree on a consistent, low-noise ruleset
  rather than either over-suppressing or drowning contributors in low-value warnings.
