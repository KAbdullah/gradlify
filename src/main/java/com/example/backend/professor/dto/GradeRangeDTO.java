/**
 * GradeRangeDTO represents a single grade range (e.g., "0–20", "21–40", etc.)
 * and the number of students who fall into that range.
 *
 * This is used as part of the GradeDistributionDTO to populate histogram charts
 * on the professor dashboard in the frontend.
 */

package com.example.backend.professor.dto;

public class GradeRangeDTO {
    private String range;
    private int count;
    public GradeRangeDTO(String range, int count) { this.range = range; this.count = count; }
    public String getRange() { return range; }
    public int getCount() { return count; }
}