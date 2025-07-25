package com.accountselling.platform.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for payment refund requests. Contains refund amount and reason for processing payment
 * refunds.
 */
public record RefundRequestDto(
    @NotNull(message = "Refund amount cannot be null")
        @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
        @Digits(
            integer = 10,
            fraction = 2,
            message = "Refund amount must have at most 10 integer digits and 2 decimal places")
        BigDecimal refundAmount,
    @NotBlank(message = "Refund reason cannot be blank") String reason,
    String notes) {

  /**
   * Check if refund is for full amount. This can be determined by comparing with the original
   * payment amount.
   *
   * @param originalAmount the original payment amount
   * @return true if refund amount equals original amount
   */
  public boolean isFullRefund(BigDecimal originalAmount) {
    return originalAmount != null && refundAmount.compareTo(originalAmount) == 0;
  }

  /**
   * Check if refund is partial.
   *
   * @param originalAmount the original payment amount
   * @return true if refund amount is less than original amount
   */
  public boolean isPartialRefund(BigDecimal originalAmount) {
    return originalAmount != null && refundAmount.compareTo(originalAmount) < 0;
  }

  /**
   * Get refund percentage.
   *
   * @param originalAmount the original payment amount
   * @return refund percentage (0-100)
   */
  public double getRefundPercentage(BigDecimal originalAmount) {
    if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) == 0) {
      return 0.0;
    }
    return refundAmount
        .multiply(BigDecimal.valueOf(100))
        .divide(originalAmount, 2, java.math.RoundingMode.HALF_UP)
        .doubleValue();
  }

  /**
   * Validate refund amount against original payment.
   *
   * @param originalAmount the original payment amount
   * @param alreadyRefunded amount already refunded
   * @return true if refund amount is valid
   */
  public boolean isValidRefundAmount(BigDecimal originalAmount, BigDecimal alreadyRefunded) {
    if (originalAmount == null) {
      return false;
    }

    BigDecimal refunded = alreadyRefunded != null ? alreadyRefunded : BigDecimal.ZERO;
    BigDecimal remainingRefundable = originalAmount.subtract(refunded);

    return refundAmount.compareTo(remainingRefundable) <= 0;
  }

  /**
   * Get formatted refund amount for display.
   *
   * @return formatted refund amount
   */
  public String getFormattedRefundAmount() {
    return String.format("฿%.2f", refundAmount);
  }

  /**
   * Check if notes are provided.
   *
   * @return true if notes are provided and not empty
   */
  public boolean hasNotes() {
    return notes != null && !notes.trim().isEmpty();
  }

  /**
   * Get trimmed reason.
   *
   * @return trimmed refund reason
   */
  public String getTrimmedReason() {
    return reason != null ? reason.trim() : null;
  }
}
