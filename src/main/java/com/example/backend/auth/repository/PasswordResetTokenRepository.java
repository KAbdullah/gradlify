/**
 * Repository for managing password reset tokens.
 * These are issued when users initiate "forgot password" flows.
 * Primary key: token (String UUID)
 *
 */

package com.example.backend.auth.repository;

import com.example.backend.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    // Lookup token entry using the token string (UUID).
    Optional<PasswordResetToken> findByToken(String token);

}
