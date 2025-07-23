package com.accountselling.platform.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * DTO for payment creation requests.
 * Contains order ID, payment method, and expiration settings for creating new payments.
 */
public record PaymentCreateRequestDto(
    
    @NotNull(message = "Order ID cannot be null")
    UUID orderId,
    
    @NotBlank(message = "Payment method cannot be blank")
    String paymentMethod,
    
    @Positive(message = "Expiration minutes must be positive")
    Integer expirationMinutes,
    
    String notes
    
) {
    
    /**
     * Get effective expiration minutes.
     * Returns the specified expiration minutes or a default value.
     * 
     * @return expiration minutes, defaults to 30 if not specified
     */
    public int getEffectiveExpirationMinutes() {
        return expirationMinutes != null ? expirationMinutes : 30;
    }
    
    /**
     * Check if custom expiration time is specified.
     * 
     * @return true if custom expiration minutes is provided
     */
    public boolean hasCustomExpiration() {
        return expirationMinutes != null;
    }
    
    /**
     * Get normalized payment method.
     * Returns the payment method in uppercase for consistency.
     * 
     * @return normalized payment method
     */
    public String getNormalizedPaymentMethod() {
        return paymentMethod != null ? paymentMethod.toUpperCase().trim() : null;
    }
    
    /**
     * Check if notes are provided.
     * 
     * @return true if notes are provided and not empty
     */
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }
}