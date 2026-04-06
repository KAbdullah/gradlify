---
description: Frontend React testing standards for Gradlify
globs: "frontend/src/**/*.{js,jsx,ts,tsx}"
alwaysApply: false
---

# React Testing Rules (Gradlify)

- Use Jest and React Testing Library for all frontend tests.
- Test user behavior and rendered outcomes, not implementation details.
- Use `MemoryRouter` for route-dependent components.

## API Mocking
- Mock axios globally for frontend tests.
- Do not perform real network calls in unit/component tests.
- Validate request intent (endpoint/payload/headers) and UI reaction to responses.

## Core Coverage Expectations
- Include both success and error scenarios for auth and protected routes.
- Add explicit tests for 401/403 handling and role-based redirects.
- Verify token/role storage and `Authorization` header behavior.

## Reliability Rules
- Avoid fixed delays (`setTimeout`-based waiting); use RTL async utilities (`findBy*`, `waitFor`).
- Keep tests independent: reset mocks and storage state between tests.
- Prefer stable selectors by accessible roles/labels/text.

## Scope Guardrails
- Component tests should mock APIs and router context.
- Escalate to integration tests only when explicitly requested.
