# CURSOR LOG

## Loop #: 1
- **Target:** `JwtUtil`
- **Initial Strategy:** Use true unit tests (no Spring context) because `JwtUtil` is a deterministic utility with crypto/time logic and no repository/network dependencies.  
  Mockito is used only for `UserDetails` inputs, while token creation/validation paths are exercised with real JJWT signing to preserve behavior fidelity.  
  This keeps tests fast and focused while still covering student-defined timing risks (clock skew and expiration boundaries).

### Refinement Iteration 2
- **Issue observed:** Specific catch block (`ExpiredJwtException`, `SignatureException`, `MalformedJwtException`) was rejected in runtime behavior and failed to reliably intercept the thrown library exception.
- **Decision:** Fix code by broadening exception handling to `JwtException`.
- **Implementation update:** `validateToken` now catches `JwtException | IllegalArgumentException` and returns `false` for malformed, expired, invalid-signature, null, or empty token inputs.
- **Justification:** Total resilience is preferred in the auth filter layer to prevent 500-level crashes on any invalid token shape across JJWT versions/subtypes.

### Refinement Iteration 4
- **Issue identified:** Mockito strictness flagged `UnnecessaryStubbingException` in expired-token test, and the near-expiry timing window was brittle.
- **Decision:** Refine test setup with lenient stubbing and wider timing windows.
- **Implementation update:** Used `lenient().when(userDetails.getUsername())` in the expired-token test; increased near-expiry token window to `2000ms` and post-check sleep to `2100ms`.
- **Justification:** Tests must be deterministic; strict-stubbing failures are noise when code intentionally exits early for security reasons (expired/invalid tokens).


### Loop 1 Final Result
- **Total Iterations:** 4
- **Real Defect Found:** `validateToken` crashed with `ExpiredJwtException` instead of returning `false`.
- **Student Decision:** I forced a source-code fix over a test-fix. I rejected the AI's first attempt (narrow catch blocks) in favor of a broader `JwtException` catch to ensure the security filter never leaks library-specific crashes.
- **Status:** All tests passing.

### Loop 1 Test Log Proof (Both Scenarios)
- **Scenario A (without `try/catch` in `JwtUtil.validateToken`):**
  - Key output:
    - `[ERROR] Tests run: 4, Failures: 0, Errors: 2, Skipped: 0`
    - `io.jsonwebtoken.ExpiredJwtException: JWT expired ...`
    - `at com.example.backend.auth.utility.JwtUtil.validateToken(JwtUtil.java:46)`
    - `[INFO] BUILD FAILURE`
- **Scenario B (with `try/catch` in `JwtUtil.validateToken`):**
  - Key output:
    - `[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`
    - `[INFO] BUILD SUCCESS`

## Loop #: 2
- **Target:** `JwtFilter`
- **Initial Strategy:** Add isolated filter unit tests (Mockito + servlet mocks, no Spring context) to verify authentication wiring and failure behavior around malformed headers/tokens.
  Use private-field injection for `jwtUtil` and `userDetailsService` to keep tests focused on request filtering logic and `SecurityContextHolder` side effects.

### Refinement Iteration 1
- **Issue observed:** The first generated malformed-token test only asserted that `extractUsername` throws, which is a flawed assertion for filter behavior because it validates library parsing, not request-pipeline safety.
- **Decision:** Fix the test and broaden scope to the real edge case: request must still reach `filterChain` and security context must remain unauthenticated.
- **Implementation update:** Replaced the throw-centric assertion with behavior assertions (`filterChain.doFilter` invoked, no auth set, no user lookup performed).
- **Justification:** Filter correctness is resilience under bad inputs; tests should assert externally visible behavior, not internal exception mechanics.

### Refinement Iteration 2
- **Issue identified:** The behavior-focused malformed-token test initially failed due to a real defect: `JwtFilter` allowed `JwtException` from `extractUsername` to bubble up and break the request path.
- **Decision (fix test vs fix code vs known issue):** **Fix code**. Keep the stronger test and patch `JwtFilter` to catch `JwtException | IllegalArgumentException` and continue filter chain.
- **Implementation update:** Wrapped JWT extraction/validation block in a guarded `try/catch` and preserved normal flow to `filterChain.doFilter`.
- **Justification:** Invalid JWTs are expected hostile input; auth filter must fail closed on auth state but fail open on request pipeline stability (no 500 from token parsing).

### Loop 2 Final Result
- **Total Iterations:** 2
- **Real Defect Found:** Malformed bearer token could crash `JwtFilter` before request chain continuation.
- **Student Decision:** I rejected the weaker exception-only test, strengthened it to a behavioral contract, then fixed production code to satisfy that contract.
- **Status:** New `JwtFilter` tests pass with resilient malformed-token handling.

### Loop 2 Test Log Proof (Both Scenarios)
- **Scenario A (without `try/catch` in `JwtFilter`):**
  - Key output:
    - `[ERROR] Tests run: 4, Failures: 0, Errors: 1, Skipped: 0`
    - `io.jsonwebtoken.MalformedJwtException: invalid jwt`
    - `at com.example.backend.auth.utility.JwtFilter.doFilterInternal(JwtFilter.java:42)`
    - `[INFO] BUILD FAILURE`
- **Scenario B (with `try/catch` in `JwtFilter`):**
  - Key output:
    - `[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`
    - `[INFO] BUILD SUCCESS`

### Phase C — Grading orchestration layer (Loop 2 extension, RED-only)
- **Loop #:** 2 (same loop, expanded scope to orchestration hardening)
- **Initial Strategy:** Security and Resource Audit of the Grading Orchestration Layer (`ProfGradeService`, embedded `InMemoryTestRunner` source, and residual `JwtFilter` failure modes).
- **Status:** RED tests added first; **no production fixes applied yet** — awaiting student review and explicit “go green” command.
- **Evidence tests (expected failing until architectural fixes land):**
  1. **RCE / sandbox gap:** Runner source must declare sandbox or process isolation (currently plain JUnit launcher in-host).
  2. **Resource hygiene:** `StandardJavaFileManager` must be closed on all compiler paths (currently no `fileManager.close()`).
  3. **Cross-request / concurrency:** `gradingResultsPath` / `gradingFileName` must not be `static` shared mutable state.
  4. **Filter resilience:** Unknown user for a parseable JWT must not abort the chain (`UsernameNotFoundException` not caught today).

### Phase D — `ProfGradeService` architectural fixes (Loop 2, GREEN)
- **Justification:** Architectural resilience is mandatory. Static mutable state in a Spring singleton is a day-one concurrency failure; unclosed compiler file managers are a common production outage pattern; grading CSVs must not fill `java.io.tmpdir` indefinitely.
- **Fix 1 — Concurrency / stateless service:** Removed `static` `gradingResultsPath` and `gradingFileName`. `runGradingOnFolder` now returns `gradingResultsPath`, `gradingFileName`, and an opaque `downloadId` in the response map. Introduced `GradingArtifactRegistry` to map `downloadId` → absolute path + filename for downloads without storing paths on `ProfGradeService`.
- **Fix 2 — Resource leak:** Wrapped `StandardJavaFileManager` and `MemoryJavaFileManager` in try-with-resources for each student compile/run block so `close()` runs on success, compilation failure, and exceptions.
- **Fix 3 — RCE / host API gate:** Added `RestrictedMemoryClassLoader` that defines in-memory compiled classes but **denies** loading `java.lang.System`, `java.io.File`, `java.lang.Runtime`, and `java.lang.ProcessBuilder` before delegating other classes to the parent. Documented in generated runner source that sandbox expectations are tied to this gate; full isolation still requires an out-of-process grader.
- **Fix 4 — Disk hygiene:** `GradingArtifactRegistry.registerArtifact` calls `deleteOnExit()` on the CSV and runs a `@Scheduled` purge (configurable via `gradify.grading.csv-max-age-hours` and `gradify.grading.csv-cleanup-interval-ms`). `@EnableScheduling` added on `AutoGraderApplication`.
- **API / client:** `GET /api/professor/grade/results/download` now **requires** query param `downloadId`. `GradePage.js` uses the dev proxy path and passes `downloadId`. Removed redundant `permitAll` on download in `SecurityConfig` (professor routes already enforce `ROLE_PROFESSOR`).
- **`JwtFilter` (Phase C item 4):** Extended catch to include `UsernameNotFoundException` so a well-formed JWT with a missing user does not abort the filter chain with 500.