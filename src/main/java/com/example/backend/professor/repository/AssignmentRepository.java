/**
 * Repository interface for managing Assignment entities.
 * Provides methods to retrieve assignments by course ID.
 * Used for CRUD operations on assignments, especially in relation to their associated courses.
 */

package com.example.backend.professor.repository;

import com.example.backend.professor.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    // Allows fetching all assignments related to a specific course by its ID
    List<Assignment> findByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);

}

