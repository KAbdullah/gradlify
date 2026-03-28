/**
 * ProfGradeService is the core service responsible for grading student submissions against JUnit-based test cases.
 * This service handles:
 * - Uploading and validating multiple student `.java` files organized by student ID.
 * - Removing package declarations and verifying class names match filenames.
 * - Compiling student code and associated test cases in memory.
 * - Running JUnit tests using an in-memory runner (`InMemoryTestRunner`) with a timeout.
 * - Recording the results (grade and diagnostic note) into the `GradingResult` database table.
 * - Generating a detailed CSV grading report, including summary statistics.
 *
 */

package com.example.backend.professor.service;

import com.example.backend.professor.entity.GradingResult;
import com.example.backend.professor.entity.TestCase;
import com.example.backend.professor.repository.GradingResultRepository;
import com.example.backend.professor.repository.TestCaseRepository;
import com.example.backend.professor.engine.MemoryJavaFileManager;
import com.example.backend.professor.engine.MemoryJavaSourceFileObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.tools.*;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.*;

@Service
public class ProfGradeService {

    public ProfGradeService(TestCaseRepository testCaseRepository, GradingResultRepository gradingResultRepository) {
        this.testCaseRepository = testCaseRepository;
        this.gradingResultRepository = gradingResultRepository;
    }

    private final TestCaseRepository testCaseRepository;
    private final GradingResultRepository gradingResultRepository;

    @Value("${gradify.compiler.classpath:}")
    private String configuredCompilerClasspath;

    @Value("${gradify.junit.console-jar:}")
    private String junitConsoleJar;

    private static String gradingResultsPath;
    public static String getGradingResultsPath() { return gradingResultsPath; }

    private static String gradingFileName;
    public static String getGradingFileName() { return gradingFileName; }

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



    /**
     * Grades a zip file containing multiple student folders.
     * Each folder should include the student's .java files.
     */
    @Transactional
    public Map<String, String> runGradingOnFolder(MultipartFile[] studentFiles, Long testCaseId, String fileName, List<String> overwriteList) throws Exception {
        System.out.println("========== Starting Grading ==========");
        System.out.println("Received " + studentFiles.length + " files for grading");

        // Initialize CSV output for grading summary
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Student ID,Grade,Note\n");

        // Retrieve test case and linked assignment ID
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test case not found"));

        Long assignmentId = testCase.getAssignment().getId();


        String rawTestCode = new String(testCase.getFileData());
        String testCode = removePackageDeclaration(rawTestCode);
        String testClass = extractClassName(testCode);
        System.out.println("Test case loaded: class = " + testClass);

        // Group student files by student folder name based on folder structure
        Map<String, List<MultipartFile>> studentGroupedFiles = new LinkedHashMap<>();
        for (MultipartFile file : studentFiles) {
            String path = file.getOriginalFilename();
            if (path == null || !path.contains("/")) continue;
            String studentId = path.split("/")[1];
            if (!path.endsWith(".java")) continue;
            studentGroupedFiles.computeIfAbsent(studentId, k -> new ArrayList<>()).add(file);
        }

        int timeoutCount = 0;
        int compilationErrorCount = 0;
        int incorrectFileNameCount = 0;
        int gradedSuccessfully = 0;
        int totalGradeSum = 0;
        int allGradeSum = 0;

        // Grade each student
        for (Map.Entry<String, List<MultipartFile>> entry : studentGroupedFiles.entrySet()) {
            String studentId = entry.getKey();

            // 🔑 Check if this student already has a grade for this assignment
            Optional<GradingResult> existing = gradingResultRepository
                    .findByAssignmentIdAndStudentId(assignmentId, studentId);

            if (existing.isPresent() && !overwriteList.contains(studentId)) {
                // Skip this student (not selected for overwrite)
                System.out.println("Skipping " + studentId + " (already graded, not selected for overwrite)");
                continue;
            }

            if (existing.isPresent() && overwriteList.contains(studentId)) {
                // Overwrite mode → delete old result
                gradingResultRepository.delete(existing.get());
                System.out.println("Overwriting grade for " + studentId);
            }




            List<MultipartFile> files = entry.getValue();
            System.out.println("\n--- Grading Student: " + studentId + " ---");

            // Setup in-memory compilation tools
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            MemoryJavaFileManager memFileManager = new MemoryJavaFileManager(fileManager);

            List<JavaFileObject> allFiles = new ArrayList<>();
            boolean hasIncorrectFileName = false;

            // Preprocess and validate file names against class names
            for (MultipartFile file : files) {
                String filename = file.getOriginalFilename();
                String rawCode = new String(file.getBytes());
                String code = removePackageDeclaration(rawCode);
                try {
                    String className = extractClassName(code);
                    if (!filename.endsWith(className + ".java")) {
                        System.out.println("File name mismatch: " + filename + " does not match class " + className);
                        hasIncorrectFileName = true;
                        continue;
                    }
                    System.out.println("Compiling class: " + className);



                    allFiles.add(new MemoryJavaSourceFileObject(className, code));
                } catch (IllegalArgumentException ignored) {}
            }

            allFiles.add(new MemoryJavaSourceFileObject("InMemoryTestRunner", getInMemoryTestRunnerCode()));
            allFiles.add(new MemoryJavaSourceFileObject(testClass, testCode));
            System.out.println("Added test class: " + testClass);

            if (hasIncorrectFileName || allFiles.size() <= 2) {
                String note = hasIncorrectFileName ? "Incorrect file name" : "No valid Java files";
                csvBuilder.append(studentId).append(",0,\"").append(note).append("\"\n");
                saveResult(assignmentId, studentId, 0, note);
                if (hasIncorrectFileName) incorrectFileNameCount++;
                continue;
            }

            // Compile student + test files in memory
            String compilerClasspath = buildCompilerClasspath();
            List<String> options = List.of("-classpath", compilerClasspath);
            JavaCompiler.CompilationTask task = compiler.getTask(null, memFileManager, diagnostics, options, null, allFiles);
            if (!task.call()) {
                compilationErrorCount++;
                System.out.println("Compilation failed for student: " + studentId);
                diagnostics.getDiagnostics().forEach(d -> System.out.println("  [Compiler] " + d));
                csvBuilder.append(studentId).append(",0,\"Compilation failed\"\n");
                saveResult(assignmentId, studentId, 0, "Compilation failed");
                continue;
            }

            // Prepare custom classloader for test execution
            Map<String, byte[]> compiledClasses = memFileManager.getCompiledClasses();

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
                // Reflectively load test runner and invoke JUnit
                Class<?> testClassLoaded = memoryClassLoader.loadClass(testClass);
                Class<?> runnerClass = memoryClassLoader.loadClass("InMemoryTestRunner");
                Method runTests = runnerClass.getMethod("runTests", Class.class);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<long[]> future = executor.submit(() -> {
                    Thread.currentThread().setContextClassLoader(combinedClassLoader);
                    return (long[]) runTests.invoke(null, testClassLoaded);
                });

                try {
                    // Collect test result (passed, failed, total)
                    long[] result = future.get(10, TimeUnit.SECONDS);
                    long passed = result[0];
                    long failed = result[1];
                    long total = result[2];

                    int grade = total == 0 ? 0 : (int) Math.round(100.0 * passed / total);
                    String note = "Passed: " + passed + ", Failed: " + failed;

                    System.out.println("Test result: Passed=" + passed + ", Failed=" + failed + ", Grade=" + grade);
                    csvBuilder.append(studentId).append(",").append(grade).append(",\"").append(note).append("\"\n");
                    saveResult(assignmentId, studentId, grade, note);

                    gradedSuccessfully++;
                    totalGradeSum += grade;
                    allGradeSum += grade;
                } catch (TimeoutException e) {
                    future.cancel(true);
                    timeoutCount++;
                    System.out.println("⏰ Test timeout for student: " + studentId);
                    csvBuilder.append(studentId).append(",0,\"Test timed out\"\n");
                    saveResult(assignmentId, studentId, 0, "Test timed out");
                } catch (Exception e) {
                    future.cancel(true);
                    timeoutCount++;
                    System.out.println("Error during test execution for student: " + studentId + " -> " + e.getMessage());
                    e.printStackTrace();
                    csvBuilder.append(studentId).append(",0,\"Error during test execution\"\n");
                    saveResult(assignmentId, studentId, 0, "Error during test execution");
                } finally {
                    executor.shutdownNow();
                }
            } catch (Exception e) {
                timeoutCount++;
                System.out.println("Unexpected error grading student " + studentId + ": " + e.getMessage());
                e.printStackTrace();
                csvBuilder.append(studentId).append(",0,\"Unexpected error\"\n");
                saveResult(assignmentId, studentId, 0, "Unexpected error");
            }



        }

        // Append grading summary statistics to CSV output
        csvBuilder.append("\nSummary\n");
        csvBuilder.append("Total students processed,").append(studentGroupedFiles.size()).append("\n");
        csvBuilder.append("Successfully graded,").append(gradedSuccessfully).append("\n");
        csvBuilder.append("Compilation errors,").append(compilationErrorCount).append("\n");
        csvBuilder.append("Incorrect file name errors,").append(incorrectFileNameCount).append("\n");
        csvBuilder.append("Timeouts / test crashes,").append(timeoutCount).append("\n");
        csvBuilder.append(String.format("Average grade (successful only),%.2f\n", gradedSuccessfully == 0 ? 0 : (double) totalGradeSum / gradedSuccessfully));
        csvBuilder.append(String.format("Average grade (including all),%.2f\n", studentGroupedFiles.size() == 0 ? 0 : (double) allGradeSum / studentGroupedFiles.size()));

        // Write CSV to temporary file for frontend download
        fileName = fileName.trim().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        if (!fileName.toLowerCase().endsWith(".csv")) fileName += ".csv";
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(csvBuilder.toString());
        }

        gradingResultsPath = tempFile.getAbsolutePath();
        gradingFileName = fileName;
        return Map.of("Message", "Grading complete. Download the CSV.");
    }

    /**
    Extracts the class name from a given Java source code string.
     It removes both block and line comments and then looks for the `class` keyword.
     */
    private String extractClassName(String code) {
        // Remove block comments, line comments, and normalize line endings.
        code = code.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//.*", "").replace("\r\n", "\n").replace("\r", "\n");
        // Iterate through lines to find the one that contains the "class" keyword
        for (String line : code.split("\n")) {
            line = line.trim();if (line.matches(".*\\bclass\\b.*")) { // Look for the keyword "class" as a word (not part of another word)
                String[] tokens = line.split("\\s+");
                for (int i = 0; i < tokens.length - 1; i++) {
                    if (tokens[i].equals("class")) {
                        String className = tokens[i + 1];
                        // Handle class names like `MyClass {` by trimming after the `{`
                        if (className.contains("{")) className = className.substring(0, className.indexOf("{"));
                        // Skip things like "class MyClass(...) {"
                        if (className.contains("(")) continue;
                        return className;
                    }
                }
            }
        }
        throw new IllegalArgumentException("No class name found in the source code.");
    }

    /**
     * Removes the package declaration from a Java source file string.
     * Useful for compiling multiple classes together in memory without package conflicts.
     */
    private String removePackageDeclaration(String code) {
        code = code.replace("\r\n", "\n").replace("\r", "\n");
        StringBuilder sb = new StringBuilder();
        // Skip any line that starts with "package"
        for (String line : code.split("\n")) {
            if (line.trim().startsWith("package ")) continue;
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     *  Saves the result of a student's grading attempt into the database.
     *  Stores assignment ID, student ID, numeric grade, and grading note (e.g., errors).
     */
    private void saveResult(Long assignmentId, String studentId, int grade, String note) {
        gradingResultRepository.save(new GradingResult(assignmentId, studentId, grade, note));
    }

    /**
     * Checks if grading results already exist for a given test case (by its ID).
     * Looks up the associated assignment and verifies presence in the GradingResult table.
     */
    public boolean gradesExistForAssignment(Long testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test case not found"));
        // Check if any grades exist for the associated assignment
        return gradingResultRepository.existsByAssignmentId(testCase.getAssignment().getId());
    }

    /**
     * Returns the source code for a helper class called `InMemoryTestRunner`.
     * This class uses JUnit's launcher API to dynamically run tests inside memory-loaded test classes.
     * It is injected into the compilation unit along with student code and the actual test class.
     */
    private String getInMemoryTestRunnerCode() {
        return """
        import org.junit.platform.launcher.Launcher;
        import org.junit.platform.launcher.LauncherDiscoveryRequest;
        import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
        import org.junit.platform.launcher.core.LauncherFactory;
        import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
        import org.junit.platform.engine.discovery.DiscoverySelectors;

        public class InMemoryTestRunner {
            public static long[] runTests(Class<?> testClass) {
                SummaryGeneratingListener listener = new SummaryGeneratingListener();
                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(testClass))
                        .build();
                Launcher launcher = LauncherFactory.create();
                launcher.registerTestExecutionListeners(listener);
                launcher.execute(request);
                long passed = listener.getSummary().getTestsSucceededCount();
                long failed = listener.getSummary().getTestsFailedCount();
                long total = listener.getSummary().getTestsFoundCount();
                return new long[]{passed, failed, total};
            }
        }
    """;
    }

}
