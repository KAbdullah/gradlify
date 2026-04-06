package com.example.backend.auth.utility;

import com.example.backend.auth.service.UserDetailsServiceImpl;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() throws Exception {
        jwtFilter = new JwtFilter();
        inject(jwtFilter, "jwtUtil", jwtUtil);
        inject(jwtFilter, "userDetailsService", userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNoAuthorizationHeader_whenFiltering_thenContinuesWithoutAuthenticationWork() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void givenValidBearerToken_whenNoExistingAuthentication_thenSetsSecurityContext() throws ServletException, IOException {
        UserDetails user = User.withUsername("student1").password("unused").authorities("ROLE_STUDENT").build();
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtUtil.extractUsername("good-token")).thenReturn("student1");
        when(userDetailsService.loadUserByUsername("student1")).thenReturn(user);
        when(jwtUtil.validateToken("good-token", user)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("student1");
    }

    @Test
    void givenMalformedBearerToken_whenFiltering_thenContinuesRequestAndKeepsContextUnauthenticated() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer malformed-token");
        when(jwtUtil.extractUsername("malformed-token"))
                .thenThrow(new MalformedJwtException("invalid jwt"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void givenExistingAuthentication_whenFilteringValidHeader_thenSkipsUserLookupAndValidation() throws ServletException, IOException {
        UserDetails existingUser = User.withUsername("already-auth").password("unused").authorities("ROLE_STUDENT").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(existingUser, null, existingUser.getAuthorities())
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtUtil.extractUsername("good-token")).thenReturn("student1");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername("student1");
        verify(jwtUtil, never()).validateToken(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("already-auth");
    }

    /**
     * Critical pattern: auth filter resilience — {@link JwtFilter} only catches
     * {@code JwtException} and {@code IllegalArgumentException}. A structurally valid JWT whose
     * subject does not exist in the user store causes {@link UsernameNotFoundException}, which
     * propagates and can yield 500 before {@code filterChain.doFilter} completes.
     * <p>
     * RED: expect the filter to swallow user-lookup failures and continue the chain unauthenticated.
     */
    @Test
    void givenBearerTokenForUnknownUser_whenFiltering_thenContinuesChainWithoutThrowing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-shaped-token");
        when(jwtUtil.extractUsername("valid-shaped-token")).thenReturn("no-such-user");
        when(userDetailsService.loadUserByUsername("no-such-user"))
                .thenThrow(new UsernameNotFoundException("user not found"));

        assertThatCode(() -> jwtFilter.doFilterInternal(request, response, filterChain))
                .doesNotThrowAnyException();

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
