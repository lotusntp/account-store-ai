package com.accountselling.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Unit tests for LoggingService. Tests structured logging functionality and MDC context management.
 */
class LoggingServiceTest {

  private LoggingService loggingService;
  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    loggingService = new LoggingService();

    // Set up test appender to capture log events
    logger = (Logger) LoggerFactory.getLogger(LoggingService.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @Test
  void testLogBusinessEvent() {
    // Given
    String eventType = "user_registration";
    String message = "New user registered";
    Map<String, Object> data = new HashMap<>();
    data.put("userId", "12345");
    data.put("email", "test@example.com");

    // When
    loggingService.logBusinessEvent(eventType, message, data);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    assertThat(logEvent.getFormattedMessage()).contains("Business Event: " + message);
  }

  @Test
  void testLogSecurityEvent() {
    // Given
    String eventType = "failed_login";
    String message = "Failed login attempt";
    String userId = "user123";
    Map<String, Object> data = new HashMap<>();
    data.put("ipAddress", "192.168.1.1");
    data.put("userAgent", "Mozilla/5.0");

    // When
    loggingService.logSecurityEvent(eventType, message, userId, data);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("WARN");
    assertThat(logEvent.getFormattedMessage()).contains("Security Event: " + message);
  }

  @Test
  void testLogSystemEvent() {
    // Given
    String eventType = "application_startup";
    String message = "Application started successfully";
    Map<String, Object> data = new HashMap<>();
    data.put("version", "1.0.0");
    data.put("profile", "dev");

    // When
    loggingService.logSystemEvent(eventType, message, data);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    assertThat(logEvent.getFormattedMessage()).contains("System Event: " + message);
  }

  @Test
  void testLogError() {
    // Given
    String eventType = "database_error";
    String message = "Database connection failed";
    Exception exception = new RuntimeException("Connection timeout");
    Map<String, Object> data = new HashMap<>();
    data.put("database", "postgresql");
    data.put("host", "localhost");

    // When
    loggingService.logError(eventType, message, exception, data);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("ERROR");
    assertThat(logEvent.getFormattedMessage()).contains("Error Event: " + message);
    assertThat(logEvent.getThrowableProxy()).isNotNull();
  }

  @Test
  void testLogUserActivity() {
    // Given
    String userId = "user123";
    String activity = "product_view";
    String details = "User viewed product details";
    Map<String, Object> data = new HashMap<>();
    data.put("productId", "prod456");
    data.put("category", "games");

    // When
    loggingService.logUserActivity(userId, activity, details, data);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    assertThat(logEvent.getFormattedMessage()).contains("User Activity: " + details);
  }

  @Test
  void testLogApiRequest() {
    // Given
    String method = "GET";
    String endpoint = "/api/products";
    String userId = "user123";
    long duration = 150L;
    int statusCode = 200;
    Map<String, Object> data = new HashMap<>();
    data.put("userAgent", "Mozilla/5.0");

    // When
    loggingService.logApiRequest(method, endpoint, userId, duration, statusCode, data);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    assertThat(logEvent.getFormattedMessage()).contains("API Request: " + method + " " + endpoint);
    assertThat(logEvent.getFormattedMessage()).contains(String.valueOf(duration));
    assertThat(logEvent.getFormattedMessage()).contains(String.valueOf(statusCode));
  }

  @Test
  void testLogDatabaseOperation() {
    // Given
    String operation = "SELECT";
    String table = "users";
    long duration = 25L;
    Map<String, Object> data = new HashMap<>();
    data.put("rowCount", 10);

    // When
    loggingService.logDatabaseOperation(operation, table, duration, data);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    assertThat(logEvent.getFormattedMessage())
        .contains("Database Operation: " + operation + " on " + table);
  }

  @Test
  void testSetAndClearCorrelationId() {
    // Given
    String correlationId = "test-correlation-id";

    // When
    loggingService.setCorrelationId(correlationId);

    // Then
    assertThat(MDC.get("requestId")).isEqualTo(correlationId);

    // When
    loggingService.clearContext();

    // Then
    assertThat(MDC.get("requestId")).isNull();
  }

  @Test
  void testSetUserContext() {
    // Given
    String userId = "user123";
    String sessionId = "session456";

    // When
    loggingService.setUserContext(userId, sessionId);

    // Then
    assertThat(MDC.get("userId")).isEqualTo(userId);
    assertThat(MDC.get("sessionId")).isEqualTo(sessionId);

    // Cleanup
    loggingService.clearContext();
  }

  @Test
  void testGenerateCorrelationId() {
    // When
    String correlationId1 = loggingService.generateCorrelationId();
    String correlationId2 = loggingService.generateCorrelationId();

    // Then
    assertThat(correlationId1).isNotNull();
    assertThat(correlationId2).isNotNull();
    assertThat(correlationId1).isNotEqualTo(correlationId2);
    assertThat(correlationId1)
        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  }

  @Test
  void testLogWithStructuredArgs() {
    // Given
    String level = "INFO";
    String message = "Test message with structured args: userId={}, action={}";
    Object[] keyValuePairs = {"userId", "user123", "action", "login"};

    // When
    loggingService.logWithStructuredArgs(level, message, keyValuePairs);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
  }

  @Test
  void testLogBusinessEventWithNullData() {
    // Given
    String eventType = "test_event";
    String message = "Test message";

    // When
    loggingService.logBusinessEvent(eventType, message, null);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    assertThat(logEvent.getFormattedMessage()).contains("Business Event: " + message);
  }

  @Test
  void testLogBusinessEventWithEmptyData() {
    // Given
    String eventType = "test_event";
    String message = "Test message";
    Map<String, Object> data = new HashMap<>();

    // When
    loggingService.logBusinessEvent(eventType, message, data);

    // Then
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList).hasSize(1);

    ILoggingEvent logEvent = logsList.get(0);
    assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    assertThat(logEvent.getFormattedMessage()).contains("Business Event: " + message);
  }
}
