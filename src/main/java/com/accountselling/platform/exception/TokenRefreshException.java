package com.accountselling.platform.exception;

/**
 * Exception thrown when token refresh operation fails.
 */
public class TokenRefreshException extends BaseException {
    
    public TokenRefreshException(String message) {
        super(message);
    }
    
    public TokenRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
