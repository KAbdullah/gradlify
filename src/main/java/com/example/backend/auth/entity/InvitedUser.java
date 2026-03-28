/**
 * This entity represents a user who has been invited to join the platform (e.g., via CSV upload by a professor).
 */


package com.example.backend.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class InvitedUser {
    @Id
    private String email;


    private String firstName;
    private String lastName;
    private long invitedAt; // The time (ms) indicating when the invite was issued

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

    public long getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(long invitedAt) {
        this.invitedAt = invitedAt;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}


