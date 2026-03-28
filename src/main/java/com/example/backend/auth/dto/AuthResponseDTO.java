/**
 * This DTO (Data Transfer Object) encapsulates the response returned to the frontend after a successful login.
 * It contains the generated JWT token and the user's role (e.g., STUDENT or PROFESSOR).
 *
 */
package com.example.backend.auth.dto;

public class AuthResponseDTO {
    private String token;
    private String role;

    public AuthResponseDTO(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getRole() { return role; }
}
