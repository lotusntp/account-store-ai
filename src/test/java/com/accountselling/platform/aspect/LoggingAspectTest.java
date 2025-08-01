package com.accountselling.platform.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.accountselling.platform.service.LoggingService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for LoggingAspect. Tests AOP-based logging for service and repository methods. */
@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

  @Mock private LoggingService loggingService;

  @Mock private ProceedingJoinPoint joinPoint;

  @Mock private Signature signature;

  private LoggingAspect loggingAspect;

  @BeforeEach
  void setUp() {
    loggingAspect = new LoggingAspect(loggingService);
  }

  @Test
  void testLogServiceExecution_Success() throws Throwable {
    // Given
    Object mockTarget = new MockService();
    Object expectedResult = "success";

    when(joinPoint.getTarget()).thenReturn(mockTarget);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("testMethod");
    when(joinPoint.getArgs()).thenReturn(new Object[] {"param1", "param2"});
    when(joinPoint.proceed()).thenReturn(expectedResult);

    // When
    Object result = loggingAspect.logServiceExecution(joinPoint);

    // Then
    assertThat(result).isEqualTo(expectedResult);

    verify(loggingService)
        .logSystemEvent(
            eq("service_method_execution"),
            eq("MockService.testMethod executed successfully"),
            any());
    verify(joinPoint).proceed();
  }

  @Test
  void testLogServiceExecution_Exception() throws Throwable {
    // Given
    Object mockTarget = new MockService();
    RuntimeException exception = new RuntimeException("Test exception");

    when(joinPoint.getTarget()).thenReturn(mockTarget);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("testMethod");
    when(joinPoint.getArgs()).thenReturn(new Object[] {"param1"});
    when(joinPoint.proceed()).thenThrow(exception);

    // When & Then
    assertThatThrownBy(() -> loggingAspect.logServiceExecution(joinPoint)).isEqualTo(exception);

    verify(loggingService)
        .logError(
            eq("service_method_error"),
            eq("MockService.testMethod execution failed"),
            eq(exception),
            any());
    verify(joinPoint).proceed();
  }

  @Test
  void testLogRepositoryExecution_Success() throws Throwable {
    // Given
    Object mockTarget = new MockRepository();
    Object expectedResult = "repository result";

    when(joinPoint.getTarget()).thenReturn(mockTarget);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("findById");
    when(joinPoint.proceed()).thenReturn(expectedResult);

    // When
    Object result = loggingAspect.logRepositoryExecution(joinPoint);

    // Then
    assertThat(result).isEqualTo(expectedResult);

    verify(loggingService).logDatabaseOperation(eq("findById"), eq("mock"), anyLong(), any());
    verify(joinPoint).proceed();
  }

  @Test
  void testLogRepositoryExecution_Exception() throws Throwable {
    // Given
    Object mockTarget = new MockRepository();
    RuntimeException exception = new RuntimeException("Database error");

    when(joinPoint.getTarget()).thenReturn(mockTarget);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("save");
    when(joinPoint.proceed()).thenThrow(exception);

    // When & Then
    assertThatThrownBy(() -> loggingAspect.logRepositoryExecution(joinPoint)).isEqualTo(exception);

    verify(loggingService)
        .logError(
            eq("repository_method_error"),
            eq("MockRepository.save execution failed"),
            eq(exception),
            any());
    verify(joinPoint).proceed();
  }

  @Test
  void testExtractTableName_StandardRepository() throws Throwable {
    // Given
    Object mockTarget = new UserRepository();

    when(joinPoint.getTarget()).thenReturn(mockTarget);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("findAll");
    when(joinPoint.proceed()).thenReturn("result");

    // When
    loggingAspect.logRepositoryExecution(joinPoint);

    // Then
    verify(loggingService).logDatabaseOperation(eq("findAll"), eq("user"), anyLong(), any());
  }

  @Test
  void testExtractTableName_CamelCaseRepository() throws Throwable {
    // Given
    Object mockTarget = new OrderItemRepository();

    when(joinPoint.getTarget()).thenReturn(mockTarget);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("findByOrderId");
    when(joinPoint.proceed()).thenReturn("result");

    // When
    loggingAspect.logRepositoryExecution(joinPoint);

    // Then
    verify(loggingService)
        .logDatabaseOperation(eq("findByOrderId"), eq("order_item"), anyLong(), any());
  }

  @Test
  void testExtractTableName_NonRepositoryClass() throws Throwable {
    // Given
    Object mockTarget = new MockService();

    when(joinPoint.getTarget()).thenReturn(mockTarget);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("someMethod");
    when(joinPoint.proceed()).thenReturn("result");

    // When
    loggingAspect.logRepositoryExecution(joinPoint);

    // Then
    verify(loggingService).logDatabaseOperation(eq("someMethod"), eq("unknown"), anyLong(), any());
  }

  @Test
  void testLogServiceExecution_WithNoParameters() throws Throwable {
    // Given
    Object mockTarget = new MockService();

    when(joinPoint.getTarget()).thenReturn(mockTarget);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("noParamMethod");
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(joinPoint.proceed()).thenReturn("result");

    // When
    Object result = loggingAspect.logServiceExecution(joinPoint);

    // Then
    assertThat(result).isEqualTo("result");

    verify(loggingService)
        .logSystemEvent(
            eq("service_method_execution"),
            eq("MockService.noParamMethod executed successfully"),
            any());
  }

  // Mock classes for testing
  private static class MockService {
    public String testMethod(String param1, String param2) {
      return "success";
    }

    public String noParamMethod() {
      return "result";
    }
  }

  private static class MockRepository {
    public String findById(Long id) {
      return "entity";
    }

    public String save(Object entity) {
      return "saved";
    }
  }

  private static class UserRepository {
    public String findAll() {
      return "users";
    }
  }

  private static class OrderItemRepository {
    public String findByOrderId(Long orderId) {
      return "orderItems";
    }
  }
}
