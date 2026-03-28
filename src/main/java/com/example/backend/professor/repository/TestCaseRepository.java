/**
 * Repository interface for managing TestCase entities.
 * Provides methods to fetch test cases by assignment,
 * check for visibility-restricted duplicates, and retrieve only visible test cases for students.
 * Used in test case management, grading, and student view logic.
 */

package com.example.backend.professor.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.professor.entity.TestCase;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    // Get all test cases for a specific assignment by its ID
    List<TestCase> findByAssignment_Id(Long assignmentId);
    void deleteByAssignment_Id(Long assignmentId);
    boolean existsByAssignment_IdAndDifficultyAndVisibleToStudentsTrue(Long assignmentId, String difficulty);
    boolean existsByAssignment_IdAndDifficultyAndVisibleToStudentsTrueAndIdNot(Long assignmentId, String difficulty, Long id);
    // Get only test cases visible to students for a specific assignment
    List<TestCase> findByAssignment_IdAndVisibleToStudentsTrue(Long assignmentId);
    // Get only test cases visible to students for a specific assignment and order by difficulty
    @Query("SELECT t FROM TestCase t " +
            "WHERE t.assignment.id = :assignmentId AND t.visibleToStudents = true " +
            "ORDER BY CASE LOWER(t.difficulty) " +
            "  WHEN 'easy' THEN 1 " +
            "  WHEN 'intermediate' THEN 2 " +
            "  WHEN 'advanced' THEN 3 " +
            "  ELSE 4 END")
    List<TestCase> findVisibleTestCasesOrdered(@Param("assignmentId") Long assignmentId);



}
