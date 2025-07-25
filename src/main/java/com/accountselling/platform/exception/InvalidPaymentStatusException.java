package com.accountselling.platform.exception;

/** Exception thrown when an invalid payment status transition is attempted. */
public class InvalidPaymentStatusException extends BaseException {

  public InvalidPaymentStatusException(String message) {
    super(message);
  }

  public InvalidPaymentStatusException(String message, Throwable cause) {
    super(message, cause);
  }
}
