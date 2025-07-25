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
    exclude = {"product", "credentials"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Stock extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  @NotNull(message = "Stock must be associated with a product")
  private Product product;

  @NotBlank(message = "Credentials cannot be blank")
  @Size(max = 1000, message = "Credentials cannot exceed 1000 characters")
  @Column(name = "credentials", nullable = false, length = 1000)
  @EqualsAndHashCode.Include
  private String credentials;

  @Column(name = "sold", nullable = false)
  private Boolean sold = false;

  @Column(name = "reserved_until")
  private LocalDateTime reservedUntil;

  @Size(max = 500, message = "Additional info cannot exceed 500 characters")
  @Column(name = "additional_info", length = 500)
  private String additionalInfo;

  @Column(name = "sold_at")
  private LocalDateTime soldAt;

  // Constructor with product and credentials
  public Stock(Product product, String credentials) {
    this.product = product;
    this.credentials = credentials;
  }

  // Constructor with product, credentials, and additional info
  public Stock(Product product, String credentials, String additionalInfo) {
    this.product = product;
    this.credentials = credentials;
    this.additionalInfo = additionalInfo;
  }

  // Business logic methods
  public boolean isReserved() {
    return reservedUntil != null && reservedUntil.isAfter(LocalDateTime.now());
  }

  public boolean isAvailable() {
    return !sold && !isReserved();
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
    this.soldAt = LocalDateTime.now();
    this.reservedUntil = null; // Clear any reservation
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
