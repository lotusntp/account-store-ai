package com.accountselling.platform.exception;

/**
 * Exception thrown when payment processing times out.
 */
public class PaymentTimeoutException extends PaymentException {

    public PaymentTimeoutException(String message) {
        super(message);
    }

    public PaymentTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}