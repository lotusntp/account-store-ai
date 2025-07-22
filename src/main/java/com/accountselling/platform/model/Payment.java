package com.accountselling.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity representing payment information for orders.
 * Contains payment details including amount, status, method, and external transaction references.
 * 
 * เอนทิตี้การชำระเงินที่แสดงข้อมูลการชำระเงินสำหรับคำสั่งซื้อ
 * ประกอบด้วยรายละเอียดการชำระเงิน รวมถึงจำนวนเงิน สถานะ วิธีการชำระเงิน และการอ้างอิงธุรกรรมภายนอก
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order", columnList = "order_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_payment_created_at", columnList = "created_at"),
    @Index(name = "idx_payment_status_created", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"order"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @NotNull(message = "Payment must be associated with an order")
    private Order order;

    @NotNull(message = "Payment amount cannot be null")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    @Digits(integer = 12, fraction = 2, message = "Payment amount must have at most 12 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Size(max = 50, message = "Payment method cannot exceed 50 characters")
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Size(max = 200, message = "Transaction ID cannot exceed 200 characters")
    @Column(name = "transaction_id", unique = true, length = 200)
    @EqualsAndHashCode.Include
    private String transactionId;

    @Size(max = 500, message = "QR code URL cannot exceed 500 characters")
    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "gateway_response", length = 2000)
    private String gatewayResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "refund_amount", precision = 14, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    // Constructor with order and amount
    public Payment(Order order, BigDecimal amount) {
        this.order = order;
        this.amount = amount;
        generatePaymentReference();
    }

    // Constructor with order, amount, and payment method
    public Payment(Order order, BigDecimal amount, String paymentMethod) {
        this.order = order;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        generatePaymentReference();
    }

    // Constructor with all main fields
    public Payment(Order order, BigDecimal amount, String paymentMethod, String qrCodeUrl) {
        this.order = order;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.qrCodeUrl = qrCodeUrl;
        generatePaymentReference();
    }

    // Business logic methods
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == PaymentStatus.CANCELLED;
    }

    public boolean isProcessing() {
        return status == PaymentStatus.PROCESSING;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    // Status transition methods
    public void markAsProcessing() {
        if (!isPending()) {
            throw new IllegalStateException("Can only mark pending payments as processing");
        }
        this.status = PaymentStatus.PROCESSING;
    }

    public void markAsCompleted(String transactionId) {
        if (!isPending() && !isProcessing()) {
            throw new IllegalStateException("Can only mark pending or processing payments as completed");
        }
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        if (isCompleted() || isRefunded()) {
            throw new IllegalStateException("Cannot mark completed or refunded payments as failed");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markAsCancelled() {
        if (isCompleted() || isRefunded()) {
            throw new IllegalStateException("Cannot cancel completed or refunded payments");
        }
        this.status = PaymentStatus.CANCELLED;
    }

    public void markAsRefunded(BigDecimal refundAmount) {
        if (!isCompleted()) {
            throw new IllegalStateException("Can only refund completed payments");
        }
        this.status = PaymentStatus.REFUNDED;
        this.refundAmount = refundAmount;
        this.refundedAt = LocalDateTime.now();
    }

    // Set expiration time
    public void setExpirationTime(int minutes) {
        this.expiresAt = LocalDateTime.now().plusMinutes(minutes);
    }

    public void setExpirationTime(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Generate unique payment reference
    private void generatePaymentReference() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        this.paymentReference = String.format("PAY-%d-%04d", timestamp, random);
    }

    // Set payment reference manually (for testing or specific requirements)
    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    // Get formatted amount for display
    public String getFormattedAmount() {
        return String.format("฿%.2f", amount);
    }

    // Get formatted refund amount for display
    public String getFormattedRefundAmount() {
        return refundAmount != null ? String.format("฿%.2f", refundAmount) : "฿0.00";
    }

    // Get order number for convenience
    public String getOrderNumber() {
        return order != null ? order.getOrderNumber() : null;
    }

    // Get username for convenience
    public String getUsername() {
        return order != null && order.getUser() != null ? order.getUser().getUsername() : null;
    }

    // Check if payment belongs to specific user
    public boolean belongsToUser(User user) {
        return order != null && order.belongsToUser(user);
    }

    // Check if payment belongs to user by username
    public boolean belongsToUser(String username) {
        return order != null && order.belongsToUser(username);
    }

    // Get payment status display name
    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "Unknown";
    }

    // Check if payment can be cancelled
    public boolean canBeCancelled() {
        return isPending() || isProcessing();
    }

    // Check if payment can be refunded
    public boolean canBeRefunded() {
        return isCompleted() && (refundAmount == null || refundAmount.compareTo(amount) < 0);
    }

    // Get remaining time until expiration in minutes
    public long getRemainingExpirationMinutes() {
        if (expiresAt == null || isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }

    // Check if payment is expiring soon (within specified minutes)
    public boolean isExpiringSoon(int withinMinutes) {
        if (expiresAt == null || isExpired()) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(withinMinutes);
        return expiresAt.isBefore(threshold);
    }

    // Get total refunded amount
    public BigDecimal getTotalRefundedAmount() {
        return refundAmount != null ? refundAmount : BigDecimal.ZERO;
    }

    // Get remaining refundable amount
    public BigDecimal getRemainingRefundableAmount() {
        if (!canBeRefunded()) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(getTotalRefundedAmount());
    }

    // Check if amount matches order total
    public boolean isAmountValid() {
        return order != null && amount != null && 
               amount.compareTo(order.getTotalAmount()) == 0;
    }

    /**
     * Payment status enumeration
     */
    public enum PaymentStatus {
        PENDING("Pending", "รอชำระเงิน"),
        PROCESSING("Processing", "กำลังดำเนินการ"),
        COMPLETED("Completed", "ชำระเงินสำเร็จ"),
        FAILED("Failed", "ชำระเงินล้มเหลว"),
        CANCELLED("Cancelled", "ยกเลิกการชำระเงิน"),
        REFUNDED("Refunded", "คืนเงินแล้ว");

        private final String displayName;
        private final String displayNameTh;

        PaymentStatus(String displayName, String displayNameTh) {
            this.displayName = displayName;
            this.displayNameTh = displayNameTh;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDisplayNameTh() {
            return displayNameTh;
        }
    }
}