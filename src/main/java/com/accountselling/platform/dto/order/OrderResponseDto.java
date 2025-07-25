package com.accountselling.platform.dto.order;

import com.accountselling.platform.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTO for order response data. Contains comprehensive order information for client responses. */
public record OrderResponseDto(
    UUID id,
    String orderNumber,
    String username,
    String userDisplayName,
    BigDecimal totalAmount,
    String formattedTotalAmount,
    OrderStatus status,
    String statusDisplayName,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemResponseDto> orderItems,
    PaymentSummaryDto payment) {

  /**
   * Check if order is in pending status.
   *
   * @return true if order is pending
   */
  public boolean isPending() {
    return status != null && status.isPending();
  }

  /**
   * Check if order is completed.
   *
   * @return true if order is completed
   */
  public boolean isCompleted() {
    return status != null && status.isCompleted();
  }

  /**
   * Check if order can be cancelled.
   *
   * @return true if order can be cancelled
   */
  public boolean canBeCancelled() {
    return status != null && status.canBeCancelled();
  }

  /**
   * Get order item count.
   *
   * @return number of items in the order
   */
  public int getItemCount() {
    return orderItems != null ? orderItems.size() : 0;
  }

  /**
   * Check if order has payment information.
   *
   * @return true if payment information is available
   */
  public boolean hasPayment() {
    return payment != null;
  }

  /** Order item response DTO nested class. */
  public record OrderItemResponseDto(
      UUID id,
      UUID stockItemId,
      String productName,
      String productDescription,
      String categoryName,
      String server,
      BigDecimal price,
      String formattedPrice,
      String notes,
      boolean stockItemSold) {}

  /** Payment summary DTO nested class. */
  public record PaymentSummaryDto(
      UUID id,
      String paymentReference,
      String transactionId,
      BigDecimal amount,
      String formattedAmount,
      String status,
      String statusDisplayName,
      String paymentMethod,
      String qrCodeUrl,
      LocalDateTime paidAt,
      LocalDateTime expiresAt,
      boolean isExpired,
      long remainingMinutes) {}
}
