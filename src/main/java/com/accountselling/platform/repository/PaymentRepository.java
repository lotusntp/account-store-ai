package com.accountselling.platform.repository;

import com.accountselling.platform.enums.PaymentStatus;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.Payment;
import com.accountselling.platform.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Payment entity operations.
 * Provides comprehensive data access methods for payment management including
 * transaction tracking, status monitoring, financial reporting, and payment analytics.
 * 
 * รีพอสิทอรี่สำหรับจัดการข้อมูลการชำระเงิน
 * รองรับการจัดการการชำระเงิน การติดตามธุรกรรม การตรวจสอบสถานะ การรายงานทางการเงิน และการวิเคราะห์การชำระเงิน
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // ==================== BASIC PAYMENT QUERIES ====================

    /**
     * Find payment by order.
     * Used for retrieving payment information for a specific order.
     * 
     * @param order the order to search payment for
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByOrder(Order order);

    /**
     * Find payment by order ID.
     * Used for order-specific payment lookup.
     * 
     * @param orderId the order ID to search payment for
     * @return Optional containing the payment if found
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    Optional<Payment> findByOrderId(@Param("orderId") UUID orderId);

    /**
     * Find payment by transaction ID.
     * Used for payment gateway callback processing and transaction lookup.
     * 
     * @param transactionId the transaction ID to search for
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Find payment by payment reference.
     * Used for internal payment reference lookup.
     * 
     * @param paymentReference the payment reference to search for
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByPaymentReference(String paymentReference);

    /**
     * Check if transaction ID exists.
     * Used for transaction ID validation and uniqueness checks.
     * 
     * @param transactionId the transaction ID to check
     * @return true if transaction ID exists, false otherwise
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Check if payment reference exists.
     * Used for payment reference validation and uniqueness checks.
     * 
     * @param paymentReference the payment reference to check
     * @return true if payment reference exists, false otherwise
     */
    boolean existsByPaymentReference(String paymentReference);

    // ==================== STATUS-BASED QUERIES ====================

    /**
     * Find payments by status.
     * Used for status-based payment management and monitoring.
     * 
     * @param status the payment status to filter by
     * @return list of payments with the specified status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find payments by status with pagination.
     * Used for paginated status-based payment management.
     * 
     * @param status the payment status to filter by
     * @param pageable pagination parameters
     * @return page of payments with the specified status
     */
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    /**
     * Find pending payments.
     * Used for processing pending payments and timeout handling.
     * 
     * @return list of pending payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' ORDER BY p.createdAt ASC")
    List<Payment> findPendingPayments();

    /**
     * Find processing payments.
     * Used for monitoring payments currently being processed.
     * 
     * @return list of processing payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PROCESSING' ORDER BY p.createdAt ASC")
    List<Payment> findProcessingPayments();

    /**
     * Find completed payments.
     * Used for financial reporting and analytics.
     * 
     * @return list of completed payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' ORDER BY p.paidAt DESC")
    List<Payment> findCompletedPayments();

    /**
     * Find failed payments.
     * Used for failure analysis and customer support.
     * 
     * @return list of failed payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' ORDER BY p.createdAt DESC")
    List<Payment> findFailedPayments();

    /**
     * Find cancelled payments.
     * Used for cancellation tracking and analysis.
     * 
     * @return list of cancelled payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'CANCELLED' ORDER BY p.createdAt DESC")
    List<Payment> findCancelledPayments();

    /**
     * Find refunded payments.
     * Used for refund tracking and financial reconciliation.
     * 
     * @return list of refunded payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'REFUNDED' ORDER BY p.refundedAt DESC")
    List<Payment> findRefundedPayments();

    // ==================== USER-BASED QUERIES ====================

    /**
     * Find payments by user.
     * Used for user payment history and analytics.
     * 
     * @param user the user to search payments for
     * @return list of payments for the user
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user = :user ORDER BY p.createdAt DESC")
    List<Payment> findByUser(@Param("user") User user);

    /**
     * Find payments by user with pagination.
     * Used for paginated user payment history.
     * 
     * @param user the user to search payments for
     * @param pageable pagination parameters
     * @return page of payments for the user
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user = :user ORDER BY p.createdAt DESC")
    Page<Payment> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * Find payments by user ID.
     * Used for user-specific payment tracking.
     * 
     * @param userId the user ID to search payments for
     * @return list of payments for the user
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user.id = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserId(@Param("userId") UUID userId);

    /**
     * Find payments by user and status.
     * Used for user-specific status-based payment filtering.
     * 
     * @param user the user to search payments for
     * @param status the payment status to filter by
     * @return list of payments for the user with the specified status
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user = :user AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByUserAndStatus(@Param("user") User user, @Param("status") PaymentStatus status);

    // ==================== DATE-BASED QUERIES ====================

    /**
     * Find payments created within date range.
     * Used for reporting and analytics within specific periods.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of payments created within the date range
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find completed payments within date range.
     * Used for financial reporting within specific periods.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of completed payments within the date range
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.paidAt >= :startDate AND p.paidAt <= :endDate ORDER BY p.paidAt DESC")
    List<Payment> findCompletedPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find payments by user within date range.
     * Used for user-specific payment history within periods.
     * 
     * @param user the user to search payments for
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of payments for the user within the date range
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user = :user AND p.createdAt >= :startDate AND p.createdAt <= :endDate ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsByUserBetweenDates(@Param("user") User user, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ==================== EXPIRATION-BASED QUERIES ====================

    /**
     * Find expired payments.
     * Used for cleanup operations and timeout handling.
     * 
     * @return list of payments that have expired
     */
    @Query("SELECT p FROM Payment p WHERE p.expiresAt IS NOT NULL AND p.expiresAt <= CURRENT_TIMESTAMP AND p.status IN ('PENDING', 'PROCESSING') ORDER BY p.expiresAt ASC")
    List<Payment> findExpiredPayments();

    /**
     * Find payments expiring soon.
     * Used for proactive payment management and notifications.
     * 
     * @param threshold the time threshold for "expiring soon"
     * @return list of payments expiring within the threshold
     */
    @Query("SELECT p FROM Payment p WHERE p.expiresAt IS NOT NULL AND p.expiresAt > CURRENT_TIMESTAMP AND p.expiresAt <= :threshold AND p.status IN ('PENDING', 'PROCESSING') ORDER BY p.expiresAt ASC")
    List<Payment> findPaymentsExpiringSoon(@Param("threshold") LocalDateTime threshold);

    /**
     * Find payments without expiration time.
     * Used for data integrity checks and cleanup.
     * 
     * @return list of payments without expiration time set
     */
    @Query("SELECT p FROM Payment p WHERE p.expiresAt IS NULL AND p.status IN ('PENDING', 'PROCESSING')")
    List<Payment> findPaymentsWithoutExpiration();

    // ==================== AMOUNT-BASED QUERIES ====================

    /**
     * Find payments by amount range.
     * Used for amount-based filtering and analysis.
     * 
     * @param minAmount minimum payment amount (inclusive)
     * @param maxAmount maximum payment amount (inclusive)
     * @return list of payments within the specified amount range
     */
    @Query("SELECT p FROM Payment p WHERE p.amount >= :minAmount AND p.amount <= :maxAmount ORDER BY p.amount DESC")
    List<Payment> findPaymentsByAmountRange(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Find payments above specified amount.
     * Used for high-value payment tracking.
     * 
     * @param amount the minimum amount threshold
     * @return list of payments with amount above the threshold
     */
    @Query("SELECT p FROM Payment p WHERE p.amount > :amount ORDER BY p.amount DESC")
    List<Payment> findPaymentsAboveAmount(@Param("amount") BigDecimal amount);

    // ==================== PAYMENT METHOD QUERIES ====================

    /**
     * Find payments by payment method.
     * Used for payment method analysis and reporting.
     * 
     * @param paymentMethod the payment method to filter by
     * @return list of payments using the specified payment method
     */
    List<Payment> findByPaymentMethod(String paymentMethod);

    /**
     * Find completed payments by payment method.
     * Used for payment method performance analysis.
     * 
     * @param paymentMethod the payment method to filter by
     * @return list of completed payments using the specified payment method
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentMethod = :paymentMethod AND p.status = 'COMPLETED' ORDER BY p.paidAt DESC")
    List<Payment> findCompletedPaymentsByMethod(@Param("paymentMethod") String paymentMethod);

    /**
     * Get distinct payment methods.
     * Used for payment method filter options and analytics.
     * 
     * @return list of distinct payment methods
     */
    @Query("SELECT DISTINCT p.paymentMethod FROM Payment p WHERE p.paymentMethod IS NOT NULL ORDER BY p.paymentMethod")
    List<String> findDistinctPaymentMethods();

    // ==================== COUNTING QUERIES ====================

    /**
     * Count payments by status.
     * Used for status-based statistics.
     * 
     * @param status the payment status to count
     * @return count of payments with the specified status
     */
    long countByStatus(PaymentStatus status);

    /**
     * Count payments by user.
     * Used for user payment statistics.
     * 
     * @param user the user to count payments for
     * @return count of payments for the user
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.order.user = :user")
    long countByUser(@Param("user") User user);

    /**
     * Count payments by user and status.
     * Used for user-specific status statistics.
     * 
     * @param user the user to count payments for
     * @param status the payment status to count
     * @return count of payments for the user with the specified status
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.order.user = :user AND p.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") PaymentStatus status);

    /**
     * Count payments created within date range.
     * Used for period-based statistics.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return count of payments created within the date range
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate")
    long countPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count completed payments within date range.
     * Used for financial statistics within periods.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return count of completed payments within the date range
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paidAt >= :startDate AND p.paidAt <= :endDate")
    long countCompletedPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ==================== AGGREGATION QUERIES ====================

    /**
     * Calculate total revenue from completed payments.
     * Used for revenue reporting and analytics.
     * 
     * @return total revenue from all completed payments
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal calculateTotalRevenue();

    /**
     * Calculate total revenue within date range.
     * Used for period-based revenue reporting.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return total revenue within the date range
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paidAt >= :startDate AND p.paidAt <= :endDate")
    BigDecimal calculateRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate average payment amount.
     * Used for payment analytics.
     * 
     * @return average payment amount from completed payments
     */
    @Query("SELECT COALESCE(AVG(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal calculateAveragePaymentAmount();

    /**
     * Calculate total revenue by user.
     * Used for customer value analysis.
     * 
     * @param user the user to calculate revenue for
     * @return total revenue from the user's completed payments
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.user = :user AND p.status = 'COMPLETED'")
    BigDecimal calculateRevenueByUser(@Param("user") User user);

    /**
     * Calculate total refunded amount.
     * Used for refund tracking and financial reconciliation.
     * 
     * @return total amount refunded from all refunded payments
     */
    @Query("SELECT COALESCE(SUM(p.refundAmount), 0) FROM Payment p WHERE p.status = 'REFUNDED' AND p.refundAmount IS NOT NULL")
    BigDecimal calculateTotalRefundedAmount();

    /**
     * Calculate total refunded amount within date range.
     * Used for period-based refund reporting.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return total refunded amount within the date range
     */
    @Query("SELECT COALESCE(SUM(p.refundAmount), 0) FROM Payment p WHERE p.status = 'REFUNDED' AND p.refundAmount IS NOT NULL AND p.refundedAt >= :startDate AND p.refundedAt <= :endDate")
    BigDecimal calculateRefundedAmountBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ==================== BULK OPERATIONS ====================

    /**
     * Mark expired payments as failed in bulk.
     * Used for scheduled cleanup operations.
     * 
     * @param failureReason the reason for marking as failed
     * @return number of payments marked as failed
     */
    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.status = 'FAILED', p.failureReason = :failureReason WHERE p.expiresAt IS NOT NULL AND p.expiresAt <= CURRENT_TIMESTAMP AND p.status IN ('PENDING', 'PROCESSING')")
    int markExpiredPaymentsAsFailed(@Param("failureReason") String failureReason);

    /**
     * Cancel payments for cancelled orders in bulk.
     * Used for order cancellation processing.
     * 
     * @param orderIds list of cancelled order IDs
     * @return number of payments cancelled
     */
    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.status = 'CANCELLED' WHERE p.order.id IN :orderIds AND p.status IN ('PENDING', 'PROCESSING')")
    int cancelPaymentsForOrders(@Param("orderIds") List<UUID> orderIds);

    // ==================== ADVANCED QUERIES ====================

    /**
     * Find recent payments for dashboard.
     * Used for admin dashboard recent activity display.
     * 
     * @param limit maximum number of payments to return
     * @return list of recent payments
     */
    @Query("SELECT p FROM Payment p ORDER BY p.createdAt DESC LIMIT :limit")
    List<Payment> findRecentPayments(@Param("limit") int limit);

    /**
     * Find payments with gateway response issues.
     * Used for payment gateway troubleshooting.
     * 
     * @return list of payments with potential gateway issues
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.gatewayResponse IS NOT NULL ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsWithGatewayIssues();

    /**
     * Find payments by username.
     * Used for admin payment search functionality.
     * 
     * @param username the username to search payments for
     * @return list of payments for the specified username
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user.username = :username ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsByUsername(@Param("username") String username);

    /**
     * Find payments by username with pagination.
     * Used for paginated admin payment search.
     * 
     * @param username the username to search payments for
     * @param pageable pagination parameters
     * @return page of payments for the specified username
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user.username = :username ORDER BY p.createdAt DESC")
    Page<Payment> findPaymentsByUsername(@Param("username") String username, Pageable pageable);

    /**
     * Search payments by multiple criteria.
     * Used for advanced payment search with multiple filters.
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
    @Query("""
        SELECT p FROM Payment p 
        WHERE (:transactionId IS NULL OR LOWER(p.transactionId) LIKE LOWER(CONCAT('%', :transactionId, '%')))
        AND (:paymentReference IS NULL OR LOWER(p.paymentReference) LIKE LOWER(CONCAT('%', :paymentReference, '%')))
        AND (:username IS NULL OR LOWER(p.order.user.username) LIKE LOWER(CONCAT('%', :username, '%')))
        AND (:status IS NULL OR p.status = :status)
        AND (:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod)
        AND (:startDate IS NULL OR p.createdAt >= :startDate)
        AND (:endDate IS NULL OR p.createdAt <= :endDate)
        AND (:minAmount IS NULL OR p.amount >= :minAmount)
        AND (:maxAmount IS NULL OR p.amount <= :maxAmount)
        ORDER BY p.createdAt DESC
        """)
    Page<Payment> searchPayments(
        @Param("transactionId") String transactionId,
        @Param("paymentReference") String paymentReference,
        @Param("username") String username,
        @Param("status") PaymentStatus status,
        @Param("paymentMethod") String paymentMethod,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("minAmount") BigDecimal minAmount,
        @Param("maxAmount") BigDecimal maxAmount,
        Pageable pageable
    );

    /**
     * Get payment statistics for dashboard.
     * Used for admin dashboard statistics display.
     * 
     * @return object array containing [total, pending, completed, failed, cancelled, refunded] counts
     */
    @Query("""
        SELECT 
            COUNT(p) as total,
            SUM(CASE WHEN p.status = 'PENDING' THEN 1 ELSE 0 END) as pending,
            SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN p.status = 'FAILED' THEN 1 ELSE 0 END) as failed,
            SUM(CASE WHEN p.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled,
            SUM(CASE WHEN p.status = 'REFUNDED' THEN 1 ELSE 0 END) as refunded
        FROM Payment p
        """)
    Object[] getPaymentStatistics();

    /**
     * Get daily payment statistics within date range.
     * Used for daily payment reporting and charts.
     * 
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of daily statistics [date, count, revenue, refunds]
     */
    @Query("""
        SELECT 
            DATE(p.createdAt) as date,
            COUNT(p) as paymentCount,
            COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END), 0) as revenue,
            COALESCE(SUM(CASE WHEN p.status = 'REFUNDED' THEN p.refundAmount ELSE 0 END), 0) as refunds
        FROM Payment p 
        WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate
        GROUP BY DATE(p.createdAt)
        ORDER BY DATE(p.createdAt)
        """)
    List<Object[]> getDailyPaymentStatistics(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get payment method statistics.
     * Used for payment method performance analysis.
     * 
     * @return list of payment method statistics [method, count, revenue, successRate]
     */
    @Query("""
        SELECT 
            p.paymentMethod as method,
            COUNT(p) as totalCount,
            COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END), 0) as revenue,
            (COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(p)) as successRate
        FROM Payment p 
        WHERE p.paymentMethod IS NOT NULL
        GROUP BY p.paymentMethod
        ORDER BY COUNT(p) DESC
        """)
    List<Object[]> getPaymentMethodStatistics();

    // ==================== ADDITIONAL METHODS FOR TESTS ====================

    /**
     * Find payments by status in list.
     * Used for multi-status filtering.
     * 
     * @param statuses list of payment statuses to filter by
     * @return list of payments with any of the specified statuses
     */
    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    /**
     * Find payments by username.
     * Used for user-based search.
     * 
     * @param username the username
     * @return list of payments for the specified user
     */
    @Query("select p from Payment p where p.order.user.username = :username")
    List<Payment> findByUsername(@Param("username") String username);

    /**
     * Find payments created between dates.
     * Used for date range filtering.
     * 
     * @param startDate start date
     * @param endDate end date
     * @return list of payments created between dates
     */
    List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find payments paid between dates.
     * Used for payment date range filtering.
     * 
     * @param startDate start date
     * @param endDate end date
     * @return list of payments paid between dates
     */
    List<Payment> findByPaidAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find payments by amount between range.
     * Used for amount range filtering.
     * 
     * @param minAmount minimum amount
     * @param maxAmount maximum amount
     * @return list of payments with amount in range
     */
    List<Payment> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Find payments by amount greater than.
     * Used for minimum amount filtering.
     * 
     * @param amount minimum amount threshold
     * @return list of payments with amount greater than threshold
     */
    List<Payment> findByAmountGreaterThan(BigDecimal amount);

    /**
     * Count payments by payment method.
     * Used for payment method statistics.
     * 
     * @param paymentMethod the payment method
     * @return count of payments using the specified method
     */
    long countByPaymentMethod(String paymentMethod);

    /**
     * Calculate total amount by status.
     * Used for status-based financial analysis.
     * 
     * @param status the payment status
     * @return total amount from payments with specified status
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal calculateTotalAmountByStatus(@Param("status") PaymentStatus status);

    /**
     * Calculate total completed amount.
     * Used for completed payment analysis.
     * 
     * @return total amount from completed payments
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal calculateTotalCompletedAmount();

    /**
     * Calculate total amount by user.
     * Used for user payment analysis.
     * 
     * @param user the user
     * @return total amount from user's payments
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.user = :user")
    BigDecimal calculateTotalAmountByUser(@Param("user") User user);

    /**
     * Find payments with QR codes.
     * Used for QR code payment analysis.
     * 
     * @return list of payments with QR code URLs
     */
    @Query("SELECT p FROM Payment p WHERE p.qrCodeUrl IS NOT NULL")
    List<Payment> findPaymentsWithQrCodes();

    /**
     * Find payments with failure reasons.
     * Used for failure analysis.
     * 
     * @return list of payments with failure reasons
     */
    @Query("SELECT p FROM Payment p WHERE p.failureReason IS NOT NULL")
    List<Payment> findPaymentsWithFailureReasons();
}