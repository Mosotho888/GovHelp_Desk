# Black-Box API Test Suite

## What this is

A standalone, framework-agnostic suite of HTTP-level tests under
`helpdesk/src/test/java/za/gov/helpdesk/api/`, written with
[REST Assured](https://rest-assured.io/). It is deliberately kept separate from the
`unit/` and `integration/` packages elsewhere in this project:

| Package        | What it tests                             | How                                                                   |
|----------------|-------------------------------------------|-----------------------------------------------------------------------|
| `unit/`        | Individual classes in isolation           | JUnit + Mockito, no Spring context                                    |
| `integration/` | The app wired together in-process         | `@SpringBootTest` + MockMvc, Testcontainers for Postgres/RabbitMQ     |
| **`api/`**     | **The deployed HTTP API, as a black box** | **REST Assured, real HTTP calls, zero knowledge of internal classes** |

The `api/` suite treats the running application exactly the way an external consumer, a QA engineer, or a pentester
would: it never imports a Spring bean, a repository, or a service class. Every assertion is made against what actually
crosses the wire - status codes, JSON shape, and business rules as observed from outside.

This is intentional. Unit and integration tests answer *"does the code do what the code intends?"* Black-box API tests
answer *"does the deployed system do what the contract with its consumers promises?"* - a different and complementary
question, and the one most QA/tester roles are hired to answer.

## Why it's excluded from the default build

`mvn test` and `mvn verify` never run these tests. That's deliberate: they require a real running instance to test
against, and a CI pipeline or a developer running `mvn test` locally should never fail just because no server happens to
be up. The classes are named `*ApiIT` (not `*Test`), which Surefire's default include pattern does not match.

## Running the suite

1. Start the application somewhere - locally, in Docker, or point at any already-deployed environment (staging, the
   public demo instance, etc).
2. Run:

   ```bash
   mvn verify -Papi-tests \
       -Dapi.baseUri=http://localhost:8080 \
       -Dapi.admin.email=admin@helpdesk.gov.za    -Dapi.admin.password=<real password> \
       -Dapi.agent.email=thabo.m@company.co.za    -Dapi.agent.password=<real password> \
       -Dapi.agent2.email=s.jenkins@it.com        -Dapi.agent2.password=<real password> \
       -Dapi.user.email=lerato.d@client.org       -Dapi.user.password=<real password> \
       -Dapi.user2.email=m.thompson@gmail.com     -Dapi.user2.password=<real password>
   ```

   Every `-D` flag has a fallback default (see `pom.xml`'s `<properties>` block and the `api-tests` profile), so the
   suite can also be run with no flags at all against `http://localhost:8080` using the seeded demo accounts - just
   supply the real passwords, which are intentionally not committed to source control.

3. To run just one class from an IDE, set the same `-D` properties as JVM system properties in the run configuration and
   execute the class directly (IDEs don't respect the Failsafe include pattern, so `*ApiIT` classes run fine as ordinary
   JUnit tests once the properties are present).

## What's covered

- **Auth** (`AuthApiIT`): login success/failure, schema validation of the token response, refresh-token exchange and
  revocation on logout, generic (non-enumerating) failure responses for both login and password reset, and a live check
  that the unauthenticated rate limiter actually returns `429`
  after repeated failed attempts.
- **Tickets** (`TicketApiIT`): full CRUD, field validation, RBAC on the agent/admin-only update and admin-only delete
  endpoints, illegal status-transition rejection (`422`), pagination and filtering, and two IDOR regression tests
  confirming the API's documented behaviour of returning a disguised `404` (never a `403`) when a user or agent requests
  a ticket they have no right to see - proving the fix doesn't just hide the ticket from listings but also blocks direct
  ID guessing.
- **Categories** (`CategoryApiIT`): tree retrieval, admin-only write RBAC, duplicate-name conflict handling (`409`), and
  soft-delete (deactivation)
  behaviour.

## Design notes worth calling out in an interview

- **No shared mutable state between tests.** Every test re-authenticates and creates its own fixture data rather than
  relying on execution order or data left behind by another test - the suite must be safe to run in any order, in
  parallel, or as a single isolated test.
- **JSON Schema validation** (`src/test/resources/schemas/*.json`) is used alongside individual field assertions for the
  two most consumer-facing response shapes, as a lightweight form of contract testing: it catches
  "field silently disappeared" or "type silently changed" regressions that field-by-field assertions alone can miss.
- **Negative and security-focused tests are treated as first-class**, not an afterthought: user-enumeration protection,
  IDOR protection, and rate limiting all have dedicated tests, not just the happy path.
- **Environment-portable by design.** The exact same test classes run against localhost, CI, or a public demo deployment
  purely by changing
  `-D` properties - no code changes, no environment-specific branches.
