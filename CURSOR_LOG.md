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