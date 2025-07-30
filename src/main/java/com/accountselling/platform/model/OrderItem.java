package com.accountselling.platform.model;

import com.accountselling.platform.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

/**
 * OrderItem entity representing individual items within an order. Links orders to specific products
 * and stock items that were purchased.
 *
 * <p>เอนทิตี้รายการสินค้าในคำสั่งซื้อที่แสดงสินค้าแต่ละรายการในคำสั่งซื้อ
 * เชื่อมโยงคำสั่งซื้อกับสินค้าและรายการสต็อกที่ถูกซื้อ
 */
@Entity
@Table(
    name = "order_items",
    indexes = {
      @Index(name = "idx_order_item_order", columnList = "order_id"),
      @Index(name = "idx_order_item_product", columnList = "product_id"),
      @Index(name = "idx_order_item_stock", columnList = "stock_item_id"),
      @Index(name = "idx_order_item_order_product", columnList = "order_id, product_id")
    })
@Getter
@Setter
@NoArgsConstructor
@ToString(
    callSuper = true,
    exclude = {"order", "product", "stockItem"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class OrderItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  @NotNull(message = "Order item must be associated with an order")
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  @NotNull(message = "Order item must be associated with a product")
  private Product product;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stock_item_id", nullable = false, unique = true)
  @NotNull(message = "Order item must be associated with a stock item")
  @EqualsAndHashCode.Include
  private Stock stockItem;

  @NotNull(message = "Price cannot be null")
  @DecimalMin(value = "0.01", message = "Price must be greater than 0")
  @Digits(
      integer = 10,
      fraction = 2,
      message = "Price must have at most 10 integer digits and 2 decimal places")
  @Column(name = "price", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(name = "product_name", nullable = false, length = 200)
  private String productName;

  @Column(name = "product_description", length = 2000)
  private String productDescription;

  @Column(name = "category_name", length = 100)
  private String categoryName;

  @Column(name = "server", length = 100)
  private String server;

  @Column(name = "notes", length = 500)
  private String notes;

  // Constructor with order, product, stock item, and price
  public OrderItem(Order order, Product product, Stock stockItem, BigDecimal price) {
    this.order = order;
    this.product = product;
    this.stockItem = stockItem;
    this.price = price;
    captureProductSnapshot();
  }

  // Constructor with all main fields
  public OrderItem(Order order, Product product, Stock stockItem, BigDecimal price, String notes) {
    this.order = order;
    this.product = product;
    this.stockItem = stockItem;
    this.price = price;
    this.notes = notes;
    captureProductSnapshot();
  }

  // Capture product information at time of purchase
  private void captureProductSnapshot() {
    if (product != null) {
      this.productName = product.getName();
      this.productDescription = product.getDescription();
      this.server = product.getServer();

      if (product.getCategory() != null) {
        this.categoryName = product.getCategory().getName();
      }
    }
  }

  // Business logic methods
  public boolean isStockItemSold() {
    return stockItem != null && stockItem.getSold();
  }

  public boolean isStockItemAvailable() {
    return stockItem != null && stockItem.isAvailable();
  }

  public String getStockCredentials() {
    return stockItem != null ? stockItem.getAccountData() : null;
  }

  public String getStockAdditionalInfo() {
    return stockItem != null ? stockItem.getAdditionalInfo() : null;
  }

  // Get formatted price for display
  public String getFormattedPrice() {
    return String.format("฿%.2f", price);
  }

  // Get order number for convenience
  public String getOrderNumber() {
    return order != null ? order.getOrderNumber() : null;
  }

  // Get order status for convenience
  public OrderStatus getOrderStatus() {
    return order != null ? order.getStatus() : null;
  }

  // Get username for convenience
  public String getUsername() {
    return order != null && order.getUser() != null ? order.getUser().getUsername() : null;
  }

  // Check if order item belongs to specific user
  public boolean belongsToUser(User user) {
    return order != null && order.belongsToUser(user);
  }

  // Check if order item belongs to user by username
  public boolean belongsToUser(String username) {
    return order != null && order.belongsToUser(username);
  }

  // Get product category path for display
  public String getProductCategoryPath() {
    return product != null ? product.getCategoryPath() : categoryName;
  }

  // Check if the order item is for a specific product
  public boolean isForProduct(Product targetProduct) {
    return product != null && product.equals(targetProduct);
  }

  // Check if the order item is for a product by ID
  public boolean isForProduct(String productId) {
    return product != null && product.getId().toString().equals(productId);
  }

  // Get display information for the item
  public String getDisplayInfo() {
    StringBuilder info = new StringBuilder();
    info.append(productName);

    if (server != null && !server.trim().isEmpty()) {
      info.append(" (").append(server).append(")");
    }

    if (categoryName != null && !categoryName.trim().isEmpty()) {
      info.append(" - ").append(categoryName);
    }

    return info.toString();
  }

  // Validate that stock item belongs to the product
  public boolean isValidStockItemForProduct() {
    return stockItem != null
        && product != null
        && stockItem.getProduct() != null
        && stockItem.getProduct().equals(product);
  }

  // Check if price matches product price at time of purchase
  public boolean isPriceValid() {
    return product != null && price != null && price.compareTo(product.getPrice()) == 0;
  }

  // Get stock item status for display
  public String getStockItemStatus() {
    return stockItem != null ? stockItem.getStatusDisplay() : "Unknown";
  }

  // Mark the associated stock item as sold
  public void markStockItemAsSold() {
    if (stockItem != null && !stockItem.getSold()) {
      stockItem.markAsSold();
    }
  }

  // Release stock item reservation if not sold
  public void releaseStockItemReservation() {
    if (stockItem != null && !stockItem.getSold()) {
      stockItem.releaseReservation();
    }
  }
}
