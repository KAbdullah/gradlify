/**
 * Repository interface for managing GradingResult entities.
 * Includes custom queries to compute averages, check existence, and delete results
 * related to specific assignments. Used extensively in instructor reports and grading analytics.
 */

package com.example.backend.professor.repository;

import com.example.backend.professor.entity.GradingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradingResultRepository extends JpaRepository<GradingResult, Long> {

    /**
     * Deletes all grading results associated with a specific assignment ID.
     * Useful for cleanup when an assignment is deleted or regraded.
     */
    void deleteByAssignmentId(Long assignmentId);

    /**
     * Checks if any grading results exist for a given assignment ID.
     * Often used to prevent duplicate grading or determine if an assignment has been graded.
     */
    boolean existsByAssignmentId(Long assignmentId);


    /**
     * Computes the average grade for each assignment in the provided list of assignment IDs.
     * Returns a list of Object arrays, where each element contains:
     * [0] = assignmentId (Long)
     * [1] = averageGrade (Double)
     */
    @Query("SELECT gr.assignmentId, AVG(gr.grade) FROM GradingResult gr WHERE gr.assignmentId IN :assignmentIds GROUP BY gr.assignmentId")
    List<Object[]> findAveragesByAssignmentIds(@Param("assignmentIds") List<Long> assignmentIds);

    /**
     * Retrieves all grading results for a list of assignment IDs.
     * Used when generating summaries or exporting detailed grading records.
     */
    List<GradingResult> findByAssignmentIdIn(List<Long> assignmentIds);    List<GradingResult> findByAssignmentId(Long assignmentId);

    //Check if a certain assignment was graded for a specific student
    Optional<GradingResult> findByAssignmentIdAndStudentId(Long assignmentId, String studentId);








}
