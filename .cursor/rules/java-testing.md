---
description: Backend Java testing standards for Gradlify
globs: "{src/main/java/**/*.java,src/test/java/**/*.java}"
alwaysApply: false
---

# Java Testing Rules (Gradlify)

- Use JUnit 5 (`org.junit.jupiter`) for all Java tests.
- Use Mockito for mocking repositories, external dependencies, and side effects.
- Use AssertJ for readable assertions.
- Prefer fast unit tests over container-heavy tests.

## Framework Constraints
- Do not use `@SpringBootTest` unless explicitly requested by the user/task.
- Prefer `@ExtendWith(MockitoExtension.class)` for service-level unit tests.
- For web-layer coverage, prefer `@WebMvcTest` with mocked collaborators.

## Isolation Rules
- Unit tests must not perform real file-system writes unless the test explicitly targets file I/O behavior.
- Unit tests must not depend on network availability.
- For grading services, mock the `InMemoryTestRunner` execution boundary; test orchestration/decision logic around it.

## Assertion Rules
- One behavioral intent per test.
- Use descriptive test names in `given_when_then` style.
- Assert both success and failure paths for high-risk methods (auth, grading, security filters).

## Determinism Rules
- Control time-dependent behavior in JWT-related tests.
- Avoid sleeping/thread timing in unit tests; simulate timeout states via mocks/stubs.
