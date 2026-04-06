package com.example.backend.professor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opaque download tokens for grading CSVs plus scheduled cleanup so tmpdir does not fill.
 * Keeps {@link ProfGradeService} stateless with respect to download paths.
 */
@Component
public class GradingArtifactRegistry {

    public record Artifact(Path path, String fileName, Instant createdAt) {}

    private final Map<String, Artifact> byId = new ConcurrentHashMap<>();

    @Value("${gradify.grading.csv-max-age-hours:24}")
    private int maxAgeHours;

    /**
     * Registers a CSV written under {@link java.io.File#createTempFile} semantics; schedules JVM exit cleanup
     * and periodic deletion via {@link #purgeExpired()}.
     */
    public String registerArtifact(Path absolutePath, String fileName) {
        String id = UUID.randomUUID().toString();
        byId.put(id, new Artifact(absolutePath, fileName, Instant.now()));
        try {
            absolutePath.toFile().deleteOnExit();
        } catch (Exception ignored) {
            // best-effort; scheduled purge still applies
        }
        return id;
    }

    public Optional<Artifact> resolve(String downloadId) {
        if (downloadId == null || downloadId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(downloadId));
    }

    public void remove(String downloadId) {
        if (downloadId != null) {
            byId.remove(downloadId);
        }
    }

    @Scheduled(fixedRateString = "${gradify.grading.csv-cleanup-interval-ms:3600000}")
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(maxAgeHours, ChronoUnit.HOURS);
        for (Map.Entry<String, Artifact> e : byId.entrySet()) {
            Artifact a = e.getValue();
            if (a.createdAt().isBefore(cutoff)) {
                deleteQuietly(a.path());
                byId.remove(e.getKey(), a);
            }
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
