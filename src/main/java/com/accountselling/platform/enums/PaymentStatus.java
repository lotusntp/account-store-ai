package com.accountselling.platform.enums;

/**
 * Payment status enumeration for tracking payment lifecycle. Represents different states a payment
 * can be in during processing.
 *
 * <p>enum สถานะการชำระเงินสำหรับติดตามวงจรชีวิตของการชำระเงิน แสดงสถานะต่างๆ
 * ที่การชำระเงินสามารถอยู่ได้ระหว่างการประมวลผล
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

  /**
   * Get the English display name for the status.
   *
   * @return English display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Get the Thai display name for the status.
   *
   * @return Thai display name
   */
  public String getDisplayNameTh() {
    return displayNameTh;
  }

  /**
   * Check if the status represents a pending state.
   *
   * @return true if status is PENDING
   */
  public boolean isPending() {
    return this == PENDING;
  }

  /**
   * Check if the status represents a processing state.
   *
   * @return true if status is PROCESSING
   */
  public boolean isProcessing() {
    return this == PROCESSING;
  }

  /**
   * Check if the status represents a completed state.
   *
   * @return true if status is COMPLETED
   */
  public boolean isCompleted() {
    return this == COMPLETED;
  }

  /**
   * Check if the status represents a failed state.
   *
   * @return true if status is FAILED
   */
  public boolean isFailed() {
    return this == FAILED;
  }

  /**
   * Check if the status represents a cancelled state.
   *
   * @return true if status is CANCELLED
   */
  public boolean isCancelled() {
    return this == CANCELLED;
  }

  /**
   * Check if the status represents a refunded state.
   *
   * @return true if status is REFUNDED
   */
  public boolean isRefunded() {
    return this == REFUNDED;
  }

  /**
   * Check if the payment can be cancelled from this status.
   *
   * @return true if payment can be cancelled
   */
  public boolean canBeCancelled() {
    return this == PENDING || this == PROCESSING;
  }

  /**
   * Check if the payment can be refunded from this status.
   *
   * @return true if payment can be refunded
   */
  public boolean canBeRefunded() {
    return this == COMPLETED;
  }

  /**
   * Check if the status represents a successful payment.
   *
   * @return true if payment was successful
   */
  public boolean isSuccessful() {
    return this == COMPLETED || this == REFUNDED;
  }

  /**
   * Check if the status represents a final state (cannot be changed).
   *
   * @return true if status is final
   */
  public boolean isFinalStatus() {
    return this == COMPLETED || this == FAILED || this == CANCELLED || this == REFUNDED;
  }

  /**
   * Check if the status represents an active state (payment is being processed).
   *
   * @return true if status is active
   */
  public boolean isActiveStatus() {
    return this == PENDING || this == PROCESSING;
  }

  /**
   * Check if the status represents an unsuccessful payment.
   *
   * @return true if payment was unsuccessful
   */
  public boolean isUnsuccessful() {
    return this == FAILED || this == CANCELLED;
  }

  /**
   * Get the next valid status transitions from current status.
   *
   * @return array of valid next statuses
   */
  public PaymentStatus[] getValidTransitions() {
    return switch (this) {
      case PENDING -> new PaymentStatus[] {PROCESSING, COMPLETED, FAILED, CANCELLED};
      case PROCESSING -> new PaymentStatus[] {COMPLETED, FAILED, CANCELLED};
      case COMPLETED -> new PaymentStatus[] {REFUNDED};
      case FAILED, CANCELLED, REFUNDED -> new PaymentStatus[] {};
    };
  }

  /**
   * Check if transition to target status is valid.
   *
   * @param targetStatus the target status to transition to
   * @return true if transition is valid
   */
  public boolean canTransitionTo(PaymentStatus targetStatus) {
    PaymentStatus[] validTransitions = getValidTransitions();
    for (PaymentStatus validStatus : validTransitions) {
      if (validStatus == targetStatus) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get status priority for sorting (lower number = higher priority).
   *
   * @return priority number
   */
  public int getPriority() {
    return switch (this) {
      case PENDING -> 1;
      case PROCESSING -> 2;
      case COMPLETED -> 3;
      case REFUNDED -> 4;
      case FAILED -> 5;
      case CANCELLED -> 6;
    };
  }
}
