package com.accountselling.platform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for RateLimitingFilter class. Tests critical methods and their behavior.
 *
 * <p>Unit tests สำหรับ RateLimitingFilter ทดสอบ methods สำคัญและพฤติกรรมการทำงาน
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter Unit Tests")
class RateLimitingFilterTest {

  @InjectMocks private RateLimitingFilter rateLimitingFilter;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @Mock private ConsumptionProbe consumptionProbe;

  private StringWriter responseWriter;
  private PrintWriter printWriter;

  @BeforeEach
  void setUp() throws IOException {
    responseWriter = new StringWriter();
    printWriter = new PrintWriter(responseWriter);
    lenient().when(response.getWriter()).thenReturn(printWriter);

    // Set test profile to avoid actual rate limiting in tests
    ReflectionTestUtils.setField(rateLimitingFilter, "activeProfile", "test");
  }

  @Test
  @DisplayName("Should skip rate limiting for API docs paths")
  void shouldSkipRateLimitingForApiDocsPaths() throws Exception {
    boolean result = invokeShouldSkipRateLimit("/api-docs/swagger-config");
    assertTrue(result, "Should skip rate limiting for API docs paths");

    result = invokeShouldSkipRateLimit("/swagger-ui/index.html");
    assertTrue(result, "Should skip rate limiting for Swagger UI paths");
  }

  @Test
  @DisplayName("Should skip rate limiting for actuator endpoints")
  void shouldSkipRateLimitingForActuatorEndpoints() throws Exception {
    boolean result = invokeShouldSkipRateLimit("/actuator/health");
    assertTrue(result, "Should skip rate limiting for actuator paths");

    result = invokeShouldSkipRateLimit("/actuator/prometheus");
    assertTrue(result, "Should skip rate limiting for actuator metrics");
  }

  @Test
  @DisplayName("Should skip rate limiting for payment webhooks")
  void shouldSkipRateLimitingForPaymentWebhooks() throws Exception {
    boolean result = invokeShouldSkipRateLimit("/api/payments/webhook");
    assertTrue(result, "Should skip rate limiting for payment webhook");
  }

  @Test
  @DisplayName("Should not skip rate limiting for regular API endpoints")
  void shouldNotSkipRateLimitingForRegularApiEndpoints() throws Exception {
    boolean result = invokeShouldSkipRateLimit("/api/users/profile");
    assertFalse(result, "Should not skip rate limiting for regular API endpoints");

    result = invokeShouldSkipRateLimit("/api/products");
    assertFalse(result, "Should not skip rate limiting for product endpoints");
  }

  @Test
  @DisplayName("Should create client identifier from IP address for unauthenticated requests")
  void shouldCreateClientIdentifierFromIpForUnauthenticatedRequests() throws Exception {
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");
    when(request.getHeader("Authorization")).thenReturn(null);

    String result = invokeGetClientIdentifier(request, "/api/products");

    assertTrue(
        result.startsWith("192.168.1.100"), "Client identifier should start with IP address");
    assertTrue(result.contains(":"), "Client identifier should contain separator");
    assertFalse(result.isEmpty(), "Client identifier should not be empty");
  }

  @Test
  @DisplayName("Should include JWT token hash for authenticated requests")
  void shouldIncludeJwtTokenHashForAuthenticatedRequests() throws Exception {
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");
    when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token-123");

    String result = invokeGetClientIdentifier(request, "/api/users/profile");

    assertTrue(
        result.startsWith("192.168.1.100"), "Client identifier should start with IP address");
    assertTrue(result.contains(":"), "Client identifier should contain separators");
    assertFalse(result.isEmpty(), "Client identifier should not be empty");
    // Should have more characters than unauthenticated version due to token hash
    assertTrue(
        result.length() > "192.168.1.100:".length(),
        "Authenticated identifier should be longer due to token hash");
  }

  @Test
  @DisplayName("Should return specific config for login endpoint rather than general pattern")
  void shouldReturnSpecificConfigForLoginEndpoint() throws Exception {
    Object config = invokeGetRateLimitConfig("/api/auth/login");

    assertNotNull(config, "Should return rate limit config");
    // Verify it's the login-specific config by checking the capacity
    int capacity = (Integer) config.getClass().getMethod("getCapacity").invoke(config);
    assertEquals(
        5, capacity, "Login endpoint should have 5 requests limit, not the general /** pattern");
  }

  @Test
  @DisplayName("Should return specific config for registration endpoint")
  void shouldReturnSpecificConfigForRegistrationEndpoint() throws Exception {
    Object config = invokeGetRateLimitConfig("/api/auth/register");

    assertNotNull(config, "Should return rate limit config");
    int capacity = (Integer) config.getClass().getMethod("getCapacity").invoke(config);
    assertEquals(3, capacity, "Registration endpoint should have 3 requests limit");
  }

  @Test
  @DisplayName("Should return auth wildcard config for other auth endpoints")
  void shouldReturnAuthWildcardConfigForOtherAuthEndpoints() throws Exception {
    Object config = invokeGetRateLimitConfig("/api/auth/refresh");

    assertNotNull(config, "Should return rate limit config");
    int capacity = (Integer) config.getClass().getMethod("getCapacity").invoke(config);
    assertEquals(
        10,
        capacity,
        "Other auth endpoints should use /api/auth/** pattern with 10 requests limit");
  }

  @Test
  @DisplayName("Should return products config for product endpoints")
  void shouldReturnProductsConfigForProductEndpoints() throws Exception {
    Object config = invokeGetRateLimitConfig("/api/products/search");

    assertNotNull(config, "Should return rate limit config");
    int capacity = (Integer) config.getClass().getMethod("getCapacity").invoke(config);
    assertEquals(200, capacity, "Product endpoints should have 200 requests limit");
  }

  @Test
  @DisplayName("Should return default config for unmatched paths")
  void shouldReturnDefaultConfigForUnmatchedPaths() throws Exception {
    Object config = invokeGetRateLimitConfig("/some/unknown/path");

    assertNotNull(config, "Should return default rate limit config");
    int capacity = (Integer) config.getClass().getMethod("getCapacity").invoke(config);
    assertEquals(
        100, capacity, "Unknown paths should use default /** pattern with 100 requests limit");
  }

  @Test
  @DisplayName("Should set correct HTTP status and content type when rate limit exceeded")
  void shouldSetCorrectHttpStatusAndContentTypeWhenRateLimitExceeded() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/test");
    when(consumptionProbe.getRemainingTokens()).thenReturn(0L);
    when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L); // 30 seconds

    invokeHandleRateLimitExceeded(request, response, "test-client", consumptionProbe);

    verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    verify(response).setContentType("application/json; charset=UTF-8");
    verify(response).setCharacterEncoding("UTF-8");
  }

  @Test
  @DisplayName("Should write correct JSON error response when rate limit exceeded")
  void shouldWriteCorrectJsonErrorResponseWhenRateLimitExceeded() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/test/endpoint");
    when(consumptionProbe.getRemainingTokens()).thenReturn(0L);
    when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(45_000_000_000L); // 45 seconds

    invokeHandleRateLimitExceeded(request, response, "client-123", consumptionProbe);

    printWriter.flush();
    String jsonResponse = responseWriter.toString();

    assertFalse(jsonResponse.isEmpty(), "Should write JSON response");
    assertTrue(jsonResponse.contains("\"status\":429"), "Should include HTTP status");
    assertTrue(
        jsonResponse.contains("\"error\":\"Too Many Requests\""), "Should include error message");
    assertTrue(
        jsonResponse.contains("\"path\":\"/api/test/endpoint\""), "Should include request path");
    assertTrue(
        jsonResponse.contains("\"retryAfterSeconds\":46"),
        "Should include retry after seconds (45+1)");
  }

  @Test
  @DisplayName("Should include timestamp in error response")
  void shouldIncludeTimestampInErrorResponse() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/test");
    when(consumptionProbe.getRemainingTokens()).thenReturn(0L);
    when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(10_000_000_000L);

    invokeHandleRateLimitExceeded(request, response, "client-456", consumptionProbe);

    printWriter.flush();
    String jsonResponse = responseWriter.toString();

    assertTrue(jsonResponse.contains("\"timestamp\":"), "Should include timestamp");
    // Verify timestamp format (ISO local date time)
    assertTrue(
        jsonResponse.matches(".*\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"),
        "Timestamp should be in ISO format");
  }

  @Test
  @DisplayName("Should handle zero wait time correctly")
  void shouldHandleZeroWaitTimeCorrectly() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/test");
    when(consumptionProbe.getRemainingTokens()).thenReturn(0L);
    when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(0L);

    invokeHandleRateLimitExceeded(request, response, "client-789", consumptionProbe);

    printWriter.flush();
    String jsonResponse = responseWriter.toString();

    assertTrue(
        jsonResponse.contains("\"retryAfterSeconds\":1"),
        "Should have minimum 1 second retry time");
  }

  @Test
  @DisplayName("Should skip processing for excluded paths in doFilterInternal")
  void shouldSkipProcessingForExcludedPathsInDoFilterInternal() throws Exception {
    when(request.getRequestURI()).thenReturn("/api-docs/swagger-config");

    rateLimitingFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    // No rate limit headers should be added for excluded paths
  }

  @Test
  @DisplayName("Should skip processing for test profile in doFilterInternal")
  void shouldSkipProcessingForTestProfileInDoFilterInternal() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/users/profile");
    ReflectionTestUtils.setField(rateLimitingFilter, "activeProfile", "test");

    rateLimitingFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    // Should not add rate limit headers in test mode
  }

  // Helper methods for testing private methods
  private boolean invokeShouldSkipRateLimit(String path) throws Exception {
    Method method = RateLimitingFilter.class.getDeclaredMethod("shouldSkipRateLimit", String.class);
    method.setAccessible(true);
    return (Boolean) method.invoke(rateLimitingFilter, path);
  }

  private String invokeGetClientIdentifier(HttpServletRequest request, String path)
      throws Exception {
    Method method =
        RateLimitingFilter.class.getDeclaredMethod(
            "getClientIdentifier", HttpServletRequest.class, String.class);
    method.setAccessible(true);
    return (String) method.invoke(rateLimitingFilter, request, path);
  }

  private void invokeHandleRateLimitExceeded(
      HttpServletRequest request,
      HttpServletResponse response,
      String clientId,
      ConsumptionProbe probe)
      throws Exception {
    Method method =
        RateLimitingFilter.class.getDeclaredMethod(
            "handleRateLimitExceeded",
            HttpServletRequest.class,
            HttpServletResponse.class,
            String.class,
            ConsumptionProbe.class);
    method.setAccessible(true);
    method.invoke(rateLimitingFilter, request, response, clientId, probe);
  }

  private Object invokeGetRateLimitConfig(String path) throws Exception {
    Method method = RateLimitingFilter.class.getDeclaredMethod("getRateLimitConfig", String.class);
    method.setAccessible(true);
    return method.invoke(rateLimitingFilter, path);
  }
}
