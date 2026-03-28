/**
 *
 * This controller handles the password reset flow in the Gradify backend system.
 *
 * It exposes two main endpoints:
 *
 * 1. POST `/auth/forgot-password`:
 *    - Accepts an email address
 *    - Generates a secure reset token (UUID)
 *    - Stores the token with a 30-minute expiry in the database
 *    - Sends a password reset email using `EmailService`
 *
 * 2. POST `/auth/change-password`:
 *    - Accepts a valid token and new password
 *    - Validates token expiration and user identity
 *    - Updates the user's password (hashed)
 *    - Deletes the used token
 *
 *  This controller ensures that only users with a valid token (received via email)
 *     can reset their password, improving security and user experience.
 *
 *  Tokens are stored in a `PasswordResetToken` table using JPA, and expire after 30 minutes.
 *
 *  Email sending is handled via `EmailService`, which can be found in `professor.service`.
 *
 */

package com.example.backend.auth.controller;

import com.example.backend.auth.entity.PasswordResetToken;
import com.example.backend.auth.repository.PasswordResetTokenRepository;
import com.example.backend.auth.repository.UserRepository;
import com.example.backend.professor.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





@RestController
@RequestMapping("/auth")
public class ForgotPasswordController {

    private static final Map<String, String> passwordResetTokens = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;


    /**
     *
     * This endpoint generates a password reset token and emails the user a reset link.
     *
     * Expected JSON payload:
     * {
     *   "email": "user@example.com"
     * }
     *
     * Response:
     * - 200 OK: if email sent successfully
     * - 400 Bad Request: if email is missing or account doesn't exist
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> sendResetLink(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }

        // Check if account exists
        if (userRepository.findByUsername(email).isEmpty()) {
            return ResponseEntity.badRequest().body("Account does not exist.");
        }

        // Generate secure token and expiration
        String token = UUID.randomUUID().toString();
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(30); // expires in 30 min
        PasswordResetToken resetToken = new PasswordResetToken(token, email, expiration);
        tokenRepository.save(resetToken);

        // Compose reset link
        String link = "https://gradify.eecs.yorku.ca/reset-password?token=" + token;
        String message = "You requested to reset your password.\n\n" +
                "Click the link below to set a new password (valid for 30 minutes):\n" +
                link;

        // Send email
        emailService.sendSimpleEmail(email, "Gradify Password Reset", message);
        return ResponseEntity.ok("Password reset link sent to " + email);
    }

    /**
     *
     * This endpoint is used to change the user's password using a token.
     * The token must be valid and unexpired, and the password must be non-empty.
     *
     * Response:
     * - 200 OK: password updated
     * - 400 Bad Request: for missing fields, invalid/expired token, or missing user
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestBody(required = false) Map<String, String> body) {

        // Retrieve token and password from request
        String rawToken = tokenParam != null ? tokenParam : (body != null ? body.get("token") : null);
        String password = (body != null ? body.get("password") : null);
        if (rawToken == null || rawToken.isBlank() || password == null || password.isBlank())
            return ResponseEntity.badRequest().body("Token and password are required.");

        // Decode token in case it's URL-encoded (e.g., %20, %2F)
        String token = java.net.URLDecoder.decode(rawToken, java.nio.charset.StandardCharsets.UTF_8).trim();

        // Validate token length
        if (token.length() != 36) return ResponseEntity.badRequest().body("Invalid or expired token.");

        // Fetch token from DB
        var tokenOpt = tokenRepository.findById(token); // token is @Id
        if (tokenOpt.isEmpty()) return ResponseEntity.badRequest().body("Invalid or expired token.");

        var resetToken = tokenOpt.get();

        // Check token expiration
        if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
            tokenRepository.deleteById(token);
            return ResponseEntity.badRequest().body("Token has expired.");
        }

        // Find user by email
        var userOpt = userRepository.findByUsername(resetToken.getEmail());
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body("User not found.");

        // Update and hash the new password
        var user = userOpt.get();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        // Clean up token after use
        tokenRepository.deleteById(token);

        return ResponseEntity.ok("Password updated successfully.");
    }


}
