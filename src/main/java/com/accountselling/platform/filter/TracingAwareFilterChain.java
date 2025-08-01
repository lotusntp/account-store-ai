package com.accountselling.platform.filter;

import com.accountselling.platform.util.TracingContextCapture;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * A wrapper around FilterChain that captures tracing context at the optimal moment. This class
 * addresses the timing issue where OpenTelemetry clears MDC context in its finally blocks after
 * request processing completes, but before our filter's finally block executes.
 *
 * <p>The key insight is to capture the tracing context immediately after the original
 * chain.doFilter() completes, but before any cleanup operations.
 */
@Slf4j
public class TracingAwareFilterChain implements FilterChain {

  private final FilterChain originalChain;
  private final Consumer<TracingContextCapture> contextCaptor;

  /**
   * Creates a new TracingAwareFilterChain wrapper
   *
   * @param originalChain The original FilterChain to wrap
   * @param contextCaptor Consumer that will receive the captured context
   */
  public TracingAwareFilterChain(
      FilterChain originalChain, Consumer<TracingContextCapture> contextCaptor) {
    this.originalChain = originalChain;
    this.contextCaptor = contextCaptor;

    if (originalChain == null) {
      throw new IllegalArgumentException("Original FilterChain cannot be null");
    }
    if (contextCaptor == null) {
      throw new IllegalArgumentException("Context captor cannot be null");
    }

    // TracingAwareFilterChain created successfully
  }

  /**
   * Executes the original filter chain and captures tracing context at the optimal timing -
   * immediately after chain completion but before any OpenTelemetry cleanup operations.
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response)
      throws IOException, ServletException {

    TracingContextCapture capturedContext = null;
    boolean chainExecutedSuccessfully = false;

    try {
      // Execute the original filter chain
      originalChain.doFilter(request, response);
      chainExecutedSuccessfully = true;

      // CRITICAL TIMING: Capture context immediately after chain completes
      // This is the key moment - after business logic but before OpenTelemetry cleanup
      capturedContext = TracingContextCapture.capture();

    } catch (IOException | ServletException e) {
      // Even if there's an exception, try to capture context for error logging
      try {
        capturedContext = TracingContextCapture.capture();
      } catch (Exception captureException) {
        log.warn("Failed to capture context after chain exception", captureException);
      }
      // Re-throw the original exception
      throw e;

    } catch (RuntimeException e) {
      // Try to capture context for error logging
      try {
        capturedContext = TracingContextCapture.capture();
      } catch (Exception captureException) {
        log.warn("Failed to capture context after runtime exception", captureException);
      }
      // Re-throw the original exception
      throw e;

    } finally {
      // Always notify the captor about the captured context (even if null)
      try {
        if (capturedContext != null) {
          contextCaptor.accept(capturedContext);
        } else {
          // Create a fallback context for correlation purposes
          TracingContextCapture fallbackContext = TracingContextCapture.capture();
          contextCaptor.accept(fallbackContext);
        }
      } catch (Exception captorException) {
        log.error("Context captor failed to process captured context", captorException);
        // Don't re-throw - we don't want to break the request flow
      }
    }
  }

  /**
   * Gets the original FilterChain that this wrapper delegates to
   *
   * @return the original FilterChain
   */
  public FilterChain getOriginalChain() {
    return originalChain;
  }

  /** Creates a string representation for debugging */
  @Override
  public String toString() {
    return String.format(
        "TracingAwareFilterChain{originalChain=%s}", originalChain.getClass().getSimpleName());
  }
}
