package com.accountselling.platform.filter;

import com.accountselling.platform.service.LoggingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Simple filter to log HTTP request and response bodies for debugging. */
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

    // Create cacheable wrappers
    CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpRequest);
    CachedBodyHttpServletResponse wrappedResponse = new CachedBodyHttpServletResponse(httpResponse);

    // Use existing requestId from RequestContextInterceptor or generate new one
    String correlationId = org.slf4j.MDC.get("request.id");
    if (correlationId == null) {
      correlationId = java.util.UUID.randomUUID().toString();
      org.slf4j.MDC.put("request.id", correlationId);
    }

    // Set requestId in MDC for consistency
    org.slf4j.MDC.put("requestId", correlationId);
    httpResponse.setHeader("X-Correlation-ID", correlationId);

    long startTime = System.currentTimeMillis();

    // Log request immediately when it comes in
    logRequest(wrappedRequest, correlationId);

    // Capture traceId before processing (might be set by OpenTelemetry later)
    String initialTraceId = org.slf4j.MDC.get("traceId");

    // Variable to capture traceId during processing
    final String[] capturedTraceId = new String[1];

    // Create a custom FilterChain wrapper to capture traceId during processing
    FilterChain traceCapturingChain =
        new FilterChain() {
          @Override
          public void doFilter(ServletRequest request, ServletResponse response)
              throws IOException, ServletException {
            // Call the original chain
            chain.doFilter(request, response);

            // Capture traceId immediately after the chain completes (before any cleanup)
            String traceId = org.slf4j.MDC.get("traceId");
            if (traceId != null) {
              capturedTraceId[0] = traceId;
            }
          }
        };

    try {
      // Process the request with our custom chain
      traceCapturingChain.doFilter(wrappedRequest, wrappedResponse);

    } finally {
      long duration = System.currentTimeMillis() - startTime;

      // Get current traceId from MDC (might have been cleared by OpenTelemetry)
      String currentTraceId = org.slf4j.MDC.get("traceId");
      String savedTraceId = capturedTraceId[0];

      // Restore all context in MDC before logging response
      org.slf4j.MDC.put("requestId", correlationId);
      org.slf4j.MDC.put("request.id", correlationId);

      // Use the best available traceId
      String traceIdToUse = null;
      if (currentTraceId != null) {
        traceIdToUse = currentTraceId;
      } else if (savedTraceId != null) {
        traceIdToUse = savedTraceId;
      } else if (initialTraceId != null) {
        traceIdToUse = initialTraceId;
      }

      if (traceIdToUse != null) {
        org.slf4j.MDC.put("traceId", traceIdToUse);
      }

      // Log response when it goes out
      logResponse(wrappedRequest, wrappedResponse, duration, correlationId);

      // Write cached response to original response
      byte[] responseBody = wrappedResponse.getBodyBytes();
      if (responseBody.length > 0) {
        httpResponse.getOutputStream().write(responseBody);
      }

      // Clear MDC after logging response
      org.slf4j.MDC.clear();
    }
  }

  private boolean shouldLog(HttpServletRequest request) {
    String uri = request.getRequestURI();

    // Log API endpoints
    if (uri.startsWith("/api/")) {
      // Exclude health checks and metrics
      return !uri.equals("/api/health") && !uri.equals("/api/metrics");
    }

    return false;
  }

  private void logRequest(CachedBodyHttpServletRequest request, String correlationId) {
    try {
      String method = request.getMethod();
      String uri = request.getRequestURI();
      String queryString = request.getQueryString();
      String requestBody = request.getBody();

      // Prepare request log data
      Map<String, Object> logData = new HashMap<>();
      logData.put("method", method);
      logData.put("uri", uri);
      logData.put("queryString", queryString);
      logData.put("requestContentType", request.getContentType());
      logData.put("userAgent", request.getHeader("User-Agent"));
      logData.put("remoteAddr", getClientIpAddress(request));
      logData.put("correlationId", correlationId);
      logData.put("phase", "request");

      // Add request body if present
      if (requestBody != null && !requestBody.trim().isEmpty()) {
        if (isSensitiveEndpoint(uri)) {
          logData.put("requestBody", maskSensitiveData(requestBody));
        } else {
          logData.put("requestBody", requestBody);
        }
      }

      // Log request
      loggingService.logSystemEvent(
          "http_request_received", String.format("HTTP Request: %s %s", method, uri), logData);

      log.info("=== REQUEST RECEIVED ===");
      log.info("Method: {}, URI: {}, CorrelationId: {}", method, uri, correlationId);
      log.info("Request Body: {}", requestBody);
      log.info("========================");

    } catch (Exception e) {
      log.error("Error logging request", e);
    }
  }

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

      // Prepare response log data
      Map<String, Object> logData = new HashMap<>();
      logData.put("method", method);
      logData.put("uri", uri);
      logData.put("statusCode", statusCode);
      logData.put("duration", duration);
      logData.put("responseContentType", response.getContentType());
      logData.put("correlationId", correlationId);
      logData.put("phase", "response");

      // Add response body if present and not too large
      if (responseBody != null && !responseBody.trim().isEmpty() && responseBody.length() < 10000) {
        if (isSensitiveEndpoint(uri)) {
          logData.put("responseBody", maskSensitiveResponseData(responseBody));
        } else {
          logData.put("responseBody", responseBody);
        }
      }

      // Log response
      loggingService.logSystemEvent(
          "http_response_sent",
          String.format("HTTP Response: %s %s - %d (%dms)", method, uri, statusCode, duration),
          logData);

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

  private boolean isSensitiveEndpoint(String uri) {
    return uri.contains("/auth/")
        || uri.contains("/login")
        || uri.contains("/password")
        || uri.contains("/token");
  }

  private String maskSensitiveData(String requestBody) {
    try {
      // Parse JSON and mask sensitive fields
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

  private String maskSensitiveResponseData(String responseBody) {
    try {
      // Parse JSON and mask sensitive fields in response
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

  private String getCurrentUserId(HttpServletRequest request) {
    // Try to get user from security context or session
    return "anonymous"; // Default value
  }

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
