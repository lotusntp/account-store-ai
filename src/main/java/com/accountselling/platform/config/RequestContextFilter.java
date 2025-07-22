package com.accountselling.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Filter to add request context information to MDC for logging */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    long startTime = System.currentTimeMillis();
    String requestId = UUID.randomUUID().toString();

    try {
      // Add HTTP request information to MDC
      MDC.put("http.request.method", request.getMethod());
      MDC.put("http.request.path", request.getRequestURI());
      MDC.put("http.request.remote_ip", getClientIp(request));
      MDC.put("request.id", requestId);

      // Log that filter is being applied
      log.debug(
          "RequestContextFilter applied for URI: {}, Method: {}, RequestId: {}",
          request.getRequestURI(),
          request.getMethod(),
          requestId);

      // Add session information if available
      if (request.getSession(false) != null) {
        MDC.put("session.id", request.getSession().getId());
      }

      // Add user information if authenticated
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
        MDC.put("user.id", auth.getName());
        MDC.put("user.name", auth.getName());
      }

      // Add request ID to response header for correlation
      response.addHeader("X-Request-ID", requestId);

      // Process the request
      ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
      filterChain.doFilter(request, responseWrapper);

      // Add response information to MDC
      MDC.put("http.response.status_code", String.valueOf(responseWrapper.getStatus()));

      // Add execution time
      long executionTime = System.currentTimeMillis() - startTime;
      MDC.put("execution.time_ms", String.valueOf(executionTime));

      // Commit the response
      responseWrapper.copyBodyToResponse();

    } finally {
      // Clean up MDC
      MDC.remove("http.request.method");
      MDC.remove("http.request.path");
      MDC.remove("http.request.remote_ip");
      MDC.remove("http.response.status_code");
      MDC.remove("request.id");
      MDC.remove("session.id");
      MDC.remove("user.id");
      MDC.remove("user.name");
      MDC.remove("execution.time_ms");
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
