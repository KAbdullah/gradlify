/**
 * This controller provides various endpoints for instructors to view analytical
 * reports on student grading performance, including:
 * - Average grades per assignment
 * - Error distribution (e.g., compilation errors)
 * - Grade distributions (bins, median, mode, std)
 * - Basic stats (min, max, average) per assignment
 */

package com.example.backend.professor.controller;

import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.professor.dto.AssignmentAverageDTO;
import com.example.backend.professor.dto.ErrorTypeDTO;
import com.example.backend.professor.dto.GradeRangeDTO;
import com.example.backend.professor.entity.GradingResult;
import com.example.backend.professor.repository.GradingResultRepository;
import com.example.backend.professor.service.InstructorReportService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping({"/professor/reports", "/api/professor/reports"})
public class InstructorReportController {

    @Autowired
    private InstructorReportService instructorReportService;

    @Autowired
    private GradingResultRepository gradingResultRepository;


    /**
     * Returns a list of average grades per assignment.
     * Used to render the average bar chart.
     */
    @PostMapping("/averages/by-assignments")
    public List<AssignmentAverageDTO> getAveragesByAssignments(@RequestBody List<Long> assignmentIds) {
        System.out.println("Received assignment IDs: " + assignmentIds);
        return instructorReportService.getAveragesForAssignments(assignmentIds);
    }

    /**
     * Returns a distribution of common error types from notes (e.g. compilation, timeout).
     * Used to render the pie chart of error categories.
     */
    @PostMapping("/errors/by-assignments")
    public List<ErrorTypeDTO> getErrorDistributionByAssignments(@RequestBody List<Long> assignmentIds) {
        List<GradingResult> results = gradingResultRepository.findByAssignmentIdIn(assignmentIds);
        Map<String, Integer> errorCounts = new HashMap<>();

        for (GradingResult r : results) {
            String note = Optional.ofNullable(r.getNote()).orElse("").toLowerCase();
            if (note.contains("compilation")) {
                errorCounts.merge("Compilation Errors", 1, Integer::sum);
            } else if (note.contains("incorrect file name")) {
                errorCounts.merge("Incorrect File Name", 1, Integer::sum);
            } else if (note.contains("timeout") || note.contains("crash")) {
                errorCounts.merge("Timeouts/Crashes", 1, Integer::sum);
            }
        }

        return errorCounts.entrySet().stream()
                .map(e -> new ErrorTypeDTO(e.getKey(), e.getValue()))
                .toList();
    }


    /**
     * Returns a histogram-like distribution of grades for selected assignments,
     * plus the median, mode, and standard deviation.
     * Used for the histogram view of grading.
     */
    @PostMapping("/distribution/by-assignments")
    public Map<String, Object> getDistributionByAssignments(@RequestBody List<Long> assignmentIds) {
    List<Double> grades = gradingResultRepository.findByAssignmentIdIn(assignmentIds).stream()
                .map(GradingResult::getGrade)
                .filter(g -> g != null && g >= 0 && g <= 100)
                .map(Integer::doubleValue)
                .toList();

        Map<String, Integer> bins = new LinkedHashMap<>(Map.of(
                "0-20", 0, "21-40", 0, "41-60", 0, "61-80", 0, "81-99", 0, "100", 0
        ));

        // Create bins for histogram
        for (double g : grades) {
            if (g <= 20) bins.merge("0-20", 1, Integer::sum);
            else if (g <= 40) bins.merge("21-40", 1, Integer::sum);
            else if (g <= 60) bins.merge("41-60", 1, Integer::sum);
            else if (g <= 80) bins.merge("61-80", 1, Integer::sum);
            else if (g < 100) bins.merge("81-99", 1, Integer::sum);
            else bins.merge("100", 1, Integer::sum);
        }

        List<GradeRangeDTO> dist = bins.entrySet().stream()
                .map(e -> new GradeRangeDTO(e.getKey(), e.getValue()))
                .toList();


        // Calculate statistics
        double median = 0, std = 0, mode = 0;
        if (!grades.isEmpty()) {
            grades = grades.stream().sorted().toList();
            int mid = grades.size() / 2;
            median = (grades.size() % 2 == 0) ? (grades.get(mid - 1) + grades.get(mid)) / 2 : grades.get(mid);

            Map<Double, Long> freq = grades.stream().collect(Collectors.groupingBy(g -> g, Collectors.counting()));
            mode = freq.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();

            double mean = grades.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            std = Math.sqrt(grades.stream().mapToDouble(g -> Math.pow(g - mean, 2)).average().orElse(0));
        }

        return Map.of(
                "bins", dist,
                "median", median,
                "mode", mode,
                "std", Math.round(std * 100.0) / 100.0
        );

    }


    /**
     * Returns average, max, and min grades for a single assignment.
     * This is used for the detailed stats view of one assignment.
     */
    @GetMapping("/submission-stats/{assignmentId}")
    public ResponseEntity<Map<String, Double>> getSubmissionStats(@PathVariable Long assignmentId) {
        List<GradingResult> results = gradingResultRepository.findByAssignmentId(assignmentId);

        if (results.isEmpty()) {
            return ResponseEntity.ok(Map.of("average", 0.0, "max", 0.0, "min", 0.0));
        }

        DoubleSummaryStatistics stats = results.stream()
                .mapToDouble(GradingResult::getGrade)
                .summaryStatistics();

        Map<String, Double> response = Map.of(
                "average", stats.getAverage(),
                "max", stats.getMax(),
                "min", stats.getMin()
        );

        return ResponseEntity.ok(response);
    }



}
