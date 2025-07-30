package com.accountselling.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Stock entity representing individual account credentials available for sale. Each stock item
 * contains encrypted credentials for a specific product.
 *
 * <p>เอนทิตี้สต็อกที่แสดงข้อมูลบัญชีแต่ละรายการที่มีจำหน่าย
 * แต่ละรายการสต็อกจะมีข้อมูลบัญชีที่เข้ารหัสแล้วสำหรับสินค้าเฉพาะ
 */
@Entity
@Table(
    name = "stock",
    indexes = {
      @Index(name = "idx_stock_product", columnList = "product_id"),
      @Index(name = "idx_stock_sold", columnList = "sold"),
      @Index(name = "idx_stock_reserved", columnList = "reserved_until"),
      @Index(name = "idx_stock_product_available", columnList = "product_id, sold")
    })
@Getter
@Setter
@NoArgsConstructor
@ToString(
    callSuper = true,
    exclude = {"product", "accountData"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Stock extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  @NotNull(message = "Stock must be associated with a product")
  private Product product;

  @NotBlank(message = "Account data cannot be blank")
  @Size(max = 2000, message = "Account data cannot exceed 2000 characters")
  @Column(name = "account_data", nullable = false, length = 2000)
  @EqualsAndHashCode.Include
  private String accountData;

  @Size(max = 100, message = "Account type cannot exceed 100 characters")
  @Column(name = "account_type", length = 100)
  private String accountType;

  @Column(name = "price")
  private Double price;

  @Column(name = "available", nullable = false)
  private Boolean available = true;

  @Column(name = "sold", nullable = false)
  private Boolean sold = false;

  @Column(name = "reserved_until")
  private LocalDateTime reservedUntil;

  @Size(max = 500, message = "Additional info cannot exceed 500 characters")
  @Column(name = "additional_info", length = 500)
  private String additionalInfo;

  @Column(name = "sold_at")
  private LocalDateTime soldAt;

  // Constructor with product and account data
  public Stock(Product product, String accountData) {
    this.product = product;
    this.accountData = accountData;
  }

  // Constructor with product, account data, and additional info
  public Stock(Product product, String accountData, String additionalInfo) {
    this.product = product;
    this.accountData = accountData;
    this.additionalInfo = additionalInfo;
  }

  // Constructor with all main fields
  public Stock(Product product, String accountData, String accountType, Double price) {
    this.product = product;
    this.accountData = accountData;
    this.accountType = accountType;
    this.price = price;
  }

  // Business logic methods
  public boolean isReserved() {
    return reservedUntil != null && reservedUntil.isAfter(LocalDateTime.now());
  }

  public boolean isAvailable() {
    return available && !sold && !isReserved();
  }

  public boolean isExpiredReservation() {
    return reservedUntil != null && reservedUntil.isBefore(LocalDateTime.now());
  }

  // Reserve this stock item for a specific duration (in minutes)
  public void reserve(int durationMinutes) {
    if (sold) {
      throw new IllegalStateException("Cannot reserve sold stock item");
    }
    this.reservedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
  }

  // Reserve this stock item until a specific time
  public void reserveUntil(LocalDateTime until) {
    if (sold) {
      throw new IllegalStateException("Cannot reserve sold stock item");
    }
    this.reservedUntil = until;
  }

  // Release reservation
  public void releaseReservation() {
    this.reservedUntil = null;
  }

  // Mark as sold
  public void markAsSold() {
    if (sold) {
      throw new IllegalStateException("Stock item is already sold");
    }
    this.sold = true;
    this.available = false;
    this.soldAt = LocalDateTime.now();
    this.reservedUntil = null; // Clear any reservation
  }

  // Mark as unavailable (disable without selling)
  public void markAsUnavailable() {
    this.available = false;
  }

  // Mark as available (re-enable)
  public void markAsAvailable() {
    if (sold) {
      throw new IllegalStateException("Cannot make sold stock item available");
    }
    this.available = true;
  }

  // Get remaining reservation time in minutes
  public long getRemainingReservationMinutes() {
    if (!isReserved()) {
      return 0;
    }
    return java.time.Duration.between(LocalDateTime.now(), reservedUntil).toMinutes();
  }

  // Check if reservation is about to expire (within specified minutes)
  public boolean isReservationExpiringSoon(int withinMinutes) {
    if (!isReserved()) {
      return false;
    }
    LocalDateTime threshold = LocalDateTime.now().plusMinutes(withinMinutes);
    return reservedUntil.isBefore(threshold);
  }

  // Get status as string for display
  public String getStatusDisplay() {
    if (sold) {
      return "SOLD";
    } else if (!available) {
      return "UNAVAILABLE";
    } else if (isReserved()) {
      return "RESERVED";
    } else {
      return "AVAILABLE";
    }
  }

  // Clean up expired reservations
  public void cleanupExpiredReservation() {
    if (isExpiredReservation()) {
      releaseReservation();
    }
  }

  // Get product name for convenience
  public String getProductName() {
    return product != null ? product.getName() : null;
  }

  // Get product category name for convenience
  public String getProductCategoryName() {
    return product != null && product.getCategory() != null
        ? product.getCategory().getName()
        : null;
  }
}
