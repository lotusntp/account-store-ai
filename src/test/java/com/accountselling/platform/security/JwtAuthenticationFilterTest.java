package com.accountselling.platform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.accountselling.platform.exception.InvalidTokenException;
import com.accountselling.platform.exception.TokenExpiredException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider tokenProvider;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

  private StringWriter responseWriter;

  @BeforeEach
  void setUp() throws IOException {
    SecurityContextHolder.clearContext();
    responseWriter = new StringWriter();
    // Only create PrintWriter when needed in specific tests
  }

  @Nested
  @DisplayName("Token Extraction Tests")
  class TokenExtractionTests {

    @Test
    @DisplayName("Should extract valid JWT token from Authorization header")
    void shouldExtractValidTokenFromAuthorizationHeader() throws ServletException, IOException {
      // Given
      String token = "validJwtToken";
      String bearerToken = "Bearer " + token;
      when(request.getHeader("Authorization")).thenReturn(bearerToken);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(tokenProvider.validateToken(token)).thenReturn(true);
      when(tokenProvider.getAuthentication(token)).thenReturn(createMockAuthentication());

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(tokenProvider).validateToken(token);
      verify(tokenProvider).getAuthentication(token);
      verify(filterChain).doFilter(request, response);
      assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should handle missing Authorization header")
    void shouldHandleMissingAuthorizationHeader() throws ServletException, IOException {
      // Given
      when(request.getHeader("Authorization")).thenReturn(null);
      when(request.getRequestURI()).thenReturn("/api/users/profile");

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(tokenProvider, never()).validateToken(any());
      verify(filterChain).doFilter(request, response);
      assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should handle Authorization header without Bearer prefix")
    void shouldHandleAuthorizationHeaderWithoutBearerPrefix() throws ServletException, IOException {
      // Given
      when(request.getHeader("Authorization")).thenReturn("invalidToken");
      when(request.getRequestURI()).thenReturn("/api/users/profile");

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(tokenProvider, never()).validateToken(any());
      verify(filterChain).doFilter(request, response);
      assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should handle empty token after Bearer prefix")
    void shouldHandleEmptyTokenAfterBearerPrefix() throws ServletException, IOException {
      // Given
      when(request.getHeader("Authorization")).thenReturn("Bearer ");
      when(request.getRequestURI()).thenReturn("/api/users/profile");

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(tokenProvider, never()).validateToken(any());
      verify(filterChain).doFilter(request, response);
      assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
  }

  @Nested
  @DisplayName("Token Validation Tests")
  class TokenValidationTests {

    @Test
    @DisplayName("Should authenticate user with valid token")
    void shouldAuthenticateUserWithValidToken() throws ServletException, IOException {
      // Given
      String token = "validJwtToken";
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(tokenProvider.validateToken(token)).thenReturn(true);

      Authentication mockAuth = createMockAuthentication();
      when(tokenProvider.getAuthentication(token)).thenReturn(mockAuth);

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
      assertEquals(mockAuth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should not authenticate user with invalid token")
    void shouldNotAuthenticateUserWithInvalidToken() throws ServletException, IOException {
      // Given
      String token = "invalidJwtToken";
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(tokenProvider.validateToken(token)).thenReturn(false);

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(tokenProvider, never()).getAuthentication(token);
      verify(filterChain).doFilter(request, response);
      assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
  }

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("Should handle ExpiredJwtException")
    void shouldHandleExpiredJwtException() throws ServletException, IOException {
      // Given
      String token = "expiredToken";
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
      when(tokenProvider.validateToken(token))
          .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(response).setContentType("application/json");
      verify(filterChain, never()).doFilter(request, response);
      assertTrue(responseWriter.toString().contains("Token expired"));
    }

    @Test
    @DisplayName("Should handle MalformedJwtException")
    void shouldHandleMalformedJwtException() throws ServletException, IOException {
      // Given
      String token = "malformedToken";
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
      when(tokenProvider.validateToken(token))
          .thenThrow(new MalformedJwtException("Malformed token"));

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(response).setContentType("application/json");
      verify(filterChain, never()).doFilter(request, response);
      assertTrue(responseWriter.toString().contains("Invalid token"));
    }

    @Test
    @DisplayName("Should handle SecurityException")
    void shouldHandleSecurityException() throws ServletException, IOException {
      // Given
      String token = "invalidSignatureToken";
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
      when(tokenProvider.validateToken(token))
          .thenThrow(new SecurityException("Invalid signature"));

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(response).setContentType("application/json");
      verify(filterChain, never()).doFilter(request, response);
      assertTrue(responseWriter.toString().contains("Invalid token"));
    }

    @Test
    @DisplayName("Should handle TokenExpiredException")
    void shouldHandleTokenExpiredException() throws ServletException, IOException {
      // Given
      String token = "expiredCustomToken";
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
      when(tokenProvider.validateToken(token))
          .thenThrow(new TokenExpiredException("Custom token expired"));

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(response).setContentType("application/json");
      verify(filterChain, never()).doFilter(request, response);
      assertTrue(responseWriter.toString().contains("Custom token expired"));
    }

    @Test
    @DisplayName("Should handle InvalidTokenException")
    void shouldHandleInvalidTokenException() throws ServletException, IOException {
      // Given
      String token = "invalidCustomToken";
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
      when(tokenProvider.validateToken(token))
          .thenThrow(new InvalidTokenException("Custom invalid token"));

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(response).setContentType("application/json");
      verify(filterChain, never()).doFilter(request, response);
      assertTrue(responseWriter.toString().contains("Custom invalid token"));
    }

    @Test
    @DisplayName("Should handle unexpected exceptions gracefully")
    void shouldHandleUnexpectedExceptionsGracefully() throws ServletException, IOException {
      // Given
      String token = "tokenCausingUnexpectedException";
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(request.getRequestURI()).thenReturn("/api/users/profile");
      when(tokenProvider.validateToken(token)).thenThrow(new RuntimeException("Unexpected error"));

      // When
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
      assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
  }

  @Nested
  @DisplayName("shouldNotFilter Tests")
  class ShouldNotFilterTests {

    @Test
    @DisplayName("Should skip authentication for auth endpoints")
    void shouldSkipAuthenticationForAuthEndpoints() {
      // Given
      when(request.getRequestURI()).thenReturn("/api/auth/login");

      // When
      boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should skip authentication for categories endpoints")
    void shouldSkipAuthenticationForCategoriesEndpoints() {
      // Given
      when(request.getRequestURI()).thenReturn("/api/categories");

      // When
      boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should skip authentication for products endpoints")
    void shouldSkipAuthenticationForProductsEndpoints() {
      // Given
      when(request.getRequestURI()).thenReturn("/api/products");

      // When
      boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should skip authentication for payment webhook")
    void shouldSkipAuthenticationForPaymentWebhook() {
      // Given
      when(request.getRequestURI()).thenReturn("/api/payments/webhook");

      // When
      boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should skip authentication for swagger endpoints")
    void shouldSkipAuthenticationForSwaggerEndpoints() {
      // Given
      when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

      // When
      boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should skip authentication for actuator endpoints")
    void shouldSkipAuthenticationForActuatorEndpoints() {
      // Given
      when(request.getRequestURI()).thenReturn("/actuator/health");

      // When
      boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should NOT skip authentication for protected endpoints")
    void shouldNotSkipAuthenticationForProtectedEndpoints() {
      // Given
      when(request.getRequestURI()).thenReturn("/api/users/profile");

      // When
      boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

      // Then
      assertFalse(result);
    }
  }

  private Authentication createMockAuthentication() {
    User userPrincipal =
        new User("testuser", "password", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
    return new UsernamePasswordAuthenticationToken(
        userPrincipal, null, userPrincipal.getAuthorities());
  }
}
