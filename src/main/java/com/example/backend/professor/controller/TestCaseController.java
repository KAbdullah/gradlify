/**
 * Controller for managing test cases in Gradify.
 *
 * Professors use this controller to:
 *
 * - Upload new JUnit test cases for a specific assignment
 * - Retrieve all test cases associated with a specific assignment
 *
 * Business logic is handled by the TestCaseService.
 */

package com.example.backend.professor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.professor.dto.TestCaseDTO;
import com.example.backend.professor.dto.TestCaseUpdateDTO;
import com.example.backend.professor.dto.TestCaseVisibilityUpdateDTO;
import com.example.backend.professor.entity.TestCase;
import com.example.backend.professor.service.TestCaseService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping({"/professor/testcases", "/api/professor/testcases"})
    public class TestCaseController{
        private final TestCaseService testCaseService;


        public TestCaseController(TestCaseService testCaseService) {
            this.testCaseService = testCaseService;
        }

    /**
     * Endpoint to upload a new test case for an assignment.
     *
     * Accepts a multipart form via `TestCaseDTO` (assignmentId, difficulty, description, and file).
     *
     * Enforces one visible test case per difficulty level per assignment.
     *
     * @param dto TestCaseDTO containing metadata and uploaded file
     * @return 200 OK on success, 409 CONFLICT if duplicate difficulty, or 400 BAD REQUEST on error
     */
    @PostMapping
    public ResponseEntity<?> uploadTestCase(@ModelAttribute TestCaseDTO dto) {
        try {
            testCaseService.saveTestCase(dto);
            return ResponseEntity.ok("Test case uploaded successfully.");
        } catch (TestCaseService.DuplicateVisibleDifficultyException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Test case with this difficulty already exists for this assignment.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    /**
     * Endpoint to retrieve all test cases for a given assignment ID.
     *
     * Used by the UI to populate test case dropdowns or management views.
     *
     * @param assignmentId ID of the assignment
     * @return List of TestCase objects linked to the given assignment
     */
    @PatchMapping("/{testCaseId}/visibility")
    public ResponseEntity<?> updateVisibility(
            @PathVariable Long testCaseId,
            @RequestBody TestCaseVisibilityUpdateDTO dto
    ) {
        try {
            testCaseService.updateVisibility(testCaseId, dto.isVisibleToStudents(), dto.getDifficulty());
            return ResponseEntity.ok("Visibility updated.");
        } catch (TestCaseService.DuplicateVisibleDifficultyException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Test case with this difficulty already exists for this assignment.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/by-assignment/{assignmentId}")
    public ResponseEntity<List<TestCase>> getTestCasesByAssignment(@PathVariable Long assignmentId) {
        List<TestCase> cases = testCaseService.getTestCasesByAssignment(assignmentId);
        return ResponseEntity.ok(cases);
    }

    @PutMapping("/{testCaseId}")
    public ResponseEntity<?> updateTestCase(
            @PathVariable Long testCaseId,
            @ModelAttribute TestCaseUpdateDTO dto
    ) {
        try {
            testCaseService.updateTestCase(testCaseId, dto);
            return ResponseEntity.ok("Test case updated.");
        } catch (TestCaseService.DuplicateVisibleDifficultyException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Test case with this difficulty already exists for this assignment.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{testCaseId}")
    public ResponseEntity<?> deleteTestCase(@PathVariable Long testCaseId) {
        try {
            testCaseService.deleteTestCase(testCaseId);
            return ResponseEntity.ok("Test case deleted.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
