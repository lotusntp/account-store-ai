package com.accountselling.platform.exception;

/** Base exception for resource-related errors. */
public class ResourceException extends BaseException {

  public ResourceException(String message) {
    super(message);
  }

  public ResourceException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceException(String message, String errorCode) {
    super(message, errorCode);
  }

  public ResourceException(String message, String errorCode, Throwable cause) {
    super(message, errorCode, cause);
  }
}
