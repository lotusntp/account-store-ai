package com.accountselling.platform.exception;

/** Exception thrown when attempting to create a resource that already exists. */
public class ResourceAlreadyExistsException extends ResourceException {

  public ResourceAlreadyExistsException(String message) {
    super(message);
  }

  public ResourceAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceAlreadyExistsException(String resourceType, Object resourceId) {
    super(String.format("%s with id '%s' already exists", resourceType, resourceId));
  }
}
