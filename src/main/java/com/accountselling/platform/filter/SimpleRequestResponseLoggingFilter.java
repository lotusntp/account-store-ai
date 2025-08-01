package com.accountselling.platform.filter;

import com.accountselling.platform.service.LoggingService;
import com.accountselling.platform.util.TracingContextCapture;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Enhanced HTTP request/response logging filter with proper tracing context preservation. This
 * filter addresses the critical issue where OpenTelemetry clears MDC context after request
 * processing, causing response logs to lose tracing information.
 *
 * <p>Key improvements: - Uses TracingContextCapture to preserve OpenTelemetry context - Uses
 * TracingAwareFilterChain for optimal context capture timing - Ensures consistent traceId/spanId
 * across request and response logs - Maintains backward compatibility with existing logging
 * functionality
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleRequestResponseLoggingFilter implements Filter {

  private final LoggingService loggingService;
  private final ObjectMapper objectMapper;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Only log API requests
    if (!shouldLog(httpRequest)) {
      chain.doFilter(request, response);
      return;
    }

    // Create cacheable wrappers for request/response body capture
    CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpRequest);
    CachedBodyHttpServletResponse wrappedResponse = new CachedBodyHttpServletResponse(httpResponse);

    // Generate or use existing correlation ID
    String correlationId = getOrGenerateCorrelationId();
    httpResponse.setHeader("X-Correlation-ID", correlationId);

    // Record start time for duration calculation
    long startTime = System.currentTimeMillis();

    // Log incoming request immediately
    logRequest(wrappedRequest, correlationId);

    // AtomicReference to hold captured tracing context
    AtomicReference<TracingContextCapture> capturedContext = new AtomicReference<>();

    // Create ResponseTracingWrapper to capture context when response is committed
    // This is more reliable than capturing after chain completion
    ResponseTracingWrapper tracingResponseWrapper =
        new ResponseTracingWrapper(wrappedResponse, capturedContext::set);

    // Create TracingAwareFilterChain as backup mechanism
    TracingAwareFilterChain tracingChain =
        new TracingAwareFilterChain(
            chain,
            context -> {
              // Only set if ResponseTracingWrapper hasn't captured yet
              if (capturedContext.get() == null) {
                capturedContext.set(context);
              }
            });

    try {
      // Process request through the tracing-aware chain with response wrapper
      // ResponseTracingWrapper will capture context when response is committed
      // TracingAwareFilterChain serves as backup
      tracingChain.doFilter(wrappedRequest, tracingResponseWrapper);

    } finally {
      // Calculate request duration
      long duration = System.currentTimeMillis() - startTime;

      // Get the captured tracing context
      TracingContextCapture context = capturedContext.get();

      if (context != null) {
        // Restore the captured tracing context to MDC
        context.restore();

        // Log metrics about successful context preservation
        String captureMethod =
            tracingResponseWrapper.isContextCaptured()
                ? "ResponseTracingWrapper"
                : "TracingAwareFilterChain";
        loggingService.logContextPreservationMetrics(
            context.hasValidContext(), captureMethod, httpRequest.getRequestURI());
      } else {
        // Enhanced fallback logging with graceful degradation
        try {
          loggingService.logFallbackCorrelation(
              correlationId, "No tracing context captured by either mechanism");
        } catch (Exception fallbackLogException) {
          log.warn(
              "Failed to log fallback correlation, using basic logging: {}",
              fallbackLogException.getMessage());
        }

        // Enhanced fallback: Use LoggingService's emergency correlation method
        try {
          String emergencyCorrelationId =
              loggingService.createEmergencyCorrelationId("filter-no-context");
          log.info(
              "Created emergency correlation ID for request without tracing context: {}",
              emergencyCorrelationId);
        } catch (Exception emergencyException) {
          // Final fallback: at least set the correlation ID manually
          try {
            org.slf4j.MDC.put("requestId", correlationId);
            org.slf4j.MDC.put("request.id", correlationId);
            org.slf4j.MDC.put("tracingDegraded", "true");
            org.slf4j.MDC.put("degradationReason", "manual_correlation_fallback");
            log.warn("Manually set correlation ID as final fallback: {}", correlationId);
          } catch (Exception manualException) {
            log.error("Critical: Cannot set any correlation context", manualException);
          }
        }
      }

      try {
        // Log response with restored tracing context
        logResponse(wrappedRequest, wrappedResponse, duration, correlationId);

      } finally {
        // Write cached response body to actual response
        writeResponseBody(wrappedResponse, httpResponse);

        // Clean up MDC context to prevent leakage
        if (context != null) {
          context.cleanup();
        } else {
          org.slf4j.MDC.clear();
        }
      }
    }
  }

  /** Determines if the request should be logged based on URI patterns */
  private boolean shouldLog(HttpServletRequest request) {
    String uri = request.getRequestURI();

    // Log API endpoints
    if (uri.startsWith("/api/")) {
      // Exclude health checks and metrics
      return !uri.equals("/api/health") && !uri.equals("/api/metrics");
    }

    return false;
  }

  /** Gets existing correlation ID from MDC or generates a new one with graceful degradation */
  private String getOrGenerateCorrelationId() {
    try {
      // Use LoggingService's robust method for ensuring correlation ID
      return loggingService.ensureCorrelationId();
    } catch (Exception e) {
      log.warn(
          "Failed to ensure correlation ID through LoggingService, using emergency fallback: {}",
          e.getMessage());

      // Emergency fallback when even LoggingService fails
      try {
        String emergencyId = "filter-emergency-" + System.currentTimeMillis();
        org.slf4j.MDC.put("requestId", emergencyId);
        org.slf4j.MDC.put("request.id", emergencyId);
        org.slf4j.MDC.put("tracingDegraded", "true");
        org.slf4j.MDC.put("degradationReason", "filter_correlation_fallback");
        org.slf4j.MDC.put("emergencyMode", "true");

        log.warn("Created emergency correlation ID in filter: {}", emergencyId);
        return emergencyId;
      } catch (Exception emergencyException) {
        // Absolute last resort
        String absoluteEmergencyId =
            "absolute-emergency-" + Math.abs(emergencyException.hashCode());
        log.error(
            "Critical: Cannot create any correlation ID, using absolute emergency: {}",
            absoluteEmergencyId);
        return absoluteEmergencyId;
      }
    }
  }

  /** Logs incoming HTTP request with structured data */
  private void logRequest(CachedBodyHttpServletRequest request, String correlationId) {
    try {
      String method = request.getMethod();
      String uri = request.getRequestURI();
      String queryString = request.getQueryString();
      String requestBody = request.getBody();

      // Prepare structured log data
      Map<String, Object> logData = new HashMap<>();
      logData.put("method", method);
      logData.put("uri", uri);
      logData.put("queryString", queryString);
      logData.put("requestContentType", request.getContentType());
      logData.put("userAgent", request.getHeader("User-Agent"));
      logData.put("remoteAddr", getClientIpAddress(request));
      logData.put("correlationId", correlationId);
      logData.put("phase", "request");

      // Add request body if present (with sensitive data masking)
      if (requestBody != null && !requestBody.trim().isEmpty()) {
        if (isSensitiveEndpoint(uri)) {
          logData.put("requestBody", maskSensitiveData(requestBody));
        } else {
          logData.put("requestBody", requestBody);
        }
      }

      // Log structured request event (tracing context may not be available yet)
      loggingService.logSystemEvent(
          "http_request_received", String.format("HTTP Request: %s %s", method, uri), logData);

      // Additional console logging for debugging
      log.info("=== REQUEST RECEIVED ===");
      log.info("Method: {}, URI: {}, CorrelationId: {}", method, uri, correlationId);
      log.info("Request Body: {}", requestBody);
      log.info("========================");

    } catch (Exception e) {
      log.error("Error logging request", e);
    }
  }

  /** Logs outgoing HTTP response with structured data and preserved tracing context */
  private void logResponse(
      CachedBodyHttpServletRequest request,
      CachedBodyHttpServletResponse response,
      long duration,
      String correlationId) {
    try {
      String method = request.getMethod();
      String uri = request.getRequestURI();
      int statusCode = response.getStatus();
      String responseBody = response.getBody();

      // Prepare structured log data
      Map<String, Object> logData = new HashMap<>();
      logData.put("method", method);
      logData.put("uri", uri);
      logData.put("statusCode", statusCode);
      logData.put("duration", duration);
      logData.put("responseContentType", response.getContentType());
      logData.put("correlationId", correlationId);
      logData.put("phase", "response");

      // Add response body if present and not too large (with sensitive data masking)
      if (responseBody != null && !responseBody.trim().isEmpty() && responseBody.length() < 10000) {
        if (isSensitiveEndpoint(uri)) {
          logData.put("responseBody", maskSensitiveResponseData(responseBody));
        } else {
          logData.put("responseBody", responseBody);
        }
      }

      // Validate tracing context before logging
      if (!loggingService.hasTracingContext()) {
        log.warn("Response logging without tracing context for {} {}", method, uri);
      }

      // Log structured response event (this should now have tracing context)
      loggingService.logSystemEvent(
          "http_response_sent",
          String.format("HTTP Response: %s %s - %d (%dms)", method, uri, statusCode, duration),
          logData);

      // Additional console logging for debugging
      log.info("=== RESPONSE SENT ===");
      log.info(
          "Method: {}, URI: {}, Status: {}, Duration: {}ms, CorrelationId: {}",
          method,
          uri,
          statusCode,
          duration,
          correlationId);
      log.info("Response Body: {}", responseBody);
      log.info("====================");

    } catch (Exception e) {
      log.error("Error logging response", e);
    }
  }

  /** Writes cached response body to the actual HTTP response */
  private void writeResponseBody(
      CachedBodyHttpServletResponse wrappedResponse, HttpServletResponse httpResponse)
      throws IOException {
    try {
      byte[] responseBody = wrappedResponse.getBodyBytes();
      if (responseBody.length > 0) {
        httpResponse.getOutputStream().write(responseBody);
      }
    } catch (Exception e) {
      log.error("Error writing response body", e);
      throw e;
    }
  }

  /** Checks if the endpoint handles sensitive data that should be masked in logs */
  private boolean isSensitiveEndpoint(String uri) {
    return uri.contains("/auth/")
        || uri.contains("/login")
        || uri.contains("/password")
        || uri.contains("/token");
  }

  /** Masks sensitive data in request body for security */
  private String maskSensitiveData(String requestBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> jsonMap = objectMapper.readValue(requestBody, Map.class);

      // Mask common sensitive fields
      if (jsonMap.containsKey("password")) {
        jsonMap.put("password", "[MASKED]");
      }
      if (jsonMap.containsKey("token")) {
        jsonMap.put("token", "[MASKED]");
      }
      if (jsonMap.containsKey("secret")) {
        jsonMap.put("secret", "[MASKED]");
      }
      if (jsonMap.containsKey("credential")) {
        jsonMap.put("credential", "[MASKED]");
      }

      return objectMapper.writeValueAsString(jsonMap);
    } catch (Exception e) {
      return "[MASKED_REQUEST_BODY]";
    }
  }

  /** Masks sensitive data in response body for security */
  private String maskSensitiveResponseData(String responseBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> jsonMap = objectMapper.readValue(responseBody, Map.class);

      // Mask tokens and sensitive data in response
      if (jsonMap.containsKey("accessToken")) {
        String token = (String) jsonMap.get("accessToken");
        if (token != null && token.length() > 10) {
          jsonMap.put("accessToken", token.substring(0, 10) + "...[MASKED]");
        }
      }
      if (jsonMap.containsKey("refreshToken")) {
        String token = (String) jsonMap.get("refreshToken");
        if (token != null && token.length() > 10) {
          jsonMap.put("refreshToken", token.substring(0, 10) + "...[MASKED]");
        }
      }

      return objectMapper.writeValueAsString(jsonMap);
    } catch (Exception e) {
      return responseBody; // Return original if not JSON or parsing fails
    }
  }

  /** Extracts client IP address from request headers */
  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null
        && !xForwardedFor.isEmpty()
        && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }
}
