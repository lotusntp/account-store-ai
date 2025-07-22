package com.accountselling.platform.exception;

/**
 * Base exception for stock-related errors.
 */
public class StockException extends BaseException {

    public StockException(String message) {
        super(message);
    }

    public StockException(String message, Throwable cause) {
        super(message, cause);
    }

    public StockException(String message, String errorCode) {
        super(message, errorCode);
    }

    public StockException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}