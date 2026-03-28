/**
 * Repository for managing registration tokens for first-time invited users.
 * These tokens are generated during invite and consumed during password setup.
 * Primary key: token (String UUID)
 */
package com.example.backend.auth.repository;

import com.example.backend.auth.entity.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, String> {

    // Lookup a token entry based on its token string.
    Optional<RegistrationToken> findByToken(String token);

    // Delete token after successful registration to prevent reuse.
    void deleteByToken(String token);

}
