package com.example.backend.student.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores Checkstyle violations and aggregates deduction values.
 */
public class ViolationCollector {
    private final Map<String, List<Violation>> violations = new HashMap<>();
    private double totalDeduction = 0.0;
    private int totalViolations = 0;
    private String projectName = "unknown";

    public static class Violation {
        public final int line;
        public final String message;
        public final String checkName;

        public Violation(int line, String message, String checkName) {
            this.line = line;
            this.message = message;
            this.checkName = checkName;
        }
    }

    public void addViolation(int line, String message, String checkName) {
        List<Violation> list = violations.get(checkName);
        if (list == null) {
            list = new ArrayList<>();
            violations.put(checkName, list);
        }
        list.add(new Violation(line, message, checkName));
        totalViolations++;
    }

    public void addDeduction(double amount) {
        totalDeduction += amount;
    }

    public List<Violation> getViolations() {
        List<Violation> allViolations = new ArrayList<>();
        for (List<Violation> list : violations.values()) {
            allViolations.addAll(list);
        }
        return allViolations;
    }

    public List<Violation> getViolationsByCheck(String checkName) {
        return violations.getOrDefault(checkName, Collections.emptyList());
    }

    public int getTotalViolations() {
        return totalViolations;
    }

    public double getTotalDeduction() {
        return totalDeduction;
    }

    public Map<String, Integer> getViolationCounts() {
        Map<String, Integer> counts = new HashMap<>();
        violations.forEach((check, list) -> counts.put(check, list.size()));
        return counts;
    }

    public void setProjectName(String name) {
        this.projectName = name;
    }

    public String getProjectName() {
        return projectName;
    }
}
