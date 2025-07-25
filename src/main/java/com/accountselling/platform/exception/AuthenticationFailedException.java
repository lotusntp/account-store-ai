package com.accountselling.platform.exception;

/** Exception thrown when user authentication fails. */
public class AuthenticationFailedException extends BaseException {

  public AuthenticationFailedException(String message) {
    super(message);
  }

  public AuthenticationFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
