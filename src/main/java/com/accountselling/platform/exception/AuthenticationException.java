package com.accountselling.platform.exception;

/** Base exception for authentication-related errors. */
public class AuthenticationException extends BaseException {

  public AuthenticationException(String message) {
    super(message);
  }

  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }

  public AuthenticationException(String message, String errorCode) {
    super(message, errorCode);
  }

  public AuthenticationException(String message, String errorCode, Throwable cause) {
    super(message, errorCode, cause);
  }
}
