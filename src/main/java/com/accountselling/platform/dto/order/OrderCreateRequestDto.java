package com.accountselling.platform.dto.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for order creation requests. Contains product quantities and validation rules for creating
 * new orders.
 */
public record OrderCreateRequestDto(
    @NotEmpty(message = "Product quantities cannot be empty")
        Map<
                @NotNull(message = "Product ID cannot be null") UUID,
                @NotNull(message = "Quantity cannot be null")
                @Positive(message = "Quantity must be positive") Integer>
            productQuantities,
    String notes) {

  /**
   * Get total number of items in the order.
   *
   * @return total quantity of all products
   */
  public int getTotalQuantity() {
    return productQuantities.values().stream().mapToInt(Integer::intValue).sum();
  }

  /**
   * Get number of different products in the order.
   *
   * @return number of unique products
   */
  public int getUniqueProductCount() {
    return productQuantities.size();
  }

  /**
   * Check if order contains a specific product.
   *
   * @param productId the product ID to check
   * @return true if order contains the product
   */
  public boolean containsProduct(UUID productId) {
    return productQuantities.containsKey(productId);
  }

  /**
   * Get quantity for a specific product.
   *
   * @param productId the product ID to get quantity for
   * @return quantity for the product, or 0 if not found
   */
  public int getQuantityForProduct(UUID productId) {
    return productQuantities.getOrDefault(productId, 0);
  }
}
