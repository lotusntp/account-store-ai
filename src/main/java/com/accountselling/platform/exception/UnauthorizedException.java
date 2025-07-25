package com.accountselling.platform.exception;

/** Exception thrown when user is not authorized to perform an action. */
public class UnauthorizedException extends BaseException {

  public UnauthorizedException(String message) {
    super(message);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }
}
