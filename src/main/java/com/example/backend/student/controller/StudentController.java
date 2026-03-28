/**
 * REST controller for student-facing APIs in Gradify
 *
 * Responsibilities:
 * - Submit code for grading and/or AI feedback
 * - View assignments, courses, and test cases
 * - Manage user-specific data (e.g., API key)
 *
 * Endpoints:
 * - POST /student/submit         → Submit code for grading and feedback
 * - POST /student/grade          → Run grading only, no AI
 * - POST /student/feedback       → Run AI feedback only (no grading if critical error)
 * - GET /student/courses         → List all courses the logged-in student is enrolled in
 * - GET /student/by-course/{id}  → Get assignments for a course
 * - GET /student/assignments     → List all assignments (admin use)
 * - GET /student/by-assignment/{id} → Get visible test cases for assignment
 * - GET /student/me/api-key      → Show whether AI API key is set
 * - PUT /student/me/api-key      → Add or remove AI API key for student
 */

package com.example.backend.student.controller;

import com.example.backend.auth.entity.User;
import com.example.backend.auth.repository.UserRepository;
import com.example.backend.auth.security.EncryptionUtil;
import com.example.backend.professor.entity.Assignment;
import com.example.backend.professor.entity.Course;
import com.example.backend.professor.entity.TestCase;
import com.example.backend.professor.repository.AssignmentRepository;
import com.example.backend.professor.repository.CourseRepository;
import com.example.backend.professor.repository.TestCaseRepository;
import com.example.backend.professor.service.TestCaseService;
import com.example.backend.student.dto.ApiKeyDTO;
import com.example.backend.student.entity.StudentCourse;
import com.example.backend.student.repository.StudentCourseRepository;
import com.example.backend.student.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping({"/student", "/api/student"})
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentCourseRepository studentCourseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private UserRepository userRepository;

    private final TestCaseService testCaseService;

    public StudentController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    /**
     * Endpoint: Submit code for grading + feedback.
     * Returns a map with `grade` and `note`.
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitCode(
            @RequestParam("file") MultipartFile javaFile,
            @RequestParam("testCaseId") Long testCaseId
    ) {
        try {
            Map<String, Object> result = studentService.gradeSingleSubmission(javaFile, testCaseId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint: Grade a submission WITHOUT running AI feedback.
     * Useful for quickly testing logic correctness.
     */
    @PostMapping("/grade")
    public ResponseEntity<Map<String, Object>> runGradingOnly(
            @RequestParam("file") MultipartFile javaFile,
            @RequestParam("testCaseId") Long testCaseId
    ) {
        try {
            Map<String, Object> result = studentService.runGradingWithoutAI(javaFile, testCaseId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * Endpoint: Run AI feedback ONLY (no grading)
     * Only allowed if:
     *   - grade < 100
     *   - no blocking errors (e.g., compilation failure, timeout, etc.)
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, String>> runFeedbackOnly(
            @RequestParam("file") MultipartFile javaFile,
            @RequestParam("testCaseId") Long testCaseId
    ) {
        try {
            Map<String, Object> gradingResult = studentService.runGradingWithoutAI(javaFile, testCaseId);

            String grade = (String) gradingResult.get("grade");
            String note = (String) gradingResult.get("note");

            // Block AI if there are critical errors (bad filename, compile error, timeout, etc.)
            if ("0".equals(grade) && (
                    "Incorrect file name".equals(note) ||
                            "Compilation failed".equals(note) ||
                            "Test timed out".equals(note) ||
                            "Test runner failed".equals(note))) {
                return ResponseEntity.ok(Map.of(
                        "aiFeedback", "AI feedback is disabled due to a blocking issue: " + note +
                                ".\nPlease fix this issue before requesting AI feedback again."
                ));
            }

            // Block AI if perfect grade
            if ("100".equals(grade)) {
                return ResponseEntity.ok(Map.of(
                        "aiFeedback", "Your grade is already 100%. No feedback necessary!"
                ));
            }

            // Proceed with AI feedback using failed test methods
            List<String> failedMethods = (List<String>) gradingResult.get("failedTests");
            String failureMessages = (String) gradingResult.get("errors");
            Map<String, String> result = studentService.runAIFeedbackOnly(
                    javaFile,
                    testCaseId,
                    failedMethods,
                    failureMessages
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("aiFeedback", "Error: " + e.getMessage()));
        }
    }


    /**
     * Endpoint: Get all courses that the logged-in student is enrolled in.
     */
    @GetMapping("/courses")
    public List<Course> getStudentCourses() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<StudentCourse> links = studentCourseRepository.findByStudentEmail(email);
        return links.stream()
                .map(StudentCourse::getCourse)
                .filter(course -> !course.isHidden())
                .toList();
    }

    @PostMapping("/courses/enroll")
    public ResponseEntity<?> enrollByCourseCode(@RequestBody Map<String, String> payload) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String courseCode = payload.getOrDefault("courseCode", "").trim();

        if (courseCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Course code is required."));
        }

        Course course = courseRepository.findByCodeIgnoreCase(courseCode)
                .orElse(null);

        if (course == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid course code."));
        }

        if (course.isHidden()) {
            return ResponseEntity.badRequest().body(Map.of("error", "This course is currently hidden."));
        }

        if (studentCourseRepository.existsByStudentEmailAndCourse_Id(email, course.getId())) {
            return ResponseEntity.ok(Map.of("message", "You are already enrolled in this course."));
        }

        studentCourseRepository.save(new StudentCourse(email, course));
        return ResponseEntity.ok(Map.of("message", "Successfully enrolled in course."));
    }


    /**
     * Endpoint: Get all assignments for a specific course.
     */
    @GetMapping("/by-course/{courseId}")
    public List<Assignment> getAssignmentsByCourse(@PathVariable Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }

    /**
     * Endpoint: Get all assignments (for admin/test use).
     */
    @GetMapping("/assignments")
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    /**
     * Endpoint: Get all test cases that are visible to students for an assignment.
     */
    @GetMapping("/by-assignment/{assignmentId}")
    public ResponseEntity<List<TestCase>> getVisibleTestCases(@PathVariable Long assignmentId) {
        List<TestCase> cases = testCaseRepository.findVisibleTestCasesOrdered(assignmentId);
        return ResponseEntity.ok(cases);
    }

    /**
     * Endpoint: Check if logged-in student has AI settings saved.
     * Does NOT return the actual API key; just status flags and configured model name.
     */
    @GetMapping("/me/api-key")
    public Map<String, Object> getMyApiKey() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByUsername(username).orElseThrow();
        boolean hasKey = u.getAiApiKey() != null && !u.getAiApiKey().isBlank();
        boolean hasModel = u.getAiModelName() != null && !u.getAiModelName().isBlank();

        return Map.of(
                "hasKey", hasKey,
                "hasModel", hasModel,
                "modelName", hasModel ? u.getAiModelName() : "",
                "masked", hasKey ? "••••••••" : ""
        );
    }

    /**
     * Endpoint: Update or clear student's saved AI API key and model.
     * Encrypts key before storing. Setting blank/null clears values.
     */
    @PutMapping("/me/api-key")
    public ResponseEntity<?> updateMyApiKey(@RequestBody ApiKeyDTO body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByUsername(username).orElseThrow();

        String newKey = body.getApiKey();
        if (newKey == null || newKey.isBlank()) {
            u.setAiApiKey(null);
        } else {
            u.setAiApiKey(EncryptionUtil.encrypt(newKey.trim()));
        }

        String newModel = body.getModelName();
        if (newModel == null || newModel.isBlank()) {
            u.setAiModelName(null);
        } else {
            u.setAiModelName(newModel.trim());
        }

        userRepository.save(u);

        return ResponseEntity.ok(Map.of("ok", true));
    }


}
