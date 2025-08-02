package com.accountselling.platform.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Service for monitoring the health of tracing context preservation system. Provides information
 * about tracing system operations and detects various issues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TracingHealthService {

  private final TracingMetricsService tracingMetricsService;

  // Health check counters
  private final AtomicLong healthCheckCount = new AtomicLong(0);
  private final AtomicLong lastHealthCheckTime = new AtomicLong(0);

  // Context consistency tracking
  private final Map<String, String> lastKnownTraceIds = new ConcurrentHashMap<>();
  private final AtomicLong contextInconsistencyCount = new AtomicLong(0);

  /**
   * Checks overall health of the tracing context system
   *
   * @return HealthStatus object with detailed information
   */
  public HealthStatus checkTracingHealth() {
    healthCheckCount.incrementAndGet();
    lastHealthCheckTime.set(System.currentTimeMillis());

    log.debug("Starting tracing context system health check");

    HealthStatus.Builder builder = HealthStatus.builder();

    // Check metrics health
    checkMetricsHealth(builder);

    // Check MDC context health
    checkMdcHealth(builder);

    // Check memory usage
    checkMemoryHealth(builder);

    // Check context consistency
    checkContextConsistency(builder);

    HealthStatus status = builder.build();

    // Log health check results
    logHealthStatus(status);

    return status;
  }

  /** Checks the health of metrics */
  private void checkMetricsHealth(HealthStatus.Builder builder) {
    try {
      double captureRate = tracingMetricsService.getContextCaptureSuccessRate();
      double restoreRate = tracingMetricsService.getContextRestoreSuccessRate();

      builder.captureSuccessRate(captureRate);
      builder.restoreSuccessRate(restoreRate);

      if (captureRate >= 95.0 && restoreRate >= 95.0) {
        builder.addHealthyComponent("metrics", "Success rates excellent");
        log.debug("Metrics health is good: Capture={}%, Restore={}%", captureRate, restoreRate);
      } else if (captureRate >= 80.0 && restoreRate >= 80.0) {
        builder.addWarningComponent(
            "metrics",
            String.format(
                "Success rates acceptable but could be better: Capture=%.1f%%, Restore=%.1f%%",
                captureRate, restoreRate));
        log.warn("Metrics have minor issues: Capture={}%, Restore={}%", captureRate, restoreRate);
      } else {
        builder.addUnhealthyComponent(
            "metrics",
            String.format(
                "Low success rates: Capture=%.1f%%, Restore=%.1f%%", captureRate, restoreRate));
        log.error(
            "Metrics have serious issues: Capture={}%, Restore={}%", captureRate, restoreRate);
      }

    } catch (Exception e) {
      builder.addUnhealthyComponent("metrics", "Failed to check metrics: " + e.getMessage());
      log.error("Unable to check metrics", e);
    }
  }

  /** Checks the health of MDC context */
  private void checkMdcHealth(HealthStatus.Builder builder) {
    try {
      Map<String, String> mdcContext = MDC.getCopyOfContextMap();

      if (mdcContext == null || mdcContext.isEmpty()) {
        builder.addWarningComponent("mdc", "No MDC context found (may be normal outside request)");
        log.debug("No MDC context found (may be normal if not within request)");
      } else {
        boolean hasTraceId = mdcContext.containsKey("traceId");
        boolean hasRequestId =
            mdcContext.containsKey("requestId") || mdcContext.containsKey("request.id");

        if (hasTraceId && hasRequestId) {
          builder.addHealthyComponent("mdc", "Complete tracing context available");
          log.debug(
              "MDC context is complete: traceId={}, requestId={}",
              mdcContext.get("traceId"),
              mdcContext.getOrDefault("requestId", mdcContext.get("request.id")));
        } else if (hasRequestId) {
          builder.addWarningComponent("mdc", "Request ID available but missing traceId");
          log.warn("MDC has requestId but missing traceId");
        } else {
          builder.addUnhealthyComponent("mdc", "Missing essential context fields");
          log.error("MDC is missing essential context data");
        }
      }

    } catch (Exception e) {
      builder.addUnhealthyComponent("mdc", "Failed to check MDC: " + e.getMessage());
      log.error("Unable to check MDC", e);
    }
  }

  /** Checks memory usage */
  private void checkMemoryHealth(HealthStatus.Builder builder) {
    try {
      long currentContexts = tracingMetricsService.getCurrentPreservedContexts();
      long peakContexts = tracingMetricsService.getPeakPreservedContexts();

      builder.currentPreservedContexts(currentContexts);
      builder.peakPreservedContexts(peakContexts);

      if (currentContexts <= 100 && peakContexts <= 500) {
        builder.addHealthyComponent("memory", "Memory usage within normal limits");
        log.debug("Memory usage is normal: current={}, peak={}", currentContexts, peakContexts);
      } else if (currentContexts <= 500 && peakContexts <= 1000) {
        builder.addWarningComponent(
            "memory",
            String.format("High memory usage: current=%d, peak=%d", currentContexts, peakContexts));
        log.warn("High memory usage: current={}, peak={}", currentContexts, peakContexts);
      } else {
        builder.addUnhealthyComponent(
            "memory",
            String.format(
                "Excessive memory usage - possible leak: current=%d, peak=%d",
                currentContexts, peakContexts));
        log.error(
            "Abnormally high memory usage - possible memory leak: current={}, peak={}",
            currentContexts,
            peakContexts);
      }

    } catch (Exception e) {
      builder.addUnhealthyComponent("memory", "Failed to check memory usage: " + e.getMessage());
      log.error("Unable to check memory usage", e);
    }
  }

  /** Checks context consistency */
  private void checkContextConsistency(HealthStatus.Builder builder) {
    try {
      String currentTraceId = MDC.get("traceId");
      String threadName = Thread.currentThread().getName();

      if (currentTraceId != null) {
        String lastTraceId = lastKnownTraceIds.get(threadName);

        if (lastTraceId != null && !lastTraceId.equals(currentTraceId)) {
          // Context changed - may be normal
          log.debug(
              "Context changed in thread {}: {} -> {}", threadName, lastTraceId, currentTraceId);
        }

        lastKnownTraceIds.put(threadName, currentTraceId);
        builder.addHealthyComponent("consistency", "Context tracking active");
      } else {
        builder.addWarningComponent("consistency", "No current traceId for consistency check");
        log.debug("No current traceId available for consistency check");
      }

    } catch (Exception e) {
      builder.addUnhealthyComponent(
          "consistency", "Failed to check consistency: " + e.getMessage());
      log.error("Unable to check consistency", e);
    }
  }

  /** Logs health check results */
  private void logHealthStatus(HealthStatus status) {
    switch (status.getOverallStatus()) {
      case HEALTHY:
        log.info("✅ Tracing system is healthy - everything is working normally");
        break;
      case WARNING:
        log.warn(
            "⚠️ Tracing system has minor issues - should monitor: {}",
            status.getWarningComponents());
        break;
      case UNHEALTHY:
        log.error(
            "❌ Tracing system has serious issues - requires immediate attention: {}",
            status.getUnhealthyComponents());
        break;
    }

    // Log additional details in debug mode
    if (log.isDebugEnabled()) {
      log.debug("Health check details:");
      log.debug("- Capture Success Rate: {}%", status.getCaptureSuccessRate());
      log.debug("- Restore Success Rate: {}%", status.getRestoreSuccessRate());
      log.debug("- Current Preserved Contexts: {}", status.getCurrentPreservedContexts());
      log.debug("- Peak Preserved Contexts: {}", status.getPeakPreservedContexts());
      log.debug("- Healthy Components: {}", status.getHealthyComponents());
      log.debug("- Warning Components: {}", status.getWarningComponents());
      log.debug("- Unhealthy Components: {}", status.getUnhealthyComponents());
    }
  }

  /** Performs debug logging for context operations */
  public void debugLogContextOperation(
      String operation, String details, Map<String, Object> context) {
    if (log.isDebugEnabled()) {
      log.debug("🔍 Context Operation: {} - {}", operation, details);

      if (context != null && !context.isEmpty()) {
        context.forEach((key, value) -> log.debug("  📋 {}: {}", key, value));
      }

      // Log current MDC state
      Map<String, String> mdcContext = MDC.getCopyOfContextMap();
      if (mdcContext != null && !mdcContext.isEmpty()) {
        log.debug("  🏷️ Current MDC Context:");
        mdcContext.forEach((key, value) -> log.debug("    - {}: {}", key, value));
      }
    }
  }

  /** Logs warnings for fallback scenarios */
  public void logFallbackScenario(String scenario, String reason, String action) {
    log.warn("⚠️ Fallback Scenario: {} - Reason: {} - Action: {}", scenario, reason, action);

    // Add debug info
    if (log.isDebugEnabled()) {
      Map<String, String> mdcContext = MDC.getCopyOfContextMap();
      log.debug("MDC Context during fallback: {}", mdcContext);
      log.debug("Thread: {}", Thread.currentThread().getName());
      log.debug("Timestamp: {}", Instant.now());
    }
  }

  /** Logs errors for critical failures */
  public void logCriticalFailure(String operation, String error, Exception exception) {
    log.error(
        "❌ Critical Failure in {}: {} - Requires immediate attention!",
        operation,
        error,
        exception);

    // Add debugging information
    log.error("Thread: {}", Thread.currentThread().getName());
    log.error("Timestamp: {}", Instant.now());

    Map<String, String> mdcContext = MDC.getCopyOfContextMap();
    if (mdcContext != null) {
      log.error("MDC Context during error: {}", mdcContext);
    }

    // Increment counter for critical failures
    contextInconsistencyCount.incrementAndGet();
  }

  /** Gets health check statistics */
  public Map<String, Object> getHealthCheckStats() {
    return Map.of(
        "totalHealthChecks", healthCheckCount.get(),
        "lastHealthCheckTime", lastHealthCheckTime.get(),
        "contextInconsistencies", contextInconsistencyCount.get(),
        "trackedThreads", lastKnownTraceIds.size());
  }

  /** Class for storing health check results */
  public static class HealthStatus {
    public enum Status {
      HEALTHY,
      WARNING,
      UNHEALTHY
    }

    private Status overallStatus = Status.HEALTHY;
    private double captureSuccessRate;
    private double restoreSuccessRate;
    private long currentPreservedContexts;
    private long peakPreservedContexts;
    private final Map<String, String> healthyComponents = new ConcurrentHashMap<>();
    private final Map<String, String> warningComponents = new ConcurrentHashMap<>();
    private final Map<String, String> unhealthyComponents = new ConcurrentHashMap<>();

    public static Builder builder() {
      return new Builder();
    }

    // Getters
    public Status getOverallStatus() {
      return overallStatus;
    }

    public double getCaptureSuccessRate() {
      return captureSuccessRate;
    }

    public double getRestoreSuccessRate() {
      return restoreSuccessRate;
    }

    public long getCurrentPreservedContexts() {
      return currentPreservedContexts;
    }

    public long getPeakPreservedContexts() {
      return peakPreservedContexts;
    }

    public Map<String, String> getHealthyComponents() {
      return healthyComponents;
    }

    public Map<String, String> getWarningComponents() {
      return warningComponents;
    }

    public Map<String, String> getUnhealthyComponents() {
      return unhealthyComponents;
    }

    public static class Builder {
      private final HealthStatus status = new HealthStatus();

      public Builder captureSuccessRate(double rate) {
        status.captureSuccessRate = rate;
        return this;
      }

      public Builder restoreSuccessRate(double rate) {
        status.restoreSuccessRate = rate;
        return this;
      }

      public Builder currentPreservedContexts(long count) {
        status.currentPreservedContexts = count;
        return this;
      }

      public Builder peakPreservedContexts(long count) {
        status.peakPreservedContexts = count;
        return this;
      }

      public Builder addHealthyComponent(String component, String message) {
        status.healthyComponents.put(component, message);
        return this;
      }

      public Builder addWarningComponent(String component, String message) {
        status.warningComponents.put(component, message);
        if (status.overallStatus == Status.HEALTHY) {
          status.overallStatus = Status.WARNING;
        }
        return this;
      }

      public Builder addUnhealthyComponent(String component, String message) {
        status.unhealthyComponents.put(component, message);
        status.overallStatus = Status.UNHEALTHY;
        return this;
      }

      public HealthStatus build() {
        return status;
      }
    }
  }
}
