package com.accountselling.platform.exception;

/**
 * Exception thrown when user is not found or not authenticated.
 */
public class UserNotFoundException extends BaseException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
