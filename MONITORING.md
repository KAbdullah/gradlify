# Step D: Monitoring & quality gates (Gradlify)

This document records **commands**, **artifact locations**, and **log snippets** for coverage (JaCoCo), Surefire test reports, and repeat-run flake checks.

---

## 1. Coverage report (JaCoCo)

**Command** (from the `gradlify` directory):

```powershell
mvn clean test
```

JaCoCo is configured in `pom.xml` (`jacoco-maven-plugin`: `prepare-agent` + `report` bound to the `test` phase). After a successful run, open the HTML report in a browser:

- **Report path:** `target/site/jacoco/index.html`

**Evidence (Maven log snippet):**

```text
[INFO] --- jacoco:0.8.11:prepare-agent (default) @ gradify ---
[INFO] argLine set to -javaagent:...org.jacoco.agent-0.8.11-runtime.jar=destfile=...\target\jacoco.exec
...
[INFO] --- jacoco:0.8.11:report (report) @ gradify ---
[INFO] Loading execution data file ...\target\jacoco.exec
[INFO] Analyzed bundle 'gradify' with 58 classes
[INFO] BUILD SUCCESS
```

*(Optional screenshot: capture `target/site/jacoco/index.html` open in a browser showing overall coverage.)*

---

## 2. Test reporting (Maven Surefire)

Surefire runs automatically with `mvn test`. XML and text summaries are written under:

- **Directory:** `target/surefire-reports/`
- **Examples:** `TEST-*.xml`, `*.txt` per test class

**Evidence (Surefire text report snippet — `com.example.backend.auth.utility.JwtUtilTest.txt`):**

```text
-------------------------------------------------------------------------------
Test set: com.example.backend.auth.utility.JwtUtilTest
-------------------------------------------------------------------------------
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.210 s -- in com.example.backend.auth.utility.JwtUtilTest
```

**Full suite summary (excerpt from `mvn clean test`):**

```text
[INFO] Results:
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 3. Repeat-run flake check (10× full suite)

**Command** (PowerShell, from `gradlify`):

```powershell
$runs = 10
$fail = 0
1..$runs | ForEach-Object {
  mvn -q test *> $null
  if ($LASTEXITCODE -ne 0) { $fail++; Write-Host "Run $_ FAILED exit=$LASTEXITCODE" }
  else { Write-Host "Run $_ OK" }
}
Write-Host "Total runs: $runs Failures: $fail"
```

**Evidence (actual run on 2026-04-06):**

```text
Run 1 OK
Run 2 OK
Run 3 OK
Run 4 OK
Run 5 OK
Run 6 OK
Run 7 OK
Run 8 OK
Run 9 OK
Run 10 OK
---
Total runs: 10 Failures: 0
```

---

## 4. Optional: flake check on one class (30×)

Assignment text allows running **target tests** many times (e.g. 30). Example for JWT utility tests only:

```powershell
$runs = 30
$fail = 0
1..$runs | ForEach-Object {
  mvn -q -Dtest=JwtUtilTest test *> $null
  if ($LASTEXITCODE -ne 0) { $fail++ }
}
Write-Host "JwtUtilTest runs: $runs failures: $fail"
```

Record the printed line as evidence if you use this variant.

---

## 5. Status checklist

| Requirement                         | Status |
|-------------------------------------|--------|
| JaCoCo coverage report generation   | Yes (`mvn clean test` → `target/site/jacoco/`) |
| Surefire / test output captured     | Yes (`target/surefire-reports/`) |
| Repeat-run flake check (10× suite)  | Done; 0 failures in recorded run |
| This `MONITORING.md` with commands + logs | Yes |
