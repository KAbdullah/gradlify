/**
 * Entity that logs each grading event triggered by a student.
 * This includes information about the test case used, whether AI was involved,
 * what grade was received, and a timestamp for when it occurred.
 */

package com.example.backend.student.entity;

import com.example.backend.professor.entity.TestCase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class GradingLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;

    @ManyToOne
    private TestCase testCase;

    private Long assignmentId;
    private String difficulty;

    private boolean usedAI;
    private int grade;



    private LocalDateTime timestamp = LocalDateTime.now();

    public GradingLog() {}

    public GradingLog(String userEmail, TestCase testCase, Long assignmentId, String difficulty, boolean usedAI, int grade) {
        this.userEmail = userEmail;
        this.testCase = testCase;
        this.assignmentId = assignmentId;
        this.difficulty = difficulty;
        this.usedAI = usedAI;
        this.grade = grade;

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isUsedAI() {
        return usedAI;
    }

    public void setUsedAI(boolean usedAI) {
        this.usedAI = usedAI;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }



    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
