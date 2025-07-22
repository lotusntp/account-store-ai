package com.accountselling.platform.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends ResourceException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId));
    }
}