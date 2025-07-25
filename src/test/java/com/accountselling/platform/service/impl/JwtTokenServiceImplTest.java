package com.accountselling.platform.service.impl;

import static org.assertj.core.api.Assertions.*;

import com.accountselling.platform.exception.InvalidTokenException;
import com.accountselling.platform.exception.TokenExpiredException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for JwtTokenServiceImpl. Tests JWT token generation, validation, and extraction
 * functionality. Updated to test new exception handling with TokenExpiredException and
 * InvalidTokenException.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenServiceImplTest {

  private JwtTokenServiceImpl jwtTokenService;
  private UserDetails userDetails;

  private static final String TEST_SECRET =
      Base64.getEncoder()
          .encodeToString("test-secret-key-that-is-long-enough-for-hmac-256".getBytes());
  private static final long ACCESS_TOKEN_EXPIRATION = 900000; // 15 minutes
  private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days

  @BeforeEach
  void setUp() {
    jwtTokenService = new JwtTokenServiceImpl();

    ReflectionTestUtils.setField(jwtTokenService, "secretKey", TEST_SECRET);
    ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
    ReflectionTestUtils.setField(
        jwtTokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

    userDetails =
        User.builder()
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
  void extractUsername_WithInvalidToken_ShouldThrowInvalidTokenException() {
    String invalidToken = "invalid.token.here";

    assertThatThrownBy(() -> jwtTokenService.extractUsername(invalidToken))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("JWT token is malformed");
  }

  @Test
  void extractUsername_WithNullToken_ShouldThrowInvalidTokenException() {
    assertThatThrownBy(() -> jwtTokenService.extractUsername(null))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("JWT token is invalid");
  }

  @Test
  void extractUsername_WithEmptyToken_ShouldThrowInvalidTokenException() {
    assertThatThrownBy(() -> jwtTokenService.extractUsername(""))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("JWT token is invalid");
  }

  @Test
  void extractUsername_WithTamperedSignature_ShouldThrowInvalidTokenException() {
    String token = jwtTokenService.generateAccessToken(userDetails);
    String tamperedToken = token.substring(0, token.lastIndexOf('.')) + ".wrong_signature";

    assertThatThrownBy(() -> jwtTokenService.extractUsername(tamperedToken))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("JWT token signature is invalid");
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

    UserDetails differentUser =
        User.builder()
            .username("differentuser")
            .password("password")
            .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
            .build();

    boolean isValid = jwtTokenService.isTokenValid(token, differentUser);

    assertThat(isValid).isFalse();
  }

  @Test
  void isTokenValid_WithInvalidToken_ShouldReturnFalse() {
    String invalidToken = "invalid.token.here";

    boolean isValid = jwtTokenService.isTokenValid(invalidToken, userDetails);

    assertThat(isValid).isFalse();
  }

  @Test
  void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
    // Create a token with very short expiration
    ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", 1L); // 1ms
    String expiredToken = jwtTokenService.generateAccessToken(userDetails);

    // Wait for token to expire
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
    }

    // Reset expiration for other operations
    ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);

    boolean isValid = jwtTokenService.isTokenValid(expiredToken, userDetails);

    assertThat(isValid).isFalse();
  }

  @Test
  void isTokenExpired_WithFreshToken_ShouldReturnFalse() {
    String token = jwtTokenService.generateAccessToken(userDetails);

    boolean isExpired = jwtTokenService.isTokenExpired(token);

    assertThat(isExpired).isFalse();
  }

  @Test
  void isTokenExpired_WithExpiredToken_ShouldReturnTrue() {
    // Create a token with very short expiration
    ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", 1L); // 1ms
    String expiredToken = jwtTokenService.generateAccessToken(userDetails);

    // Wait for token to expire
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
    }

    // Reset expiration for other operations
    ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);

    boolean isExpired = jwtTokenService.isTokenExpired(expiredToken);

    assertThat(isExpired).isTrue();
  }

  @Test
  void isTokenExpired_WithInvalidToken_ShouldReturnTrue() {
    String invalidToken = "invalid.token.here";

    boolean isExpired = jwtTokenService.isTokenExpired(invalidToken);

    assertThat(isExpired).isTrue();
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

  // Additional tests for comprehensive coverage of new exception handling

  @Test
  void extractUsername_FromExpiredToken_ShouldThrowTokenExpiredException() {
    // Create a token with very short expiration
    ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", 1L); // 1ms
    String expiredToken = jwtTokenService.generateAccessToken(userDetails);

    // Wait for token to expire
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
    }

    // Reset expiration for other operations
    ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);

    assertThatThrownBy(() -> jwtTokenService.extractUsername(expiredToken))
        .isInstanceOf(TokenExpiredException.class)
        .hasMessageContaining("JWT token has expired");
  }

  @Test
  void tokenValidation_HandlesExceptionsGracefully() {
    // Test that validation methods handle exceptions gracefully and return false
    // instead of throwing exceptions in isTokenValid and isTokenExpired methods

    String invalidToken = "completely.invalid.token";

    // These should not throw exceptions
    assertThat(jwtTokenService.isTokenValid(invalidToken, userDetails)).isFalse();
    assertThat(jwtTokenService.isTokenExpired(invalidToken)).isTrue();
  }
}
