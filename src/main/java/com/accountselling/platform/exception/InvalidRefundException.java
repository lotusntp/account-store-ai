package com.accountselling.platform.exception;

/**
 * Exception thrown when refund validation fails.
 */
public class InvalidRefundException extends BaseException {
    
    public InvalidRefundException(String message) {
        super(message);
    }
    
    public InvalidRefundException(String message, Throwable cause) {
        super(message, cause);
    }
}