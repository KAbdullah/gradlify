/**
 * Core user entity for the system. Represents both students and professors.
 * Holds login credentials, metadata like optional LLM API key for feedback features.
 *
 * 🚨 Note:
 * - Table is named "user" in double quotes to avoid conflict with PostgreSQL reserved word.
 * - Passwords are securely encoded with BCrypt.
 */

package com.example.backend.auth.entity;

import jakarta.persistence.*;
@Entity
@Table(name = "\"user\"") // PostgreSQL reserved word
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;
    private String role;

    @Column(name = "ai_api_key")
    private String aiApiKey;

    @Column(name = "ai_model_name")
    private String aiModelName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }





    public String getAiApiKey() { return aiApiKey; }
    public void setAiApiKey(String aiApiKey) { this.aiApiKey = aiApiKey; }

    public String getAiModelName() {
        return aiModelName;
    }

    public void setAiModelName(String aiModelName) {
        this.aiModelName = aiModelName;
    }


    private String firstName;
    private String lastName;



}
