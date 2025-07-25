package com.accountselling.platform.exception;

/** Exception thrown when order validation fails. */
public class InvalidOrderException extends BaseException {

  public InvalidOrderException(String message) {
    super(message);
  }

  public InvalidOrderException(String message, Throwable cause) {
    super(message, cause);
  }
}
