package com.accountselling.platform.exception;

/** Exception thrown when payment processing fails. */
public class PaymentFailedException extends PaymentException {

  public PaymentFailedException(String message) {
    super(message);
  }

  public PaymentFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
