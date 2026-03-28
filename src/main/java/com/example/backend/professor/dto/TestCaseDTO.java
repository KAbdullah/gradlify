/**
 * DTO for uploading test cases.
 * This object encapsulates all fields sent by the frontend when a professor
 * uploads a new test case for a specific assignment
 */

package com.example.backend.professor.dto;

import org.springframework.web.multipart.MultipartFile;

public class TestCaseDTO {
    private Long assignmentId;
    private String description;
    private String difficulty;
    private MultipartFile file;
    private boolean visibleToStudents;



    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public MultipartFile getFile() {
        return this.file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public boolean isVisibleToStudents() {
        return this.visibleToStudents;
    }

    public void setVisibleToStudents(boolean visibleToStudents) {
        this.visibleToStudents = visibleToStudents;
    }
}
