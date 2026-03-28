package com.example.backend.auth.controller;

import com.example.backend.auth.dto.AuthRequestDTO;
import com.example.backend.auth.dto.AuthResponseDTO;
import com.example.backend.auth.dto.SignupRequestDTO;
import com.example.backend.auth.entity.User;
import com.example.backend.auth.repository.UserRepository;
import com.example.backend.auth.utility.JwtUtil;
import com.example.backend.professor.entity.Course;
import com.example.backend.professor.repository.CourseRepository;
import com.example.backend.student.entity.StudentCourse;
import com.example.backend.student.repository.StudentCourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/auth", "/api/auth"})
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentCourseRepository studentCourseRepository;

    @Value("${gradify.dev-auth.enabled:false}")
    private boolean devAuthEnabled;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO request) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            Optional<User> user = userRepository.findByUsername(request.getUsername());
            String role = user.map(User::getRole).orElse("STUDENT");

            return ResponseEntity.ok(new AuthResponseDTO(token, role));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequestDTO request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim().toLowerCase();
        String password = request.getPassword();
        String firstName = request.getFirstName() == null ? "" : request.getFirstName().trim();
        String lastName = request.getLastName() == null ? "" : request.getLastName().trim();
        String role = request.getRole() == null ? "STUDENT" : request.getRole().trim().toUpperCase();
        if ("TEACHER".equals(role)) {
            role = "PROFESSOR";
        }

        if (username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required.");
        }

        if (firstName.isBlank() || lastName.isBlank()) {
            return ResponseEntity.badRequest().body("First name and last name are required.");
        }

        if (!"STUDENT".equals(role) && !"PROFESSOR".equals(role)) {
            return ResponseEntity.badRequest().body("Invalid role selected.");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists.");
        }

        Course course = null;
        if ("STUDENT".equals(role)) {
            String courseCode = request.getCourseCode() == null ? "" : request.getCourseCode().trim();
            if (!courseCode.isBlank()) {
                Optional<Course> courseOpt = courseRepository.findByCodeIgnoreCase(courseCode);
                if (courseOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Invalid course code.");
                }
                course = courseOpt.get();
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        userRepository.save(user);

        if (course != null && !studentCourseRepository.existsByStudentEmailAndCourse_Id(username, course.getId())) {
            studentCourseRepository.save(new StudentCourse(username, course));
        }

        String token = jwtUtil.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponseDTO(token, user.getRole()));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        return ResponseEntity.ok().body("Token is valid");
    }

    @PostMapping("/dev/seed")
    public ResponseEntity<?> seedDevUsers() {
        if (!devAuthEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dev auth is disabled");
        }

        Map<String, String> professor = createUserIfMissing(
                "professor@local.test",
                "professor123",
                "PROFESSOR"
        );
        Map<String, String> student = createUserIfMissing(
                "student@local.test",
                "student123",
                "STUDENT"
        );

        return ResponseEntity.ok(Map.of(
                "professor", professor,
                "student", student
        ));
    }

    private Map<String, String> createUserIfMissing(String username, String rawPassword, String role) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User created = new User();
                    created.setUsername(username);
                    created.setPassword(passwordEncoder.encode(rawPassword));
                    created.setRole(role);
                    return userRepository.save(created);
                });

        return Map.of(
                "username", user.getUsername(),
                "password", rawPassword,
                "role", user.getRole()
        );
    }
}
