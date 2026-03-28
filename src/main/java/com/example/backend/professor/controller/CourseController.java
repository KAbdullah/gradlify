/**
 * This controller handles course-related operations from the professor's side.
 * It exposes endpoints for:
 * - Creating a new course
 * - Retrieving all available courses
 *
 */

package com.example.backend.professor.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.professor.entity.Assignment;
import com.example.backend.professor.entity.Course;
import com.example.backend.professor.repository.AssignmentRepository;
import com.example.backend.professor.repository.CourseRepository;
import com.example.backend.professor.repository.GradingResultRepository;
import com.example.backend.professor.repository.TestCaseRepository;
import com.example.backend.student.repository.StudentCourseRepository;

@RestController
@RequestMapping({"/professor/courses", "/api/professor/courses"})
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private GradingResultRepository gradingResultRepository;

    @Autowired
    private StudentCourseRepository studentCourseRepository;


    //  Endpoint to create new course
    @PostMapping
    public ResponseEntity<?> addCourse(@RequestBody Course course) {
        if (course.getCode() == null || course.getCode().isBlank()) {
            return ResponseEntity.badRequest().body("Course code is required.");
        }

        String normalizedCode = course.getCode().trim().toUpperCase();
        if (courseRepository.findByCodeIgnoreCase(normalizedCode).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Course code already exists.");
        }

        course.setCode(normalizedCode);
        course.setHidden(false); // Default to visible when created
        return ResponseEntity.status(HttpStatus.CREATED).body(courseRepository.save(course));
    }


    /**
     * Endpoint to return all courses in the system
     * Used for displaying a course selection dropdown in UI
     */
    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @PatchMapping("/{courseId}/hidden")
    public ResponseEntity<?> updateCourseHidden(
            @PathVariable Long courseId,
            @RequestBody Map<String, Boolean> payload
    ) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found.");
        }

        boolean hidden = Boolean.TRUE.equals(payload.get("hidden"));
        course.setHidden(hidden);
        courseRepository.save(course);
        return ResponseEntity.ok(course);
    }

    @DeleteMapping("/{courseId}")
    @Transactional
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found.");
        }

        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
        for (Assignment assignment : assignments) {
            testCaseRepository.deleteByAssignment_Id(assignment.getId());
            gradingResultRepository.deleteByAssignmentId(assignment.getId());
        }

        assignmentRepository.deleteByCourseId(courseId);
        studentCourseRepository.deleteByCourse_Id(courseId);
        courseRepository.deleteById(courseId);

        return ResponseEntity.ok("Course and associated data deleted.");
    }

}

