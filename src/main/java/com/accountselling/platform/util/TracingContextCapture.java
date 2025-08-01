package com.accountselling.platform.util;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Utility class for capturing and preserving OpenTelemetry tracing context. This class addresses
 * the issue where OpenTelemetry clears MDC context after request processing, causing response logs
 * to lose tracing information.
 */
@Slf4j
public class TracingContextCapture {

  private final String traceId;
  private final String spanId;
  private final String requestId;
  private final long captureTimestamp;
  private final Map<String, String> mdcSnapshot;
  private final boolean hasValidContext;

  /** Private constructor to create an immutable TracingContextCapture instance */
  private TracingContextCapture(
      String traceId,
      String spanId,
      String requestId,
      Map<String, String> mdcSnapshot,
      boolean hasValidContext) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.requestId = requestId;
    this.captureTimestamp = Instant.now().toEpochMilli();
    this.mdcSnapshot = new HashMap<>(mdcSnapshot != null ? mdcSnapshot : new HashMap<>());
    this.hasValidContext = hasValidContext;
  }

  /**
   * Captures the current tracing context from OpenTelemetry and MDC. This method should be called
   * immediately after request processing completes but before OpenTelemetry clears the context.
   *
   * @return TracingContextCapture instance with preserved context
   */
  public static TracingContextCapture capture() {
    try {
      // Get current span from OpenTelemetry
      Span currentSpan = Span.current();
      SpanContext spanContext = currentSpan.getSpanContext();

      String traceId = null;
      String spanId = null;
      boolean hasValidContext = false;

      // Extract traceId and spanId from OpenTelemetry
      if (spanContext.isValid()) {
        traceId = spanContext.getTraceId();
        spanId = spanContext.getSpanId();
        hasValidContext = true;
        // OpenTelemetry context captured successfully
      }

      // Fallback to MDC if OpenTelemetry context is not available
      if (!hasValidContext) {
        // Try common OpenTelemetry MDC keys
        traceId = MDC.get("traceId");
        spanId = MDC.get("spanId");

        // Also try alternative MDC keys that OpenTelemetry might use
        if (traceId == null) {
          traceId = MDC.get("trace_id");
        }
        if (spanId == null) {
          spanId = MDC.get("span_id");
        }

        // Try X-Trace-Id and X-Span-Id headers stored in MDC
        if (traceId == null) {
          traceId = MDC.get("X-Trace-Id");
        }
        if (spanId == null) {
          spanId = MDC.get("X-Span-Id");
        }

        if (traceId != null) {
          hasValidContext = true;
        }
      }

      // Get requestId from MDC
      String requestId = MDC.get("request.id");
      if (requestId == null) {
        requestId = MDC.get("requestId");
      }
      if (requestId == null) {
        requestId = UUID.randomUUID().toString();
      }

      // Capture current MDC state
      Map<String, String> mdcSnapshot = MDC.getCopyOfContextMap();

      return new TracingContextCapture(traceId, spanId, requestId, mdcSnapshot, hasValidContext);

    } catch (Exception e) {
      log.warn("Failed to capture tracing context: {}", e.getMessage());
      log.debug("Full exception details for tracing context capture failure", e);

      // Attempt to create a minimal fallback context
      try {
        String fallbackRequestId = UUID.randomUUID().toString();
        Map<String, String> fallbackMdc = new HashMap<>();

        // Try to preserve at least the request ID if available
        String existingRequestId = null;
        try {
          existingRequestId = MDC.get("request.id");
          if (existingRequestId == null) {
            existingRequestId = MDC.get("requestId");
          }
        } catch (Exception mdcException) {
          log.debug("Failed to get existing request ID from MDC", mdcException);
        }

        if (existingRequestId != null) {
          fallbackRequestId = existingRequestId;
          fallbackMdc.put("requestId", existingRequestId);
          fallbackMdc.put("request.id", existingRequestId);
        }

        return new TracingContextCapture(null, null, fallbackRequestId, fallbackMdc, false);

      } catch (Exception fallbackException) {
        log.error(
            "Critical failure: Unable to create even fallback tracing context", fallbackException);
        // Last resort: return minimal context
        return new TracingContextCapture(
            null, null, "emergency-" + System.currentTimeMillis(), new HashMap<>(), false);
      }
    }
  }

  /**
   * Restores the captured tracing context back to MDC. This method should be called before logging
   * response to ensure tracing information is available.
   */
  public void restore() {
    try {
      // Clear current MDC to prevent context pollution
      MDC.clear();

      // Restore captured MDC state
      if (mdcSnapshot != null && !mdcSnapshot.isEmpty()) {
        mdcSnapshot.forEach(MDC::put);
      }

      // Ensure tracing fields are set
      if (traceId != null) {
        MDC.put("traceId", traceId);
      }
      if (spanId != null) {
        MDC.put("spanId", spanId);
      }
      if (requestId != null) {
        MDC.put("requestId", requestId);
        MDC.put("request.id", requestId);
      }

      // Tracing context restored successfully

    } catch (Exception e) {
      log.error("Failed to restore tracing context", e);
      // At minimum, try to restore the request ID for correlation
      if (requestId != null) {
        try {
          MDC.put("requestId", requestId);
          MDC.put("request.id", requestId);
        } catch (Exception fallbackError) {
          log.error("Failed to restore even basic correlation context", fallbackError);
        }
      }
    }
  }

  /**
   * Cleans up the current MDC context. This method should be called after response logging is
   * complete to prevent context leakage between requests.
   */
  public void cleanup() {
    try {
      MDC.clear();
    } catch (Exception e) {
      log.error("Failed to cleanup MDC context", e);
    }
  }

  /**
   * Checks if this capture contains valid tracing context
   *
   * @return true if valid tracing context is available
   */
  public boolean hasValidContext() {
    return hasValidContext && traceId != null;
  }

  /**
   * Gets the captured trace ID
   *
   * @return trace ID or null if not available
   */
  public String getTraceId() {
    return traceId;
  }

  /**
   * Gets the captured span ID
   *
   * @return span ID or null if not available
   */
  public String getSpanId() {
    return spanId;
  }

  /**
   * Gets the request/correlation ID
   *
   * @return request ID (never null)
   */
  public String getRequestId() {
    return requestId;
  }

  /**
   * Gets the timestamp when this context was captured
   *
   * @return capture timestamp in milliseconds
   */
  public long getCaptureTimestamp() {
    return captureTimestamp;
  }

  /**
   * Gets a copy of the captured MDC snapshot
   *
   * @return copy of MDC state at capture time
   */
  public Map<String, String> getMdcSnapshot() {
    return new HashMap<>(mdcSnapshot);
  }

  /** Creates a string representation for debugging */
  @Override
  public String toString() {
    return String.format(
        "TracingContextCapture{traceId='%s', spanId='%s', requestId='%s', "
            + "hasValidContext=%s, captureTimestamp=%d}",
        traceId, spanId, requestId, hasValidContext, captureTimestamp);
  }
}
