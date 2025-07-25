package com.accountselling.platform.controller;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to demonstrate Micrometer Tracing and logging functionality This is for testing
 * purposes only
 */
@Slf4j
@RestController
@RequestMapping("/api/observability")
@RequiredArgsConstructor
public class ObservabilityDemoController {

  private final Tracer tracer;

  @GetMapping("/trace")
  public ResponseEntity<Map<String, String>> traceDemo() {
    log.info("Received request to trace demo endpoint");

    // Create response
    Map<String, String> response = new HashMap<>();
    response.put("status", "success");
    response.put("message", "Trace created successfully");

    return ResponseEntity.ok(response);
  }

  private void performTracedOperation() {
    Span span = tracer.nextSpan().name("traced-operation");
    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
      log.info("Performing traced operation");
      // Simulate work
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        log.error("Operation interrupted", e);
        Thread.currentThread().interrupt();
      }
    } finally {
      span.end();
    }
  }
}
