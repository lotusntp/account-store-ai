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
   * Creates an emergency TracingContextCapture for fallback scenarios This is used when
   * TracingContextCapture.capture() is not accessible
   */
  public static TracingContextCapture createEmergency(
      String traceId,
      String spanId,
      String requestId,
      Map<String, String> mdcSnapshot,
      boolean hasValidContext) {
    return new TracingContextCapture(traceId, spanId, requestId, mdcSnapshot, hasValidContext);
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

      TracingContextCapture capture =
          new TracingContextCapture(traceId, spanId, requestId, mdcSnapshot, hasValidContext);

      // Debug logging for context capture
      if (log.isDebugEnabled()) {
        log.debug(
            "🎯 TracingContextCapture created successfully: traceId={}, spanId={}, requestId={},"
                + " hasValidContext={}",
            traceId,
            spanId,
            requestId,
            hasValidContext);
        if (mdcSnapshot != null && !mdcSnapshot.isEmpty()) {
          log.debug("📋 MDC Snapshot: {}", mdcSnapshot);
        }
      }

      return capture;

    } catch (Exception e) {
      log.warn(
          "Failed to capture tracing context, attempting graceful degradation: {}", e.getMessage());
      log.debug("Full exception details for tracing context capture failure", e);

      // Graceful degradation: Attempt to create a minimal fallback context
      // This ensures the application never breaks due to tracing context failures
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
          log.debug(
              "Failed to get existing request ID from MDC during graceful degradation",
              mdcException);
        }

        if (existingRequestId != null) {
          fallbackRequestId = existingRequestId;
          fallbackMdc.put("requestId", existingRequestId);
          fallbackMdc.put("request.id", existingRequestId);
        }

        // Add fallback markers to indicate degraded mode
        fallbackMdc.put("tracingDegraded", "true");
        fallbackMdc.put("degradationReason", "context_capture_failure");

        log.info(
            "Successfully created fallback tracing context with correlation ID: {}",
            fallbackRequestId);
        return new TracingContextCapture(null, null, fallbackRequestId, fallbackMdc, false);

      } catch (Exception fallbackException) {
        log.error(
            "Critical failure during graceful degradation: Unable to create even fallback tracing"
                + " context",
            fallbackException);

        // Emergency fallback: return absolute minimal context to prevent system failure
        // This guarantees the application functionality is never compromised
        String emergencyId = "emergency-" + System.currentTimeMillis();
        Map<String, String> emergencyMdc = new HashMap<>();
        emergencyMdc.put("tracingDegraded", "true");
        emergencyMdc.put("degradationReason", "critical_fallback_failure");
        emergencyMdc.put("emergencyMode", "true");

        log.warn("Using emergency fallback context with ID: {}", emergencyId);
        return new TracingContextCapture(null, null, emergencyId, emergencyMdc, false);
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

      // Debug logging for successful restoration
      if (log.isDebugEnabled()) {
        log.debug(
            "✅ TracingContext restoration successful: traceId={}, spanId={}, requestId={}",
            traceId,
            spanId,
            requestId);
      }

    } catch (Exception e) {
      log.warn(
          "Failed to restore tracing context, attempting graceful recovery: {}", e.getMessage());
      log.debug("Full exception details for context restoration failure", e);

      // Graceful degradation: At minimum, try to restore the request ID for correlation
      if (requestId != null) {
        try {
          MDC.put("requestId", requestId);
          MDC.put("request.id", requestId);
          MDC.put("tracingDegraded", "true");
          MDC.put("degradationReason", "context_restoration_failure");
          log.info("Successfully restored basic correlation context with ID: {}", requestId);
        } catch (Exception fallbackError) {
          log.error(
              "Critical failure: Unable to restore even basic correlation context during graceful"
                  + " degradation",
              fallbackError);

          // Emergency fallback: Create a new correlation ID to maintain some traceability
          try {
            String emergencyId = "emergency-restore-" + System.currentTimeMillis();
            MDC.put("requestId", emergencyId);
            MDC.put("request.id", emergencyId);
            MDC.put("tracingDegraded", "true");
            MDC.put("degradationReason", "emergency_restoration_fallback");
            MDC.put("emergencyMode", "true");
            log.warn("Using emergency correlation ID for traceability: {}", emergencyId);
          } catch (Exception emergencyError) {
            log.error("Absolute failure: Cannot establish any correlation context", emergencyError);
            // At this point, we've done everything possible - let the application continue
          }
        }
      } else {
        // No requestId available, create emergency correlation
        try {
          String emergencyId = "emergency-no-request-" + System.currentTimeMillis();
          MDC.put("requestId", emergencyId);
          MDC.put("request.id", emergencyId);
          MDC.put("tracingDegraded", "true");
          MDC.put("degradationReason", "no_request_id_available");
          MDC.put("emergencyMode", "true");
          log.warn("Created emergency correlation ID due to missing requestId: {}", emergencyId);
        } catch (Exception emergencyError) {
          log.error("Cannot create emergency correlation ID", emergencyError);
        }
      }
    }
  }

  /**
   * Cleans up the current MDC context with enhanced leakage prevention. This method should be
   * called after response logging is complete to prevent context leakage between requests when
   * threads are reused.
   *
   * <p>Enhanced with comprehensive cleanup verification and fallback mechanisms.
   */
  public void cleanup() {
    try {
      // Capture current state for verification
      Map<String, String> beforeCleanup = MDC.getCopyOfContextMap();
      boolean hadContext = beforeCleanup != null && !beforeCleanup.isEmpty();

      // Primary cleanup method
      MDC.clear();

      // Verify cleanup was successful
      Map<String, String> afterCleanup = MDC.getCopyOfContextMap();
      boolean cleanupSuccessful = afterCleanup == null || afterCleanup.isEmpty();

      if (!cleanupSuccessful) {
        log.warn(
            "Primary MDC cleanup failed, {} keys remain: {}",
            afterCleanup.size(),
            afterCleanup.keySet());

        // Fallback: Remove keys individually
        performIndividualKeyCleanup(afterCleanup.keySet());

        // Re-verify after fallback cleanup
        Map<String, String> afterFallback = MDC.getCopyOfContextMap();
        boolean fallbackSuccessful = afterFallback == null || afterFallback.isEmpty();

        if (!fallbackSuccessful) {
          log.error(
              "Critical: Both primary and fallback MDC cleanup failed, {} keys still remain: {}",
              afterFallback.size(),
              afterFallback.keySet());
        } else {
          log.info("Fallback MDC cleanup successful after primary cleanup failure");
        }
      } else if (hadContext) {
        log.debug("MDC context cleanup completed successfully");
      }

    } catch (Exception e) {
      log.warn(
          "Failed to cleanup MDC context, continuing with graceful degradation: {}",
          e.getMessage());
      log.debug("Full exception details for MDC cleanup failure", e);

      // Graceful degradation: Try alternative cleanup methods
      try {
        // Try to remove individual keys if full clear fails
        String[] criticalKeys = {
          "traceId",
          "spanId",
          "requestId",
          "request.id",
          "tracingDegraded",
          "degradationReason",
          "emergencyMode",
          "userId",
          "sessionId",
          "correlationIdGenerated"
        };

        performIndividualKeyCleanup(java.util.Set.of(criticalKeys));
        log.debug("Successfully performed partial MDC cleanup");

      } catch (Exception partialCleanupError) {
        log.error(
            "Critical: Cannot perform any MDC cleanup, potential context leakage risk",
            partialCleanupError);
        // Continue execution - context leakage is less critical than application failure
      }
    }
  }

  /**
   * Performs individual key cleanup as fallback mechanism. This method attempts to remove specific
   * MDC keys when bulk cleanup fails.
   *
   * @param keysToRemove set of MDC keys to remove
   */
  private void performIndividualKeyCleanup(java.util.Set<String> keysToRemove) {
    int successCount = 0;
    int failCount = 0;

    for (String key : keysToRemove) {
      try {
        MDC.remove(key);
        successCount++;
      } catch (Exception keyRemovalException) {
        failCount++;
        log.debug("Failed to remove MDC key '{}': {}", key, keyRemovalException.getMessage());
      }
    }

    if (failCount > 0) {
      log.warn(
          "Individual key cleanup completed with {} successes and {} failures",
          successCount,
          failCount);
    } else {
      log.debug("Individual key cleanup completed successfully for {} keys", successCount);
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
