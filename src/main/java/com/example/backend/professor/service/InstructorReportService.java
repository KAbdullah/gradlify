/**
 * Service responsible for generating instructor reports based on grading data.
 * Provides functionality to compute average grades for a list of assignments.
 * Used in dashboards and analytics views for professors.
 */
package com.example.backend.professor.service;


import com.example.backend.professor.dto.AssignmentAverageDTO;
import com.example.backend.professor.repository.AssignmentRepository;
import com.example.backend.professor.repository.GradingResultRepository;
import com.example.backend.professor.entity.Assignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InstructorReportService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private GradingResultRepository gradingResultRepository;

    /**
     * Computes the average grade for each assignment ID provided.
     * Returns a list of AssignmentAverageDTO containing assignment ID, name, and average.
     *
     * @param assignmentIds List of assignment IDs to compute averages for
     * @return List of AssignmentAverageDTO objects
     */
    public List<AssignmentAverageDTO> getAveragesForAssignments(List<Long> assignmentIds) {
        List<Object[]> raw = gradingResultRepository.findAveragesByAssignmentIds(assignmentIds);
        System.out.println("Raw query results: " + raw.size());

        List<AssignmentAverageDTO> result = new ArrayList<>();
        for (Object[] row : raw) {
            Long assignmentId = (Long) row[0];
            Double avg = (Double) row[1];

            String name = assignmentRepository.findById(assignmentId)
                    .map(a -> a.getName())
                    .orElse("Assignment " + assignmentId);

            System.out.println("Calculated average for " + name + " (ID " + assignmentId + ") = " + avg);

            result.add(new AssignmentAverageDTO(assignmentId, name, avg));
        }

        return result;
    }


}