/**
 * Data Transfer Object for Assignment Entity
 * Used to pass assignment creation data from the frontend to the backend
 * without exposing the full assignment entity
 */

package com.example.backend.professor.dto;

public class AssignmentDTO {
    private String name;
    private String type;
    private Long courseId;
    private String courseTerm;
    private int courseYear;


    public AssignmentDTO() {
    }

    public AssignmentDTO(String name, String type, Long courseId) {
        this.name = name;
        this.type = type;
        this.courseId = courseId;
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

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
