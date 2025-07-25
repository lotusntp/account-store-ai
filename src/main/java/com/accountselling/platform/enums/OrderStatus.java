package com.accountselling.platform.enums;

/**
 * Order status enumeration for tracking order lifecycle. Represents different states an order can
 * be in during processing.
 *
 * <p>enum สถานะคำสั่งซื้อสำหรับติดตามวงจรชีวิตของคำสั่งซื้อ แสดงสถานะต่างๆ
 * ที่คำสั่งซื้อสามารถอยู่ได้ระหว่างการประมวลผล
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
   * Check if the order can be cancelled from this status.
   *
   * @return true if order can be cancelled
   */
  public boolean canBeCancelled() {
    return this == PENDING || this == PROCESSING;
  }

  /**
   * Check if the order can be refunded from this status.
   *
   * @return true if order can be refunded
   */
  public boolean canBeRefunded() {
    return this == COMPLETED;
  }

  /**
   * Check if the status represents a final state (cannot be changed).
   *
   * @return true if status is final
   */
  public boolean isFinalStatus() {
    return this == COMPLETED || this == FAILED || this == CANCELLED;
  }

  /**
   * Check if the status represents an active state (order is being processed).
   *
   * @return true if status is active
   */
  public boolean isActiveStatus() {
    return this == PENDING || this == PROCESSING;
  }

  /**
   * Get the next valid status transitions from current status.
   *
   * @return array of valid next statuses
   */
  public OrderStatus[] getValidTransitions() {
    return switch (this) {
      case PENDING -> new OrderStatus[] {PROCESSING, COMPLETED, FAILED, CANCELLED};
      case PROCESSING -> new OrderStatus[] {COMPLETED, FAILED, CANCELLED};
      case COMPLETED, FAILED, CANCELLED -> new OrderStatus[] {};
    };
  }

  /**
   * Check if transition to target status is valid.
   *
   * @param targetStatus the target status to transition to
   * @return true if transition is valid
   */
  public boolean canTransitionTo(OrderStatus targetStatus) {
    OrderStatus[] validTransitions = getValidTransitions();
    for (OrderStatus validStatus : validTransitions) {
      if (validStatus == targetStatus) {
        return true;
      }
    }
    return false;
  }
}
