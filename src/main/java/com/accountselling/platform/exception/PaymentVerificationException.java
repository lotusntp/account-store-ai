package com.accountselling.platform.exception;

/** Exception thrown when payment verification fails. */
public class PaymentVerificationException extends BaseException {

  public PaymentVerificationException(String message) {
    super(message);
  }

  public PaymentVerificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
