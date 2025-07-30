package com.accountselling.platform.security;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final int BEARER_PREFIX_LENGTH = 7;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String requestURI = request.getRequestURI();
    log.debug("Processing JWT authentication for request: {}", requestURI);

    try {
      String jwt = extractJwtFromRequest(request);

      if (StringUtils.hasText(jwt)) {
        validateAndSetAuthentication(jwt, request);
      } else {
        log.debug("No JWT token found in request to {}", requestURI);
      }
    } catch (ExpiredJwtException ex) {
      log.warn("JWT token expired for request to {}: {}", requestURI, ex.getMessage());
      handleTokenException(response, "Token expired", HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (SecurityException | MalformedJwtException ex) {
      log.warn("Invalid JWT token for request to {}: {}", requestURI, ex.getMessage());
      handleTokenException(response, "Invalid token", HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (TokenExpiredException ex) {
      log.warn("Token expired for request to {}: {}", requestURI, ex.getMessage());
      handleTokenException(response, ex.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (InvalidTokenException ex) {
      log.warn("Invalid token for request to {}: {}", requestURI, ex.getMessage());
      handleTokenException(response, ex.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (Exception ex) {
      log.error("Unexpected error processing JWT authentication for request to {}", requestURI, ex);
      // Don't return here - let the request continue without authentication
      // This allows the security configuration to handle unauthorized access
    }

    filterChain.doFilter(request, response);
  }

  private void validateAndSetAuthentication(String jwt, HttpServletRequest request) {
    if (tokenProvider.validateToken(jwt)) {
      Authentication authentication = tokenProvider.getAuthentication(jwt);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      String username = authentication.getName();
      log.debug(
          "Successfully authenticated user: {} for request: {}", username, request.getRequestURI());
    } else {
      log.debug("JWT token validation failed for request: {}", request.getRequestURI());
    }
  }

  private String extractJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      String token = bearerToken.substring(BEARER_PREFIX_LENGTH);

      // Additional validation for token format
      if (token.trim().isEmpty()) {
        log.warn("Empty JWT token found in Authorization header");
        return null;
      }

      return token;
    }

    return null;
  }

  private void handleTokenException(HttpServletResponse response, String message, int statusCode)
      throws IOException {
    response.setStatus(statusCode);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    String jsonResponse =
        String.format(
            "{\"error\": \"Authentication failed\", \"message\": \"%s\", \"timestamp\": %d}",
            message, System.currentTimeMillis());

    response.getWriter().write(jsonResponse);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();

    // Skip JWT authentication for public endpoints
    return path.startsWith("/api/auth/")
        || path.startsWith("/api/categories")
        || path.startsWith("/api/products")
        || path.equals("/api/payments/webhook")
        || path.startsWith("/api-docs")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/actuator")
        || path.startsWith("/api/observability");
  }
}
