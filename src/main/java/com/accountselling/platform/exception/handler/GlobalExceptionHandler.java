package com.accountselling.platform.exception.handler;

import com.accountselling.platform.dto.error.ErrorResponse;
import com.accountselling.platform.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * Global exception handler for standardized error responses.
 * Handles all exceptions across the application and returns consistent ErrorResponse format.
 * 
 * ตัวจัดการ exception แบบรวมศูนย์เพื่อให้ error response เป็นมาตรฐานเดียวกัน
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotation.
     * 
     * @param ex the MethodArgumentNotValidException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
            
        log.warn("Validation error at {}: {}", request.getRequestURI(), message);
        
        ErrorResponse error = ErrorResponse.badRequest(message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle authentication exceptions.
     * 
     * @param ex the AuthenticationException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.unauthorized(
            "Authentication failed", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle access denied exceptions.
     * 
     * @param ex the AccessDeniedException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.forbidden(
            "Access denied", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle resource not found exceptions.
     * 
     * @param ex the ResourceNotFoundException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.notFound(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle resource already exists exceptions.
     * 
     * @param ex the ResourceAlreadyExistsException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(
            ResourceAlreadyExistsException ex, HttpServletRequest request) {
        
        log.warn("Resource conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.conflict(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle insufficient stock exceptions.
     * 
     * @param ex the InsufficientStockException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStockException(
            InsufficientStockException ex, HttpServletRequest request) {
        
        log.warn("Insufficient stock at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.badRequest(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle invalid order exceptions.
     * 
     * @param ex the InvalidOrderException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderException(
            InvalidOrderException ex, HttpServletRequest request) {
        
        log.warn("Invalid order at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.badRequest(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle invalid credentials exceptions.
     * 
     * @param ex the InvalidCredentialsException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException ex, HttpServletRequest request) {
        
        log.warn("Invalid credentials at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.unauthorized(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle HTTP method not supported.
     * 
     * @param ex the HttpRequestMethodNotSupportedException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        log.warn("Method not supported at {}: {}", request.getRequestURI(), ex.getMessage());
        
        String message = String.format("Method '%s' not supported. Supported methods: %s", 
            ex.getMethod(), String.join(", ", ex.getSupportedMethods()));
            
        ErrorResponse error = ErrorResponse.of(405, "Method Not Allowed", message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * Handle missing request parameters.
     * 
     * @param ex the MissingServletRequestParameterException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        log.warn("Missing parameter at {}: {}", request.getRequestURI(), ex.getMessage());
        
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        ErrorResponse error = ErrorResponse.badRequest(message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle method argument type mismatch.
     * 
     * @param ex the MethodArgumentTypeMismatchException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        log.warn("Type mismatch at {}: {}", request.getRequestURI(), ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'", 
            ex.getValue(), ex.getName());
        ErrorResponse error = ErrorResponse.badRequest(message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle malformed JSON requests.
     * 
     * @param ex the HttpMessageNotReadableException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJsonException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        log.warn("Malformed JSON at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.badRequest(
            "Malformed JSON request", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle no handler found (404 errors).
     * 
     * @param ex the NoHandlerFoundException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        log.warn("No handler found for {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.notFound(
            "Endpoint not found", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle illegal argument exceptions.
     * 
     * @param ex the IllegalArgumentException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.badRequest(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle illegal state exceptions.
     * 
     * @param ex the IllegalStateException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        
        log.warn("Illegal state at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.badRequest(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle all other unexpected exceptions.
     * 
     * @param ex the Exception
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.internalServerError(
            "An unexpected error occurred", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle username already exists exceptions.
     * 
     * @param ex the UsernameAlreadyExistsException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExistsException(
            UsernameAlreadyExistsException ex, HttpServletRequest request) {
        
        log.warn("Username already exists at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.conflict(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle user not found exceptions.
     * 
     * @param ex the UserNotFoundException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {
        
        log.warn("User not found at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.notFound(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle authentication failed exceptions.
     * 
     * @param ex the AuthenticationFailedException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailedException(
            AuthenticationFailedException ex, HttpServletRequest request) {
        
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.unauthorized(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle order access denied exceptions.
     * 
     * @param ex the OrderAccessDeniedException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(OrderAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleOrderAccessDeniedException(
            OrderAccessDeniedException ex, HttpServletRequest request) {
        
        log.warn("Order access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.forbidden(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle order not completed exceptions.
     * 
     * @param ex the OrderNotCompletedException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(OrderNotCompletedException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotCompletedException(
            OrderNotCompletedException ex, HttpServletRequest request) {
        
        log.warn("Order not completed at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.badRequest(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle token refresh exceptions.
     * 
     * @param ex the TokenRefreshException
     * @param request the HTTP request
     * @return standardized error response
     */
    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(
            TokenRefreshException ex, HttpServletRequest request) {
        
        log.warn("Token refresh failed at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.unauthorized(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}