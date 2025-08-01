package com.accountselling.platform.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.accountselling.platform.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for LoggingInterceptor. Tests HTTP request/response logging functionality. */
@ExtendWith(MockitoExtension.class)
class LoggingInterceptorTest {

  @Mock private LoggingService loggingService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private HttpSession session;

  @Mock private Authentication authentication;

  @Mock private SecurityContext securityContext;

  private LoggingInterceptor loggingInterceptor;

  @BeforeEach
  void setUp() {
    loggingInterceptor = new LoggingInterceptor(loggingService);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void testPreHandle_WithAuthenticatedUser() throws Exception {
    // Given
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/products");
    when(request.getQueryString()).thenReturn("category=games");
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(request.getRemoteAddr()).thenReturn("192.168.1.1");
    when(request.getSession()).thenReturn(session);
    when(session.getId()).thenReturn("session123");

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("user123");

    when(loggingService.generateCorrelationId()).thenReturn("correlation-123");

    // When
    boolean result = loggingInterceptor.preHandle(request, response, new Object());

    // Then
    assertThat(result).isTrue();

    verify(loggingService).generateCorrelationId();
    verify(loggingService).setCorrelationId("correlation-123");
    verify(loggingService).setUserContext("user123", "session123");
    verify(response).setHeader("X-Correlation-ID", "correlation-123");
    verify(loggingService)
        .logSystemEvent(eq("http_request_start"), eq("HTTP request started"), any());
    verify(request).setAttribute(eq("startTime"), any(Long.class));
  }

  @Test
  void testPreHandle_WithAnonymousUser() throws Exception {
    // Given
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/auth/login");
    when(request.getQueryString()).thenReturn(null);
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(request.getRemoteAddr()).thenReturn("192.168.1.1");

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("anonymousUser");

    when(loggingService.generateCorrelationId()).thenReturn("correlation-456");

    // When
    boolean result = loggingInterceptor.preHandle(request, response, new Object());

    // Then
    assertThat(result).isTrue();

    verify(loggingService).generateCorrelationId();
    verify(loggingService).setCorrelationId("correlation-456");
    verify(loggingService, never()).setUserContext(anyString(), anyString());
    verify(response).setHeader("X-Correlation-ID", "correlation-456");
    verify(loggingService)
        .logSystemEvent(eq("http_request_start"), eq("HTTP request started"), any());
  }

  @Test
  void testPreHandle_WithNoAuthentication() throws Exception {
    // Given
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/public");
    when(request.getQueryString()).thenReturn(null);
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(request.getRemoteAddr()).thenReturn("192.168.1.1");

    when(securityContext.getAuthentication()).thenReturn(null);
    when(loggingService.generateCorrelationId()).thenReturn("correlation-789");

    // When
    boolean result = loggingInterceptor.preHandle(request, response, new Object());

    // Then
    assertThat(result).isTrue();

    verify(loggingService).generateCorrelationId();
    verify(loggingService).setCorrelationId("correlation-789");
    verify(loggingService, never()).setUserContext(anyString(), anyString());
    verify(response).setHeader("X-Correlation-ID", "correlation-789");
    verify(loggingService)
        .logSystemEvent(eq("http_request_start"), eq("HTTP request started"), any());
  }

  @Test
  void testAfterCompletion_SuccessfulRequest() throws Exception {
    // Given
    long startTime = System.currentTimeMillis() - 100;
    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/products");
    when(response.getStatus()).thenReturn(200);
    when(response.getContentType()).thenReturn("application/json");
    when(response.getHeader("Content-Length")).thenReturn("1024");
    when(response.getHeader("X-Correlation-ID")).thenReturn("correlation-123");

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("user123");

    // When
    loggingInterceptor.afterCompletion(request, response, new Object(), null);

    // Then
    verify(loggingService)
        .logApiRequest(eq("GET"), eq("/api/products"), eq("user123"), anyLong(), eq(200), any());
    verify(loggingService).clearContext();
  }

  @Test
  void testAfterCompletion_WithException() throws Exception {
    // Given
    long startTime = System.currentTimeMillis() - 200;
    Exception exception = new RuntimeException("Test exception");

    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/orders");
    when(response.getStatus()).thenReturn(500);
    when(response.getContentType()).thenReturn("application/json");
    when(response.getHeader("Content-Length")).thenReturn("512");
    when(response.getHeader("X-Correlation-ID")).thenReturn("correlation-456");

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("user456");

    // When
    loggingInterceptor.afterCompletion(request, response, new Object(), exception);

    // Then
    verify(loggingService)
        .logApiRequest(eq("POST"), eq("/api/orders"), eq("user456"), anyLong(), eq(500), any());
    verify(loggingService)
        .logError(eq("http_request_error"), eq("HTTP request failed"), eq(exception), any());
    verify(loggingService).clearContext();
  }

  @Test
  void testAfterCompletion_AnonymousUser() throws Exception {
    // Given
    long startTime = System.currentTimeMillis() - 50;
    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/public");
    when(response.getStatus()).thenReturn(200);
    when(response.getContentType()).thenReturn("application/json");
    when(response.getHeader("Content-Length")).thenReturn("256");
    when(response.getHeader("X-Correlation-ID")).thenReturn("correlation-789");

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("anonymousUser");

    // When
    loggingInterceptor.afterCompletion(request, response, new Object(), null);

    // Then
    verify(loggingService)
        .logApiRequest(eq("GET"), eq("/api/public"), eq("anonymous"), anyLong(), eq(200), any());
    verify(loggingService).clearContext();
  }

  @Test
  void testAfterCompletion_NoStartTime() throws Exception {
    // Given
    when(request.getAttribute("startTime")).thenReturn(null);

    // When
    loggingInterceptor.afterCompletion(request, response, new Object(), null);

    // Then
    verify(loggingService, never())
        .logApiRequest(anyString(), anyString(), anyString(), anyLong(), anyInt(), any());
    verify(loggingService).clearContext();
  }

  @Test
  void testGetClientIpAddress_WithXForwardedFor() throws Exception {
    // Given
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getQueryString()).thenReturn(null);
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(loggingService.generateCorrelationId()).thenReturn("correlation-test");

    // When
    loggingInterceptor.preHandle(request, response, new Object());

    // Then
    verify(loggingService)
        .logSystemEvent(eq("http_request_start"), eq("HTTP request started"), any());
  }

  @Test
  void testGetClientIpAddress_WithXRealIp() throws Exception {
    // Given
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.2");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getQueryString()).thenReturn(null);
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(loggingService.generateCorrelationId()).thenReturn("correlation-test");

    // When
    loggingInterceptor.preHandle(request, response, new Object());

    // Then
    verify(loggingService)
        .logSystemEvent(eq("http_request_start"), eq("HTTP request started"), any());
  }

  @Test
  void testGetClientIpAddress_FallbackToRemoteAddr() throws Exception {
    // Given
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getQueryString()).thenReturn(null);
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(loggingService.generateCorrelationId()).thenReturn("correlation-test");

    // When
    loggingInterceptor.preHandle(request, response, new Object());

    // Then
    verify(loggingService)
        .logSystemEvent(eq("http_request_start"), eq("HTTP request started"), any());
  }
}
