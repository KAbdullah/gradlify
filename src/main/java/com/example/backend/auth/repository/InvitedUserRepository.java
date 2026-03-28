/**
 * Repository interface for accessing invited users who have been sent an email for registration
 * Primary key: email (String)
 */

package com.example.backend.auth.repository;

import com.example.backend.auth.entity.InvitedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitedUserRepository extends JpaRepository<InvitedUser, String> {}
