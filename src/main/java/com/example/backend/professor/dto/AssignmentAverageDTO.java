/**
 * DTO for sending average grade information about an assignment.
 *
 * This object is used when generating reports (e.g., instructor dashboards)
 * to display the average grade per assignment along with course and term info.
 */

package com.example.backend.professor.dto;

public class AssignmentAverageDTO {
    private Long assignmentId;
    private String assignmentName;
    private double averageGrade;

    private String courseCode;
    private String term;
    private int year;

    // Optional helper for frontend display
    public String getLabel() {
        return assignmentName + " - " + courseCode + " " + term + " " + year;
    }

    public AssignmentAverageDTO() {}

    public AssignmentAverageDTO(Long assignmentId, String assignmentName, double averageGrade) {
        this.assignmentId = assignmentId;
        this.assignmentName = assignmentName;
        this.averageGrade = averageGrade;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentName() {
        return assignmentName;
    }

    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }

    public double getAverageGrade() {
        return averageGrade;
    }

    public void setAverageGrade(double averageGrade) {
        this.averageGrade = averageGrade;
    }
}
