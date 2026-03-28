package com.example.backend.professor.dto;

public class TestCaseVisibilityUpdateDTO {
    private boolean visibleToStudents;
    private String difficulty;

    public boolean isVisibleToStudents() {
        return visibleToStudents;
    }

    public void setVisibleToStudents(boolean visibleToStudents) {
        this.visibleToStudents = visibleToStudents;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
