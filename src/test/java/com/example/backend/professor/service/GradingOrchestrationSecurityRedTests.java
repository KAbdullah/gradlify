package com.example.backend.professor.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardrail tests for Step C Loop 2 orchestration security (sandbox doc, compiler hygiene, no static CSV fields).
 */
class GradingOrchestrationSecurityRedTests {

    /**
     * Critical pattern: RCE / no sandbox — {@code InMemoryTestRunner} (embedded in ProfGradeService)
     * runs professor + student bytecode in the host JVM with no isolation.
     * <p>
     * This test encodes a minimum bar: the runner source must reference an explicit sandbox
     * or out-of-process isolation strategy. Today it does not — expect failure until architecture changes.
     */
    @Test
    void inMemoryTestRunner_mustDeclareSandboxOrProcessIsolation() throws Exception {
        ProfGradeService service = new ProfGradeService(null, null, null);
        Method m = ProfGradeService.class.getDeclaredMethod("getInMemoryTestRunnerCode");
        m.setAccessible(true);
        String runnerSource = (String) m.invoke(service);

        assertThat(runnerSource)
                .as("Student code shares the grader JVM; runner must document SecurityManager, "
                        + "subprocess isolation, or equivalent sandbox hooks")
                .containsAnyOf("SecurityManager", "ProcessBuilder", "Process", "sandbox", "isolate");
    }

    /**
     * Critical pattern: resource hygiene — {@code StandardJavaFileManager} is obtained from the
     * system compiler but ProfGradeService never closes it. On compiler failure or timeout paths,
     * native/temporary resources may leak across many submissions.
     * <p>
     * This test reads main sources from the workspace (fails in CI without checkout — use Assume if needed).
     */
    @Test
    void profGradeService_mustCloseStandardFileManagerAfterCompilation() throws Exception {
        Path src = Path.of("src/main/java/com/example/backend/professor/service/ProfGradeService.java");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(src),
                "Source file not on classpath; run from project root");

        String content = Files.readString(src);
        assertThat(content)
                .as("Compiler StandardJavaFileManager must be closed via try-with-resources on all compile paths")
                .contains("try (StandardJavaFileManager");
    }

    /**
     * Critical pattern: thread safety / cross-request bleed — grading download path uses
     * static {@code gradingResultsPath} / {@code gradingFileName}, so concurrent professors
     * can overwrite each other's CSV location (wrong download / cross-session mix-up).
     */
    @Test
    void gradingOutputPaths_mustNotBeStaticSharedMutableState() throws Exception {
        assertThatThrownBy(() -> ProfGradeService.class.getDeclaredField("gradingResultsPath"))
                .as("grading CSV path must not live on ProfGradeService as static mutable state")
                .isInstanceOf(NoSuchFieldException.class);
        assertThatThrownBy(() -> ProfGradeService.class.getDeclaredField("gradingFileName"))
                .isInstanceOf(NoSuchFieldException.class);
    }
}
