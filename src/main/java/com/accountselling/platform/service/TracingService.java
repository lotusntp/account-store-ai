package com.accountselling.platform.service;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to demonstrate tracing functionality using Micrometer Tracer. Shows how to create custom
 * spans and add trace information to logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TracingService {

  private final Tracer tracer;

  /**
   * Demonstrates creating a custom span with tags. This method shows how to manually instrument
   * code for tracing.
   */
  public String performTracedOperation(String operationName, String userId) {
    // Create a new span for this operation
    Span span =
        tracer
            .nextSpan()
            .name("traced-operation")
            .tag("operation.name", operationName)
            .tag("user.id", userId);

    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
      log.info("Starting traced operation: {} for user: {}", operationName, userId);

      // Simulate some work
      performSubOperation("sub-operation-1");
      performSubOperation("sub-operation-2");

      log.info("Completed traced operation: {} for user: {}", operationName, userId);

      return "Operation completed successfully";

    } catch (Exception e) {
      // Record the error in the span
      span.tag("error", "true");
      span.tag("error.message", e.getMessage());

      log.error("Error in traced operation: {} for user: {}", operationName, userId, e);
      throw e;

    } finally {
      span.end();
    }
  }

  /**
   * Demonstrates nested span creation. Child spans automatically inherit the trace context from
   * parent spans.
   */
  private void performSubOperation(String subOperationName) {
    Span childSpan =
        tracer.nextSpan().name("sub-operation").tag("sub.operation.name", subOperationName);

    try (Tracer.SpanInScope ws = tracer.withSpan(childSpan.start())) {
      log.info("Executing sub-operation: {}", subOperationName);

      // Simulate some processing time
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Sub-operation interrupted", e);
      }

      childSpan.tag("status", "completed");
      log.info("Sub-operation completed: {}", subOperationName);

    } finally {
      childSpan.end();
    }
  }

  /**
   * Demonstrates automatic tracing of service methods. Spring Boot automatically creates spans
   * for @Service methods.
   */
  public String performAutoTracedOperation(String data) {
    log.info("Performing auto-traced operation with data: {}", data);

    // This method will be automatically traced by Spring Boot
    // The trace ID will be automatically included in logs

    try {
      // Simulate some processing
      Thread.sleep(50);

      log.info("Auto-traced operation completed successfully");
      return "Auto-traced operation result: " + data.toUpperCase();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Auto-traced operation interrupted", e);
      throw new RuntimeException("Operation interrupted", e);
    }
  }

  /** Demonstrates error handling in traced operations. */
  public void performOperationWithError() {
    Span span = tracer.nextSpan().name("error-operation").tag("operation.type", "error-demo");

    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
      log.info("Starting operation that will fail");

      // Simulate an error
      throw new RuntimeException("Simulated error for tracing demonstration");

    } catch (Exception e) {
      // Properly record the error in the span
      span.tag("error", "true");
      span.tag("error.type", e.getClass().getSimpleName());
      span.tag("error.message", e.getMessage());

      log.error("Operation failed with error", e);
      throw e;

    } finally {
      span.end();
    }
  }
}
