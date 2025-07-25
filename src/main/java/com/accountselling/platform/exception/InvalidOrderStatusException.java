package com.accountselling.platform.exception;

/** Exception thrown when an invalid order status transition is attempted. */
public class InvalidOrderStatusException extends BaseException {

  public InvalidOrderStatusException(String message) {
    super(message);
  }

  public InvalidOrderStatusException(String message, Throwable cause) {
    super(message, cause);
  }
}
