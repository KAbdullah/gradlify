/**
 * Represents a temporary token issued to users when they request a password reset.
 * Tokens expire after a fixed time (e.g., 30 minutes).
 *
 */

package com.example.backend.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PasswordResetToken {

    @Id
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDateTime expiration;

    public PasswordResetToken() {}

    public PasswordResetToken(String token, String email, LocalDateTime expiration) {
        this.token = token;
        this.email = email;
        this.expiration = expiration;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public LocalDateTime getExpiration() { return expiration; }

    public void setToken(String token) { this.token = token; }
    public void setEmail(String email) { this.email = email; }
    public void setExpiration(LocalDateTime expiration) { this.expiration = expiration; }
}
