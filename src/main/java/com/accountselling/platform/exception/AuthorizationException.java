package com.accountselling.platform.exception;

/**
 * Base exception for authorization-related errors.
 */
public class AuthorizationException extends BaseException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthorizationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public AuthorizationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}