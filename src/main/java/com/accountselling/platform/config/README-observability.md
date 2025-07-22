# Observability Configuration

This document describes the observability setup for the Account Selling Platform, including OpenTelemetry for tracing and structured logging with Elasticsearch integration.

## OpenTelemetry Configuration

The platform uses Spring Boot's auto-configuration for OpenTelemetry through Micrometer Tracing.

### Key Components:

1. **Micrometer Tracing**: Provides integration between Spring Boot and OpenTelemetry
2. **Auto-configuration**: Spring Boot automatically configures OpenTelemetry based on application.yml settings
3. **MDC Integration**: Trace IDs are automatically added to MDC for correlation in logs

### Usage in Code:

```java
// Inject the Micrometer tracer
private final io.micrometer.tracing.Tracer tracer;

public MyService(io.micrometer.tracing.Tracer tracer) {
    this.tracer = tracer;
}

// Create and use spans
public void myMethod() {
    Span span = tracer.nextSpan().name("operation-name");
    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
        // Your code here
        span.tag("attribute-name", "value");
        
        // Child operations will automatically be part of this trace
        anotherMethod();
    } finally {
        span.end();
    }
}
```

### Automatic Tracing:

Spring Boot automatically traces web requests and other operations. You don't need to use any special annotations:

```java
// Methods in @RestController, @Controller, @Service, etc. are automatically traced
public void myMethod() {
    // Method will be automatically traced by Spring Boot
}
```

## Structured Logging

The platform uses SLF4J with Logback for structured logging, configured to output in ECS format and send logs to Elasticsearch.

### Key Components:

1. **logback-spring.xml**: Configures appenders and log format
2. **Lombok @Slf4j**: Simplifies logger creation

### Usage in Code:

```java
// Use Lombok to create a logger
@Slf4j
public class MyClass {
    
    public void myMethod() {
        // Basic logging
        log.info("Processing request");
        log.error("Error occurred", exception);
        
        // Trace IDs are automatically included in logs
        log.info("This log will include the current trace ID");
        
        // You can add custom MDC values if needed
        MDC.put("userId", "123");
        MDC.put("accountId", "456");
        try {
            log.info("Processing order");
        } finally {
            MDC.remove("userId");
            MDC.remove("accountId");
        }
    }
}
```

### Available MDC Fields for Kibana

The following MDC fields are automatically added to logs and available in Kibana:

#### Trace Information
- `trace_id` - OpenTelemetry trace ID
- `span_id` - OpenTelemetry span ID
- `trace_flags` - OpenTelemetry trace flags

#### HTTP Request Information
- `http.request.method` - HTTP method (GET, POST, etc.)
- `http.request.path` - Request URI
- `http.response.status_code` - HTTP status code
- `http.request.remote_ip` - Client IP address

#### User Information
- `user.id` - User ID (if authenticated)
- `user.name` - Username (if authenticated)

#### Application Metrics
- `execution.time_ms` - Method execution time in milliseconds
- `memory.used` - Memory used (when available)

#### Error Information
- `error.type` - Exception class name
- `error.code` - Error code (when available)

#### Business Context
- `request.id` - Unique ID for each request
- `session.id` - HTTP session ID (when available)
- `account.id` - Account ID (when added manually)
- `transaction.id` - Transaction ID (when added manually)

### Adding Custom Fields

You can add custom fields to your logs by using MDC:

```java
// Add business context
MDC.put("accountId", account.getId());
MDC.put("transactionId", transaction.getId());
MDC.put("productId", product.getId());

try {
    // Your business logic here
    log.info("Processing transaction");
} finally {
    // Clean up MDC to prevent memory leaks
    MDC.remove("accountId");
    MDC.remove("transactionId");
    MDC.remove("productId");
}
```
```

## Configuration Properties

The following properties can be configured in `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Sampling rate (0.0 to 1.0)
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces  # OTLP endpoint

logging:
  elasticsearch:
    host: localhost  # Elasticsearch host
    port: 5000       # Elasticsearch port
```

## Testing the Setup

1. Use the `/api/observability/trace` endpoint to generate a test trace
2. Check the response for the trace ID
3. Look for the trace in your OpenTelemetry backend (Jaeger, Zipkin, etc.)
4. Check Elasticsearch for the corresponding log entries with the same trace ID