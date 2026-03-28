/**
 * Entity that links students (by email) to the courses they are enrolled in.
 * Used to restrict assignment visibility and handle submissions by enrolled students.
 */

package com.example.backend.student.entity;

import com.example.backend.professor.entity.Course;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class StudentCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String studentEmail;

    @ManyToOne
    private Course course;

    public StudentCourse() {}

    public StudentCourse(String studentEmail, Course course) {
        this.studentEmail = studentEmail;
        this.course = course;
    }

    public Long getId() {
        return id;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
