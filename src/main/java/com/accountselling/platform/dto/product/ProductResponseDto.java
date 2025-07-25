package com.accountselling.platform.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for product information in API responses. Contains essential product data without sensitive
 * information.
 *
 * <p>DTO สำหรับข้อมูลสินค้าใน API responses ประกอบด้วยข้อมูลสินค้าสำคัญโดยไม่มีข้อมูลที่เป็นความลับ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

  private UUID id;
  private String name;
  private String description;
  private BigDecimal price;
  private String formattedPrice;
  private String imageUrl;
  private String server;
  private Boolean active;
  private Integer sortOrder;

  // Category information
  private CategoryInfo category;

  // Stock information
  private StockInfo stock;

  // Audit information
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CategoryInfo {
    private UUID id;
    private String name;
    private String fullPath;
    private Boolean active;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StockInfo {
    private Long totalStock;
    private Long availableStock;
    private Long soldStock;
    private Long reservedStock;
    private Boolean inStock;
    private Boolean lowStock;
    private Integer lowStockThreshold;
  }

  /** Check if product is available for purchase. ตรวจสอบว่าสินค้าพร้อมจำหน่ายหรือไม่ */
  public boolean isAvailableForPurchase() {
    return active && stock != null && stock.inStock;
  }

  /** Get display status for the product. ดึงสถานะการแสดงผลของสินค้า */
  public String getDisplayStatus() {
    if (!active) {
      return "INACTIVE";
    }
    if (stock == null) {
      return "NO_STOCK_INFO";
    }
    if (!stock.inStock) {
      return "OUT_OF_STOCK";
    }
    if (stock.lowStock) {
      return "LOW_STOCK";
    }
    return "AVAILABLE";
  }
}
