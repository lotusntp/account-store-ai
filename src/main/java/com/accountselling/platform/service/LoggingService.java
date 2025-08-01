package com.accountselling.platform.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.marker.Markers;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Service for structured logging with Elasticsearch integration. Provides methods for logging
 * business events, security events, and system events with proper structured data for analysis and
 * monitoring.
 */
@Slf4j
@Service
public class LoggingService {

  private static final String TRACE_ID_KEY = "traceId";
  private static final String SPAN_ID_KEY = "spanId";
  private static final String USER_ID_KEY = "userId";
  private static final String SESSION_ID_KEY = "sessionId";
  private static final String REQUEST_ID_KEY = "requestId";

  /** Add MDC context to marker for consistent tracing */
  private net.logstash.logback.marker.LogstashMarker addMdcToMarker(
      net.logstash.logback.marker.LogstashMarker marker) {
    // Add tracing context from preserved MDC
    String traceId = MDC.get(TRACE_ID_KEY);
    if (traceId != null) {
      marker = marker.and(Markers.append(TRACE_ID_KEY, traceId));
    }

    String spanId = MDC.get(SPAN_ID_KEY);
    if (spanId != null) {
      marker = marker.and(Markers.append(SPAN_ID_KEY, spanId));
    }

    // Add request correlation IDs
    String requestId = MDC.get(REQUEST_ID_KEY);
    if (requestId != null) {
      marker = marker.and(Markers.append(REQUEST_ID_KEY, requestId));
    }

    String mdcRequestId = MDC.get("request.id");
    if (mdcRequestId != null) {
      marker = marker.and(Markers.append("request.id", mdcRequestId));
    }

    // Add user context if available
    String userId = MDC.get(USER_ID_KEY);
    if (userId != null) {
      marker = marker.and(Markers.append(USER_ID_KEY, userId));
    }

    String sessionId = MDC.get(SESSION_ID_KEY);
    if (sessionId != null) {
      marker = marker.and(Markers.append(SESSION_ID_KEY, sessionId));
    }

    return marker;
  }

  /** Log a business event with structured data */
  public void logBusinessEvent(String eventType, String message, Map<String, Object> data) {
    var baseMarker =
        Markers.append("eventType", eventType)
            .and(Markers.append("timestamp", Instant.now().toString()))
            .and(Markers.append("category", "business"));

    // Add MDC context (traceId, requestId, etc.)
    var markerWithMdc = addMdcToMarker(baseMarker);

    // Create final marker for lambda usage
    final var finalMarker =
        (data != null && !data.isEmpty())
            ? data.entrySet().stream()
                .reduce(
                    markerWithMdc,
                    (marker, entry) -> marker.and(Markers.append(entry.getKey(), entry.getValue())),
                    (m1, m2) -> m1)
            : markerWithMdc;

    log.info(finalMarker, "Business Event: {}", message);
  }

  /** Log a security event with structured data */
  public void logSecurityEvent(
      String eventType, String message, String userId, Map<String, Object> data) {
    var marker =
        Markers.append("eventType", eventType)
            .and(Markers.append("timestamp", Instant.now().toString()))
            .and(Markers.append("category", "security"))
            .and(Markers.append("userId", userId));

    // Add MDC context (traceId, requestId, etc.)
    var finalMarker = addMdcToMarker(marker);

    if (data != null && !data.isEmpty()) {
      data.forEach((key, value) -> finalMarker.and(Markers.append(key, value)));
    }

    log.warn(finalMarker, "Security Event: {}", message);
  }

  /** Log a system event with structured data */
  public void logSystemEvent(String eventType, String message, Map<String, Object> data) {
    var baseMarker =
        Markers.append("eventType", eventType)
            .and(Markers.append("timestamp", Instant.now().toString()))
            .and(Markers.append("category", "system"));

    // Check if tracing context is available
    boolean hasTracing = hasTracingContext();
    if (!hasTracing) {
      // Add warning marker when tracing context is missing
      baseMarker = baseMarker.and(Markers.append("tracingContextMissing", true));
    }

    // Add MDC context (traceId, requestId, etc.)
    var markerWithMdc = addMdcToMarker(baseMarker);

    // Create final marker for lambda usage
    final var finalMarker =
        (data != null && !data.isEmpty())
            ? data.entrySet().stream()
                .reduce(
                    markerWithMdc,
                    (marker, entry) -> marker.and(Markers.append(entry.getKey(), entry.getValue())),
                    (m1, m2) -> m1)
            : markerWithMdc;

    log.info(finalMarker, "System Event: {}", message);
  }

  /** Log an error with structured data and exception details */
  public void logError(
      String eventType, String message, Throwable throwable, Map<String, Object> data) {
    var marker =
        Markers.append("eventType", eventType)
            .and(Markers.append("timestamp", Instant.now().toString()))
            .and(Markers.append("category", "error"))
            .and(Markers.append("exceptionClass", throwable.getClass().getSimpleName()))
            .and(Markers.append("exceptionMessage", throwable.getMessage()));

    if (data != null && !data.isEmpty()) {
      data.forEach((key, value) -> marker.and(Markers.append(key, value)));
    }

    log.error(marker, "Error Event: {}", message, throwable);
  }

  /** Log user activity with structured data */
  public void logUserActivity(
      String userId, String activity, String details, Map<String, Object> data) {
    var marker =
        Markers.append("eventType", "user_activity")
            .and(Markers.append("timestamp", Instant.now().toString()))
            .and(Markers.append("category", "user"))
            .and(Markers.append("userId", userId))
            .and(Markers.append("activity", activity));

    if (data != null && !data.isEmpty()) {
      data.forEach((key, value) -> marker.and(Markers.append(key, value)));
    }

    log.info(marker, "User Activity: {}", details);
  }

  /** Log API request with structured data */
  public void logApiRequest(
      String method,
      String endpoint,
      String userId,
      long duration,
      int statusCode,
      Map<String, Object> data) {
    var marker =
        Markers.append("eventType", "api_request")
            .and(Markers.append("timestamp", Instant.now().toString()))
            .and(Markers.append("category", "api"))
            .and(Markers.append("httpMethod", method))
            .and(Markers.append("endpoint", endpoint))
            .and(Markers.append("userId", userId))
            .and(Markers.append("duration", duration))
            .and(Markers.append("statusCode", statusCode));

    if (data != null && !data.isEmpty()) {
      data.forEach((key, value) -> marker.and(Markers.append(key, value)));
    }

    log.info(
        marker, "API Request: {} {} - {} ms - Status: {}", method, endpoint, duration, statusCode);
  }

  /** Log database operation with structured data */
  public void logDatabaseOperation(
      String operation, String table, long duration, Map<String, Object> data) {
    var marker =
        Markers.append("eventType", "database_operation")
            .and(Markers.append("timestamp", Instant.now().toString()))
            .and(Markers.append("category", "database"))
            .and(Markers.append("operation", operation))
            .and(Markers.append("table", table))
            .and(Markers.append("duration", duration));

    if (data != null && !data.isEmpty()) {
      data.forEach((key, value) -> marker.and(Markers.append(key, value)));
    }

    log.info(marker, "Database Operation: {} on {} - {} ms", operation, table, duration);
  }

  /** Set correlation ID for request tracing */
  public void setCorrelationId(String correlationId) {
    MDC.put(REQUEST_ID_KEY, correlationId);
  }

  /** Set user context for logging */
  public void setUserContext(String userId, String sessionId) {
    MDC.put(USER_ID_KEY, userId);
    MDC.put(SESSION_ID_KEY, sessionId);
  }

  /** Clear MDC context */
  public void clearContext() {
    MDC.clear();
  }

  /** Generate a new correlation ID with graceful degradation */
  public String generateCorrelationId() {
    try {
      return UUID.randomUUID().toString();
    } catch (Exception e) {
      // Fallback to timestamp-based ID if UUID fails
      try {
        String fallbackId =
            "fallback-"
                + System.currentTimeMillis()
                + "-"
                + Math.abs(Thread.currentThread().hashCode());
        log.warn("UUID generation failed, using fallback correlation ID: {}", fallbackId);
        return fallbackId;
      } catch (Exception fallbackException) {
        // Absolute fallback
        String emergencyId = "emergency-" + System.nanoTime();
        System.err.println(
            "CRITICAL: Cannot generate any correlation ID, using emergency: " + emergencyId);
        return emergencyId;
      }
    }
  }

  /**
   * Creates an emergency correlation ID when all other methods fail This method guarantees a
   * correlation ID is always available
   *
   * @param context additional context for the emergency ID generation
   * @return emergency correlation ID that will never be null
   */
  public String createEmergencyCorrelationId(String context) {
    try {
      String emergencyId = "emergency-" + context + "-" + System.currentTimeMillis();

      // Try to set it in MDC if possible
      try {
        MDC.put(REQUEST_ID_KEY, emergencyId);
        MDC.put("request.id", emergencyId);
        MDC.put("tracingDegraded", "true");
        MDC.put("degradationReason", "emergency_correlation_creation");
        MDC.put("emergencyMode", "true");

        log.warn("Created emergency correlation ID: {} for context: {}", emergencyId, context);
      } catch (Exception mdcException) {
        // Even if MDC fails, return the ID
        log.error("Cannot set emergency correlation ID in MDC: {}", mdcException.getMessage());
      }

      return emergencyId;
    } catch (Exception e) {
      // Absolute fallback - should never happen but just in case
      String absoluteFallback = "absolute-fallback-" + Math.abs(e.hashCode());
      System.err.println(
          "CRITICAL: Emergency correlation ID creation failed, using absolute fallback: "
              + absoluteFallback);
      return absoluteFallback;
    }
  }

  /**
   * Validates and repairs correlation context in MDC Ensures there's always some form of
   * correlation ID available
   *
   * @return the correlation ID that is now guaranteed to be in MDC
   */
  public String ensureCorrelationId() {
    try {
      // Check for existing correlation ID
      String existingId = MDC.get(REQUEST_ID_KEY);
      if (existingId == null) {
        existingId = MDC.get("request.id");
      }

      if (existingId != null && !existingId.trim().isEmpty()) {
        // Ensure both keys are set
        MDC.put(REQUEST_ID_KEY, existingId);
        MDC.put("request.id", existingId);
        return existingId;
      }

      // No existing ID found, create new one
      String newId = generateCorrelationId();
      MDC.put(REQUEST_ID_KEY, newId);
      MDC.put("request.id", newId);
      MDC.put("correlationIdGenerated", "true");

      log.debug("Generated new correlation ID: {}", newId);
      return newId;

    } catch (Exception e) {
      // Fallback to emergency creation
      log.warn("Failed to ensure correlation ID, creating emergency ID: {}", e.getMessage());
      return createEmergencyCorrelationId("ensure-fallback");
    }
  }

  /**
   * Validates that tracing context is present in MDC
   *
   * @return true if tracing context (traceId) is available
   */
  public boolean hasTracingContext() {
    return MDC.get(TRACE_ID_KEY) != null;
  }

  /**
   * Gets current tracing context information for debugging
   *
   * @return Map containing current MDC tracing fields
   */
  public Map<String, String> getTracingContext() {
    Map<String, String> context = new java.util.HashMap<>();

    String traceId = MDC.get(TRACE_ID_KEY);
    if (traceId != null) {
      context.put(TRACE_ID_KEY, traceId);
    }

    String spanId = MDC.get(SPAN_ID_KEY);
    if (spanId != null) {
      context.put(SPAN_ID_KEY, spanId);
    }

    String requestId = MDC.get(REQUEST_ID_KEY);
    if (requestId != null) {
      context.put(REQUEST_ID_KEY, requestId);
    }

    String mdcRequestId = MDC.get("request.id");
    if (mdcRequestId != null) {
      context.put("request.id", mdcRequestId);
    }

    return context;
  }

  /**
   * Log metrics about tracing context preservation
   *
   * @param contextPreserved whether context was successfully preserved
   * @param captureMethod the method used to capture context (e.g., "ResponseTracingWrapper",
   *     "TracingAwareFilterChain")
   * @param requestUri the request URI for context
   */
  public void logContextPreservationMetrics(
      boolean contextPreserved, String captureMethod, String requestUri) {
    var marker =
        Markers.append("eventType", "context_preservation_metrics")
            .and(Markers.append("timestamp", Instant.now().toString()))
            .and(Markers.append("category", "metrics"))
            .and(Markers.append("contextPreserved", contextPreserved))
            .and(Markers.append("captureMethod", captureMethod))
            .and(Markers.append("requestUri", requestUri));

    // Add current MDC context
    var finalMarker = addMdcToMarker(marker);

    if (contextPreserved) {
      log.info(finalMarker, "Tracing context successfully preserved using {}", captureMethod);
    } else {
      log.warn(
          finalMarker,
          "Failed to preserve tracing context for {} using {}",
          requestUri,
          captureMethod);
    }
  }

  /**
   * Log fallback correlation when tracing context is unavailable Enhanced with graceful degradation
   * capabilities
   *
   * @param correlationId the fallback correlation ID used
   * @param reason the reason why tracing context was unavailable
   */
  public void logFallbackCorrelation(String correlationId, String reason) {
    try {
      var marker =
          Markers.append("eventType", "fallback_correlation")
              .and(Markers.append("timestamp", Instant.now().toString()))
              .and(Markers.append("category", "fallback"))
              .and(Markers.append("correlationId", correlationId))
              .and(Markers.append("reason", reason))
              .and(Markers.append("tracingContextMissing", true));

      log.warn(marker, "Using fallback correlation ID {} due to: {}", correlationId, reason);
    } catch (Exception e) {
      // Graceful degradation: If even logging the fallback fails, use basic logging
      try {
        log.warn(
            "Fallback correlation ID: {} (reason: {}) - structured logging failed: {}",
            correlationId,
            reason,
            e.getMessage());
      } catch (Exception basicLogException) {
        // Last resort: system out
        System.err.println(
            "CRITICAL: Cannot log fallback correlation - "
                + correlationId
                + " - "
                + reason
                + " - "
                + basicLogException.getMessage());
      }
    }
  }

  /** Log with structured arguments for better Elasticsearch indexing */
  public void logWithStructuredArgs(String level, String message, Object... keyValuePairs) {
    // Convert key-value pairs to structured arguments
    Object[] structuredArgs = new Object[keyValuePairs.length / 2];
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      if (i + 1 < keyValuePairs.length) {
        structuredArgs[i / 2] =
            StructuredArguments.keyValue(keyValuePairs[i].toString(), keyValuePairs[i + 1]);
      }
    }

    switch (level.toUpperCase()) {
      case "DEBUG":
        log.debug(message, structuredArgs);
        break;
      case "INFO":
        log.info(message, structuredArgs);
        break;
      case "WARN":
        log.warn(message, structuredArgs);
        break;
      case "ERROR":
        log.error(message, structuredArgs);
        break;
      default:
        log.info(message, structuredArgs);
    }
  }
}
