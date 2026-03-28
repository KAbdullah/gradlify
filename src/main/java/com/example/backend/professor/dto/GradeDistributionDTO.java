/**
 * GradeDistributionDTO is a Data Transfer Object used to return statistical
 * information about grade distributions for a given assignment.
 *
 * This object includes:
 * - A list of GradeRangeDTOs representing how many students fall into each grade range
 * - The median grade
 * - The mode (most frequent grade)
 * - The standard deviation (std) of the grades
 *
 * It is primarily used by the backend to power the "Grade Distribution" report
 * for professors on the frontend charts.
 */

package com.example.backend.professor.dto;

import java.util.List;

public class GradeDistributionDTO {
    private List<GradeRangeDTO> distribution;
    private double median;
    private double mode;
    private double std;

    public GradeDistributionDTO(List<GradeRangeDTO> distribution, double median, double mode, double std) {
        this.distribution = distribution;
        this.median = median;
        this.mode = mode;
        this.std = std;
    }

    public List<GradeRangeDTO> getDistribution() { return distribution; }
    public double getMedian() { return median; }
    public double getMode() { return mode; }
    public double getStd() { return std; }
}