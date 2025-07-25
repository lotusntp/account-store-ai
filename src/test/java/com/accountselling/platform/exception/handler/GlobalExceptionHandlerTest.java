package com.accountselling.platform.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.accountselling.platform.dto.error.ErrorResponse;
import com.accountselling.platform.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Comprehensive unit tests for GlobalExceptionHandler. Tests all exception handling methods and
 * error response formatting.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @InjectMocks private GlobalExceptionHandler globalExceptionHandler;

  @Mock private HttpServletRequest request;

  @Mock private BindingResult bindingResult;

  @Mock private MethodParameter methodParameter;

  @BeforeEach
  void setUp() {
    when(request.getRequestURI()).thenReturn("/api/test");
  }

  // ==================== VALIDATION EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle MethodArgumentNotValidException - Success")
  void handleValidationErrors_Success() {
    // Arrange
    FieldError fieldError1 = new FieldError("user", "username", "Username cannot be blank");
    FieldError fieldError2 = new FieldError("user", "email", "Email must be valid");

    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(methodParameter, bindingResult);

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleValidationErrors(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("Bad Request");
    assertThat(response.getBody().message()).contains("Username cannot be blank");
    assertThat(response.getBody().message()).contains("Email must be valid");
    assertThat(response.getBody().path()).isEqualTo("/api/test");
  }

  @Test
  @DisplayName("Handle ConstraintViolationException - Success")
  void handleConstraintViolationException_Success() {
    // Arrange
    Set<ConstraintViolation<?>> violations = new HashSet<>();

    ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
    when(violation1.getMessage()).thenReturn("Age must be greater than 0");
    violations.add(violation1);

    ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
    when(violation2.getMessage()).thenReturn("Name cannot be null");
    violations.add(violation2);

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleConstraintViolationException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message())
        .containsAnyOf("Age must be greater than 0", "Name cannot be null");
  }

  // ==================== AUTHENTICATION & AUTHORIZATION TESTS ====================

  @Test
  @DisplayName("Handle AuthenticationException - Success")
  void handleAuthenticationException_Success() {
    // Arrange
    AuthenticationException exception = new BadCredentialsException("Invalid credentials");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleAuthenticationException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(401);
    assertThat(response.getBody().error()).isEqualTo("Unauthorized");
    assertThat(response.getBody().message()).isEqualTo("Authentication failed");
  }

  @Test
  @DisplayName("Handle AccessDeniedException - Success")
  void handleAccessDeniedException_Success() {
    // Arrange
    AccessDeniedException exception = new AccessDeniedException("Access denied");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleAccessDeniedException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(403);
    assertThat(response.getBody().error()).isEqualTo("Forbidden");
    assertThat(response.getBody().message()).isEqualTo("Access denied");
  }

  @Test
  @DisplayName("Handle AuthorizationException - Success")
  void handleAuthorizationException_Success() {
    // Arrange
    AuthorizationException exception = new AuthorizationException("Authorization failed");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleAuthorizationException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(403);
    assertThat(response.getBody().message()).isEqualTo("Authorization failed");
  }

  // ==================== RESOURCE EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle ResourceNotFoundException - Success")
  void handleResourceNotFoundException_Success() {
    // Arrange
    ResourceNotFoundException exception = new ResourceNotFoundException("User", "123");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleResourceNotFoundException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(404);
    assertThat(response.getBody().error()).isEqualTo("Not Found");
    assertThat(response.getBody().message()).contains("User");
  }

  @Test
  @DisplayName("Handle ResourceAlreadyExistsException - Success")
  void handleResourceAlreadyExistsException_Success() {
    // Arrange
    ResourceAlreadyExistsException exception =
        new ResourceAlreadyExistsException("User", "john@example.com");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleResourceAlreadyExistsException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(409);
    assertThat(response.getBody().error()).isEqualTo("Conflict");
  }

  // ==================== STOCK EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle StockException - Success")
  void handleStockException_Success() {
    // Arrange
    StockException exception = new StockException("Stock operation failed");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleStockException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).isEqualTo("Stock operation failed");
  }

  @Test
  @DisplayName("Handle InsufficientStockException - Success")
  void handleInsufficientStockException_Success() {
    // Arrange
    InsufficientStockException exception =
        new InsufficientStockException("Not enough stock available");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleInsufficientStockException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(409);
    assertThat(response.getBody().message()).isEqualTo("Not enough stock available");
  }

  @Test
  @DisplayName("Handle OutOfStockException - Success")
  void handleOutOfStockException_Success() {
    // Arrange
    OutOfStockException exception = new OutOfStockException("Product is out of stock");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleOutOfStockException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(409);
    assertThat(response.getBody().message()).isEqualTo("Product is out of stock");
  }

  // ==================== PAYMENT EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle PaymentException - Success")
  void handlePaymentException_Success() {
    // Arrange
    PaymentException exception = new PaymentException("Payment processing failed");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handlePaymentException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).isEqualTo("Payment processing failed");
  }

  @Test
  @DisplayName("Handle PaymentGatewayException - Success")
  void handlePaymentGatewayException_Success() {
    // Arrange
    PaymentGatewayException exception = new PaymentGatewayException("Payment gateway unavailable");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handlePaymentGatewayException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(500);
    assertThat(response.getBody().message()).isEqualTo("Payment gateway error");
  }

  @Test
  @DisplayName("Handle PaymentTimeoutException - Success")
  void handlePaymentTimeoutException_Success() {
    // Arrange
    PaymentTimeoutException exception = new PaymentTimeoutException("Payment timed out");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handlePaymentTimeoutException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(408);
    assertThat(response.getBody().message()).isEqualTo("Payment timed out");
  }

  // ==================== ORDER EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle InvalidOrderException - Success")
  void handleInvalidOrderException_Success() {
    // Arrange
    InvalidOrderException exception = new InvalidOrderException("Invalid order status");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleInvalidOrderException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).isEqualTo("Invalid order status");
  }

  @Test
  @DisplayName("Handle OrderAccessDeniedException - Success")
  void handleOrderAccessDeniedException_Success() {
    // Arrange
    OrderAccessDeniedException exception =
        new OrderAccessDeniedException("Cannot access this order");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleOrderAccessDeniedException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(403);
    assertThat(response.getBody().message()).isEqualTo("Cannot access this order");
  }

  // ==================== TOKEN EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle InvalidTokenException - Success")
  void handleInvalidTokenException_Success() {
    // Arrange
    InvalidTokenException exception = new InvalidTokenException("Token is invalid");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleInvalidTokenException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(401);
    assertThat(response.getBody().message()).isEqualTo("Token is invalid");
  }

  @Test
  @DisplayName("Handle TokenExpiredException - Success")
  void handleTokenExpiredException_Success() {
    // Arrange
    TokenExpiredException exception = new TokenExpiredException("Token has expired");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleTokenExpiredException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(401);
    assertThat(response.getBody().message()).isEqualTo("Token has expired");
  }

  // ==================== HTTP EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle HttpRequestMethodNotSupportedException - Success")
  void handleMethodNotSupportedException_Success() {
    // Arrange
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("POST", List.of("GET", "PUT"));

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleMethodNotSupportedException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(405);
    assertThat(response.getBody().error()).isEqualTo("Method Not Allowed");
    assertThat(response.getBody().message()).contains("POST");
    assertThat(response.getBody().message()).contains("GET, PUT");
  }

  @Test
  @DisplayName("Handle MissingServletRequestParameterException - Success")
  void handleMissingParameterException_Success() {
    // Arrange
    MissingServletRequestParameterException exception =
        new MissingServletRequestParameterException("userId", "String");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleMissingParameterException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).contains("userId");
  }

  @Test
  @DisplayName("Handle MethodArgumentTypeMismatchException - Success")
  void handleTypeMismatchException_Success() {
    // Arrange
    MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
    when(exception.getValue()).thenReturn("invalid-uuid");
    when(exception.getName()).thenReturn("id");
    when(exception.getMessage()).thenReturn("Type conversion failed");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleTypeMismatchException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).contains("invalid-uuid");
    assertThat(response.getBody().message()).contains("id");
  }

  @Test
  @DisplayName("Handle HttpMessageNotReadableException - Success")
  void handleMalformedJsonException_Success() {
    // Arrange
    HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
    when(exception.getMessage()).thenReturn("Malformed JSON");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleMalformedJsonException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).isEqualTo("Malformed JSON request");
  }

  @Test
  @DisplayName("Handle NoHandlerFoundException - Success")
  void handleNoHandlerFoundException_Success() {
    // Arrange
    NoHandlerFoundException exception =
        new NoHandlerFoundException("GET", "/api/nonexistent", null);

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleNoHandlerFoundException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(404);
    assertThat(response.getBody().message()).isEqualTo("Endpoint not found");
  }

  // ==================== GENERIC EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle IllegalArgumentException - Success")
  void handleIllegalArgumentException_Success() {
    // Arrange
    IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleIllegalArgumentException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).isEqualTo("Invalid argument provided");
  }

  @Test
  @DisplayName("Handle IllegalStateException - Success")
  void handleIllegalStateException_Success() {
    // Arrange
    IllegalStateException exception = new IllegalStateException("Invalid state");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleIllegalStateException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).isEqualTo("Invalid state");
  }

  @Test
  @DisplayName("Handle Generic Exception - Success")
  void handleGenericException_Success() {
    // Arrange
    Exception exception = new RuntimeException("Unexpected error occurred");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleGenericException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(500);
    assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
    assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
  }

  // ==================== USER EXCEPTION TESTS ====================

  @Test
  @DisplayName("Handle UsernameAlreadyExistsException - Success")
  void handleUsernameAlreadyExistsException_Success() {
    // Arrange
    UsernameAlreadyExistsException exception = new UsernameAlreadyExistsException("johndoe");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleUsernameAlreadyExistsException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(409);
    assertThat(response.getBody().message()).isEqualTo("Username 'johndoe' already exists");
  }

  @Test
  @DisplayName("Handle UserNotFoundException - Success")
  void handleUserNotFoundException_Success() {
    // Arrange
    UserNotFoundException exception = new UserNotFoundException("User not found");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleUserNotFoundException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(404);
    assertThat(response.getBody().message()).isEqualTo("User not found");
  }

  @Test
  @DisplayName("Handle InsufficientPermissionsException - Success")
  void handleInsufficientPermissionsException_Success() {
    // Arrange
    InsufficientPermissionsException exception = 
        new InsufficientPermissionsException("Insufficient permissions to access this resource");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleInsufficientPermissionsException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(403);
    assertThat(response.getBody().error()).isEqualTo("Forbidden");
    assertThat(response.getBody().message()).isEqualTo("Insufficient permissions to access this resource");
  }

  @Test
  @DisplayName("Handle ResourceInvalidException - Success")
  void handleResourceInvalidException_Success() {
    // Arrange
    ResourceInvalidException exception = new ResourceInvalidException("Resource validation failed");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleResourceInvalidException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("Bad Request");
    assertThat(response.getBody().message()).isEqualTo("Resource validation failed");
  }

  @Test
  @DisplayName("Handle ResourceException - Success")
  void handleResourceException_Success() {
    // Arrange
    ResourceException exception = new ResourceException("Generic resource error");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleResourceException(exception, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("Bad Request");
    assertThat(response.getBody().message()).isEqualTo("Generic resource error");
  }

  // ==================== ERROR RESPONSE STRUCTURE TESTS ====================

  @Test
  @DisplayName("Error response contains all required fields")
  void errorResponse_ContainsAllRequiredFields() {
    // Arrange
    ResourceNotFoundException exception = new ResourceNotFoundException("Product", "123");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleResourceNotFoundException(exception, request);

    // Assert
    ErrorResponse errorResponse = response.getBody();
    assertThat(errorResponse).isNotNull();
    assertThat(errorResponse.status()).isNotNull();
    assertThat(errorResponse.error()).isNotNull();
    assertThat(errorResponse.message()).isNotNull();
    assertThat(errorResponse.path()).isNotNull();
    assertThat(errorResponse.timestamp()).isNotNull();
  }

  @Test
  @DisplayName("Error response path matches request URI")
  void errorResponse_PathMatchesRequestURI() {
    // Arrange
    when(request.getRequestURI()).thenReturn("/api/products/123");
    ResourceNotFoundException exception = new ResourceNotFoundException("Product", "123");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleResourceNotFoundException(exception, request);

    // Assert
    assertThat(response.getBody().path()).isEqualTo("/api/products/123");
  }

  @Test
  @DisplayName("Sensitive information is not exposed in error responses")
  void errorResponse_DoesNotExposeSensitiveInformation() {
    // Arrange
    Exception exception = new RuntimeException("Database connection failed: password=secret123");

    // Act
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleGenericException(exception, request);

    // Assert
    // The generic handler should not expose the original exception message
    assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    assertThat(response.getBody().message()).doesNotContain("password");
    assertThat(response.getBody().message()).doesNotContain("secret123");
  }
}
