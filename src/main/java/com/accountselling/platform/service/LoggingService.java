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
    String traceId = MDC.get("traceId");
    if (traceId != null) {
      marker = marker.and(Markers.append("traceId", traceId));
    }

    String requestId = MDC.get("requestId");
    if (requestId != null) {
      marker = marker.and(Markers.append("requestId", requestId));
    }

    String mdcRequestId = MDC.get("request.id");
    if (mdcRequestId != null) {
      marker = marker.and(Markers.append("request.id", mdcRequestId));
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

  /** Generate a new correlation ID */
  public String generateCorrelationId() {
    return UUID.randomUUID().toString();
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
