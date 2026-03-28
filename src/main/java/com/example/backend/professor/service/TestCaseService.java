/**
 * Business logic for managing test cases in Gradify
 *
 * This service class handles:
 * - Creating and saving test cases for assignments
 * - Enforcing rules on visibility and difficulty of test cases
 * - Returning all test cases for a given assignment
 *
 * - For *visible* test cases, ensures a difficulty level is set and unique per assignment.
 * - For *hidden* test cases, difficulty is not stored.
 * - Files are stored as raw byte arrays in the database.
 *
 */

package com.example.backend.professor.service;

import com.example.backend.professor.dto.TestCaseDTO;
import com.example.backend.professor.dto.TestCaseUpdateDTO;
import com.example.backend.professor.entity.Assignment;
import com.example.backend.professor.entity.TestCase;
import com.example.backend.professor.repository.AssignmentRepository;
import com.example.backend.professor.repository.TestCaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.util.List;

@Service
public class TestCaseService {
    private final TestCaseRepository testCaseRepository;
    private final AssignmentRepository assignmentRepository;

    public TestCaseService(TestCaseRepository testCaseRepository, AssignmentRepository assignmentRepository) {
        this.testCaseRepository = testCaseRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Save a test case to the database, enforcing visibility and difficulty constraints.
     *
     * @param dto The test case DTO from the frontend form.
     */
    @Transactional
    public void saveTestCase(TestCaseDTO dto) {
        Assignment assignment = assignmentRepository.findById(dto.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + dto.getAssignmentId()));

        boolean visible = dto.isVisibleToStudents();

        if (visible) {
            // Require difficulty for visible test cases
            if (dto.getDifficulty() == null || dto.getDifficulty().isBlank()) {
                throw new IllegalArgumentException("Difficulty is required for visible test cases.");
            }
            // Normalize difficulty to lowercase
            String norm = dto.getDifficulty().trim().toLowerCase();
            // Ensure no duplicate difficulty level exists for this assignment
            boolean exists = testCaseRepository
                    .existsByAssignment_IdAndDifficultyAndVisibleToStudentsTrue(assignment.getId(), norm);
            if (exists) {
                throw new DuplicateVisibleDifficultyException(
                        "Test case with this difficulty already exists for this assignment."
                );
            }
        }

        try {
            TestCase testCase = new TestCase();
            testCase.setAssignment(assignment);
            testCase.setDescription(dto.getDescription());
            if (visible) {
                testCase.setDifficulty(dto.getDifficulty().trim().toLowerCase());
                testCase.setVisibleToStudents(true);
            } else {
                testCase.setDifficulty(null); // hidden: do not store difficulty
                testCase.setVisibleToStudents(false);
            }
            // Convert file to byte[] if uploaded
            testCase.setFileData(dto.getFile() != null ? dto.getFile().getBytes() : null);

            testCaseRepository.save(testCase);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }
    }

    /**
     * Returns all test cases associated with a specific assignment.
     *
     * @param assignmentId Assignment ID to query
     * @return List of TestCase entities
     */
    @Transactional(readOnly = true)
    public List<TestCase> getTestCasesByAssignment(Long assignmentId) {
        return testCaseRepository.findByAssignment_Id(assignmentId);
    }

    @Transactional
    public void updateVisibility(Long testCaseId, boolean visibleToStudents, String difficulty) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

        if (visibleToStudents) {
            String nextDifficulty = difficulty;
            if (nextDifficulty == null || nextDifficulty.isBlank()) {
                nextDifficulty = testCase.getDifficulty();
            }
            if (nextDifficulty == null || nextDifficulty.isBlank()) {
                throw new IllegalArgumentException("Difficulty is required when making a test case visible.");
            }

            String normalized = nextDifficulty.trim().toLowerCase();
            boolean duplicate = testCaseRepository
                    .existsByAssignment_IdAndDifficultyAndVisibleToStudentsTrueAndIdNot(
                            testCase.getAssignment().getId(), normalized, testCaseId);
            if (duplicate) {
                throw new DuplicateVisibleDifficultyException(
                        "Test case with this difficulty already exists for this assignment."
                );
            }

            testCase.setVisibleToStudents(true);
            testCase.setDifficulty(normalized);
        } else {
            testCase.setVisibleToStudents(false);
            if (difficulty != null && !difficulty.isBlank()) {
                testCase.setDifficulty(difficulty.trim().toLowerCase());
            }
        }

        testCaseRepository.save(testCase);
    }

    @Transactional
    public void updateTestCase(Long testCaseId, TestCaseUpdateDTO dto) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

        boolean visible = dto.isVisibleToStudents();
        String normalizedDifficulty = dto.getDifficulty() == null ? null : dto.getDifficulty().trim().toLowerCase();

        if (visible && (normalizedDifficulty == null || normalizedDifficulty.isBlank())) {
            throw new IllegalArgumentException("Difficulty is required for visible test cases.");
        }

        if (visible && normalizedDifficulty != null && !normalizedDifficulty.isBlank()) {
            boolean duplicate = testCaseRepository
                    .existsByAssignment_IdAndDifficultyAndVisibleToStudentsTrueAndIdNot(
                            testCase.getAssignment().getId(), normalizedDifficulty, testCaseId);
            if (duplicate) {
                throw new DuplicateVisibleDifficultyException(
                        "Test case with this difficulty already exists for this assignment."
                );
            }
        }

        testCase.setDescription(dto.getDescription());
        testCase.setVisibleToStudents(visible);
        testCase.setDifficulty((normalizedDifficulty == null || normalizedDifficulty.isBlank()) ? null : normalizedDifficulty);

        try {
            if (dto.getFile() != null && !dto.getFile().isEmpty()) {
                testCase.setFileData(dto.getFile().getBytes());
            }
            testCaseRepository.save(testCase);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }
    }

    @Transactional
    public void deleteTestCase(Long testCaseId) {
        if (!testCaseRepository.existsById(testCaseId)) {
            throw new IllegalArgumentException("Test case not found: " + testCaseId);
        }
        testCaseRepository.deleteById(testCaseId);
    }

    /**
     * Custom exception thrown when a visible test case with a duplicate difficulty is added.
     */
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateVisibleDifficultyException extends RuntimeException {
        public DuplicateVisibleDifficultyException(String msg) { super(msg); }
    }
}
