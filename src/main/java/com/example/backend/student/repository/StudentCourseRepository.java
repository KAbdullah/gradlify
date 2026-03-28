/**
 * Repository for managing the mapping between students and the courses they are enrolled in.
 */

package com.example.backend.student.repository;

import com.example.backend.student.entity.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {

    /**
     * Retrieves all courses a student is enrolled in based on their email.
     */
    List<StudentCourse> findByStudentEmail(String email);

    boolean existsByStudentEmailAndCourse_Id(String studentEmail, Long courseId);

    void deleteByCourse_Id(Long courseId);
}
