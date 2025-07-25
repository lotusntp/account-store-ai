package com.accountselling.platform.exception;

/** Exception thrown when resource validation fails. */
public class ResourceInvalidException extends ResourceException {

  public ResourceInvalidException(String message) {
    super(message);
  }

  public ResourceInvalidException(String message, Throwable cause) {
    super(message, cause);
  }
}
