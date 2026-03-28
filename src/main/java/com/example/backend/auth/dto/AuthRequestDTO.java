/**
 * This DTO (Data Transfer Object) is used to encapsulate the data required for login requests.
 * It carries the user's login credentials — username and password — from the client to the server.
 *
 */
package com.example.backend.auth.dto;

public class AuthRequestDTO {
    private String username;
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
