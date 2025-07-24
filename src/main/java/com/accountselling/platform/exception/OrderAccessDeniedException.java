package com.accountselling.platform.exception;

/**
 * Exception thrown when order does not belong to the requesting user.
 */
public class OrderAccessDeniedException extends BaseException {
    
    public OrderAccessDeniedException(String orderId, String username) {
        super(String.format("Access denied - order %s does not belong to user %s", orderId, username));
    }
    
    public OrderAccessDeniedException(String message) {
        super(message);
    }
}
