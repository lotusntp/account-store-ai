package com.accountselling.platform.exception;

/**
 * Exception thrown when there is insufficient stock for an order.
 */
public class InsufficientStockException extends BaseException {
    
    public InsufficientStockException(String message) {
        super(message);
    }
    
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}