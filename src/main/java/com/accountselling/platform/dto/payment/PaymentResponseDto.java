package com.accountselling.platform.dto.payment;

import com.accountselling.platform.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for payment response data. Contains comprehensive payment information for client responses.
 */
public record PaymentResponseDto(
    UUID id,
    String paymentReference,
    String transactionId,
    BigDecimal amount,
    String formattedAmount,
    PaymentStatus status,
    String statusDisplayName,
    String paymentMethod,
    String qrCodeUrl,
    LocalDateTime paidAt,
    LocalDateTime expiresAt,
    String failureReason,
    BigDecimal refundAmount,
    String formattedRefundAmount,
    LocalDateTime refundedAt,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    OrderSummaryDto order) {

  /**
   * Check if payment is pending.
   *
   * @return true if payment is pending
   */
  public boolean isPending() {
    return status != null && status.isPending();
  }

  /**
   * Check if payment is completed.
   *
   * @return true if payment is completed
   */
  public boolean isCompleted() {
    return status != null && status.isCompleted();
  }

  /**
   * Check if payment has failed.
   *
   * @return true if payment has failed
   */
  public boolean isFailed() {
    return status != null && status.isFailed();
  }

  /**
   * Check if payment is expired.
   *
   * @return true if payment has expired
   */
  public boolean isExpired() {
    return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
  }

  /**
   * Get remaining minutes until expiration.
   *
   * @return remaining minutes, or 0 if expired or no expiration set
   */
  public long getRemainingMinutes() {
    if (expiresAt == null || isExpired()) {
      return 0;
    }
    return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
  }

  /**
   * Check if payment has QR code.
   *
   * @return true if QR code URL is available
   */
  public boolean hasQrCode() {
    return qrCodeUrl != null && !qrCodeUrl.trim().isEmpty();
  }

  /**
   * Check if payment can be cancelled.
   *
   * @return true if payment can be cancelled
   */
  public boolean canBeCancelled() {
    return status != null && status.canBeCancelled();
  }

  /**
   * Check if payment can be refunded.
   *
   * @return true if payment can be refunded
   */
  public boolean canBeRefunded() {
    return status != null && status.canBeRefunded();
  }

  /**
   * Check if payment has been refunded.
   *
   * @return true if payment has refund information
   */
  public boolean hasRefund() {
    return refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Get remaining refundable amount.
   *
   * @return remaining amount that can be refunded
   */
  public BigDecimal getRemainingRefundableAmount() {
    if (!canBeRefunded() || amount == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal refunded = refundAmount != null ? refundAmount : BigDecimal.ZERO;
    return amount.subtract(refunded);
  }

  /** Order summary DTO nested class. */
  public record OrderSummaryDto(
      UUID id,
      String orderNumber,
      String username,
      BigDecimal totalAmount,
      String formattedTotalAmount,
      String status,
      LocalDateTime createdAt) {}
}
