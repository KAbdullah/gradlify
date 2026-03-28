/**
 * Represents an assignment entity that belongs to a specific course.
 * Each assignment has a name and type (e.g., lab, project, quiz) and is linked to one Course.
 * Used for managing assignment-level test cases, grading, and reports.
 */

package com.example.backend.professor.entity;

import jakarta.persistence.*;

@Entity
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    // Constructors
    public Assignment() {
    }

    public Assignment(String name, String type, Course course) {
        this.name = name;
        this.type = type;
        this.course = course;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
