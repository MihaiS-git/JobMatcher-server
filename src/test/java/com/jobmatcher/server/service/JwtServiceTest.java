package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.InvalidAuthException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private final String secretKeyBase64 = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());
    private final long jwtExpirationMs = 1000 * 60 * 60; // 1 hour

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        var secretField = JwtService.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(jwtService, secretKeyBase64);

        var expField = JwtService.class.getDeclaredField("jwtTokenExpiration");
        expField.setAccessible(true);
        expField.set(jwtService, jwtExpirationMs);
    }

    @Test
    void testGenerateToken_and_extractUsername() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.CUSTOMER);

        String token = jwtService.generateToken(user);
        assertThat(token).isNotNull();

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo(user.getEmail());
    }

    @Test
    void testGenerateToken_withExtraClaims() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.CUSTOMER);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("custom", "value");

        String token = jwtService.generateToken(extraClaims, user);
        assertThat(token).isNotNull();

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("CUSTOMER");

        String custom = jwtService.extractClaim(token, claims -> claims.get("custom", String.class));
        assertThat(custom).isEqualTo("value");
    }

    @Test
    void testIsTokenValid_whenValid() {
        User user = new User();
        user.setEmail("valid@example.com");
        user.setRole(Role.CUSTOMER);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void testIsTokenValid_whenExpired() throws Exception {
        // Override expiration to a past date
        jwtService = new JwtService();

        var secretField = JwtService.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(jwtService, secretKeyBase64);

        var expField = JwtService.class.getDeclaredField("jwtTokenExpiration");
        expField.setAccessible(true);
        // Set expiration to -1 ms to expire immediately
        expField.set(jwtService, -1L);

        User user = new User();
        user.setEmail("expired@example.com");
        user.setRole(Role.CUSTOMER);

        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenExpired(token)).isTrue();
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void testExtractClaim_withInvalidToken_throwsInvalidAuthException() {
        String invalidToken = "invalid.token.value";

        assertThatThrownBy(() -> jwtService.extractClaim(invalidToken, Claims::getSubject))
                .isInstanceOf(InvalidAuthException.class)
                .hasMessageContaining("JWT token is invalid");
    }

    @Test
    void testIsTokenValid_withInvalidToken_returnsFalse() {
        String invalidToken = "bad.token.value";

        boolean result = jwtService.isTokenValid(invalidToken);
        assertThat(result).isFalse();
    }

    @Test
    void testExtractExpiration_returnsCorrectExpiration() {
        User user = new User();
        user.setEmail("expire@example.com");
        user.setRole(Role.CUSTOMER);

        String token = jwtService.generateToken(user);
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void testExtractUsername_withInvalidToken_throwsInvalidAuthException() {
        String invalidToken = "invalid.token.value";

        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(InvalidAuthException.class)
                .hasMessageContaining("JWT token is invalid");
    }

    @Test
    void testIsTokenValid_catchesInvalidAuthException() throws Exception {
        JwtService spyJwtService = org.mockito.Mockito.spy(jwtService);
        String token = "any.token.value";

        org.mockito.Mockito.doThrow(new InvalidAuthException("forced")).when(spyJwtService).isTokenExpired(token);

        boolean result = spyJwtService.isTokenValid(token);

        assertThat(result).isFalse();
    }
}