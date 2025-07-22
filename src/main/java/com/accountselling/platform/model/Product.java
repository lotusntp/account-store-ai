package com.accountselling.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Product entity representing items available for sale in the platform. Contains product
 * information such as name, description, price, and category association.
 *
 * <p>เอนทิตี้สินค้าที่แสดงรายการสินค้าที่มีจำหน่ายในแพลตฟอร์ม ประกอบด้วยข้อมูลสินค้า เช่น ชื่อ
 * คำอธิบาย ราคา และการเชื่อมโยงกับหมวดหมู่
 */
@Entity
@Table(
    name = "products",
    indexes = {
      @Index(name = "idx_product_name", columnList = "name"),
      @Index(name = "idx_product_category", columnList = "category_id"),
      @Index(name = "idx_product_active", columnList = "active"),
      @Index(name = "idx_product_price", columnList = "price"),
      @Index(name = "idx_product_server", columnList = "server")
    })
@Getter
@Setter
@NoArgsConstructor
@ToString(
    callSuper = true,
    exclude = {"category", "stockItems"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Product extends BaseEntity {

  @NotBlank(message = "Product name cannot be blank")
  @Size(max = 200, message = "Product name cannot exceed 200 characters")
  @Column(name = "name", nullable = false, length = 200)
  @EqualsAndHashCode.Include
  private String name;

  @Size(max = 2000, message = "Product description cannot exceed 2000 characters")
  @Column(name = "description", length = 2000)
  private String description;

  @NotNull(message = "Product price cannot be null")
  @DecimalMin(value = "0.01", message = "Product price must be greater than 0")
  @Digits(
      integer = 10,
      fraction = 2,
      message = "Product price must have at most 10 integer digits and 2 decimal places")
  @Column(name = "price", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Size(max = 500, message = "Image URL cannot exceed 500 characters")
  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Size(max = 100, message = "Server name cannot exceed 100 characters")
  @Column(name = "server", length = 100)
  private String server;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Min(value = 0, message = "Sort order cannot be negative")
  @Column(name = "sort_order")
  private Integer sortOrder = 0;

  @Min(value = 0, message = "Low stock threshold cannot be negative")
  @Column(name = "low_stock_threshold")
  private Integer lowStockThreshold = 5;

  // Many-to-one relationship with Category
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  @NotNull(message = "Product must belong to a category")
  private Category category;

  // One-to-many relationship with Stock items
  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Stock> stockItems = new HashSet<>();

  // Constructor with name and price
  public Product(String name, BigDecimal price) {
    this.name = name;
    this.price = price;
  }

  // Constructor with name, price, and category
  public Product(String name, BigDecimal price, Category category) {
    this.name = name;
    this.price = price;
    this.category = category;
  }

  // Constructor with all main fields
  public Product(String name, String description, BigDecimal price, Category category) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.category = category;
  }

  // Helper methods for managing stock items
  public void addStockItem(Stock stockItem) {
    stockItems.add(stockItem);
    stockItem.setProduct(this);
  }

  public void removeStockItem(Stock stockItem) {
    stockItems.remove(stockItem);
    stockItem.setProduct(null);
  }

  // Business logic methods
  public long getAvailableStockCount() {
    return stockItems.stream().filter(stock -> !stock.getSold() && !stock.isReserved()).count();
  }

  public long getTotalStockCount() {
    return stockItems.size();
  }

  public long getSoldStockCount() {
    return stockItems.stream().filter(Stock::getSold).count();
  }

  public long getReservedStockCount() {
    return stockItems.stream().filter(stock -> !stock.getSold() && stock.isReserved()).count();
  }

  public boolean isInStock() {
    return getAvailableStockCount() > 0;
  }

  public boolean isOutOfStock() {
    return getAvailableStockCount() == 0;
  }

  public boolean isLowStock() {
    return getAvailableStockCount() <= lowStockThreshold;
  }

  public boolean isAvailableForPurchase() {
    return active && isInStock();
  }

  // Get next available stock item
  public Stock getNextAvailableStock() {
    return stockItems.stream()
        .filter(stock -> !stock.getSold() && !stock.isReserved())
        .findFirst()
        .orElse(null);
  }

  // Get category path for display
  public String getCategoryPath() {
    return category != null ? category.getFullPath() : "";
  }

  // Format price for display
  public String getFormattedPrice() {
    return String.format("฿%.2f", price);
  }

  // Check if product belongs to specific category or its descendants
  public boolean belongsToCategory(Category targetCategory) {
    if (category == null || targetCategory == null) {
      return false;
    }

    Category current = category;
    while (current != null) {
      if (current.equals(targetCategory)) {
        return true;
      }
      current = current.getParentCategory();
    }
    return false;
  }
}
