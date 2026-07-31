# Diagrams

All architecture diagrams in this repository are written as [Mermaid](https://mermaid.js.org/)
code blocks directly inside the relevant markdown file, rather than as separate binary image
files. This keeps diagrams:

- **Diffable** — a diagram change shows up as a readable text diff in a pull request.
- **Renderable everywhere that matters** — GitHub, GitLab, and most modern markdown viewers
  render Mermaid natively, and VS Code renders it with the Markdown Preview Mermaid Support
  extension.
- **Co-located with the explanation** — a diagram next to the prose that explains it, instead
  of a cross-reference to a separate image file that can silently go stale.

## Where the diagrams live

| Diagram | Location |
|---|---|
| C4 Level 1 — System Context | [`docs/architecture/c4-context.md`](../architecture/c4-context.md) |
| C4 Level 2 — Containers | [`docs/architecture/c4-container.md`](../architecture/c4-container.md) |
| C4 Level 3 — Components (API) | [`docs/architecture/c4-component.md`](../architecture/c4-component.md) |
| Production deployment topology | [`docs/architecture/deployment.md`](../architecture/deployment.md) |
| Entity-relationship diagram | [`docs/database/README.md`](../database/README.md) |

## Regenerating a static image (optional)

If a static PNG/SVG export is ever needed (e.g. for a slide deck or a README badge), export it
with the [Mermaid CLI](https://github.com/mermaid-js/mermaid-cli) and drop the result in
[`docs/images/`](../images/):

```bash
npx -p @mermaid-js/mermaid-cli mmdc -i c4-container.mmd -o ../images/c4-container.png
```

Treat any exported image as a point-in-time snapshot, not the source of truth — the Mermaid
block in the corresponding markdown file is always authoritative.
