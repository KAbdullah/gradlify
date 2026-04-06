/**
 * Controller for professor grading operations in Gradify.
 *
 * Provides endpoints that allow professors to:
 *
 * - Grade a folder of student submissions using a selected test case
 * - Check if grading results already exist for a given test case
 * - Download a CSV file with the latest grading results
 *
 * All grading logic is delegated to the ProfGradeService.
 */

package com.example.backend.professor.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.professor.entity.TestCase;
import com.example.backend.professor.repository.GradingResultRepository;
import com.example.backend.professor.repository.TestCaseRepository;
import com.example.backend.professor.service.GradingArtifactRegistry;
import com.example.backend.professor.service.ProfGradeService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping({"/professor/grade", "/api/professor/grade"})
public class ProfGradeController {

    @Autowired
    private ProfGradeService profGradeService;

    @Autowired
    private GradingArtifactRegistry gradingArtifactRegistry;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private GradingResultRepository gradingResultRepository;


    /**
     * Endpoint to grade a folder of student submissions.
     *
     * @param studentFolders array of uploaded ZIP files or directories, each representing a student submission
     * @param testCaseId     ID of the test case to use for grading
     * @param fileName       the name of the main Java file or class to grade (e.g., "Main.java" or "StudentCode.java")
     * @param overwriteJson  whether to override existing grades (if false, already-graded students are skipped)
     * @return a map of student identifiers to grading results (or error messages)
     */
    @PostMapping("/run-folder")
    public ResponseEntity<Map<String, String>> gradeFolderOfStudents(
            @RequestParam("studentFolders") MultipartFile[] studentFolders,
            @RequestParam("testCaseId") Long testCaseId,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "overwriteList", required = false) String overwriteJson) {
        try {
            List<String> overwriteList = new ArrayList<>();
            if (overwriteJson != null && !overwriteJson.isEmpty()) {
                overwriteList = Arrays.asList(overwriteJson.replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .split(","));
            }

            Map<String, String> results = profGradeService.runGradingOnFolder(
                    studentFolders, testCaseId, fileName, overwriteList);

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * Endpoint to download a grading result CSV using the opaque {@code downloadId} from {@code /run-folder}.
     */
    @GetMapping("/results/download")
    public ResponseEntity<Resource> downloadResults(@RequestParam("downloadId") String downloadId) {
        Optional<GradingArtifactRegistry.Artifact> artifactOpt = gradingArtifactRegistry.resolve(downloadId);
        if (artifactOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        GradingArtifactRegistry.Artifact artifact = artifactOpt.get();
        File f = artifact.path().toFile();
        if (!f.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource file = new FileSystemResource(f);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + artifact.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(file);
    }


    /**
     * Endpoint to check if grades already exist for a given test case.
     *
     * Useful for frontend logic — e.g., showing a confirmation before overwriting.
     *
     * @param testCaseId ID of the test case to check
     * @return true if grades exist, false otherwise
     */
    @GetMapping("/check-existing-grades")
    public ResponseEntity<Boolean> checkGradesExist(@RequestParam("testCaseId") Long testCaseId) {
        try {
            boolean exists = profGradeService.gradesExistForAssignment(testCaseId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/check-existing-students")
    public ResponseEntity<List<String>> checkExistingStudents(
            @RequestParam("studentFolders") MultipartFile[] studentFolders,
            @RequestParam("testCaseId") Long testCaseId) throws Exception {

        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test case not found"));
        Long assignmentId = testCase.getAssignment().getId();

        Set<String> existingStudents = new HashSet<>();
        for (MultipartFile file : studentFolders) {
            String path = file.getOriginalFilename();
            if (path == null || !path.contains("/")) continue;
            String studentId = path.split("/")[1];
            if (!gradingResultRepository.findByAssignmentIdAndStudentId(assignmentId, studentId).isEmpty()) {
                existingStudents.add(studentId);
            }
        }

        return ResponseEntity.ok(new ArrayList<>(existingStudents));
    }






}
