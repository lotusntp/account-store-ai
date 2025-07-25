package com.accountselling.platform.exception;

/** Exception thrown when attempting to create a payment that already exists. */
public class PaymentAlreadyExistsException extends BaseException {

  public PaymentAlreadyExistsException(String message) {
    super(message);
  }

  public PaymentAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
