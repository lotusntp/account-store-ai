package com.accountselling.platform.exception;

/** Exception thrown when attempting to register with a username that already exists. */
public class UsernameAlreadyExistsException extends BaseException {

  public UsernameAlreadyExistsException(String username) {
    super(String.format("Username '%s' already exists", username));
  }

  public UsernameAlreadyExistsException(String username, Throwable cause) {
    super(String.format("Username '%s' already exists", username), cause);
  }
}
