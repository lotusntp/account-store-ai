package com.accountselling.platform.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.accountselling.platform.config.TestContainersConfig;
import com.accountselling.platform.service.LoggingService;
import com.accountselling.platform.util.TracingContextCapture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Performance and stress tests for tracing filter functionality.
 *
 * <p>Tests cover: - Overhead measurement of context capture and restoration operations - Memory
 * usage validation under high concurrent load scenarios - Context leakage prevention between
 * requests - Long-running request handling and resource cleanup - Performance benchmarks and
 * threshold validation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class TracingFilterPerformanceTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private LoggingService loggingService;

  private ListAppender<ILoggingEvent> logAppender;
  private Logger logger;
  private MemoryMXBean memoryBean;

  // Performance thresholds
  private static final long MAX_CONTEXT_OPERATION_TIME_MS = 10; // Max time for capture/restore
  private static final long MAX_MEMORY_INCREASE_MB = 50; // Max memory increase during tests
  private static final int HIGH_CONCURRENCY_LOAD = 100; // Number of concurrent requests
  private static final int STRESS_TEST_ITERATIONS = 1000; // Number of stress test iterations

  @BeforeEach
  void setUp() {
    MDC.clear();

    // Set up memory monitoring
    memoryBean = ManagementFactory.getMemoryMXBean();
    System.gc(); // Clean up before tests

    // Set up logging capture
    logger = (Logger) LoggerFactory.getLogger("com.accountselling.platform");
    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    MDC.clear();

    if (logAppender != null) {
      logger.detachAppender(logAppender);
    }

    // Clean up memory after tests
    System.gc();
  }

  // Test 1: Measure overhead of context capture and restoration
  @Test
  @WithMockUser(roles = "USER")
  void testContextCaptureAndRestoreOverhead() throws Exception {
    System.out.println("=== Context Capture/Restore Overhead Test ===");

    // Warm up JVM
    for (int i = 0; i < 100; i++) {
      TracingContextCapture.capture().cleanup();
    }

    int iterations = 10000;
    List<Long> captureTimes = new ArrayList<>();
    List<Long> restoreTimes = new ArrayList<>();
    List<Long> cleanupTimes = new ArrayList<>();

    // Test context operations performance
    for (int i = 0; i < iterations; i++) {
      // Set up test data
      MDC.put("testRequestId", "perf-test-" + i);
      MDC.put("testTraceId", "trace-" + i);
      MDC.put("testSpanId", "span-" + i);

      // Measure capture time
      long captureStart = System.nanoTime();
      TracingContextCapture context = TracingContextCapture.capture();
      long captureEnd = System.nanoTime();
      captureTimes.add((captureEnd - captureStart) / 1_000_000); // Convert to milliseconds

      // Clear MDC to simulate context loss
      MDC.clear();

      // Measure restore time
      long restoreStart = System.nanoTime();
      context.restore();
      long restoreEnd = System.nanoTime();
      restoreTimes.add((restoreEnd - restoreStart) / 1_000_000);

      // Measure cleanup time
      long cleanupStart = System.nanoTime();
      context.cleanup();
      long cleanupEnd = System.nanoTime();
      cleanupTimes.add((cleanupEnd - cleanupStart) / 1_000_000);
    }

    // Calculate statistics
    double avgCaptureTime = captureTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    double avgRestoreTime = restoreTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    double avgCleanupTime = cleanupTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

    long maxCaptureTime = captureTimes.stream().mapToLong(Long::longValue).max().orElse(0);
    long maxRestoreTime = restoreTimes.stream().mapToLong(Long::longValue).max().orElse(0);
    long maxCleanupTime = cleanupTimes.stream().mapToLong(Long::longValue).max().orElse(0);

    // Report results
    System.out.printf("Average context capture time: %.2f ms%n", avgCaptureTime);
    System.out.printf("Average context restore time: %.2f ms%n", avgRestoreTime);
    System.out.printf("Average context cleanup time: %.2f ms%n", avgCleanupTime);
    System.out.printf("Max capture time: %d ms%n", maxCaptureTime);
    System.out.printf("Max restore time: %d ms%n", maxRestoreTime);
    System.out.printf("Max cleanup time: %d ms%n", maxCleanupTime);

    // Verify performance thresholds
    assertThat(avgCaptureTime)
        .as("Average capture time should be under threshold")
        .isLessThan(MAX_CONTEXT_OPERATION_TIME_MS);
    assertThat(avgRestoreTime)
        .as("Average restore time should be under threshold")
        .isLessThan(MAX_CONTEXT_OPERATION_TIME_MS);
    assertThat(avgCleanupTime)
        .as("Average cleanup time should be under threshold")
        .isLessThan(MAX_CONTEXT_OPERATION_TIME_MS);

    assertThat(maxCaptureTime)
        .as("Max capture time should be reasonable")
        .isLessThan(MAX_CONTEXT_OPERATION_TIME_MS * 5);
    assertThat(maxRestoreTime)
        .as("Max restore time should be reasonable")
        .isLessThan(MAX_CONTEXT_OPERATION_TIME_MS * 5);
    assertThat(maxCleanupTime)
        .as("Max cleanup time should be reasonable")
        .isLessThan(MAX_CONTEXT_OPERATION_TIME_MS * 5);
  }

  // Test 2: Test memory usage under high concurrent load
  @Test
  @WithMockUser(roles = "USER")
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void testMemoryUsageUnderHighConcurrentLoad() throws Exception {
    System.out.println("=== Memory Usage Under High Concurrent Load Test ===");

    // Baseline memory measurement
    System.gc();
    Thread.sleep(100); // Allow GC to complete
    MemoryUsage baselineMemory = memoryBean.getHeapMemoryUsage();
    long baselineUsedMemory = baselineMemory.getUsed();

    System.out.printf("Baseline memory usage: %d MB%n", baselineUsedMemory / (1024 * 1024));

    // High concurrency test setup
    int numberOfThreads = HIGH_CONCURRENCY_LOAD;
    int requestsPerThread = 50;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

    AtomicInteger successfulOperations = new AtomicInteger(0);
    AtomicInteger failedOperations = new AtomicInteger(0);
    ConcurrentHashMap<String, Exception> errors = new ConcurrentHashMap<>();

    List<Future<?>> futures = new ArrayList<>();

    // Launch concurrent workload
    for (int threadId = 0; threadId < numberOfThreads; threadId++) {
      final int currentThreadId = threadId;
      Future<?> future =
          executor.submit(
              () -> {
                try {
                  startLatch.await(); // Wait for all threads to be ready

                  for (int i = 0; i < requestsPerThread; i++) {
                    try {
                      // Simulate request processing with context operations
                      String requestId = "load-test-" + currentThreadId + "-" + i;
                      MDC.put("requestId", requestId);
                      MDC.put("threadId", String.valueOf(currentThreadId));
                      MDC.put("iterationId", String.valueOf(i));

                      // Perform context operations
                      TracingContextCapture context = TracingContextCapture.capture();

                      // Simulate some processing time
                      Thread.sleep(1); // 1ms processing time

                      // Context restoration and cleanup
                      MDC.clear();
                      context.restore();

                      // Verify context is properly restored
                      String restoredRequestId = MDC.get("requestId");
                      if (!requestId.equals(restoredRequestId)) {
                        throw new IllegalStateException(
                            "Context restoration failed for " + requestId);
                      }

                      context.cleanup();
                      successfulOperations.incrementAndGet();

                    } catch (Exception e) {
                      failedOperations.incrementAndGet();
                      errors.put("thread-" + currentThreadId + "-iter-" + i, e);
                    }
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  errors.put("thread-" + currentThreadId + "-interrupted", e);
                } finally {
                  MDC.clear();
                  completionLatch.countDown();
                }
              });
      futures.add(future);
    }

    // Start the load test
    Instant loadTestStart = Instant.now();
    startLatch.countDown();

    // Wait for completion
    boolean completed = completionLatch.await(4, TimeUnit.MINUTES);
    Instant loadTestEnd = Instant.now();
    executor.shutdown();

    // Measure memory after load test
    System.gc();
    Thread.sleep(100); // Allow GC to complete
    MemoryUsage afterLoadMemory = memoryBean.getHeapMemoryUsage();
    long afterLoadUsedMemory = afterLoadMemory.getUsed();
    long memoryIncrease = (afterLoadUsedMemory - baselineUsedMemory) / (1024 * 1024);

    // Calculate performance metrics
    Duration testDuration = Duration.between(loadTestStart, loadTestEnd);
    int totalOperations = numberOfThreads * requestsPerThread;
    double operationsPerSecond = totalOperations / (testDuration.toMillis() / 1000.0);

    // Report results
    System.out.printf("Load test completed: %s%n", completed);
    System.out.printf("Test duration: %d ms%n", testDuration.toMillis());
    System.out.printf("Total operations: %d%n", totalOperations);
    System.out.printf("Successful operations: %d%n", successfulOperations.get());
    System.out.printf("Failed operations: %d%n", failedOperations.get());
    System.out.printf("Operations per second: %.2f%n", operationsPerSecond);
    System.out.printf("Memory after load test: %d MB%n", afterLoadUsedMemory / (1024 * 1024));
    System.out.printf("Memory increase: %d MB%n", memoryIncrease);

    // Verify test success
    assertThat(completed).as("Load test should complete within timeout").isTrue();
    assertThat(successfulOperations.get())
        .as("Most operations should succeed")
        .isGreaterThan((int) (totalOperations * 0.95));
    assertThat(failedOperations.get())
        .as("Failed operations should be minimal")
        .isLessThan((int) (totalOperations * 0.05));
    assertThat(memoryIncrease)
        .as("Memory increase should be within limits")
        .isLessThan(MAX_MEMORY_INCREASE_MB);

    // Report any errors
    if (!errors.isEmpty()) {
      System.out.println("Errors encountered during load test:");
      errors.entrySet().stream()
          .limit(10)
          .forEach(
              entry ->
                  System.out.printf("  %s: %s%n", entry.getKey(), entry.getValue().getMessage()));
    }
  }

  // Test 3: Validate no context leakage between requests
  @Test
  @WithMockUser(roles = "USER")
  void testContextLeakagePrevention() throws Exception {
    System.out.println("=== Context Leakage Prevention Test ===");

    int numberOfIterations = STRESS_TEST_ITERATIONS;
    AtomicInteger leakageDetected = new AtomicInteger(0);
    List<String> leakageDetails = new ArrayList<>();

    for (int iteration = 0; iteration < numberOfIterations; iteration++) {
      // Clear MDC to simulate clean state
      MDC.clear();

      // Verify MDC is clean before starting
      if (MDC.getCopyOfContextMap() != null && !MDC.getCopyOfContextMap().isEmpty()) {
        leakageDetected.incrementAndGet();
        leakageDetails.add(
            "Iteration " + iteration + ": MDC not clean at start - " + MDC.getCopyOfContextMap());
      }

      // Simulate request processing
      String requestId = "leakage-test-" + iteration;
      MDC.put("requestId", requestId);
      MDC.put("testData", "iteration-" + iteration);
      MDC.put("timestamp", String.valueOf(System.currentTimeMillis()));

      // Perform context operations
      TracingContextCapture context = TracingContextCapture.capture();

      // Simulate context restoration in different thread (common scenario)
      final String finalRequestId = requestId;
      final int finalIteration = iteration;
      Thread contextThread =
          new Thread(
              () -> {
                try {
                  // Verify thread-local isolation
                  if (MDC.getCopyOfContextMap() != null && !MDC.getCopyOfContextMap().isEmpty()) {
                    synchronized (leakageDetails) {
                      leakageDetected.incrementAndGet();
                      leakageDetails.add(
                          "Iteration "
                              + finalIteration
                              + ": Context leaked to new thread - "
                              + MDC.getCopyOfContextMap());
                    }
                  }

                  // Restore context in this thread
                  context.restore();

                  // Verify restoration worked
                  String restoredRequestId = MDC.get("requestId");
                  if (!finalRequestId.equals(restoredRequestId)) {
                    synchronized (leakageDetails) {
                      leakageDetected.incrementAndGet();
                      leakageDetails.add(
                          "Iteration "
                              + finalIteration
                              + ": Context restoration failed - expected: "
                              + finalRequestId
                              + ", got: "
                              + restoredRequestId);
                    }
                  }

                  // Cleanup context
                  context.cleanup();

                  // Verify cleanup worked
                  if (MDC.getCopyOfContextMap() != null && !MDC.getCopyOfContextMap().isEmpty()) {
                    synchronized (leakageDetails) {
                      leakageDetected.incrementAndGet();
                      leakageDetails.add(
                          "Iteration "
                              + finalIteration
                              + ": Context not cleaned up properly - "
                              + MDC.getCopyOfContextMap());
                    }
                  }

                } catch (Exception e) {
                  synchronized (leakageDetails) {
                    leakageDetected.incrementAndGet();
                    leakageDetails.add(
                        "Iteration "
                            + finalIteration
                            + ": Exception in context thread - "
                            + e.getMessage());
                  }
                }
              });

      contextThread.start();
      contextThread.join(1000); // Wait max 1 second

      // Back in main thread, verify no leakage
      if (MDC.getCopyOfContextMap() != null && !MDC.getCopyOfContextMap().isEmpty()) {
        leakageDetected.incrementAndGet();
        leakageDetails.add(
            "Iteration "
                + iteration
                + ": Context leaked back to main thread - "
                + MDC.getCopyOfContextMap());
      }

      // Final cleanup
      MDC.clear();
    }

    // Report results
    System.out.printf("Context leakage test completed: %d iterations%n", numberOfIterations);
    System.out.printf("Leakage incidents detected: %d%n", leakageDetected.get());

    if (leakageDetected.get() > 0) {
      System.out.println("Leakage details:");
      leakageDetails.stream().limit(20).forEach(detail -> System.out.println("  " + detail));
    }

    // Verify no context leakage
    assertThat(leakageDetected.get()).as("No context leakage should be detected").isEqualTo(0);
  }

  // Test 4: Test long-running request scenarios
  @Test
  @WithMockUser(roles = "USER")
  @Timeout(value = 3, unit = TimeUnit.MINUTES)
  void testLongRunningRequestScenarios() throws Exception {
    System.out.println("=== Long-Running Request Scenarios Test ===");

    // Baseline memory
    System.gc();
    Thread.sleep(100);
    MemoryUsage baselineMemory = memoryBean.getHeapMemoryUsage();
    long baselineUsedMemory = baselineMemory.getUsed();

    int numberOfLongRequests = 20;
    long requestDurationMs = 5000; // 5 seconds per request
    ExecutorService executor = Executors.newFixedThreadPool(numberOfLongRequests);
    CountDownLatch completionLatch = new CountDownLatch(numberOfLongRequests);

    AtomicInteger successfulRequests = new AtomicInteger(0);
    AtomicInteger failedRequests = new AtomicInteger(0);
    AtomicLong totalProcessingTime = new AtomicLong(0);
    ConcurrentHashMap<String, String> requestResults = new ConcurrentHashMap<>();

    List<Future<?>> futures = new ArrayList<>();
    Instant testStart = Instant.now();

    // Launch long-running requests
    for (int i = 0; i < numberOfLongRequests; i++) {
      final int requestId = i;
      Future<?> future =
          executor.submit(
              () -> {
                String requestIdentifier = "long-request-" + requestId;
                Instant requestStart = Instant.now();

                try {
                  // Set up request context
                  MDC.put("requestId", requestIdentifier);
                  MDC.put("startTime", String.valueOf(System.currentTimeMillis()));
                  MDC.put("requestType", "long-running");

                  // Capture initial context
                  TracingContextCapture initialContext = TracingContextCapture.capture();

                  // Simulate long-running processing with periodic context operations
                  long endTime = System.currentTimeMillis() + requestDurationMs;
                  int contextOperations = 0;

                  while (System.currentTimeMillis() < endTime) {
                    // Simulate some processing
                    Thread.sleep(100);

                    // Periodic context operations (every 500ms)
                    if (contextOperations % 5 == 0) {
                      // Clear and restore context to simulate complex processing
                      MDC.clear();
                      initialContext.restore();

                      // Verify context is still valid
                      String currentRequestId = MDC.get("requestId");
                      if (!requestIdentifier.equals(currentRequestId)) {
                        throw new IllegalStateException(
                            "Context corruption detected for " + requestIdentifier);
                      }
                    }

                    contextOperations++;
                  }

                  // Final context cleanup
                  initialContext.cleanup();

                  Instant requestEnd = Instant.now();
                  long processingTime = Duration.between(requestStart, requestEnd).toMillis();
                  totalProcessingTime.addAndGet(processingTime);

                  successfulRequests.incrementAndGet();
                  requestResults.put(requestIdentifier, "SUCCESS - " + processingTime + "ms");

                } catch (Exception e) {
                  failedRequests.incrementAndGet();
                  requestResults.put(requestIdentifier, "FAILED - " + e.getMessage());
                } finally {
                  MDC.clear();
                  completionLatch.countDown();
                }
              });
      futures.add(future);
    }

    // Wait for all requests to complete
    boolean allCompleted = completionLatch.await(2, TimeUnit.MINUTES);
    executor.shutdown();

    Instant testEnd = Instant.now();
    Duration totalTestTime = Duration.between(testStart, testEnd);

    // Measure memory after long-running test
    System.gc();
    Thread.sleep(100);
    MemoryUsage afterTestMemory = memoryBean.getHeapMemoryUsage();
    long afterTestUsedMemory = afterTestMemory.getUsed();
    long memoryIncrease = (afterTestUsedMemory - baselineUsedMemory) / (1024 * 1024);

    // Calculate metrics
    double avgProcessingTime =
        successfulRequests.get() > 0
            ? totalProcessingTime.get() / (double) successfulRequests.get()
            : 0;

    // Report results
    System.out.printf("Long-running request test completed: %s%n", allCompleted);
    System.out.printf("Total test duration: %d ms%n", totalTestTime.toMillis());
    System.out.printf("Successful requests: %d%n", successfulRequests.get());
    System.out.printf("Failed requests: %d%n", failedRequests.get());
    System.out.printf("Average processing time: %.2f ms%n", avgProcessingTime);
    System.out.printf("Memory increase: %d MB%n", memoryIncrease);

    // Verify test success
    assertThat(allCompleted).as("All long-running requests should complete").isTrue();
    assertThat(successfulRequests.get())
        .as("Most requests should succeed")
        .isEqualTo(numberOfLongRequests);
    assertThat(failedRequests.get()).as("No requests should fail").isEqualTo(0);
    assertThat(avgProcessingTime)
        .as("Average processing time should be reasonable")
        .isBetween(requestDurationMs * 0.9, requestDurationMs * 1.2);
    assertThat(memoryIncrease)
        .as("Memory increase should be controlled")
        .isLessThan(MAX_MEMORY_INCREASE_MB);

    // Report any failures
    if (failedRequests.get() > 0) {
      System.out.println("Failed request details:");
      requestResults.entrySet().stream()
          .filter(entry -> entry.getValue().startsWith("FAILED"))
          .forEach(entry -> System.out.printf("  %s: %s%n", entry.getKey(), entry.getValue()));
    }
  }

  // Test 5: Stress test with rapid context operations
  @Test
  @WithMockUser(roles = "USER")
  @Timeout(value = 2, unit = TimeUnit.MINUTES)
  void testRapidContextOperationsStress() throws Exception {
    System.out.println("=== Rapid Context Operations Stress Test ===");

    int rapidOperations = 50000;
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);
    List<String> errorMessages = new ArrayList<>();

    Instant stressTestStart = Instant.now();

    // Perform rapid context operations
    for (int i = 0; i < rapidOperations; i++) {
      try {
        // Rapid fire context operations
        MDC.put("rapidTest", "iteration-" + i);
        MDC.put("timestamp", String.valueOf(System.nanoTime()));

        TracingContextCapture context = TracingContextCapture.capture();
        MDC.clear();
        context.restore();

        // Verify restoration
        String rapidTest = MDC.get("rapidTest");
        if (!("iteration-" + i).equals(rapidTest)) {
          throw new IllegalStateException("Rapid context operation failed at iteration " + i);
        }

        context.cleanup();
        successCount.incrementAndGet();

        // Occasional thread yield to allow other operations
        if (i % 1000 == 0) {
          Thread.yield();
        }

      } catch (Exception e) {
        errorCount.incrementAndGet();
        synchronized (errorMessages) {
          if (errorMessages.size() < 100) { // Limit error collection
            errorMessages.add("Iteration " + i + ": " + e.getMessage());
          }
        }
      }
    }

    Instant stressTestEnd = Instant.now();
    Duration stressTestDuration = Duration.between(stressTestStart, stressTestEnd);
    double operationsPerSecond = rapidOperations / (stressTestDuration.toMillis() / 1000.0);

    // Report results
    System.out.printf("Rapid context operations stress test completed%n");
    System.out.printf("Total operations: %d%n", rapidOperations);
    System.out.printf("Successful operations: %d%n", successCount.get());
    System.out.printf("Failed operations: %d%n", errorCount.get());
    System.out.printf("Test duration: %d ms%n", stressTestDuration.toMillis());
    System.out.printf("Operations per second: %.2f%n", operationsPerSecond);
    System.out.printf(
        "Success rate: %.2f%%%n", (successCount.get() / (double) rapidOperations) * 100);

    // Verify stress test success
    assertThat(successCount.get())
        .as("Most operations should succeed")
        .isGreaterThan((int) (rapidOperations * 0.95));
    assertThat(errorCount.get())
        .as("Error rate should be minimal")
        .isLessThan((int) (rapidOperations * 0.05));
    assertThat(operationsPerSecond).as("Should maintain reasonable throughput").isGreaterThan(1000);

    // Report first few errors if any
    if (!errorMessages.isEmpty()) {
      System.out.println("Sample error messages:");
      errorMessages.stream().limit(10).forEach(msg -> System.out.println("  " + msg));
    }
  }
}
