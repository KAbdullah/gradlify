--- CURSOR DRAFT ---

# Gradlify Agentic Test Plan (Step B)

## Scope And Goals
- Establish a deterministic, scalable test foundation from a 0% baseline.
- Prioritize P1 modules from `target_list.md` to protect auth, grading, and API boundaries first.
- Separate strict unit tests from integration/system tests so failures localize quickly.

## Unit Boundaries

### Backend (Spring Boot)
- **Unit test (in-scope):**
  - Pure business logic in service/controller helpers.
  - Decision logic for token validation, role checks, and grading result mapping.
  - Error-path behavior (invalid input, expired token, repository errors).
- **Mocked (out-of-scope for unit tests):**
  - Database repositories (`*Repository`) via Mockito.
  - Mail/network/external integrations.
  - File system and compiler/JUnit execution side effects.
  - **`InMemoryTestRunner` execution path in grading services**:
    - Treat runtime compilation + in-memory JUnit execution as external behavior.
    - Validate inputs/outputs around runner invocation with controlled stubs.
    - Use a seam (spy/facade/wrapper abstraction) to mock runner outcome states:
      - success
      - compilation failure
      - timeout
      - runtime exception
- **Deferred integration coverage:**
  - End-to-end compile-and-run tests of real `InMemoryTestRunner` done in a separate integration suite, not blocking core unit suite.

### Frontend (React)
- **Unit/component test (in-scope):**
  - Rendering logic, user flows, conditional UI, route guards.
  - Local storage token/role handling logic.
  - Form submit and status-state transitions.
- **Mocked (out-of-scope for unit tests):**
  - **All API calls** via mocked axios (global mock setup).
  - Router environment via `MemoryRouter`.
  - Browser APIs (`localStorage`, timers) with controlled test doubles.
- **Deferred integration coverage:**
  - Real API + UI contract tests (if added later) should run in a dedicated integration stage.

## Test Categories And Strategy

### Happy Paths
- Backend:
  - Successful grading pipeline returns expected grade/result persistence.
  - Valid JWT generation/validation for expected user and role.
  - Security config permits intended public endpoints and protects role-scoped endpoints.
- Frontend:
  - Successful login stores token/role and navigates correctly.
  - `RequireRole` grants access with valid token + matching role.
  - Axios interceptor attaches `Authorization: Bearer <token>` when token exists.

### Edge Cases
- Backend:
  - Empty submission set / missing files in grading request.
  - Multiple files with invalid class/file name mismatch.
  - Duplicate or malformed test case definitions.
- Frontend:
  - Role mismatch redirects user from protected route.
  - Empty code submission UI path and validation messaging.
  - Missing token with stale role in storage.

### Error Handling
- Backend:
  - JWT expiration path rejects access as expected.
  - Runner timeout or compile error returns stable, parseable error result.
  - Repository exception path maps to safe API response (no stacktrace leak).
- Frontend:
  - 401/403 responses show expected UX state and navigation fallback.
  - Axios rejection bubbles to components without unhandled promise errors.
  - Login failure displays deterministic error feedback.

### Boundary Conditions
- Backend:
  - File upload size near configured limits (under/equal/over).
  - Large batch grading requests near concurrency and timeout boundaries.
  - Token expiration near boundary time (just-valid vs just-expired).
- Frontend:
  - Large payload form state handling for test-case creation.
  - Input max lengths and invalid character handling.
  - Route switching when storage changes during session.

## Flakiness And Non-Determinism Risks

### I/O And Runtime Risks (`ProfGradeService`)
- Dynamic compilation and classloading depend on filesystem/classpath state.
- Runner timing can vary with machine load and Java process conditions.
- File upload and temp file handling can create order/timing sensitivity.
- CSV/report output and persistence ordering can vary under concurrent grading.

**Mitigations**
- Keep unit tests isolated from actual runtime compilation by mocking runner seam.
- Use deterministic fixtures and stable ordering assertions.
- Avoid real disk I/O in unit tests; use in-memory streams/stubs.
- Move true compile/runtime behavior into separate integration tests with generous, explicit timeouts.

### Clock/Timing Risks (JWT Validation)
- `validateToken` behavior depends on current wall-clock time.
- Clock skew between issuer and validator can produce intermittent failures near expiry.

**Mitigations**
- Use controlled clock strategy in tests (time abstraction or narrow token windows with deterministic setup).
- Validate both sides of expiration threshold with explicit time offsets.
- Avoid relying on test runtime speed; assert with fixed epoch-derived timestamps.

## Prioritized Implementation Checklist (Start With P1)

### Phase 1: P1 Backend
- [ ] Create `src/test/java` structure and base test config utilities.
- [ ] `ProfGradeService` unit tests: success, compile fail, timeout, persistence mapping, empty submissions.
- [ ] `StudentService` unit tests for mirrored grading path consistency.
- [ ] `JwtUtil` tests: token generation claims, valid token, expired token.
- [ ] `JwtFilter` tests: header absent, malformed bearer token, valid auth context injection.
- [ ] `SecurityConfig` slice tests: permit-all and role-based endpoint access matrix.

### Phase 2: P1 Frontend
- [ ] Fix/replace obsolete smoke test and establish stable Jest + RTL setup.
- [ ] `AxiosInterceptor` tests: attach token header, no-header behavior without token.
- [ ] `RequireRole` tests: allow matching role, redirect on missing token/role mismatch.
- [ ] `Login` tests: success flow (store token/role + navigate), error flow (401/403 handling).

### Phase 3: P2 Coverage Expansion
- [ ] Backend controllers/services in auth and grading orchestration (`AuthController`, `ProfGradeController`, `TestCaseService`).
- [ ] Frontend auth recovery/registration components (`Signup`, `ForgotPassword`, `ResetForgottenPassword`, `SetPasswordPage`).
- [ ] High-complexity pages (`GradePage`, `AddTestCase`, `StudentPage`) with mocked API and route contexts.

### Phase 4: P3 Stabilization
- [ ] Utility/security/reporting modules (`EncryptionUtil`, checks, report/mail services).
- [ ] App/router regression tests aligned to real route map.
- [ ] Flakiness audit pass: rerun unstable tests and eliminate timing/network assumptions.

## Execution Model For Agentic Generation
- Implement tests in thin vertical slices (one module + one behavior family at a time).
- After each slice:
  - run targeted tests,
  - fix deterministic failures,
  - then proceed to next item.
- Keep fixtures localized per module to reduce hidden coupling.

## Rule-Driven Strategy (`.cursor/rules`)
- The agent should **selectively apply rules based on files under edit**:
  - Use `java-testing.md` when creating/updating tests in `src/test/java` and Java source under `src/main/java`.
  - Use `react-testing.md` when creating/updating frontend tests under `frontend/src`.
- For mixed changes (frontend + backend in same task), apply each rule only within its scope; avoid cross-pollinating frameworks or assertions.
- If a task explicitly asks for integration/system testing, the agent may extend beyond rule defaults, but it must state the exception in PR notes or task output.

## Exit Criteria For Step B Completion
- Test plan artifacts exist and are agreed.
- Test scaffolding can be generated without ambiguity for P1 targets.
- Boundaries between unit and integration behavior are explicit, especially around `InMemoryTestRunner` and API mocking.
