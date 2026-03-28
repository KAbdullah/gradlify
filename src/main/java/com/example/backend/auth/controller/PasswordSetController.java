package com.example.backend.auth.controller;

import com.example.backend.auth.entity.InvitedUser;
import com.example.backend.auth.entity.RegistrationToken;
import com.example.backend.auth.entity.User;
import com.example.backend.auth.repository.InvitedUserRepository;
import com.example.backend.auth.repository.RegistrationTokenRepository;
import com.example.backend.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class PasswordSetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationTokenRepository registrationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvitedUserRepository invitedUserRepository;

    @PostMapping("/set-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");

        if (token == null || password == null || token.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body("Token and password are required.");
        }

        Optional<RegistrationToken> regTokenOpt = registrationTokenRepository.findById(token);
        if (regTokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }

        RegistrationToken regToken = regTokenOpt.get();

        long now = System.currentTimeMillis();
        if (regToken.getExpiresAt() < now) {
            registrationTokenRepository.deleteById(token);
            return ResponseEntity.badRequest().body("Token has expired.");
        }

        String email = regToken.getEmail();
        if (userRepository.findByUsername(email).isPresent()) {
            return ResponseEntity.badRequest().body("Account already exists.");
        }

        Optional<InvitedUser> invitedOpt = invitedUserRepository.findById(email);
        if (invitedOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invitation not found.");
        }

        InvitedUser invited = invitedOpt.get();

        User newUser = new User();
        newUser.setUsername(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("STUDENT");
        newUser.setFirstName(invited.getFirstName());
        newUser.setLastName(invited.getLastName());
        userRepository.save(newUser);

        registrationTokenRepository.deleteById(token);

        return ResponseEntity.ok("Account created successfully.");
    }
}
