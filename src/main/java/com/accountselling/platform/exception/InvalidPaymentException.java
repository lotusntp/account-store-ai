package com.accountselling.platform.exception;

/**
 * Exception thrown when payment validation fails.
 */
public class InvalidPaymentException extends BaseException {
    
    public InvalidPaymentException(String message) {
        super(message);
    }
    
    public InvalidPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}