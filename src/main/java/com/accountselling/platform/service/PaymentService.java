package com.accountselling.platform.service;

import com.accountselling.platform.dto.statistics.DailyPaymentStatistics;
import com.accountselling.platform.dto.statistics.PaymentMethodStatistics;
import com.accountselling.platform.dto.statistics.PaymentStatistics;
import com.accountselling.platform.enums.PaymentStatus;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.Payment;
import com.accountselling.platform.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for Payment management operations.
 * Provides comprehensive payment processing functionality including creation,
 * QR code generation, status management, payment gateway integration, and financial reporting.
 * 
 * This service handles:
 * - Payment creation and QR code generation
 * - Payment status lifecycle management
 * - Payment gateway webhook processing
 * - Payment validation and verification
 * - Financial reporting and analytics
 * - Payment method management
 */
public interface PaymentService {

    // ==================== PAYMENT CREATION ====================

    /**
     * Create a new payment for an order with QR code generation.
     * This method creates a payment record and generates a QR code for payment processing.
     * 
     * @param order the order to create payment for
     * @param paymentMethod the payment method to use
     * @param expirationMinutes the number of minutes until payment expires
     * @return the created payment with QR code URL
     * @throws com.accountselling.platform.exception.InvalidPaymentException if payment creation fails
     * @throws com.accountselling.platform.exception.PaymentAlreadyExistsException if payment already exists for order
     */
    Payment createPayment(Order order, String paymentMethod, int expirationMinutes);

    /**
     * Create a new payment for an order with default expiration time.
     * Uses system default expiration time for payment.
     * 
     * @param order the order to create payment for
     * @param paymentMethod the payment method to use
     * @return the created payment with QR code URL
     * @throws com.accountselling.platform.exception.InvalidPaymentException if payment creation fails
     * @throws com.accountselling.platform.exception.PaymentAlreadyExistsException if payment already exists for order
     */
    Payment createPayment(Order order, String paymentMethod);

    /**
     * Create a new payment by order ID.
     * 
     * @param orderId the order ID to create payment for
     * @param paymentMethod the payment method to use
     * @param expirationMinutes the number of minutes until payment expires
     * @return the created payment with QR code URL
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
     * @throws com.accountselling.platform.exception.InvalidPaymentException if payment creation fails
     * @throws com.accountselling.platform.exception.PaymentAlreadyExistsException if payment already exists for order
     */
    Payment createPaymentByOrderId(UUID orderId, String paymentMethod, int expirationMinutes);

    // ==================== QR CODE MANAGEMENT ====================

    /**
     * Generate QR code for payment.
     * Creates a QR code URL/content for payment processing.
     * 
     * @param payment the payment to generate QR code for
     * @return the QR code URL or content
     * @throws com.accountselling.platform.exception.QrCodeGenerationException if QR code generation fails
     */
    String generateQrCode(Payment payment);

    /**
     * Regenerate QR code for existing payment.
     * Useful when payment QR code expires or needs to be refreshed.
     * 
     * @param paymentId the payment ID to regenerate QR code for
     * @return the updated payment with new QR code
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.QrCodeGenerationException if QR code generation fails
     * @throws com.accountselling.platform.exception.InvalidPaymentStatusException if payment cannot be regenerated
     */
    Payment regenerateQrCode(UUID paymentId);

    /**
     * Get QR code content for payment.
     * Returns the QR code content for display or processing.
     * 
     * @param paymentId the payment ID to get QR code for
     * @return the QR code content
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    String getQrCodeContent(UUID paymentId);

    // ==================== PAYMENT RETRIEVAL ====================

    /**
     * Find payment by ID.
     * 
     * @param paymentId the payment ID to search for
     * @return the payment if found
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    Payment findById(UUID paymentId);

    /**
     * Find payment by transaction ID.
     * Used for payment gateway callback processing.
     * 
     * @param transactionId the transaction ID to search for
     * @return the payment if found
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    Payment findByTransactionId(String transactionId);

    /**
     * Find payment by payment reference.
     * 
     * @param paymentReference the payment reference to search for
     * @return the payment if found
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    Payment findByPaymentReference(String paymentReference);

    /**
     * Find payment by order.
     * 
     * @param order the order to find payment for
     * @return the payment if found
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    Payment findByOrder(Order order);

    /**
     * Find payment by order ID.
     * 
     * @param orderId the order ID to find payment for
     * @return the payment if found
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    Payment findByOrderId(UUID orderId);

    // ==================== PAYMENT STATUS MANAGEMENT ====================

    /**
     * Mark payment as processing.
     * This typically happens when payment gateway starts processing.
     * 
     * @param paymentId the payment ID to update
     * @return the updated payment
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.InvalidPaymentStatusException if status transition is invalid
     */
    Payment markPaymentAsProcessing(UUID paymentId);

    /**
     * Mark payment as completed.
     * This happens when payment is successfully processed by gateway.
     * 
     * @param paymentId the payment ID to update
     * @param transactionId the transaction ID from payment gateway
     * @param gatewayResponse the response from payment gateway
     * @return the updated payment
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.InvalidPaymentStatusException if status transition is invalid
     */
    Payment markPaymentAsCompleted(UUID paymentId, String transactionId, String gatewayResponse);

    /**
     * Mark payment as failed.
     * This happens when payment processing fails.
     * 
     * @param paymentId the payment ID to update
     * @param failureReason the reason for failure
     * @param gatewayResponse the response from payment gateway
     * @return the updated payment
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.InvalidPaymentStatusException if status transition is invalid
     */
    Payment markPaymentAsFailed(UUID paymentId, String failureReason, String gatewayResponse);

    /**
     * Cancel payment.
     * This cancels a pending or processing payment.
     * 
     * @param paymentId the payment ID to cancel
     * @param reason the reason for cancellation
     * @return the updated payment
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.InvalidPaymentStatusException if payment cannot be cancelled
     */
    Payment cancelPayment(UUID paymentId, String reason);

    /**
     * Process refund for completed payment.
     * This creates a refund for a completed payment.
     * 
     * @param paymentId the payment ID to refund
     * @param refundAmount the amount to refund (can be partial)
     * @param reason the reason for refund
     * @return the updated payment with refund information
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.InvalidPaymentStatusException if payment cannot be refunded
     * @throws com.accountselling.platform.exception.InvalidRefundException if refund amount is invalid
     */
    Payment processRefund(UUID paymentId, BigDecimal refundAmount, String reason);

    // ==================== PAYMENT GATEWAY INTEGRATION ====================

    /**
     * Process payment gateway webhook.
     * Handles incoming webhook notifications from payment gateway.
     * 
     * @param transactionId the transaction ID from webhook
     * @param status the payment status from webhook
     * @param gatewayResponse the full response from gateway
     * @return the updated payment
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.WebhookProcessingException if webhook processing fails
     */
    Payment processWebhook(String transactionId, String status, String gatewayResponse);

    /**
     * Verify payment with gateway.
     * Verifies payment status directly with payment gateway.
     * 
     * @param paymentId the payment ID to verify
     * @return the updated payment with verified status
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.PaymentVerificationException if verification fails
     */
    Payment verifyPaymentWithGateway(UUID paymentId);

    /**
     * Check payment status with gateway.
     * Queries payment gateway for current payment status.
     * 
     * @param transactionId the transaction ID to check
     * @return the current payment status from gateway
     * @throws com.accountselling.platform.exception.PaymentGatewayException if gateway query fails
     */
    String checkPaymentStatusWithGateway(String transactionId);

    // ==================== PAYMENT VALIDATION ====================

    /**
     * Validate payment before creation.
     * Checks order eligibility and payment constraints.
     * 
     * @param order the order to create payment for
     * @param paymentMethod the payment method to validate
     * @throws com.accountselling.platform.exception.InvalidPaymentException if validation fails
     */
    void validatePayment(Order order, String paymentMethod);

    /**
     * Check if payment is expired.
     * 
     * @param paymentId the payment ID to check
     * @return true if payment has expired
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    boolean isPaymentExpired(UUID paymentId);

    /**
     * Check if payment can be cancelled.
     * 
     * @param paymentId the payment ID to check
     * @return true if payment can be cancelled
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    boolean canPaymentBeCancelled(UUID paymentId);

    /**
     * Check if payment can be refunded.
     * 
     * @param paymentId the payment ID to check
     * @return true if payment can be refunded
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    boolean canPaymentBeRefunded(UUID paymentId);

    /**
     * Validate refund amount.
     * 
     * @param paymentId the payment ID to validate refund for
     * @param refundAmount the refund amount to validate
     * @return true if refund amount is valid
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     */
    boolean isRefundAmountValid(UUID paymentId, BigDecimal refundAmount);

    // ==================== PAYMENT EXPIRATION MANAGEMENT ====================

    /**
     * Process expired payments.
     * Marks expired payments as failed and releases associated resources.
     * This is typically called by scheduled tasks.
     * 
     * @return the number of payments processed
     */
    int processExpiredPayments();

    /**
     * Get payments expiring soon.
     * Returns payments that will expire within specified minutes.
     * 
     * @param withinMinutes the time threshold in minutes
     * @return list of payments expiring soon
     */
    List<Payment> getPaymentsExpiringSoon(int withinMinutes);

    /**
     * Extend payment expiration.
     * Extends the expiration time for a payment.
     * 
     * @param paymentId the payment ID to extend
     * @param additionalMinutes the additional minutes to add
     * @return the updated payment
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if payment not found
     * @throws com.accountselling.platform.exception.InvalidPaymentStatusException if payment cannot be extended
     */
    Payment extendPaymentExpiration(UUID paymentId, int additionalMinutes);

    // ==================== PAYMENT SEARCH AND REPORTING ====================

    /**
     * Get payments by user with pagination.
     * 
     * @param user the user to get payments for
     * @param pageable pagination parameters
     * @return page of payments for the user
     */
    Page<Payment> getPaymentsByUser(User user, Pageable pageable);

    /**
     * Get payments by username with pagination.
     * 
     * @param username the username to get payments for
     * @param pageable pagination parameters
     * @return page of payments for the user
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if user not found
     */
    Page<Payment> getPaymentsByUsername(String username, Pageable pageable);

    /**
     * Get payments by status with pagination.
     * 
     * @param status the payment status to filter by
     * @param pageable pagination parameters
     * @return page of payments with the specified status
     */
    Page<Payment> getPaymentsByStatus(PaymentStatus status, Pageable pageable);

    /**
     * Search payments with multiple criteria.
     * 
     * @param transactionId transaction ID pattern (can be null)
     * @param paymentReference payment reference pattern (can be null)
     * @param username username pattern (can be null)
     * @param status payment status (can be null)
     * @param paymentMethod payment method (can be null)
     * @param startDate start date (can be null)
     * @param endDate end date (can be null)
     * @param minAmount minimum amount (can be null)
     * @param maxAmount maximum amount (can be null)
     * @param pageable pagination parameters
     * @return page of payments matching the criteria
     */
    Page<Payment> searchPayments(String transactionId, String paymentReference, String username,
                               PaymentStatus status, String paymentMethod,
                               LocalDateTime startDate, LocalDateTime endDate,
                               BigDecimal minAmount, BigDecimal maxAmount,
                               Pageable pageable);

    /**
     * Get payment statistics for dashboard.
     * 
     * @return payment statistics including counts by status and revenue
     */
    PaymentStatistics getPaymentStatistics();

    /**
     * Get payment statistics within date range.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return payment statistics for the specified period
     */
    PaymentStatistics getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get daily payment statistics within date range.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of daily payment statistics
     */
    List<DailyPaymentStatistics> getDailyPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get payment method statistics.
     * 
     * @return list of payment method statistics
     */
    List<PaymentMethodStatistics> getPaymentMethodStatistics();

    /**
     * Get recent payments for dashboard.
     * 
     * @param limit maximum number of payments to return
     * @return list of recent payments
     */
    List<Payment> getRecentPayments(int limit);

    // ==================== PAYMENT UTILITY METHODS ====================

    /**
     * Calculate total revenue from completed payments.
     * 
     * @return total revenue from all completed payments
     */
    BigDecimal calculateTotalRevenue();

    /**
     * Calculate revenue within date range.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return total revenue within the date range
     */
    BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Calculate total refunded amount.
     * 
     * @return total amount refunded from all refunded payments
     */
    BigDecimal calculateTotalRefundedAmount();

    /**
     * Get supported payment methods.
     * 
     * @return list of supported payment methods
     */
    List<String> getSupportedPaymentMethods();

    /**
     * Get default payment expiration time in minutes.
     * 
     * @return default expiration time in minutes
     */
    int getDefaultExpirationMinutes();

}   