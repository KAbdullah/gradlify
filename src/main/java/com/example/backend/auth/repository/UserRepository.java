/**
 * Core repository interface for managing registered users (students, professors).
 * Primary key: id (Long)
 *
 */

package com.example.backend.auth.repository;

import com.example.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Finds a user by username (typically their email).
    Optional<User> findByUsername(String username);
}
