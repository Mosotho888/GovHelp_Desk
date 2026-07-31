# Images

This folder holds static image exports (screenshots, exported diagrams) referenced from the docs - for example, Grafana
dashboard screenshots or Swagger UI captures for the README.

It's intentionally empty in this scaffold. Source-of-truth diagrams live as Mermaid code in the relevant `docs/`
markdown files - see [`docs/diagrams/diagrams.md`](../diagrams/diagrams.md)
for why, and for how to export a static copy here if one is ever needed.

Suggested naming convention: `<area>-<description>.png`, e.g. `grafana-ticket-dashboard.png`,
`swagger-ui-auth-endpoints.png`.
