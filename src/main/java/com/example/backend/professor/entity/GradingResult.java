/**
 * Entity representing the grading outcome for a student on a particular assignment.
 * Stores the assignment ID, student ID, numeric grade, and a feedback note (e.g., error info or status).
 * Used to persist grading results into the database for reporting and download.
 */

package com.example.backend.professor.entity;

import jakarta.persistence.*;

@Entity
public class GradingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long assignmentId;
    private String studentId;
    private int grade;

    @Column(length = 500)
    private String note;

    // Constructors
    public GradingResult() {}

    public GradingResult(Long assignmentId, String studentId, int grade, String note) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.grade = grade;
        this.note = note;
    }

    // Getters and setters
    public Long getId() { return id; }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
