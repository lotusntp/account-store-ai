# Design Document

## Overview

This design addresses the critical issue where HTTP response logs lose tracing context (traceId, spanId) due to OpenTelemetry's MDC cleanup process. The solution involves redesigning the logging filter architecture to capture and preserve tracing context throughout the entire request-response lifecycle.

The core challenge is that OpenTelemetry clears MDC context in its own finally blocks after request processing completes, but before our filter's finally block executes. This timing issue causes response logs to lose essential tracing information.

## Architecture

### Current Problem Flow
```
1. Request enters Filter
2. OpenTelemetry sets traceId/spanId in MDC
3. Business logic executes with tracing context
4. Business logic completes
5. OpenTelemetry clears MDC in its finally block ← PROBLEM
6. Filter's finally block executes (tracing context lost)
7. Response logged without traceId/spanId
```

### Proposed Solution Flow
```
1. Request enters Filter
2. Capture initial tracing context (if any)
3. OpenTelemetry sets traceId/spanId in MDC
4. Business logic executes with tracing context
5. Immediately capture tracing context after chain.doFilter()
6. OpenTelemetry clears MDC in its finally block
7. Filter's finally block restores captured tracing context
8. Response logged with preserved traceId/spanId
9. Clean up preserved context
```

## Components and Interfaces

### 1. TracingContextCapture
A utility class responsible for capturing and preserving tracing context.

```java
public class TracingContextCapture {
    private String traceId;
    private String spanId;
    private String requestId;
    private Map<String, String> additionalContext;
    
    public static TracingContextCapture capture();
    public void restore();
    public boolean hasValidContext();
}
```

### 2. Enhanced SimpleRequestResponseLoggingFilter
Redesigned filter with proper tracing context preservation.

Key changes:
- Capture tracing context immediately after chain.doFilter()
- Use custom response wrapper to intercept response completion
- Restore tracing context before response logging
- Implement proper cleanup to prevent context leakage

### 3. TracingAwareFilterChain
A wrapper around FilterChain that captures tracing context at the optimal moment.

```java
public class TracingAwareFilterChain implements FilterChain {
    private final FilterChain originalChain;
    private final Consumer<TracingContextCapture> contextCaptor;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response) 
        throws IOException, ServletException;
}
```

### 4. ResponseTracingWrapper
A custom HttpServletResponseWrapper that captures tracing context when response is committed.

```java
public class ResponseTracingWrapper extends HttpServletResponseWrapper {
    private final Consumer<TracingContextCapture> onResponseCommit;
    
    @Override
    public void flushBuffer() throws IOException;
    @Override
    public PrintWriter getWriter() throws IOException;
    @Override
    public ServletOutputStream getOutputStream() throws IOException;
}
```

## Data Models

### TracingContext
```java
public class TracingContext {
    private final String traceId;
    private final String spanId;
    private final String requestId;
    private final long captureTimestamp;
    private final Map<String, String> mdcSnapshot;
    
    // Immutable data structure for thread safety
}
```

### LoggingMetrics
```java
public class LoggingMetrics {
    private final long startTime;
    private final long endTime;
    private final long duration;
    private final int statusCode;
    private final String method;
    private final String uri;
}
```

## Error Handling

### Context Capture Failures
- If tracing context capture fails, log a warning but continue processing
- Fall back to correlation ID for request-response matching
- Ensure application functionality is never compromised

### MDC Restoration Failures
- If MDC restoration fails, log the error with available context
- Attempt to restore at least the correlation ID
- Clean up any partial state to prevent context leakage

### Concurrent Request Handling
- Use thread-local storage for context preservation
- Implement proper cleanup in finally blocks
- Handle edge cases where threads are reused

## Testing Strategy

### Unit Tests
1. **TracingContextCapture Tests**
   - Test context capture with valid OpenTelemetry context
   - Test context capture with missing tracing information
   - Test context restoration and cleanup

2. **Filter Integration Tests**
   - Test request-response logging with tracing context
   - Test concurrent request handling
   - Test error scenarios and fallback behavior

3. **Performance Tests**
   - Measure overhead of context capture and restoration
   - Test memory usage with high concurrent load
   - Validate no context leakage between requests

### Integration Tests
1. **End-to-End Tracing Tests**
   - Verify traceId consistency across request-response logs
   - Test with real OpenTelemetry configuration
   - Validate distributed tracing propagation

2. **Stress Tests**
   - High concurrent request load
   - Memory pressure scenarios
   - Long-running request scenarios

### Mock Tests
1. **OpenTelemetry Interaction Tests**
   - Mock OpenTelemetry MDC behavior
   - Test timing of context cleanup
   - Verify filter behavior with different OpenTelemetry configurations

## Implementation Phases

### Phase 1: Core Context Capture
- Implement TracingContextCapture utility
- Create basic context preservation mechanism
- Add unit tests for context capture/restore

### Phase 2: Filter Redesign
- Redesign SimpleRequestResponseLoggingFilter
- Implement TracingAwareFilterChain
- Add integration with existing logging service

### Phase 3: Response Wrapper
- Implement ResponseTracingWrapper
- Add response commit detection
- Integrate with context preservation

### Phase 4: Testing and Optimization
- Comprehensive testing suite
- Performance optimization
- Documentation and monitoring

## Monitoring and Observability

### Metrics to Track
- Context capture success rate
- Context restoration success rate
- Filter processing overhead
- Memory usage of preserved contexts

### Logging for Debugging
- Debug logs for context capture/restore operations
- Warning logs for fallback scenarios
- Error logs for critical failures

### Health Checks
- Verify tracing context consistency
- Monitor for context leakage
- Alert on high failure rates

## Backward Compatibility

- Maintain existing LoggingService interface
- Preserve current log format and structure
- Ensure no breaking changes to existing functionality
- Support gradual rollout with feature flags if needed