package com.accountselling.platform.exception;

/** Exception thrown when user provides invalid credentials during authentication. */
public class InvalidCredentialsException extends AuthenticationException {

  public InvalidCredentialsException(String message) {
    super(message);
  }

  public InvalidCredentialsException(String message, Throwable cause) {
    super(message, cause);
  }
}
