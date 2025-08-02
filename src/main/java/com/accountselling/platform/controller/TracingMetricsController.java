package com.accountselling.platform.controller;

import com.accountselling.platform.service.TracingHealthService;
import com.accountselling.platform.service.TracingMetricsService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for exposing tracing context metrics. Provides endpoints for monitoring tracing
 * context preservation performance.
 */
@RestController
@RequestMapping("/api/metrics/tracing")
@RequiredArgsConstructor
public class TracingMetricsController {

  private final TracingMetricsService tracingMetricsService;
  private final TracingHealthService tracingHealthService;

  /**
   * Get comprehensive tracing metrics summary
   *
   * @return Map containing all tracing metrics
   */
  @GetMapping("/summary")
  public ResponseEntity<Map<String, Object>> getMetricsSummary() {
    Map<String, Object> metrics = tracingMetricsService.getMetricsSummary();
    return ResponseEntity.ok(metrics);
  }

  /**
   * Get context capture success rate
   *
   * @return success rate as percentage
   */
  @GetMapping("/capture-success-rate")
  public ResponseEntity<Double> getCaptureSuccessRate() {
    double rate = tracingMetricsService.getContextCaptureSuccessRate();
    return ResponseEntity.ok(rate);
  }

  /**
   * Get context restoration success rate
   *
   * @return success rate as percentage
   */
  @GetMapping("/restore-success-rate")
  public ResponseEntity<Double> getRestoreSuccessRate() {
    double rate = tracingMetricsService.getContextRestoreSuccessRate();
    return ResponseEntity.ok(rate);
  }

  /**
   * Get average filter processing time
   *
   * @return average processing time in milliseconds
   */
  @GetMapping("/average-processing-time")
  public ResponseEntity<Double> getAverageProcessingTime() {
    double time = tracingMetricsService.getAverageFilterProcessingTime();
    return ResponseEntity.ok(time);
  }

  /**
   * Get current memory usage (preserved contexts)
   *
   * @return current number of preserved contexts
   */
  @GetMapping("/memory-usage")
  public ResponseEntity<Map<String, Long>> getMemoryUsage() {
    Map<String, Long> usage =
        Map.of(
            "current", tracingMetricsService.getCurrentPreservedContexts(),
            "peak", tracingMetricsService.getPeakPreservedContexts());
    return ResponseEntity.ok(usage);
  }

  /**
   * Reset all metrics (useful for testing or periodic resets)
   *
   * @return confirmation message
   */
  @PostMapping("/reset")
  public ResponseEntity<String> resetMetrics() {
    tracingMetricsService.resetMetrics();
    return ResponseEntity.ok("Tracing metrics reset successfully");
  }

  /**
   * Log current metrics summary to application logs
   *
   * @return confirmation message
   */
  @PostMapping("/log-summary")
  public ResponseEntity<String> logMetricsSummary() {
    tracingMetricsService.logMetricsSummary();
    return ResponseEntity.ok("Metrics summary logged successfully");
  }

  /**
   * Health check endpoint for tracing metrics
   *
   * @return health status based on comprehensive health checks
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> getHealthStatus() {
    TracingHealthService.HealthStatus healthStatus = tracingHealthService.checkTracingHealth();

    Map<String, Object> response =
        Map.of(
            "status", healthStatus.getOverallStatus().toString().toLowerCase(),
            "captureSuccessRate", healthStatus.getCaptureSuccessRate(),
            "restoreSuccessRate", healthStatus.getRestoreSuccessRate(),
            "currentPreservedContexts", healthStatus.getCurrentPreservedContexts(),
            "peakPreservedContexts", healthStatus.getPeakPreservedContexts(),
            "healthyComponents", healthStatus.getHealthyComponents(),
            "warningComponents", healthStatus.getWarningComponents(),
            "unhealthyComponents", healthStatus.getUnhealthyComponents(),
            "timestamp", java.time.Instant.now().toString());

    // Return appropriate HTTP status based on health
    switch (healthStatus.getOverallStatus()) {
      case HEALTHY:
        return ResponseEntity.ok(response);
      case WARNING:
        return ResponseEntity.status(200).body(response); // Still OK but with warnings
      case UNHEALTHY:
        return ResponseEntity.status(503).body(response); // Service Unavailable
      default:
        return ResponseEntity.status(500).body(response);
    }
  }

  /**
   * Detailed health check with debug information
   *
   * @return comprehensive health status with debug details
   */
  @GetMapping("/health/detailed")
  public ResponseEntity<Map<String, Object>> getDetailedHealthStatus() {
    TracingHealthService.HealthStatus healthStatus = tracingHealthService.checkTracingHealth();
    Map<String, Object> healthCheckStats = tracingHealthService.getHealthCheckStats();
    Map<String, Object> metrics = tracingMetricsService.getMetricsSummary();

    Map<String, Object> response =
        Map.of(
            "healthStatus",
            Map.of(
                "status", healthStatus.getOverallStatus().toString().toLowerCase(),
                "captureSuccessRate", healthStatus.getCaptureSuccessRate(),
                "restoreSuccessRate", healthStatus.getRestoreSuccessRate(),
                "currentPreservedContexts", healthStatus.getCurrentPreservedContexts(),
                "peakPreservedContexts", healthStatus.getPeakPreservedContexts(),
                "healthyComponents", healthStatus.getHealthyComponents(),
                "warningComponents", healthStatus.getWarningComponents(),
                "unhealthyComponents", healthStatus.getUnhealthyComponents()),
            "healthCheckStats",
            healthCheckStats,
            "metrics",
            metrics,
            "timestamp",
            java.time.Instant.now().toString());

    return ResponseEntity.ok(response);
  }
}
