/**
 * This controller handles professor-side operations for managing assignments.
 * It provides REST endpoints for:
 * - Adding a new assignment to a course
 * - Fetching all assignments
 * - Fetching assignments belonging to a specific course
 *
 */
package com.example.backend.professor.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.professor.dto.AssignmentDTO;
import com.example.backend.professor.entity.Assignment;
import com.example.backend.professor.entity.Course;
import com.example.backend.professor.repository.AssignmentRepository;
import com.example.backend.professor.repository.CourseRepository;


@RestController
@RequestMapping({"/professor/assignments", "/api/professor/assignments"})
@CrossOrigin(origins = "*")
public class AssignmentController {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Adds a new assignment to a given course.
     * Receives an AssignmentDTO (name, type, courseId) from the frontend
     */
    @PostMapping
    public Assignment addAssignment(@RequestBody AssignmentDTO dto) {
        // Check if course exists
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Create the Assignment entity and fill in the data from the DTO
        Assignment assignment = new Assignment();
        assignment.setName(dto.getName());
        assignment.setType(dto.getType());
        assignment.setCourse(course); // Link assignment to its parent course

        // Save it to database
        return assignmentRepository.save(assignment);
    }


    /**
     * Returns a list of all assignments that belong to a given course
     * Used to populate the assignment dropdown after a course is selected
     */
    @GetMapping("/by-course/{courseId}")
    public List<Assignment> getAssignmentsByCourse(@PathVariable Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }

    @GetMapping
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

}
