package com.accountselling.platform.exception;

/**
 * Exception thrown when QR code generation fails.
 */
public class QrCodeGenerationException extends BaseException {
    
    public QrCodeGenerationException(String message) {
        super(message);
    }
    
    public QrCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}