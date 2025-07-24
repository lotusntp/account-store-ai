package com.accountselling.platform.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHierarchyTest {

    @Test
    void testBaseExceptionHierarchy() {
        // Test that all exceptions extend BaseException
        assertTrue(BaseException.class.isAssignableFrom(AuthenticationException.class));
        assertTrue(BaseException.class.isAssignableFrom(AuthorizationException.class));
        assertTrue(BaseException.class.isAssignableFrom(ResourceException.class));
        assertTrue(BaseException.class.isAssignableFrom(PaymentException.class));
        assertTrue(BaseException.class.isAssignableFrom(StockException.class));
    }

    @Test
    void testAuthenticationExceptionHierarchy() {
        assertTrue(AuthenticationException.class.isAssignableFrom(InvalidCredentialsException.class));
        assertTrue(AuthenticationException.class.isAssignableFrom(TokenExpiredException.class));
        assertTrue(AuthenticationException.class.isAssignableFrom(InvalidTokenException.class));
    }

    @Test
    void testAuthorizationExceptionHierarchy() {
        assertTrue(AuthorizationException.class.isAssignableFrom(InsufficientPermissionsException.class));
    }

    @Test
    void testResourceExceptionHierarchy() {
        assertTrue(ResourceException.class.isAssignableFrom(ResourceAlreadyExistsException.class));
        assertTrue(ResourceException.class.isAssignableFrom(ResourceInvalidException.class));
    }

    @Test
    void testPaymentExceptionHierarchy() {
        assertTrue(PaymentException.class.isAssignableFrom(PaymentFailedException.class));
        assertTrue(PaymentException.class.isAssignableFrom(PaymentTimeoutException.class));
    }

    @Test
    void testStockExceptionHierarchy() {
        assertTrue(StockException.class.isAssignableFrom(OutOfStockException.class));
        assertTrue(StockException.class.isAssignableFrom(StockReservationException.class));
    }

    @Test
    void testBaseExceptionErrorCode() {
        InvalidCredentialsException exception = new InvalidCredentialsException("Test message");
        assertEquals("InvalidCredentialsException", exception.getErrorCode());
    }

    @Test
    void testResourceNotFoundExceptionWithParameters() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "123");
        assertEquals("User with id '123' not found", exception.getMessage());
    }

    @Test
    void testResourceAlreadyExistsExceptionWithParameters() {
        ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException("Product", "456");
        assertEquals("Product with id '456' already exists", exception.getMessage());
    }

    @Test
    void testOutOfStockExceptionWithProductName() {
        OutOfStockException exception = OutOfStockException.forProduct("Gaming Account");
        assertEquals("Product 'Gaming Account' is out of stock", exception.getMessage());
    }

    @Test
    void testExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        InvalidCredentialsException exception = new InvalidCredentialsException("Authentication failed", cause);
        
        assertEquals("Authentication failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}