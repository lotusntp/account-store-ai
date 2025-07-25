package com.accountselling.platform.exception;

/** Exception thrown when webhook processing fails. */
public class WebhookProcessingException extends BaseException {

  public WebhookProcessingException(String message) {
    super(message);
  }

  public WebhookProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
