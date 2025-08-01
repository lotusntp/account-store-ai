package com.accountselling.platform.controller;

import com.accountselling.platform.service.TracingService;
import io.micrometer.tracing.Tracer;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to demonstrate OpenTelemetry tracing and logging functionality. This controller
 * provides endpoints to test tracing capabilities.
 */
@Slf4j
@RestController
@RequestMapping("/api/observability")
@RequiredArgsConstructor
public class ObservabilityDemoController {

  private final TracingService tracingService;
  private final Tracer tracer;

  @GetMapping("/trace")
  public ResponseEntity<Map<String, String>> traceDemo() {
    log.info("Received request to trace demo endpoint");

    // Create response with trace information
    Map<String, String> response = new HashMap<>();
    response.put("status", "success");
    response.put("message", "Trace created successfully");

    // Add trace ID to response if available
    if (tracer.currentSpan() != null) {
      response.put("traceId", tracer.currentSpan().context().traceId());
      response.put("spanId", tracer.currentSpan().context().spanId());
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping("/trace/custom")
  public ResponseEntity<Map<String, String>> customTraceDemo(
      @RequestParam(defaultValue = "demo-operation") String operation,
      @RequestParam(defaultValue = "test-user") String userId) {

    log.info("Received request to custom trace demo endpoint");

    try {
      String result = tracingService.performTracedOperation(operation, userId);

      Map<String, String> response = new HashMap<>();
      response.put("status", "success");
      response.put("result", result);
      response.put("operation", operation);
      response.put("userId", userId);

      // Add trace information
      if (tracer.currentSpan() != null) {
        response.put("traceId", tracer.currentSpan().context().traceId());
        response.put("spanId", tracer.currentSpan().context().spanId());
      }

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error in custom trace demo", e);

      Map<String, String> response = new HashMap<>();
      response.put("status", "error");
      response.put("message", e.getMessage());

      return ResponseEntity.internalServerError().body(response);
    }
  }

  @GetMapping("/trace/auto")
  public ResponseEntity<Map<String, String>> autoTraceDemo(
      @RequestParam(defaultValue = "test-data") String data) {

    log.info("Received request to auto trace demo endpoint");

    try {
      String result = tracingService.performAutoTracedOperation(data);

      Map<String, String> response = new HashMap<>();
      response.put("status", "success");
      response.put("result", result);
      response.put("inputData", data);

      // Add trace information
      if (tracer.currentSpan() != null) {
        response.put("traceId", tracer.currentSpan().context().traceId());
        response.put("spanId", tracer.currentSpan().context().spanId());
      }

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error in auto trace demo", e);

      Map<String, String> response = new HashMap<>();
      response.put("status", "error");
      response.put("message", e.getMessage());

      return ResponseEntity.internalServerError().body(response);
    }
  }

  @GetMapping("/trace/error")
  public ResponseEntity<Map<String, String>> errorTraceDemo() {
    log.info("Received request to error trace demo endpoint");

    try {
      tracingService.performOperationWithError();

      // This should not be reached
      Map<String, String> response = new HashMap<>();
      response.put("status", "unexpected");
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Expected error in error trace demo", e);

      Map<String, String> response = new HashMap<>();
      response.put("status", "error");
      response.put("message", e.getMessage());
      response.put("errorType", e.getClass().getSimpleName());

      // Add trace information even for errors
      if (tracer.currentSpan() != null) {
        response.put("traceId", tracer.currentSpan().context().traceId());
        response.put("spanId", tracer.currentSpan().context().spanId());
      }

      return ResponseEntity.internalServerError().body(response);
    }
  }
}
