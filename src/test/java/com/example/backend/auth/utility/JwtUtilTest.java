package com.example.backend.auth.utility;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private static final String VALID_SECRET = "gradifySecretKeyForJwtSigningMustBeLongEnough12345";

    @Mock
    private UserDetails userDetails;

    @Test
    void givenValidUserDetails_whenGenerateToken_thenTokenContainsUsernameAndIsValid() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET);
        UserDetails realUserDetails = User.withUsername("student1")
                .password("unused")
                .authorities("ROLE_STUDENT")
                .build();

        String token = jwtUtil.generateToken(realUserDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("student1");
        assertThat(jwtUtil.validateToken(token, realUserDetails)).isTrue();
    }

    @Test
    void givenExpiredToken_whenValidateToken_thenValidationFails() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET);

        // validateToken exits early on expired tokens; keep this lenient to avoid strict-stubbing noise.
        lenient().when(userDetails.getUsername()).thenReturn("student1");

        String expiredToken = Jwts.builder()
                .setSubject("student1")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000L))
                .setExpiration(new Date(System.currentTimeMillis() - 1_000L))
                .signWith(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();

        assertThat(jwtUtil.validateToken(expiredToken, userDetails)).isFalse();
    }

    @Test
    void givenTokenExpiringSoon_whenValidatedBeforeAndAfterBoundary_thenCrossesFromValidToInvalid() throws InterruptedException {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET);

        when(userDetails.getUsername()).thenReturn("student1");

        String nearExpiryToken = Jwts.builder()
                .setSubject("student1")
                .setIssuedAt(new Date(System.currentTimeMillis() - 100L))
                .setExpiration(new Date(System.currentTimeMillis() + 2_000L))
                .signWith(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();

        assertThat(jwtUtil.validateToken(nearExpiryToken, userDetails)).isTrue();

        // Boundary-focused check to surface timing/clock-skew risk behavior.
        Thread.sleep(2_100L);

        assertThat(jwtUtil.validateToken(nearExpiryToken, userDetails)).isFalse();
    }

    @Test
    void givenTooShortSecret_whenConstructingJwtUtil_thenThrowsWeakKeyException() {
        assertThatThrownBy(() -> new JwtUtil("short-secret"))
                .isInstanceOf(WeakKeyException.class);
    }
}
