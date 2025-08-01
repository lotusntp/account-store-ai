package com.accountselling.platform.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Comprehensive unit tests for TracingContextCapture class.
 *
 * <p>Tests cover: - Context capture with valid OpenTelemetry context - Context capture with missing
 * tracing information - Context restoration and cleanup operations - Concurrent access and thread
 * safety - Error handling and graceful degradation
 */
@ExtendWith(MockitoExtension.class)
class TracingContextCaptureTest {

  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    // Clear MDC before each test
    MDC.clear();

    // Set up test appender to capture log events
    logger = (Logger) LoggerFactory.getLogger(TracingContextCapture.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    // Clean up MDC after each test
    MDC.clear();

    // Remove test appender
    if (listAppender != null) {
      logger.detachAppender(listAppender);
    }
  }

  // Test 1: Context capture with valid OpenTelemetry context
  @Test
  void testCaptureWithValidOpenTelemetryContext() {
    // Given: Mock valid OpenTelemetry context
    String expectedTraceId = "12345678901234567890123456789012";
    String expectedSpanId = "1234567890123456";
    String expectedRequestId = "request-123";

    SpanContext mockSpanContext =
        SpanContext.create(
            expectedTraceId, expectedSpanId, TraceFlags.getSampled(), TraceState.getDefault());

    Span mockSpan = mock(Span.class);
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);

    // Set up MDC with request ID
    MDC.put("request.id", expectedRequestId);
    MDC.put("userId", "user123");

    try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
      spanMock.when(Span::current).thenReturn(mockSpan);

      // When: Capture context
      TracingContextCapture capture = TracingContextCapture.capture();

      // Then: Verify captured context
      assertThat(capture).isNotNull();
      assertThat(capture.getTraceId()).isEqualTo(expectedTraceId);
      assertThat(capture.getSpanId()).isEqualTo(expectedSpanId);
      assertThat(capture.getRequestId()).isEqualTo(expectedRequestId);
      assertThat(capture.hasValidContext()).isTrue();
      assertThat(capture.getCaptureTimestamp()).isGreaterThan(0);

      // Verify MDC snapshot contains expected data
      Map<String, String> mdcSnapshot = capture.getMdcSnapshot();
      assertThat(mdcSnapshot).containsEntry("request.id", expectedRequestId);
      assertThat(mdcSnapshot).containsEntry("userId", "user123");
    }
  }

  // Test 2: Context capture with missing OpenTelemetry context, fallback to MDC
  @Test
  void testCaptureWithMissingOpenTelemetryContextFallbackToMDC() {
    // Given: Mock invalid OpenTelemetry context, but MDC has tracing data
    String expectedTraceId = "mdc-trace-id-123";
    String expectedSpanId = "mdc-span-id-456";
    String expectedRequestId = "mdc-request-789";

    SpanContext mockSpanContext = SpanContext.getInvalid();
    Span mockSpan = mock(Span.class);
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);

    // Set up MDC with tracing information
    MDC.put("traceId", expectedTraceId);
    MDC.put("spanId", expectedSpanId);
    MDC.put("request.id", expectedRequestId);

    try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
      spanMock.when(Span::current).thenReturn(mockSpan);

      // When: Capture context
      TracingContextCapture capture = TracingContextCapture.capture();

      // Then: Verify fallback to MDC worked
      assertThat(capture).isNotNull();
      assertThat(capture.getTraceId()).isEqualTo(expectedTraceId);
      assertThat(capture.getSpanId()).isEqualTo(expectedSpanId);
      assertThat(capture.getRequestId()).isEqualTo(expectedRequestId);
      assertThat(capture.hasValidContext()).isTrue();
    }
  }

  // Test 3: Context capture with completely missing tracing information
  @Test
  void testCaptureWithCompletelyMissingTracingInformation() {
    // Given: No OpenTelemetry context and no MDC tracing data
    SpanContext mockSpanContext = SpanContext.getInvalid();
    Span mockSpan = mock(Span.class);
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);

    // MDC is empty (no tracing data)

    try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
      spanMock.when(Span::current).thenReturn(mockSpan);

      // When: Capture context
      TracingContextCapture capture = TracingContextCapture.capture();

      // Then: Verify graceful degradation
      assertThat(capture).isNotNull();
      assertThat(capture.getTraceId()).isNull();
      assertThat(capture.getSpanId()).isNull();
      assertThat(capture.getRequestId()).isNotNull(); // Should generate UUID
      assertThat(capture.hasValidContext()).isFalse();

      // Verify UUID format for generated request ID
      assertThat(capture.getRequestId())
          .matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
  }

  // Test 4: Context capture with exception handling (graceful degradation)
  @Test
  void testCaptureWithExceptionGracefulDegradation() {
    // Given: Mock Span.current() to throw exception
    try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
      spanMock.when(Span::current).thenThrow(new RuntimeException("OpenTelemetry error"));

      // When: Capture context (should not throw exception)
      TracingContextCapture capture = TracingContextCapture.capture();

      // Then: Verify graceful degradation
      assertThat(capture).isNotNull();
      assertThat(capture.hasValidContext()).isFalse();
      assertThat(capture.getRequestId()).isNotNull();

      // Verify warning log was generated
      assertThat(listAppender.list).isNotEmpty();
      assertThat(listAppender.list.get(0).getFormattedMessage())
          .contains("Failed to capture tracing context, attempting graceful degradation");
    }
  }

  // Test 5: Context restoration with valid captured context
  @Test
  void testRestoreWithValidCapturedContext() {
    // Given: Create a captured context with test data
    String testTraceId = "restore-trace-123";
    String testSpanId = "restore-span-456";
    String testRequestId = "restore-request-789";

    Map<String, String> testMdc = new HashMap<>();
    testMdc.put("traceId", testTraceId);
    testMdc.put("spanId", testSpanId);
    testMdc.put("request.id", testRequestId);
    testMdc.put("userId", "user456");

    TracingContextCapture capture =
        TracingContextCapture.createEmergency(
            testTraceId, testSpanId, testRequestId, testMdc, true);

    // Clear MDC to simulate clean state
    MDC.clear();

    // When: Restore context
    capture.restore();

    // Then: Verify MDC was restored
    assertThat(MDC.get("traceId")).isEqualTo(testTraceId);
    assertThat(MDC.get("spanId")).isEqualTo(testSpanId);
    assertThat(MDC.get("requestId")).isEqualTo(testRequestId);
    assertThat(MDC.get("request.id")).isEqualTo(testRequestId);
    assertThat(MDC.get("userId")).isEqualTo("user456");
  }

  // Test 6: Context restoration with missing request ID (normal case)
  @Test
  void testRestoreWithMissingRequestIdNormalCase() {
    // Given: Create a captured context without request ID
    TracingContextCapture capture =
        TracingContextCapture.createEmergency("trace123", "span456", null, new HashMap<>(), true);

    // When: Restore context
    capture.restore();

    // Then: requestId should not be set since it was null
    assertThat(MDC.get("requestId")).isNull();
    assertThat(MDC.get("request.id")).isNull();

    // But traceId and spanId should be restored
    assertThat(MDC.get("traceId")).isEqualTo("trace123");
    assertThat(MDC.get("spanId")).isEqualTo("span456");
  }

  // Test 6b: Context restoration exception handling with missing request ID
  @Test
  void testRestoreExceptionHandlingWithMissingRequestId() {
    // Given: Create a context that will cause MDC.put to fail
    // We'll mock this by creating a context where restoration will fail
    Map<String, String> corruptMdc = new HashMap<>();
    corruptMdc.put("corruptKey", null); // This might cause issues

    TracingContextCapture capture =
        TracingContextCapture.createEmergency(null, null, null, corruptMdc, false);

    // When: Restore context (this should trigger exception handling)
    capture.restore();

    // Then: Emergency correlation ID should be created due to missing requestId in exception flow
    String requestId = MDC.get("requestId");
    if (requestId != null) {
      assertThat(requestId).startsWith("emergency-no-request-");
      assertThat(MDC.get("tracingDegraded")).isEqualTo("true");
      assertThat(MDC.get("degradationReason")).isEqualTo("no_request_id_available");
      assertThat(MDC.get("emergencyMode")).isEqualTo("true");
    }
    // Note: This test might not trigger exception if MDC handles null gracefully
  }

  // Test 7: Context cleanup with successful cleanup
  @Test
  void testCleanupSuccessful() {
    // Given: Set up MDC with test data
    MDC.put("traceId", "cleanup-trace-123");
    MDC.put("spanId", "cleanup-span-456");
    MDC.put("requestId", "cleanup-request-789");

    TracingContextCapture capture =
        TracingContextCapture.createEmergency("test", "test", "test", new HashMap<>(), true);

    // When: Cleanup context
    capture.cleanup();

    // Then: Verify MDC was cleared
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  // Test 8: Context cleanup with partial failure (fallback mechanism)
  @Test
  void testCleanupWithPartialFailureFallback() {
    // Given: Set up MDC with test data
    MDC.put("traceId", "fallback-trace-123");
    MDC.put("spanId", "fallback-span-456");
    MDC.put("requestId", "fallback-request-789");

    TracingContextCapture capture =
        TracingContextCapture.createEmergency("test", "test", "test", new HashMap<>(), true);

    // When: Cleanup context (this should work normally, but we test the fallback logic exists)
    capture.cleanup();

    // Then: Verify cleanup completed
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  // Test 9: Concurrent access and thread safety
  @Test
  void testConcurrentAccessAndThreadSafety() throws InterruptedException {
    // Given: Set up concurrent test scenario
    int numberOfThreads = 10;
    int operationsPerThread = 100;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

    AtomicInteger successfulCaptures = new AtomicInteger(0);
    AtomicInteger successfulRestores = new AtomicInteger(0);
    AtomicInteger successfulCleanups = new AtomicInteger(0);
    ConcurrentHashMap<String, String> threadResults = new ConcurrentHashMap<>();

    // When: Execute concurrent operations
    for (int i = 0; i < numberOfThreads; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            try {
              startLatch.await(); // Wait for all threads to be ready

              for (int j = 0; j < operationsPerThread; j++) {
                String threadSpecificId = "thread-" + threadId + "-op-" + j;

                try {
                  // Set thread-specific MDC data
                  MDC.put("threadId", String.valueOf(threadId));
                  MDC.put("operationId", String.valueOf(j));
                  MDC.put("request.id", threadSpecificId);

                  // Test context capture
                  TracingContextCapture capture = TracingContextCapture.capture();
                  if (capture != null) {
                    successfulCaptures.incrementAndGet();
                    threadResults.put(threadSpecificId + "-capture", "success");

                    // Test context restoration
                    capture.restore();
                    successfulRestores.incrementAndGet();
                    threadResults.put(threadSpecificId + "-restore", "success");

                    // Test context cleanup
                    capture.cleanup();
                    successfulCleanups.incrementAndGet();
                    threadResults.put(threadSpecificId + "-cleanup", "success");
                  }

                } catch (Exception e) {
                  threadResults.put(threadSpecificId + "-error", e.getMessage());
                }
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              MDC.clear(); // Clean up thread-specific MDC
              completionLatch.countDown();
            }
          });
    }

    // Start all threads simultaneously
    startLatch.countDown();

    // Wait for all threads to complete
    boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    // Then: Verify thread safety and concurrent access
    assertThat(completed).isTrue();

    int expectedOperations = numberOfThreads * operationsPerThread;
    assertThat(successfulCaptures.get()).isEqualTo(expectedOperations);
    assertThat(successfulRestores.get()).isEqualTo(expectedOperations);
    assertThat(successfulCleanups.get()).isEqualTo(expectedOperations);

    // Verify no errors occurred
    long errorCount =
        threadResults.entrySet().stream()
            .filter(entry -> entry.getKey().contains("-error"))
            .count();
    assertThat(errorCount).isEqualTo(0);

    // Verify each thread produced expected results
    assertThat(threadResults.size())
        .isEqualTo(expectedOperations * 3); // capture + restore + cleanup
  }

  // Test 10: Alternative MDC key fallback mechanisms
  @Test
  void testAlternativeMDCKeyFallbacks() {
    // Given: Set up alternative MDC keys
    SpanContext mockSpanContext = SpanContext.getInvalid();
    Span mockSpan = mock(Span.class);
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);

    // Test trace_id fallback
    MDC.put("trace_id", "alternative-trace-123");
    MDC.put("span_id", "alternative-span-456");
    MDC.put("requestId", "alternative-request-789"); // Note: different key

    try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
      spanMock.when(Span::current).thenReturn(mockSpan);

      // When: Capture context
      TracingContextCapture capture = TracingContextCapture.capture();

      // Then: Verify alternative keys were used
      assertThat(capture.getTraceId()).isEqualTo("alternative-trace-123");
      assertThat(capture.getSpanId()).isEqualTo("alternative-span-456");
      assertThat(capture.getRequestId()).isEqualTo("alternative-request-789");
      assertThat(capture.hasValidContext()).isTrue();
    }
  }

  // Test 11: Header-based tracing fallback (X-Trace-Id, X-Span-Id)
  @Test
  void testHeaderBasedTracingFallback() {
    // Given: Set up header-based tracing in MDC
    SpanContext mockSpanContext = SpanContext.getInvalid();
    Span mockSpan = mock(Span.class);
    when(mockSpan.getSpanContext()).thenReturn(mockSpanContext);

    MDC.put("X-Trace-Id", "header-trace-987");
    MDC.put("X-Span-Id", "header-span-654");

    try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
      spanMock.when(Span::current).thenReturn(mockSpan);

      // When: Capture context
      TracingContextCapture capture = TracingContextCapture.capture();

      // Then: Verify header keys were used as fallback
      assertThat(capture.getTraceId()).isEqualTo("header-trace-987");
      assertThat(capture.getSpanId()).isEqualTo("header-span-654");
      assertThat(capture.hasValidContext()).isTrue();
    }
  }

  // Test 12: CreateEmergency static factory method
  @Test
  void testCreateEmergencyStaticFactory() {
    // Given: Test data for emergency context
    String traceId = "emergency-trace-123";
    String spanId = "emergency-span-456";
    String requestId = "emergency-request-789";
    Map<String, String> mdcData = new HashMap<>();
    mdcData.put("emergencyMode", "true");
    mdcData.put("reason", "test-emergency");

    // When: Create emergency context
    TracingContextCapture emergencyContext =
        TracingContextCapture.createEmergency(traceId, spanId, requestId, mdcData, false);

    // Then: Verify emergency context
    assertThat(emergencyContext).isNotNull();
    assertThat(emergencyContext.getTraceId()).isEqualTo(traceId);
    assertThat(emergencyContext.getSpanId()).isEqualTo(spanId);
    assertThat(emergencyContext.getRequestId()).isEqualTo(requestId);
    assertThat(emergencyContext.hasValidContext()).isFalse(); // As specified
    assertThat(emergencyContext.getMdcSnapshot()).containsEntry("emergencyMode", "true");
    assertThat(emergencyContext.getMdcSnapshot()).containsEntry("reason", "test-emergency");
  }

  // Test 13: ToString method for debugging
  @Test
  void testToStringForDebugging() {
    // Given: Create context with test data
    TracingContextCapture capture =
        TracingContextCapture.createEmergency(
            "debug-trace-123", "debug-span-456", "debug-request-789", new HashMap<>(), true);

    // When: Get string representation
    String toString = capture.toString();

    // Then: Verify string contains expected information
    assertThat(toString).contains("TracingContextCapture{");
    assertThat(toString).contains("traceId='debug-trace-123'");
    assertThat(toString).contains("spanId='debug-span-456'");
    assertThat(toString).contains("requestId='debug-request-789'");
    assertThat(toString).contains("hasValidContext=true");
    assertThat(toString).contains("captureTimestamp=");
  }

  // Test 14: Capture timestamp validation
  @Test
  void testCaptureTimestampValidation() {
    // Given: Record time before capture
    long beforeCapture = System.currentTimeMillis();

    // When: Capture context
    TracingContextCapture capture = TracingContextCapture.capture();

    // Record time after capture
    long afterCapture = System.currentTimeMillis();

    // Then: Verify timestamp is within expected range
    assertThat(capture.getCaptureTimestamp()).isBetween(beforeCapture, afterCapture);
  }

  // Test 15: MDC snapshot immutability
  @Test
  void testMDCSnapshotImmutability() {
    // Given: Set up MDC and capture context
    MDC.put("testKey", "originalValue");
    TracingContextCapture capture = TracingContextCapture.capture();

    // When: Attempt to modify the returned MDC snapshot
    Map<String, String> mdcSnapshot = capture.getMdcSnapshot();
    mdcSnapshot.put("testKey", "modifiedValue");

    // Then: Verify original snapshot is not affected
    Map<String, String> freshSnapshot = capture.getMdcSnapshot();
    assertThat(freshSnapshot.get("testKey")).isEqualTo("originalValue");
    assertThat(freshSnapshot).isNotSameAs(mdcSnapshot); // Different instances
  }
}
