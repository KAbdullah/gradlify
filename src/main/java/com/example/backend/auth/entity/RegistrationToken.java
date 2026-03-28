/**
 * Used to represent time-bound tokens for new users to register and set their password.
 * These are created when professors invite users, and verified during registration.
 */

 package com.example.backend.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RegistrationToken {
    @Id
    private String token;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private String email;
    private long createdAt;
    private long expiresAt;


}