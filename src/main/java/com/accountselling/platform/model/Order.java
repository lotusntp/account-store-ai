package com.accountselling.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Order entity representing customer purchases.
 * Contains order information including customer, total amount, status, and order items.
 * 
 * เอนทิตี้คำสั่งซื้อที่แสดงการซื้อของลูกค้า
 * ประกอบด้วยข้อมูลคำสั่งซื้อ รวมถึงลูกค้า จำนวนเงินรวม สถานะ และรายการสินค้าในคำสั่งซื้อ
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "created_at"),
    @Index(name = "idx_order_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"user", "orderItems", "payment"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Order must be associated with a user")
    private User user;

    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Digits(integer = 12, fraction = 2, message = "Total amount must have at most 12 integer digits and 2 decimal places")
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "order_number", unique = true, length = 50)
    private String orderNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    // One-to-many relationship with OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<OrderItem> orderItems = new HashSet<>();

    // One-to-one relationship with Payment
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Payment payment;

    // Constructor with user and total amount
    public Order(User user, BigDecimal totalAmount) {
        this.user = user;
        this.totalAmount = totalAmount;
        generateOrderNumber();
    }

    // Constructor with user, total amount, and status
    public Order(User user, BigDecimal totalAmount, OrderStatus status) {
        this.user = user;
        this.totalAmount = totalAmount;
        this.status = status;
        generateOrderNumber();
    }

    // Helper methods for managing order items
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void removeOrderItem(OrderItem orderItem) {
        orderItems.remove(orderItem);
        orderItem.setOrder(null);
    }

    // Helper method for managing payment
    public void setPayment(Payment payment) {
        this.payment = payment;
        if (payment != null) {
            payment.setOrder(this);
        }
    }

    // Business logic methods
    public BigDecimal calculateTotalAmount() {
        return orderItems.stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updateTotalAmount() {
        this.totalAmount = calculateTotalAmount();
    }

    public int getTotalItemCount() {
        return orderItems.size();
    }

    public boolean isEmpty() {
        return orderItems.isEmpty();
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == OrderStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean isProcessing() {
        return status == OrderStatus.PROCESSING;
    }

    // Status transition methods
    public void markAsProcessing() {
        if (!isPending()) {
            throw new IllegalStateException("Can only mark pending orders as processing");
        }
        this.status = OrderStatus.PROCESSING;
    }

    public void markAsCompleted() {
        if (!isProcessing() && !isPending()) {
            throw new IllegalStateException("Can only mark pending or processing orders as completed");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void markAsFailed() {
        if (isCompleted()) {
            throw new IllegalStateException("Cannot mark completed orders as failed");
        }
        this.status = OrderStatus.FAILED;
    }

    public void markAsCancelled() {
        if (isCompleted()) {
            throw new IllegalStateException("Cannot cancel completed orders");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // Generate unique order number
    private void generateOrderNumber() {
        // Generate order number based on timestamp and random component
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        this.orderNumber = String.format("ORD-%d-%03d", timestamp, random);
    }

    // Set order number manually (for testing or specific requirements)
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    // Get formatted total amount for display
    public String getFormattedTotalAmount() {
        return String.format("฿%.2f", totalAmount);
    }

    // Get user display name
    public String getUserDisplayName() {
        return user != null ? user.getFullName() : "Unknown User";
    }

    // Get username
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }

    // Check if order belongs to specific user
    public boolean belongsToUser(User targetUser) {
        return user != null && user.equals(targetUser);
    }

    // Check if order belongs to user by username
    public boolean belongsToUser(String username) {
        return user != null && user.getUsername().equals(username);
    }

    // Get order status display name
    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "Unknown";
    }

    // Check if order can be cancelled
    public boolean canBeCancelled() {
        return isPending() || isProcessing();
    }

    // Check if order can be refunded
    public boolean canBeRefunded() {
        return isCompleted();
    }

    /**
     * Order status enumeration
     */
    public enum OrderStatus {
        PENDING("Pending", "รอดำเนินการ"),
        PROCESSING("Processing", "กำลังดำเนินการ"),
        COMPLETED("Completed", "เสร็จสิ้น"),
        FAILED("Failed", "ล้มเหลว"),
        CANCELLED("Cancelled", "ยกเลิก");

        private final String displayName;
        private final String displayNameTh;

        OrderStatus(String displayName, String displayNameTh) {
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