package com.accountselling.platform.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.accountselling.platform.util.TracingContextCapture;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class TracingAwareFilterChainTest {

  @Mock private FilterChain originalChain;

  @Mock private ServletRequest request;

  @Mock private ServletResponse response;

  private AtomicReference<TracingContextCapture> capturedContext;
  private Consumer<TracingContextCapture> contextCaptor;

  @BeforeEach
  void setUp() {
    capturedContext = new AtomicReference<>();
    contextCaptor = capturedContext::set;
    MDC.clear();
  }

  @Test
  void constructor_WithValidParameters_ShouldCreateInstance() {
    // When
    TracingAwareFilterChain filterChain = new TracingAwareFilterChain(originalChain, contextCaptor);

    // Then
    assertNotNull(filterChain);
    assertEquals(originalChain, filterChain.getOriginalChain());
  }

  @Test
  void constructor_WithNullOriginalChain_ShouldThrowException() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> new TracingAwareFilterChain(null, contextCaptor));
  }

  @Test
  void constructor_WithNullContextCaptor_ShouldThrowException() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> new TracingAwareFilterChain(originalChain, null));
  }

  @Test
  void doFilter_WithSuccessfulChainExecution_ShouldCaptureContext()
      throws IOException, ServletException {
    // Given
    TracingAwareFilterChain filterChain = new TracingAwareFilterChain(originalChain, contextCaptor);

    // Set up some MDC context to be captured
    MDC.put("requestId", "test-request-123");
    MDC.put("traceId", "test-trace-456");

    // When
    filterChain.doFilter(request, response);

    // Then
    verify(originalChain).doFilter(request, response);
    assertNotNull(capturedContext.get());
    assertEquals("test-request-123", capturedContext.get().getRequestId());
  }

  @Test
  void doFilter_WithChainException_ShouldStillCaptureContextAndRethrow()
      throws IOException, ServletException {
    // Given
    TracingAwareFilterChain filterChain = new TracingAwareFilterChain(originalChain, contextCaptor);
    ServletException expectedException = new ServletException("Test exception");

    doThrow(expectedException).when(originalChain).doFilter(request, response);

    // Set up some MDC context
    MDC.put("requestId", "test-request-exception");

    // When & Then
    ServletException thrownException =
        assertThrows(ServletException.class, () -> filterChain.doFilter(request, response));

    assertEquals(expectedException, thrownException);
    verify(originalChain).doFilter(request, response);

    // Context should still be captured even after exception
    assertNotNull(capturedContext.get());
    assertEquals("test-request-exception", capturedContext.get().getRequestId());
  }

  @Test
  void doFilter_WithIOException_ShouldStillCaptureContextAndRethrow()
      throws IOException, ServletException {
    // Given
    TracingAwareFilterChain filterChain = new TracingAwareFilterChain(originalChain, contextCaptor);
    IOException expectedException = new IOException("Test IO exception");

    doThrow(expectedException).when(originalChain).doFilter(request, response);

    // Set up some MDC context
    MDC.put("requestId", "test-request-io-exception");

    // When & Then
    IOException thrownException =
        assertThrows(IOException.class, () -> filterChain.doFilter(request, response));

    assertEquals(expectedException, thrownException);
    verify(originalChain).doFilter(request, response);

    // Context should still be captured even after exception
    assertNotNull(capturedContext.get());
    assertEquals("test-request-io-exception", capturedContext.get().getRequestId());
  }

  @Test
  void doFilter_WithRuntimeException_ShouldStillCaptureContextAndRethrow()
      throws IOException, ServletException {
    // Given
    TracingAwareFilterChain filterChain = new TracingAwareFilterChain(originalChain, contextCaptor);
    RuntimeException expectedException = new RuntimeException("Test runtime exception");

    doThrow(expectedException).when(originalChain).doFilter(request, response);

    // Set up some MDC context
    MDC.put("requestId", "test-request-runtime-exception");

    // When & Then
    RuntimeException thrownException =
        assertThrows(RuntimeException.class, () -> filterChain.doFilter(request, response));

    assertEquals(expectedException, thrownException);
    verify(originalChain).doFilter(request, response);

    // Context should still be captured even after exception
    assertNotNull(capturedContext.get());
    assertEquals("test-request-runtime-exception", capturedContext.get().getRequestId());
  }

  @Test
  void doFilter_WithContextCaptorException_ShouldNotBreakRequestFlow()
      throws IOException, ServletException {
    // Given
    Consumer<TracingContextCapture> faultyCaptor =
        context -> {
          throw new RuntimeException("Captor failed");
        };
    TracingAwareFilterChain filterChain = new TracingAwareFilterChain(originalChain, faultyCaptor);

    // When - should not throw exception despite captor failure
    assertDoesNotThrow(() -> filterChain.doFilter(request, response));

    // Then
    verify(originalChain).doFilter(request, response);
  }

  @Test
  void toString_ShouldReturnMeaningfulRepresentation() {
    // Given
    TracingAwareFilterChain filterChain = new TracingAwareFilterChain(originalChain, contextCaptor);

    // When
    String result = filterChain.toString();

    // Then
    assertTrue(result.contains("TracingAwareFilterChain"));
    assertTrue(result.contains("FilterChain"));
  }
}
