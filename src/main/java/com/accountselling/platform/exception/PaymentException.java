package com.accountselling.platform.exception;

/** Base exception for payment-related errors. */
public class PaymentException extends BaseException {

  public PaymentException(String message) {
    super(message);
  }

  public PaymentException(String message, Throwable cause) {
    super(message, cause);
  }

  public PaymentException(String message, String errorCode) {
    super(message, errorCode);
  }

  public PaymentException(String message, String errorCode, Throwable cause) {
    super(message, errorCode, cause);
  }
}
