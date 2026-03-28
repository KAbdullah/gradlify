/**
 * UserDetailsServiceImpl.java
 *
 * This class implements Spring Security's UserDetailsService interface.
 * It is used during authentication to load user details from the database based on the provided username.
 *
 * Key Responsibilities:
 * - Fetches the User entity using the username (email)
 * - Converts it into Spring Security's `UserDetails` object
 * - Attaches the user’s role as a granted authority (e.g., ROLE_STUDENT or ROLE_PROFESSOR)
 *
 */

package com.example.backend.auth.service;

import com.example.backend.auth.entity.User;
import com.example.backend.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Loads user details by username (email in this case).
     * Converts our custom `User` entity into Spring Security’s `UserDetails` object.
     *
     * @param username The user's email used for login
     * @return UserDetails object used for Spring Security
     * @throws UsernameNotFoundException if user does not exist
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Look up the user from the database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        // Return Spring Security-compatible user
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
