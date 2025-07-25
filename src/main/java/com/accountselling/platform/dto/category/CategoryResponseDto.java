package com.accountselling.platform.dto.category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for category information in API responses. Contains category data with hierarchical
 * information.
 *
 * <p>DTO สำหรับข้อมูลหมวดหมู่ใน API responses ประกอบด้วยข้อมูลหมวดหมู่พร้อมข้อมูลลำดับชั้น
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {

  private UUID id;
  private String name;
  private String description;
  private Boolean active;
  private Integer sortOrder;
  private String fullPath;
  private Integer level;

  // Parent information
  private ParentInfo parent;

  // Children information
  private List<CategoryResponseDto> subcategories;

  // Statistics
  private Long productCount;
  private Long activeProductCount;

  // Audit information
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ParentInfo {
    private UUID id;
    private String name;
    private String fullPath;
  }

  /** Check if this is a root category. ตรวจสอบว่าเป็นหมวดหมู่รากหรือไม่ */
  public boolean isRootCategory() {
    return parent == null;
  }

  /** Check if this category has subcategories. ตรวจสอบว่ามีหมวดหมู่ย่อยหรือไม่ */
  public boolean hasSubcategories() {
    return subcategories != null && !subcategories.isEmpty();
  }

  /** Check if this category has products. ตรวจสอบว่ามีสินค้าในหมวดหมู่นี้หรือไม่ */
  public boolean hasProducts() {
    return productCount != null && productCount > 0;
  }

  /** Get display status for the category. ดึงสถานะการแสดงผลของหมวดหมู่ */
  public String getDisplayStatus() {
    if (!active) {
      return "INACTIVE";
    }
    if (!hasProducts() && !hasSubcategories()) {
      return "EMPTY";
    }
    if (hasProducts() && activeProductCount != null && activeProductCount == 0) {
      return "NO_ACTIVE_PRODUCTS";
    }
    return "ACTIVE";
  }
}
