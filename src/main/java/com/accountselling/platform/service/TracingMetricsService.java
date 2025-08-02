package com.accountselling.platform.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for collecting and reporting metrics related to tracing context operations. Tracks
 * success/failure rates, performance metrics, and memory usage for tracing context preservation
 * mechanisms.
 */
@Slf4j
@Service
public class TracingMetricsService {

  // Context capture metrics
  private final LongAdder contextCaptureAttempts = new LongAdder();
  private final LongAdder contextCaptureSuccesses = new LongAdder();
  private final LongAdder contextCaptureFailures = new LongAdder();

  // Context restoration metrics
  private final LongAdder contextRestoreAttempts = new LongAdder();
  private final LongAdder contextRestoreSuccesses = new LongAdder();
  private final LongAdder contextRestoreFailures = new LongAdder();

  // Capture method metrics
  private final Map<String, LongAdder> captureMethodCounts = new ConcurrentHashMap<>();
  private final Map<String, LongAdder> captureMethodSuccesses = new ConcurrentHashMap<>();

  // Performance metrics
  private final LongAdder totalFilterProcessingTime = new LongAdder();
  private final AtomicLong maxFilterProcessingTime = new AtomicLong(0);
  private final AtomicLong minFilterProcessingTime = new AtomicLong(Long.MAX_VALUE);

  // Memory usage tracking
  private final LongAdder preservedContextsCount = new LongAdder();
  private final AtomicLong peakPreservedContexts = new AtomicLong(0);

  // Fallback metrics
  private final LongAdder fallbackCorrelationUsed = new LongAdder();
  private final Map<String, LongAdder> fallbackReasons = new ConcurrentHashMap<>();

  /**
   * Records a context capture attempt
   *
   * @param method the capture method used (e.g., "ResponseTracingWrapper",
   *     "TracingAwareFilterChain")
   * @param success whether the capture was successful
   * @param hasValidContext whether the captured context contains valid tracing information
   */
  public void recordContextCapture(String method, boolean success, boolean hasValidContext) {
    contextCaptureAttempts.increment();

    // Track by method
    captureMethodCounts.computeIfAbsent(method, k -> new LongAdder()).increment();

    if (success) {
      contextCaptureSuccesses.increment();
      if (hasValidContext) {
        captureMethodSuccesses.computeIfAbsent(method, k -> new LongAdder()).increment();
      }
    } else {
      contextCaptureFailures.increment();
    }

    log.debug(
        "Context capture recorded: method={}, success={}, hasValidContext={}",
        method,
        success,
        hasValidContext);
  }

  /**
   * Records a context restoration attempt
   *
   * @param success whether the restoration was successful
   */
  public void recordContextRestore(boolean success) {
    contextRestoreAttempts.increment();

    if (success) {
      contextRestoreSuccesses.increment();
    } else {
      contextRestoreFailures.increment();
    }

    log.debug("Context restore recorded: success={}", success);
  }

  /**
   * Records filter processing time
   *
   * @param processingTimeMs the processing time in milliseconds
   */
  public void recordFilterProcessingTime(long processingTimeMs) {
    totalFilterProcessingTime.add(processingTimeMs);

    // Update min/max
    maxFilterProcessingTime.updateAndGet(current -> Math.max(current, processingTimeMs));
    minFilterProcessingTime.updateAndGet(current -> Math.min(current, processingTimeMs));

    log.debug("Filter processing time recorded: {}ms", processingTimeMs);
  }

  /** Records when a context is preserved (increments active count) */
  public void recordContextPreserved() {
    long current = preservedContextsCount.sum();
    preservedContextsCount.increment();

    // Update peak
    peakPreservedContexts.updateAndGet(peak -> Math.max(peak, current + 1));

    log.debug("Context preserved, current count: {}", current + 1);
  }

  /** Records when a context is cleaned up (decrements active count) */
  public void recordContextCleaned() {
    preservedContextsCount.decrement();

    log.debug("Context cleaned, current count: {}", preservedContextsCount.sum());
  }

  /**
   * Records when fallback correlation is used
   *
   * @param reason the reason for using fallback correlation
   */
  public void recordFallbackCorrelation(String reason) {
    fallbackCorrelationUsed.increment();
    fallbackReasons.computeIfAbsent(reason, k -> new LongAdder()).increment();

    log.debug("Fallback correlation recorded: reason={}", reason);
  }

  /**
   * Gets context capture success rate
   *
   * @return success rate as percentage (0-100)
   */
  public double getContextCaptureSuccessRate() {
    long attempts = contextCaptureAttempts.sum();
    if (attempts == 0) return 0.0;

    return (contextCaptureSuccesses.sum() * 100.0) / attempts;
  }

  /**
   * Gets context restoration success rate
   *
   * @return success rate as percentage (0-100)
   */
  public double getContextRestoreSuccessRate() {
    long attempts = contextRestoreAttempts.sum();
    if (attempts == 0) return 0.0;

    return (contextRestoreSuccesses.sum() * 100.0) / attempts;
  }

  /**
   * Gets average filter processing time
   *
   * @return average processing time in milliseconds
   */
  public double getAverageFilterProcessingTime() {
    long attempts = contextCaptureAttempts.sum();
    if (attempts == 0) return 0.0;

    return (double) totalFilterProcessingTime.sum() / attempts;
  }

  /**
   * Gets current number of preserved contexts in memory
   *
   * @return current count of preserved contexts
   */
  public long getCurrentPreservedContexts() {
    return Math.max(0, preservedContextsCount.sum());
  }

  /**
   * Gets peak number of preserved contexts
   *
   * @return peak count of preserved contexts
   */
  public long getPeakPreservedContexts() {
    return peakPreservedContexts.get();
  }

  /**
   * Gets comprehensive metrics summary
   *
   * @return Map containing all metrics
   */
  public Map<String, Object> getMetricsSummary() {
    Map<String, Object> metrics = new ConcurrentHashMap<>();

    // Capture metrics
    metrics.put("contextCaptureAttempts", contextCaptureAttempts.sum());
    metrics.put("contextCaptureSuccesses", contextCaptureSuccesses.sum());
    metrics.put("contextCaptureFailures", contextCaptureFailures.sum());
    metrics.put("contextCaptureSuccessRate", getContextCaptureSuccessRate());

    // Restore metrics
    metrics.put("contextRestoreAttempts", contextRestoreAttempts.sum());
    metrics.put("contextRestoreSuccesses", contextRestoreSuccesses.sum());
    metrics.put("contextRestoreFailures", contextRestoreFailures.sum());
    metrics.put("contextRestoreSuccessRate", getContextRestoreSuccessRate());

    // Performance metrics
    metrics.put("averageFilterProcessingTime", getAverageFilterProcessingTime());
    metrics.put("maxFilterProcessingTime", maxFilterProcessingTime.get());
    metrics.put(
        "minFilterProcessingTime",
        minFilterProcessingTime.get() == Long.MAX_VALUE ? 0 : minFilterProcessingTime.get());

    // Memory metrics
    metrics.put("currentPreservedContexts", getCurrentPreservedContexts());
    metrics.put("peakPreservedContexts", getPeakPreservedContexts());

    // Fallback metrics
    metrics.put("fallbackCorrelationUsed", fallbackCorrelationUsed.sum());

    // Method-specific metrics
    Map<String, Object> methodMetrics = new ConcurrentHashMap<>();
    for (Map.Entry<String, LongAdder> entry : captureMethodCounts.entrySet()) {
      String method = entry.getKey();
      long attempts = entry.getValue().sum();
      long successes = captureMethodSuccesses.getOrDefault(method, new LongAdder()).sum();

      Map<String, Object> methodData = new ConcurrentHashMap<>();
      methodData.put("attempts", attempts);
      methodData.put("successes", successes);
      methodData.put("successRate", attempts > 0 ? (successes * 100.0) / attempts : 0.0);

      methodMetrics.put(method, methodData);
    }
    metrics.put("captureMethodMetrics", methodMetrics);

    // Fallback reasons
    Map<String, Long> reasonCounts = new ConcurrentHashMap<>();
    for (Map.Entry<String, LongAdder> entry : fallbackReasons.entrySet()) {
      reasonCounts.put(entry.getKey(), entry.getValue().sum());
    }
    metrics.put("fallbackReasons", reasonCounts);

    metrics.put("timestamp", Instant.now().toString());

    return metrics;
  }

  /** Resets all metrics (useful for testing or periodic resets) */
  public void resetMetrics() {
    contextCaptureAttempts.reset();
    contextCaptureSuccesses.reset();
    contextCaptureFailures.reset();

    contextRestoreAttempts.reset();
    contextRestoreSuccesses.reset();
    contextRestoreFailures.reset();

    captureMethodCounts.clear();
    captureMethodSuccesses.clear();

    totalFilterProcessingTime.reset();
    maxFilterProcessingTime.set(0);
    minFilterProcessingTime.set(Long.MAX_VALUE);

    preservedContextsCount.reset();
    peakPreservedContexts.set(0);

    fallbackCorrelationUsed.reset();
    fallbackReasons.clear();

    log.info("Tracing metrics reset");
  }

  /** Logs current metrics summary */
  public void logMetricsSummary() {
    Map<String, Object> metrics = getMetricsSummary();

    log.info("=== Tracing Context Metrics Summary ===");
    log.info(
        "Context Capture: {}/{} attempts ({}% success)",
        metrics.get("contextCaptureSuccesses"),
        metrics.get("contextCaptureAttempts"),
        String.format("%.1f", metrics.get("contextCaptureSuccessRate")));

    log.info(
        "Context Restore: {}/{} attempts ({}% success)",
        metrics.get("contextRestoreSuccesses"),
        metrics.get("contextRestoreAttempts"),
        String.format("%.1f", metrics.get("contextRestoreSuccessRate")));

    log.info(
        "Filter Performance: avg={}ms, min={}ms, max={}ms",
        String.format("%.1f", metrics.get("averageFilterProcessingTime")),
        metrics.get("minFilterProcessingTime"),
        metrics.get("maxFilterProcessingTime"));

    log.info(
        "Memory Usage: current={}, peak={} preserved contexts",
        metrics.get("currentPreservedContexts"),
        metrics.get("peakPreservedContexts"));

    log.info("Fallback Usage: {} times", metrics.get("fallbackCorrelationUsed"));
    log.info("=====================================");
  }
}
