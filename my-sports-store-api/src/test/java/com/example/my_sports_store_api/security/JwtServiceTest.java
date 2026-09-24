package com.example.my_sports_store_api.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-0123456789";

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
        userDetails = User.builder()
                .username("user@example.com")
                .password("irrelevant")
                .authorities(List.of(() -> "ROLE_USER"))
                .build();
    }

    @Test
    void generateToken_thenExtractUsername_roundTripsCorrectly() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void isTokenValid_forMatchingUserAndUnexpiredToken_returnsTrue() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_forDifferentUsername_returnsFalse() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = User.builder()
                .username("other@example.com")
                .password("irrelevant")
                .authorities(List.of(() -> "ROLE_USER"))
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_forExpiredToken_throwsJwtException() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1_000);
        String expiredToken = shortLivedJwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractUsername_forTokenSignedWithDifferentKey_throwsJwtException() {
        JwtService otherKeyJwtService = new JwtService("a-completely-different-secret-key-0123456789", 60_000);
        String token = otherKeyJwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }
}
