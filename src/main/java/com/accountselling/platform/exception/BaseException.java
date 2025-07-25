package com.accountselling.platform.exception;

/**
 * Abstract base exception for all application-specific exceptions. Provides common functionality
 * and structure for the exception hierarchy.
 */
public abstract class BaseException extends RuntimeException {

  private final String errorCode;

  protected BaseException(String message) {
    super(message);
    this.errorCode = getClass().getSimpleName();
  }

  protected BaseException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = getClass().getSimpleName();
  }

  protected BaseException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  protected BaseException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
