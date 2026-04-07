
## Where Cursor helped the most

Cursor was most useful for **turning a vague testing goal into runnable JUnit tests** (`JwtUtil`, `JwtFilter`) and for **interpreting Maven stack traces** when runs failed. It also sped up drafting the written **test plan** and **problem reports**, and suggested concrete patterns (Mockito + servlet mocks, real JJWT tokens for expiry cases) that matched how we wanted to test auth without booting the full Spring context.

## End-to-end process for the agentic test suite

We treated testing as an **adversarial refinement loop**: (1) pick a high-risk class, (2) ask Cursor to generate tests, (3) **add them to the repo**, (4) run `mvn test` and capture failures, (5) feed logs back and decide whether to fix **tests or production code**, (6) repeat. We ran this on **`JwtUtil`** then **`JwtFilter`**, logged iterations in `CURSOR_LOG.md`, and captured broader strategy in `TEST_PLAN.md`. In parallel, **problem reports** recorded **eight** issues (low to critical) across JWT utilities, the filter, and professor-side grading—concurrency, resources, and unsafe execution of student code—with mitigations in the templates and a noted future direction of **isolating the grade runner** (e.g. containers) for user-to-user separation.

## Incorrect or fragile output from Cursor—and how we caught it

These items are the ones recorded in **`CURSOR_LOG.md`** (refinement iterations), not generic claims:

- **Narrow `catch` types in `validateToken`:** The first suggestion listed specific exception types; runtime still threw `ExpiredJwtException`. **`mvn test`** showed errors until we broadened handling to **`JwtException`** (and **`IllegalArgumentException`**) per Loop 1, Refinement Iteration 2.
- **Malformed-token test that only asserted `extractUsername` throws:** The log flags this as testing the library, not filter behavior. We **replaced** it with checks that **`filterChain.doFilter`** still runs, **no user lookup**, **unauthenticated context** (Loop 2, Refinement Iteration 1)—which then exposed a real **`JwtFilter`** defect (exception bubbled) fixed in Refinement Iteration 2.
- **Brittle timing / Mockito strictness:** **`UnnecessaryStubbingException`** and a tight near-expiry window are documented in Loop 1, Refinement Iteration 4; we used **`lenient()`** stubbing and **wider timing** after Surefire output.

## What we asked Cursor to focus on (from `CURSOR_LOG.md` only)

We did **not** preserve full chat transcripts. **`CURSOR_LOG.md`** is the authoritative record of **scope and iterations**:

- **Loop 1 (`JwtUtil`):** *Initial strategy* — true unit tests, **no Spring context**, **Mockito** only for **`UserDetails`**, **real JJWT** for signing/validation paths, and **expiration / clock-skew** boundaries (see Loop #: 1, opening bullets).
- **Loop 2 (`JwtFilter`):** *Initial strategy* — **Mockito + servlet mocks**, **no Spring context**, **private-field injection** for **`jwtUtil`** and **`userDetailsService`**, emphasis on **`SecurityContextHolder`** and **malformed** tokens (see Loop #: 2, opening bullets).
- **After failures:** The log’s **Refinement Iteration** sections describe what we changed when **`mvn test`** failed (e.g. catch breadth, behavioral assertions, **`lenient()`**, timers)—that is the same feedback we gave Cursor, quoted there as decisions, not as verbatim prompts.
- **Later scope:** The log adds **Phase C / Phase D** notes on grading orchestration (sandbox, file managers, static state, downloads, scheduling); details belong in that file.

## What we would change next time

We would **write the same kind of “initial strategy” bullets up front** (as in the log) **before** generating tests, so the first answer already matches our **no-Spring / behavioral-assertion** rules. We would also run **`mvn test` twice** before accepting a generated class, and extend the log with **paste-in snippets of the exact prompt** whenever we start a new loop so Step E can cite real wording without guessing.
