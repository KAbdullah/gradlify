/**
 *
 * This service handles grading of student Java code submissions against instructor-defined test cases.
 * It supports:
 * - Compilation of in-memory Java files (student + test case + runner)
 * - Execution of JUnit test cases with timeouts
 * - Logging of grading outcomes
 * - Optional AI-generated feedback
 *
 * Key concepts:
 * - Uses in-memory compilation via JavaCompiler API
 * - Dynamically executes compiled test classes using reflection
 * - Handles compilation errors, test failures, and timeout scenarios
 * - Calls external LLM (Google AI API) for hint-based feedback
 *
 */
package com.example.backend.student.service;
import com.example.backend.auth.repository.UserRepository;
import com.example.backend.auth.security.EncryptionUtil;
import com.example.backend.student.entity.GradingLog;
import com.example.backend.student.checks.CurlyBracketCheck;
import com.example.backend.student.checks.ViolationCollector;
import com.example.backend.student.repository.GradingLogRepository;
import org.springframework.beans.factory.annotation.Value;

import com.example.backend.professor.entity.TestCase;
import com.example.backend.professor.repository.TestCaseRepository;
import com.example.backend.professor.engine.MemoryJavaFileManager;
import com.example.backend.professor.engine.MemoryJavaSourceFileObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.tools.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import com.example.backend.auth.entity.User;

@Service
public class StudentService {

    //@Value("${togetherAi.api.key}")
    //private String togetherAi;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GradingLogRepository gradingLogRepository;

    @Value("${gradify.compiler.classpath:}")
    private String configuredCompilerClasspath;

    @Value("${gradify.junit.console-jar:}")
    private String junitConsoleJar;

    private String buildCompilerClasspath() {
        List<String> classpathEntries = new ArrayList<>();
        String runtimeClasspath = System.getProperty("java.class.path");
        if (runtimeClasspath != null && !runtimeClasspath.isBlank()) {
            classpathEntries.add(runtimeClasspath);
        }
        if (configuredCompilerClasspath != null && !configuredCompilerClasspath.isBlank()) {
            classpathEntries.add(configuredCompilerClasspath);
        }
        if (junitConsoleJar != null && !junitConsoleJar.isBlank()) {
            classpathEntries.add(junitConsoleJar);
        }
        return String.join(File.pathSeparator, classpathEntries);
    }

    private ClassLoader buildTestClassLoader(ClassLoader parent) throws Exception {
        if (junitConsoleJar == null || junitConsoleJar.isBlank()) {
            return parent;
        }
        URL[] urls = {new File(junitConsoleJar).toURI().toURL()};
        return new URLClassLoader(urls, parent);
    }


    private static class AiSettings {
        private final String apiKey;
        private final String modelName;

        private AiSettings(String apiKey, String modelName) {
            this.apiKey = apiKey;
            this.modelName = modelName;
        }
    }

    /**
     * Helper method to retrieve the currently logged-in student's AI settings.
     * Returns null when key/model are missing.
     */
    private AiSettings getCurrentUserAiSettingsOrNull() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .filter(user -> user.getAiApiKey() != null && !user.getAiApiKey().isBlank())
                .filter(user -> user.getAiModelName() != null && !user.getAiModelName().isBlank())
                .map(user -> new AiSettings(EncryptionUtil.decrypt(user.getAiApiKey()), user.getAiModelName().trim()))
                .orElse(null);
    }



    /**
     * Logs grading activity (with or without AI) to the database.
     */
    private void logGradingAction(String email, TestCase testCase, boolean usedAI, int grade) {
        gradingLogRepository.save(new GradingLog(
                email,
                testCase,
                testCase.getAssignment().getId(),
                testCase.getDifficulty(),
                usedAI,
                grade

        ));
    }


    public Map<String, Object> gradeSingleSubmission(MultipartFile javaFile, Long testCaseId) throws Exception {
        Map<String, Object> grading = runGradingWithoutAI(javaFile, testCaseId);

        String grade = (String) grading.get("grade");
        String note = (String) grading.get("note");

        if ("0".equals(grade) && (
                "Incorrect file name".equals(note) ||
                        "Compilation failed".equals(note) ||
                        "Test timed out".equals(note) ||
                        "Test runner failed".equals(note))) {

            grading.put("aiFeedback",
                    "AI feedback is disabled due to a blocking issue: " + note + ".\n" +
                            "Please fix this issue before requesting AI feedback again.");
            return grading;
        }

        if ("100".equals(grade)) {
            grading.put("aiFeedback",
                    "AI feedback is not necessary because your submission received a perfect score.");
            return grading;
        }

// Get failed test methods
        List<String> failedTests = (List<String>) grading.get("failedTests");

        Map<String, String> feedback = runAIFeedbackOnly(javaFile, testCaseId, failedTests, (String) grading.get("errors"));
        grading.putAll(feedback);

        return grading;

    }


    /**
     * Runs compilation and JUnit execution of student code against test case. Returns structured result.
     * Does not invoke AI.
     */
        public Map<String, Object> runGradingWithoutAI(MultipartFile javaFile, Long testCaseId) throws Exception {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test case not found"));



        // Load test case and student code into strings, removing package statements
        String testCode = removePackageDeclaration(new String(testCase.getFileData()));
        String testClass = extractClassName(testCode);


        String filename = javaFile.getOriginalFilename();
        String studentCode = removePackageDeclaration(new String(javaFile.getBytes()));
        String studentClass = extractClassName(studentCode);

        // Check file naming convention
        if (filename == null || !filename.endsWith(studentClass + ".java")) {
            logGradingAction(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    testCase,
                    false,
                    0

            );

            return Map.of(
                    "grade", "0",
                    "note", "Incorrect file name",
                    "errors", "File name does not match class name: expected " + studentClass + ".java"
            );
        }

        // Set up in-memory Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        MemoryJavaFileManager memFileManager = new MemoryJavaFileManager(fileManager);

        List<JavaFileObject> allFiles = new ArrayList<>();
        allFiles.add(new MemoryJavaSourceFileObject(studentClass, studentCode));
        allFiles.add(new MemoryJavaSourceFileObject("InMemoryTestRunner", getInMemoryTestRunnerCode()));
        allFiles.add(new MemoryJavaSourceFileObject(testClass, testCode));

        // Compile using runtime classpath (+ optional overrides)
        String compilerClasspath = buildCompilerClasspath();
        List<String> options = List.of("-classpath", compilerClasspath);
        JavaCompiler.CompilationTask task = compiler.getTask(null, memFileManager, diagnostics, options, null, allFiles);

        if (!task.call()) {
            logGradingAction(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    testCase,
                    false,
                    0

            );

            return Map.of(
                    "grade", "0",
                    "note", "Compilation failed",
                    "errors", diagnostics.getDiagnostics().toString()
            );
        }


        Map<String, byte[]> compiledClasses = memFileManager.getCompiledClasses();

        // Create dynamic classloader with compiled class bytes + JUnit jar
        ClassLoader memoryClassLoader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = compiledClasses.get(name);
                if (bytes != null) return defineClass(name, bytes, 0, bytes.length);
                return super.findClass(name);
            }
        };


        ClassLoader combinedClassLoader = buildTestClassLoader(memoryClassLoader);

        try {
            Class<?> testClassLoaded = memoryClassLoader.loadClass(testClass);
            Class<?> runnerClass = memoryClassLoader.loadClass("InMemoryTestRunner");
            Method runTests = runnerClass.getMethod("runTestsWithErrors", Class.class);

            // Run tests in separate thread with timeout enforcement
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Object[]> future = executor.submit(() -> {
                Thread.currentThread().setContextClassLoader(combinedClassLoader);
                return (Object[]) runTests.invoke(null, testClassLoaded);
            });

            try {
                Object[] result = future.get(10, TimeUnit.SECONDS);
                long passed = (long) result[0];
                long failed = (long) result[1];
                long total = (long) result[2];
                String failureMessages = (String) result[3];        // descriptive messages
                List<String> failedTestNames = (List<String>) result[4]; // clean names

                int grade = total == 0 ? 0 : (int) Math.round(100.0 * passed / total);
                String note = "Passed: " + passed + ", Total: " + total;

                logGradingAction(
                        SecurityContextHolder.getContext().getAuthentication().getName(),
                        testCase,
                        false,
                        grade
                );

                ViolationCollector styleData = runStyleChecksOnSubmission(filename, studentCode);
                int styleDeduction = (int) Math.round(styleData.getTotalDeduction());
                int finalGrade = Math.max(0, grade - styleDeduction);

                Map<String, Object> response = new HashMap<>();
                response.put("grade", String.valueOf(grade));
                response.put("note", note);
                response.put("errors", failureMessages == null ? "" : failureMessages.trim());
                response.put("failedTests", failedTestNames == null ? List.of() : failedTestNames);
                response.put("styleViolations", styleData.getTotalViolations());
                response.put("styleDeduction", styleDeduction);
                response.put("finalGrade", finalGrade);
                response.put("styleViolationCounts", styleData.getViolationCounts());
                return response;

            } catch (TimeoutException e) {
                future.cancel(true);

                logGradingAction(
                        SecurityContextHolder.getContext().getAuthentication().getName(),
                        testCase,
                        false,
                        0
                );

                return Map.of(
                        "grade", "0",
                        "note", "Test timed out",
                        "errors", "Test execution exceeded time limit"
                );
            } catch (Exception e) {
                future.cancel(true);

                logGradingAction(
                        SecurityContextHolder.getContext().getAuthentication().getName(),
                        testCase,
                        false,
                        0
                );

                return Map.of(
                        "grade", "0",
                        "note", "Test runner failed",
                        "errors", e.getMessage() == null ? "Unexpected test runner error" : e.getMessage()
                );
            } finally {
                executor.shutdownNow();
            }
        } finally {

        }
    }

    private ViolationCollector runBracketCheck(List<File> files) throws Exception {
        Configuration config = ConfigurationLoader.loadConfiguration(
                getClass().getClassLoader().getResource("checkstyle.xml").toString(),
                new PropertiesExpander(System.getProperties()),
                ConfigurationLoader.IgnoredModulesOptions.OMIT
        );

        Checker checker = new Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.configure(config);

        CurlyBracketCheck listener = new CurlyBracketCheck();
        checker.addListener(listener);
        checker.process(files);
        checker.destroy();
        return listener.getViolationData();
    }

    private ViolationCollector runStyleChecksOnSubmission(String filename, String studentCode) throws Exception {
        String safeFilename = (filename == null || filename.isBlank()) ? "StudentSubmission.java" : filename;
        Path tempDir = Files.createTempDirectory("gradify-style-");
        Path tempFile = tempDir.resolve(safeFilename);
        Files.writeString(tempFile, studentCode, StandardCharsets.UTF_8);
        try {
            ViolationCollector collector = runBracketCheck(List.of(tempFile.toFile()));
            collector.setProjectName(safeFilename);
            return collector;
        } finally {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Handles AI-based feedback generation without grading.
     * It verifies the user has an API key, then sends student code and test code
     * to the Google AI API and returns constructive feedback.
     */
    public Map<String, String> runAIFeedbackOnly(MultipartFile javaFile, Long testCaseId,  List<String> failedTests, String failureMessages) throws Exception {
        AiSettings aiSettings = getCurrentUserAiSettingsOrNull();
        if (aiSettings == null) {
            return Map.of("aiFeedback",
                    "You must save both your Google AI API key and model name in AI Settings before using feedback.");
        }

        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test case not found"));

        String testCode = removePackageDeclaration(new String(testCase.getFileData()));
        String studentCode = removePackageDeclaration(new String(javaFile.getBytes()));

        // Identify the failed test methods in full from the test code
        List<String> fullFailedMethods = extractFailedTestMethods(testCode, failedTests);

        // 🟢 DEBUG: print what’s being sent to AI
        System.out.println("\n=== AI INPUT DEBUG ===");
        System.out.println("Failed tests: " + failedTests);
        System.out.println("Failure messages: " + failureMessages);
        System.out.println("Student code:\n" + studentCode);
        System.out.println("Test code:\n" + testCode);
        System.out.println("Full failed methods:\n" + fullFailedMethods);
        System.out.println("======================\n");

        // Use the external LLM to generate feedback
        String feedback = callAIWithKey(studentCode, testCode, aiSettings.apiKey, aiSettings.modelName, fullFailedMethods, failureMessages);


                logGradingAction(
                SecurityContextHolder.getContext().getAuthentication().getName(),
                testCase,
                true,
                -1
        );

        return Map.of("aiFeedback", feedback);
    }


    /**
     * Sends the student code and test code to the Google AI API for feedback generation.
     */
    private String callAIWithKey(String studentCode, String testCode, String apiKey, String modelName, List<String> failedTests, String failureMessages) {
        try {
            // Construct a well-defined system prompt guiding the AI assistant
            String prompt = """
You are an AI teaching assistant.

The student wrote this Java code:
\"\"\"
""" + studentCode + """
\"\"\"

The following JUnit tests failed:
\"\"\"
""" + failedTests + """
\"\"\"

Failure messages:
\"\"\"
""" + failureMessages + """
\"\"\"

Assignment instructions:
1. Implement public static String getMyID() that returns the student's 9-digit ID as a string.
2. Implement public static String getLetterGrade(double) that maps numeric grades to letters using this table:
   A+: ≥ 90, A: 80–89, B+: 75–79, B: 70–74, C+: 65–69, C: 60–64,
   D+: 55–59, D: 50–54, E: 45–49, F: < 45.
3. Implement public static double addAndChangeScale(ArrayList<Double>) that computes the average scaled to 10.
   Example: [90,95,100] → 9.50, [92.5,94,100,100,90] → 9.53.

Instructions:
- Only analyze the student's code versus the failed test methods above.
- Do NOT infer requirements from test names or outside knowledge.
- Do NOT mention passing tests, coding style, or general improvements.
- Focus ONLY on the logic related to the listed failed tests. Ignore unrelated code.
- Do NOT suggest changes to conditions that already exist and work correctly.
- Always explain failures in terms of missing or misplaced numeric ranges (e.g., “Missing condition for 55–59”).
- If a condition or branch is missing, explicitly say so (e.g., “Missing condition for 60–64”).
- Do NOT explain what the test case is testing or reveal expected outputs.
- IMPORTANT: Give at most 3 sentences per failed test, only as many as are actually needed (don’t force extra sentences).

Output format:
For each failed test, write:
Feedback for <methodName>:


Provide concise hints (1–3 sentences each).
""";



            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.0);
            generationConfig.put("topP", 1.0);
            generationConfig.put("maxOutputTokens", 1000);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("contents", contents);
            jsonBody.put("generationConfig", generationConfig);

            String encodedModelName = java.net.URLEncoder.encode(modelName, StandardCharsets.UTF_8);
            String encodedApiKey = java.net.URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                    + encodedModelName + ":generateContent?key=" + encodedApiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            byte[] jsonBytes = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
            connection.getOutputStream().write(jsonBytes);

            int status = connection.getResponseCode();
            Scanner scanner = (status == 200)
                    ? new Scanner(connection.getInputStream()).useDelimiter("\\A")
                    : new Scanner(connection.getErrorStream()).useDelimiter("\\A");

            String response = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            if (status == 200) {
                JSONObject json = new JSONObject(response);
                JSONArray candidates = json.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject candidate = candidates.getJSONObject(0);
                    JSONObject contentObj = candidate.optJSONObject("content");
                    if (contentObj != null) {
                        JSONArray outputParts = contentObj.optJSONArray("parts");
                        if (outputParts != null && outputParts.length() > 0) {
                            return outputParts.getJSONObject(0).optString("text", "").trim();
                        }
                    }
                }
                return "AI feedback generation failed: empty response from Google AI API.";
            }

            return "AI feedback generation failed (" + status + "): " + response;

        } catch (Exception e) {
            return "AI feedback generation failed: " + e.getMessage();
        }
    }



    private String extractClassName(String code) {
        code = code.replaceAll("(?s)/\\*.*?\\*/", "");
        code = code.replaceAll("//.*", "");
        for (String line : code.split("\\n")) {
            line = line.trim();
            if (line.matches(".*\\bclass\\b.*")) {
                String[] tokens = line.split("\\s+");
                for (int i = 0; i < tokens.length - 1; i++) {
                    if (tokens[i].equals("class")) {
                        return tokens[i + 1].split("[\\\\{\\\\(]")[0];
                    }
                }
            }
        }
        throw new IllegalArgumentException("No class name found");
    }

    private String removePackageDeclaration(String code) {
        StringBuilder sb = new StringBuilder();
        for (String line : code.split("\\n")) {
            if (!line.trim().startsWith("package ")) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private String getInMemoryTestRunnerCode() {
        return """
        import org.junit.platform.launcher.Launcher;
        import org.junit.platform.launcher.LauncherDiscoveryRequest;
        import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
        import org.junit.platform.launcher.core.LauncherFactory;
        import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
        import org.junit.platform.engine.discovery.DiscoverySelectors;
        import org.junit.platform.launcher.listeners.TestExecutionSummary;
        import java.util.List;
        import java.util.ArrayList;

        public class InMemoryTestRunner {
            public static Object[] runTestsWithErrors(Class<?> testClass) {
                SummaryGeneratingListener listener = new SummaryGeneratingListener();
                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(testClass))
                        .build();
                Launcher launcher = LauncherFactory.create();
                launcher.registerTestExecutionListeners(listener);
                launcher.execute(request);

                TestExecutionSummary summary = listener.getSummary();
                long passed = summary.getTestsSucceededCount();
                long failed = summary.getTestsFailedCount();
                long total = summary.getTestsFoundCount();

                List<String> failedTestNames = new ArrayList<>();
                List<String> failureMessages = new ArrayList<>();

                summary.getFailures().forEach(failure -> {
                    String methodName = failure.getTestIdentifier().getLegacyReportingName();
                    failedTestNames.add(methodName);
                    failureMessages.add(methodName + ": " + failure.getException().getMessage());
                });



                return new Object[]{
                                passed,
                                failed,
                                total,
                                String.join("\\n", failureMessages), // index 3 → descriptive errors
                                failedTestNames                      // index 4 → clean names
                            };
            }
        }
    """;
    }


    /**
     * Extracts full test method bodies from the test code based on method names that failed.
     */
    private List<String> extractFailedTestMethods(String testCode, List<String> failedNames) {
        List<String> extracted = new ArrayList<>();
        String[] lines = testCode.split("\\n");

        for (String methodName : failedNames) {
            boolean inside = false;
            int braceCount = 0;
            StringBuilder method = new StringBuilder();

            for (String line : lines) {
                if (!inside && line.contains(methodName) && line.contains("void")) {
                    inside = true;
                }

                if (inside) {
                    method.append(line).append("\n");
                    if (line.contains("{")) braceCount++;
                    if (line.contains("}")) braceCount--;
                    if (braceCount == 0 && method.length() > 0) break;
                }
            }

            if (method.length() > 0) {
                extracted.add(method.toString());
            }
        }

        return extracted;
    }











}


