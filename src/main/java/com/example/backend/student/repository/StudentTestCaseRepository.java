/**
 * Repository for fetching test cases that students are allowed to view or use.
 * This repository is filtered to ensure only public (visibleToStudents = true) test cases are shown when appropriate.
 */

package com.example.backend.student.repository;

import com.example.backend.professor.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("studentTestCaseRepository")
public interface StudentTestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByAssignment_Id(Long assignmentId);
    List<TestCase> findByAssignment_IdAndVisibleToStudentsTrue(Long assignmentId);

    boolean existsByAssignment_IdAndDifficultyAndVisibleToStudentsTrue(Long assignmentId, String difficulty);
}


