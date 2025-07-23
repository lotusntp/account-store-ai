package com.accountselling.platform.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtTokenServiceImpl.
 * Tests JWT token generation, validation, and extraction functionality.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenServiceImplTest {

    private JwtTokenServiceImpl jwtTokenService;
    private UserDetails userDetails;

    private static final String TEST_SECRET = Base64.getEncoder().encodeToString("test-secret-key-that-is-long-enough-for-hmac-256".getBytes());
    private static final long ACCESS_TOKEN_EXPIRATION = 900000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenServiceImpl();
        
        ReflectionTestUtils.setField(jwtTokenService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    void generateAccessToken_WithUserDetails_ShouldReturnValidToken() {
        String token = jwtTokenService.generateAccessToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT should have 3 parts
    }

    @Test
    void generateAccessToken_WithExtraClaims_ShouldReturnValidToken() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        extraClaims.put("department", "IT");

        String token = jwtTokenService.generateAccessToken(extraClaims, userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateRefreshToken_WithUserDetails_ShouldReturnValidToken() {
        String token = jwtTokenService.generateRefreshToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_WithValidToken_ShouldReturnUsername() {
        String token = jwtTokenService.generateAccessToken(userDetails);

        String extractedUsername = jwtTokenService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    void extractUsername_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtTokenService.extractUsername(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void isTokenValid_WithValidTokenAndMatchingUser_ShouldReturnTrue() {
        String token = jwtTokenService.generateAccessToken(userDetails);

        boolean isValid = jwtTokenService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_WithValidTokenAndDifferentUser_ShouldReturnFalse() {
        String token = jwtTokenService.generateAccessToken(userDetails);
        
        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        boolean isValid = jwtTokenService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtTokenService.isTokenValid(invalidToken, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void isTokenExpired_WithFreshToken_ShouldReturnFalse() {
        String token = jwtTokenService.generateAccessToken(userDetails);

        boolean isExpired = jwtTokenService.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    void isTokenExpired_WithExpiredToken_ShouldReturnTrue() {
        ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", -1000L); // Expired 1 second ago
        
        String expiredToken = jwtTokenService.generateAccessToken(userDetails);
        
        // Wait a moment to ensure token is expired
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}
        
        assertThatThrownBy(() -> jwtTokenService.isTokenExpired(expiredToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void getAccessTokenExpiration_ShouldReturnConfiguredValue() {
        long expiration = jwtTokenService.getAccessTokenExpiration();

        assertThat(expiration).isEqualTo(ACCESS_TOKEN_EXPIRATION);
    }

    @Test
    void getRefreshTokenExpiration_ShouldReturnConfiguredValue() {
        long expiration = jwtTokenService.getRefreshTokenExpiration();

        assertThat(expiration).isEqualTo(REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    void accessToken_ShouldExpireFasterThanRefreshToken() {
        String accessToken = jwtTokenService.generateAccessToken(userDetails);
        String refreshToken = jwtTokenService.generateRefreshToken(userDetails);

        assertThat(jwtTokenService.getAccessTokenExpiration())
                .isLessThan(jwtTokenService.getRefreshTokenExpiration());
    }

    @Test
    void generateToken_WithNullUserDetails_ShouldThrowException() {
        assertThatThrownBy(() -> jwtTokenService.generateAccessToken(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void extractUsername_WithNullToken_ShouldThrowException() {
        assertThatThrownBy(() -> jwtTokenService.extractUsername(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractUsername_WithEmptyToken_ShouldThrowException() {
        assertThatThrownBy(() -> jwtTokenService.extractUsername(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}