# Gradlify — Target List for New Tests

Priorities are ordered by **testing priority** (risk to core behavior: auth, grading, API boundaries).  
**Evidence for “lacks coverage”:** No `src/test/java`; no `@Test` in the repo; frontend Jest run produced **0%** coverage and did not execute assertions.

---

## Backend (Spring Boot) — `com.example.backend`

| Priority | Module / area | File / symbol (focus) | Rationale |
|:--------:|---------------|----------------------|-----------|
| P1 | Professor grading | `professor/service/ProfGradeService.java` — in-memory compile, `InMemoryTestRunner`, timeouts, persistence to `GradingResult` | Core product value; filesystem/classpath/JUnit execution paths are high-risk and easy to break silently. |
| P1 | Student grading | `student/service/StudentService.java` — overlapping in-memory runner / compile path (`InMemoryTestRunner`) | Duplicated critical logic increases regression risk; must stay consistent with professor flow. |
| P1 | Authentication & tokens | `auth/utility/JwtUtil.java` — `generateToken`, `validateToken`, `extractClaim` | Incorrect token handling breaks all role-protected APIs. |
| P1 | Security boundary | `auth/utility/JwtFilter.java` | Every authenticated request depends on filter behavior (headers, errors, chain). |
| P1 | HTTP security policy | `auth/security/SecurityConfig.java` — `filterChain`, role matchers, `permitAll` exceptions | Misconfiguration exposes professor/student endpoints or blocks legitimate traffic. |
| P2 | Auth API | `auth/controller/AuthController.java` — `/login`, signup-related flows, `devAuthEnabled` | Public entry points; bugs affect all users. |
| P2 | Password / invite flows | `auth/controller/ForgotPasswordController.java`, `PasswordSetController.java` | Sensitive flows; errors leak account state or lock users out. |
| P2 | User load | `auth/service/UserDetailsServiceImpl.java` | Spring Security integration; failures appear as generic auth errors. |
| P2 | Professor grading API | `professor/controller/ProfGradeController.java` | Boundary between upload/grade orchestration and service layer. |
| P2 | Test case management | `professor/service/TestCaseService.java`, `professor/controller/TestCaseController.java` | Defines what gets executed during grading. |
| P3 | Student API | `student/controller/StudentController.java` | Student-facing operations and consistency with services. |
| P3 | Static analysis checks | `student/checks/CurlyBracketCheck.java`, `student/checks/ViolationCollector.java` | Rule logic should be deterministic and unit-testable. |
| P3 | Crypto helper | `auth/security/EncryptionUtil.java` | Crypto misuse is a security defect. |
| P3 | Reporting / mail | `professor/service/InstructorReportService.java`, `professor/service/EmailService.java` | Data correctness and failure handling affect instructors. |

---

## Frontend (React) — `frontend/src`

| Priority | Module / area | File / symbol (focus) | Rationale |
|:--------:|---------------|----------------------|-----------|
| P1 | Global API auth | `auth/AxiosInterceptor.js` — request interceptor, `Authorization` header | All API calls depend on JWT attachment; regressions look like random 401s. |
| P1 | Route protection | `auth/RequireRole.js` — token/role check, `Navigate` | Wrong logic bypasses UI gating or locks out users (**note:** component navigates to `/login` while `App.js` registers login at `/`; behavior worth verifying in tests). |
| P1 | Authentication UI | `auth/Login.js` | Primary entry; integrates with backend auth and storage. |
| P2 | Registration / password | `auth/Signup.js`, `auth/ForgotPassword.js`, `auth/ResetForgottenPassword.js`, `auth/SetPasswordPage.js` | High-impact user flows; many branches and API calls. |
| P2 | Professor workflow | `professor/GradePage.js`, `professor/AddTestCase.js`, `professor/CourseDetailsPage.js` | Large components with grading and CRUD behavior. |
| P2 | Student workflow | `student/StudentPage.js` | Large surface area for submissions and course UI. |
| P3 | Navigation shell | `professor/ProfessorMenu.js`, `professor/ProfessorDashboard.js` | Routing and layout; lower risk than grading/auth but still user-facing. |
| P3 | App router | `App.js` | Regression tests should reflect real routes (replace obsolete CRA smoke test expectations). |

---

## Suggested next engineering steps (optional)

1. Fix Jest resolution for `react-router-dom` v7 (or adjust test tooling) so suites **run**.  
2. Add `src/test/java` and Spring Boot tests with `@WebMvcTest` / `@SpringBootTest` as appropriate.  
3. Add JaCoCo to `pom.xml` to produce measurable Java coverage on CI.
