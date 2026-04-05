# Gradlify — Baseline Audit Log (Step A)

**Audit date:** 2026-04-05  
**Scope:** `frontend/src`, `src/main/java/com/example/backend`, and backend test layout under `src/test/java`.

---

## 1. Frontend test inventory (`frontend/src`)

| Pattern        | Files found |
|----------------|-------------|
| `*.test.js`    | `App.test.js` |
| `*.spec.js`    | *(none)*    |
| `*.test.jsx`   | *(none)*    |
| `*.spec.jsx`   | *(none)*    |

**Supporting Jest setup (not counted as test specs):** `setupTests.js`.

**Conclusion:** Exactly **one** test file exists under `frontend/src`.

---

## 2. Backend test inventory

| Location              | Status |
|-----------------------|--------|
| `src/test/java`       | **Absent** — no `src/test` tree exists in the repository (verified via glob search). |
| `@Test` in `*.java`   | **None** — repository-wide search for `@Test` returned no matches. |

**Conclusion:** There are **no** JUnit (or other annotated) automated test sources for the Spring Boot backend in this repo.

---

## 3. Test execution summary

### 3.1 Frontend (Jest / `react-scripts test`)

**Command executed:** `npm test -- --coverage --watchAll=false` with `CI=true` (PowerShell), from `frontend/`.

| Metric | Result |
|--------|--------|
| Test suites | **1 total**, **1 failed** |
| Tests run | **0** (suite failed before any test body executed) |
| Passing | **0** |
| Failing | **0** (assertions never reached) |
| Flakiness | **Not observed** — single run; failure was a deterministic module-resolution error |

**Failure detail:** `Cannot find module 'react-router-dom' from 'src/App.js'` when loading `App.test.js`. Dependencies are present under `frontend/node_modules`, but Jest’s resolver did not resolve `react-router-dom` (installed version **7.7.1** with conditional `package.json` `exports`).

**Jest coverage output (from the same run):**

| Scope | % Stmts | % Branch | % Funcs | % Lines |
|-------|---------|----------|---------|---------|
| **All files** | **0** | **0** | **0** | **0** |

The coverage table lists all application sources under `src/` at **0%** because no tests successfully executed application code.

**Note:** The text in `App.test.js` still expects a “learn react” link; the current `App.js` is a Gradlify router and does not render that string. Even after fixing the module resolution issue, that assertion would need to be updated for the test to pass.

### 3.2 Backend (`mvn test`)

**Command attempted:** `mvn -q test` from the project root.

| Metric | Result |
|--------|--------|
| Outcome | **Not executed** — `mvn` is **not available** on the PATH in this environment (`mvn : The term 'mvn' is not recognized`). |
| Expected behavior given repo state | With **no** `src/test/java` and **no** `@Test` methods, Maven would run **zero** backend tests even if the CLI were available. |

**Backend coverage tooling:** Root `pom.xml` does **not** configure JaCoCo (or similar). No Java coverage percentage was produced.

---

## 4. Baseline interpretation (for agentic test generation)

- **Frontend measured baseline (this run):** **0%** statements/branches/functions/lines per Jest’s coverage report; **0** passing tests.
- **Backend measured baseline:** **No test sources** and **no coverage report** — for planning purposes, treat automated coverage as **0%** until tests and a coverage plugin (e.g. JaCoCo) are added and executed successfully.

This documents a **near-zero effective baseline** and justifies the upcoming test-generation phase.

---

## 5. Repo notes (non-test)

- Maven Wrapper (`mvnw`) is **not** present; CI/local runs depend on an installed Maven unless a wrapper is added.
- No `src/test/java` directory: backend tests must be introduced as **new** source files.
