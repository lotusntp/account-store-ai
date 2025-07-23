package com.accountselling.platform.exception;

/**
 * Exception thrown when payment gateway operations fail.
 */
public class PaymentGatewayException extends BaseException {
    
    public PaymentGatewayException(String message) {
        super(message);
    }
    
    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}