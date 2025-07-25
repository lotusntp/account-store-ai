package com.accountselling.platform.exception;

/** Exception thrown when a user lacks required permissions for an operation. */
public class InsufficientPermissionsException extends AuthorizationException {

  public InsufficientPermissionsException(String message) {
    super(message);
  }

  public InsufficientPermissionsException(String message, Throwable cause) {
    super(message, cause);
  }
}
