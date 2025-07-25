package com.accountselling.platform.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

  private JwtTokenProvider tokenProvider;

  @BeforeEach
  void setUp() {
    tokenProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(
        tokenProvider, "jwtSecret", "testSecretKeyWithAtLeast32CharactersForHmacSha256Algorithm");
    ReflectionTestUtils.setField(tokenProvider, "jwtExpirationMs", 60000); // 1 minute
    ReflectionTestUtils.setField(tokenProvider, "refreshExpirationMs", 120000); // 2 minutes
    tokenProvider.init();
  }

  @Test
  void generateTokenAndValidateToken() {
    // Create test user
    User userDetails =
        new User(
            "testuser",
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    // Generate token
    String token = tokenProvider.generateToken(authentication);

    // Validate token
    assertTrue(tokenProvider.validateToken(token));

    // Check username extraction
    assertEquals("testuser", tokenProvider.getUsernameFromToken(token));

    // Check authentication
    Authentication resultAuth = tokenProvider.getAuthentication(token);
    assertEquals("testuser", resultAuth.getName());
    assertTrue(
        resultAuth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
  }

  @Test
  void generateRefreshToken() {
    // Create test user
    User userDetails =
        new User(
            "testuser",
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    // Generate refresh token
    String refreshToken = tokenProvider.generateRefreshToken(authentication);

    // Validate token
    assertTrue(tokenProvider.validateToken(refreshToken));

    // Check username extraction
    assertEquals("testuser", tokenProvider.getUsernameFromToken(refreshToken));
  }
}
