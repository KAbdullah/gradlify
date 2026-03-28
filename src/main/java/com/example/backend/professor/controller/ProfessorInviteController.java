/**
 * Controller responsible for managing student invitations and bulk enrollment
 * into Gradify by professors. This includes:
 *
 * - Sending individual email invitations with temporary registration tokens
 * - Uploading a CSV file to bulk invite students to a course
 * - Automatically storing invited user metadata in the database
 * - Generating time-limited registration tokens (7 days)
 * - Sending email notifications to students with registration links
 *
 */

package com.example.backend.professor.controller;

import com.example.backend.auth.entity.InvitedUser;
import com.example.backend.auth.entity.RegistrationToken;
import com.example.backend.auth.repository.InvitedUserRepository;
import com.example.backend.auth.repository.RegistrationTokenRepository;
import com.example.backend.auth.repository.UserRepository;
import com.example.backend.professor.entity.Course;
import com.example.backend.professor.repository.CourseRepository;
import com.example.backend.professor.service.EmailService;
import com.example.backend.student.entity.StudentCourse;
import com.example.backend.student.repository.StudentCourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping({"/professor", "/api/professor"})
public class ProfessorInviteController {

    private static final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    @Autowired
    private EmailService emailService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentCourseRepository studentCourseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationTokenRepository registrationTokenRepository;

    @Autowired
    private InvitedUserRepository invitedUserRepository;


    /**
    @PostMapping("/invite")
    public ResponseEntity<String> inviteStudent(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        // Skip if user already exists
        if (userRepository.findByUsername(email).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists.");
        }

        String token = UUID.randomUUID().toString();
        tokenStore.put(token, email);

        String link = "https://gradify.eecs.yorku.ca/set-password?token=" + token;

        String message = "You've been invited to Gradify.\n\n" +
                "Click the link below to set your password:\n" +
                link + "\n\n" +
                "Note: This link is valid until the server restarts.";

        emailService.sendSimpleEmail(email, "Set Your Gradify Password", message);

        return ResponseEntity.ok("Password setup email sent to " + email);
    }
    */


    public static String getEmailForToken(String token) {
        return tokenStore.get(token);
    }

    /**
     * Utility method to remove a used or expired token from in-memory storage
     */
    public static void removeToken(String token) {
        tokenStore.remove(token);
    }


    /**
     * Bulk invitation endpoint to enroll multiple students into a specific course.
     * Accepts a CSV file with the format: email,firstName,lastName
     * - Creates entries in InvitedUser and StudentCourse tables
     * - Generates 7-day registration tokens and sends setup emails
     * - Skips students who are already registered or previously invited
     */
    @PostMapping("/bulk-invite")
    public ResponseEntity<String> bulkInviteStudents(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("courseId") Long courseId) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            long now = System.currentTimeMillis();
            long oneWeekMs = 7 * 24 * 60 * 60 * 1000L;

            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Invalid course ID"));

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // skip header
                }

                String[] tokens = line.split(",");
                if (tokens.length != 3) continue;

                String email = tokens[0].trim();
                String firstName = tokens[1].trim();
                String lastName = tokens[2].trim();

                // Skip existing users or already invited ones
                if (userRepository.findByUsername(email).isPresent()) continue;
                if (invitedUserRepository.existsById(email)) continue;

                // Store invited user metadata
                InvitedUser invite = new InvitedUser();
                invite.setEmail(email);
                invite.setFirstName(firstName);
                invite.setLastName(lastName);
                invite.setInvitedAt(now);
                invitedUserRepository.save(invite);

                // Save course enrollment
                StudentCourse studentCourse = new StudentCourse(email, course);
                studentCourseRepository.save(studentCourse);

                // Send registration token
                String token = UUID.randomUUID().toString();
                RegistrationToken registrationToken = new RegistrationToken();
                registrationToken.setToken(token);
                registrationToken.setEmail(email);
                registrationToken.setCreatedAt(now);
                registrationToken.setExpiresAt(now + oneWeekMs);
                registrationTokenRepository.save(registrationToken);

                String link = "https://gradify.eecs.yorku.ca/set-password?token=" + token;
                String msg = "You've been invited to Gradify.\n\nClick to set your password:\n" + link +
                        "\n\nNote: This link is valid for 7 days.";
                emailService.sendSimpleEmail(email, "Set Your Gradify Password", msg);
            }

            return ResponseEntity.ok("Invitations processed.");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to read file.");
        }
    }

    @DeleteMapping("/uninvite/{courseId}/{email}")
    public ResponseEntity<String> uninviteStudent(@PathVariable Long courseId,
                                                  @PathVariable String email) {
        // Remove from invited users
        invitedUserRepository.deleteById(email);

        // Remove from student-course mapping
        studentCourseRepository.findByStudentEmail(email).stream()
                .filter(sc -> sc.getCourse().getId().equals(courseId))
                .forEach(studentCourseRepository::delete);

        // Remove registration token if exists
        registrationTokenRepository.findAll().stream()
                .filter(token -> token.getEmail().equals(email))
                .forEach(token -> registrationTokenRepository.deleteById(token.getToken()));

        return ResponseEntity.ok("Uninvited " + email + " from course " + courseId);
    }

    @GetMapping("/invited/{courseId}")
    public ResponseEntity<List<InvitedUser>> getInvitedUsers(@PathVariable Long courseId) {
        List<StudentCourse> enrollments = studentCourseRepository.findAll();
        List<InvitedUser> invited = enrollments.stream()
                .filter(sc -> sc.getCourse().getId().equals(courseId))
                .map(sc -> invitedUserRepository.findById(sc.getStudentEmail()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(invite -> userRepository.findByUsername(invite.getEmail()).isEmpty())
                .toList();
        return ResponseEntity.ok(invited);
    }





}
