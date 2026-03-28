/**
 * Repository for accessing and managing GradingLog entries.
 * Used to track student submissions and grading events (with or without AI).
 */

package com.example.backend.student.repository;

import com.example.backend.student.entity.GradingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingLogRepository extends JpaRepository<GradingLog, Long> {
}
