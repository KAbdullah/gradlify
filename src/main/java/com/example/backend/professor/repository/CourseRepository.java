/**
 * Repository interface for managing Course entities.
 * Supports standard CRUD operations for courses.
 * Courses serve as containers for assignments in the system.
 */

package com.example.backend.professor.repository;

import com.example.backend.professor.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCodeIgnoreCase(String code);
}
