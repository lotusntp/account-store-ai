package com.accountselling.platform.service;

import com.accountselling.platform.dto.statistics.DailyOrderStatistics;
import com.accountselling.platform.dto.statistics.OrderStatistics;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Order management operations. Provides comprehensive order processing
 * functionality including creation, status management, payment integration, and business logic
 * operations.
 *
 * <p>This service handles: - Order creation with stock reservation - Order status lifecycle
 * management - Integration with payment processing - Order validation and business rules - Order
 * history and reporting
 */
public interface OrderService {

  // ==================== ORDER CREATION ====================

  /**
   * Create a new order for a user with specified products and quantities. This method handles stock
   * reservation and order validation.
   *
   * @param user the user creating the order
   * @param productQuantities map of product IDs to quantities requested
   * @return the created order with reserved stock items
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if user or product not
   *     found
   * @throws com.accountselling.platform.exception.InsufficientStockException if insufficient stock
   *     available
   * @throws com.accountselling.platform.exception.InvalidOrderException if order validation fails
   */
  Order createOrder(User user, Map<UUID, Integer> productQuantities);

  /**
   * Create a new order for a user with a single product. Convenience method for single-product
   * orders.
   *
   * @param user the user creating the order
   * @param productId the product ID to order
   * @param quantity the quantity requested
   * @return the created order with reserved stock item
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if user or product not
   *     found
   * @throws com.accountselling.platform.exception.InsufficientStockException if insufficient stock
   *     available
   * @throws com.accountselling.platform.exception.InvalidOrderException if order validation fails
   */
  Order createOrder(User user, UUID productId, int quantity);

  /**
   * Create a new order by username with specified products and quantities.
   *
   * @param username the username of the user creating the order
   * @param productQuantities map of product IDs to quantities requested
   * @return the created order with reserved stock items
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if user or product not
   *     found
   * @throws com.accountselling.platform.exception.InsufficientStockException if insufficient stock
   *     available
   * @throws com.accountselling.platform.exception.InvalidOrderException if order validation fails
   */
  Order createOrderByUsername(String username, Map<UUID, Integer> productQuantities);

  // ==================== ORDER RETRIEVAL ====================

  /**
   * Find order by ID.
   *
   * @param orderId the order ID to search for
   * @return the order if found
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   */
  Order findById(UUID orderId);

  /**
   * Find order by order number.
   *
   * @param orderNumber the order number to search for
   * @return the order if found
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   */
  Order findByOrderNumber(String orderNumber);

  /**
   * Get all orders for a user with pagination.
   *
   * @param user the user to get orders for
   * @param pageable pagination parameters
   * @return page of orders for the user
   */
  Page<Order> getOrdersByUser(User user, Pageable pageable);

  /**
   * Get all orders for a user by username with pagination.
   *
   * @param username the username to get orders for
   * @param pageable pagination parameters
   * @return page of orders for the user
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if user not found
   */
  Page<Order> getOrdersByUsername(String username, Pageable pageable);

  /**
   * Get orders by status with pagination.
   *
   * @param status the order status to filter by
   * @param pageable pagination parameters
   * @return page of orders with the specified status
   */
  Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable);

  /**
   * Get orders by user and status.
   *
   * @param user the user to get orders for
   * @param status the order status to filter by
   * @return list of orders for the user with the specified status
   */
  List<Order> getOrdersByUserAndStatus(User user, OrderStatus status);

  // ==================== ORDER STATUS MANAGEMENT ====================

  /**
   * Mark order as processing. This typically happens when payment is initiated.
   *
   * @param orderId the order ID to update
   * @return the updated order
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   * @throws com.accountselling.platform.exception.InvalidOrderStatusException if status transition
   *     is invalid
   */
  Order markOrderAsProcessing(UUID orderId);

  /**
   * Mark order as completed. This happens when payment is confirmed and stock items are delivered.
   *
   * @param orderId the order ID to update
   * @return the updated order
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   * @throws com.accountselling.platform.exception.InvalidOrderStatusException if status transition
   *     is invalid
   */
  Order markOrderAsCompleted(UUID orderId);

  /**
   * Mark order as failed. This happens when payment fails or other processing errors occur.
   *
   * @param orderId the order ID to update
   * @param reason the reason for failure
   * @return the updated order
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   * @throws com.accountselling.platform.exception.InvalidOrderStatusException if status transition
   *     is invalid
   */
  Order markOrderAsFailed(UUID orderId, String reason);

  /**
   * Cancel order. This releases reserved stock items and cancels associated payment.
   *
   * @param orderId the order ID to cancel
   * @param reason the reason for cancellation
   * @return the updated order
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   * @throws com.accountselling.platform.exception.InvalidOrderStatusException if order cannot be
   *     cancelled
   */
  Order cancelOrder(UUID orderId, String reason);

  /**
   * Cancel order by user. Allows users to cancel their own orders with validation.
   *
   * @param orderId the order ID to cancel
   * @param user the user requesting cancellation
   * @param reason the reason for cancellation
   * @return the updated order
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   * @throws com.accountselling.platform.exception.UnauthorizedException if user doesn't own the
   *     order
   * @throws com.accountselling.platform.exception.InvalidOrderStatusException if order cannot be
   *     cancelled
   */
  Order cancelOrderByUser(UUID orderId, User user, String reason);

  // ==================== ORDER VALIDATION ====================

  /**
   * Validate order before creation. Checks product availability, user eligibility, and business
   * rules.
   *
   * @param user the user creating the order
   * @param productQuantities map of product IDs to quantities requested
   * @throws com.accountselling.platform.exception.InvalidOrderException if validation fails
   */
  void validateOrder(User user, Map<UUID, Integer> productQuantities);

  /**
   * Check if user can create orders. Validates user status and restrictions.
   *
   * @param user the user to check
   * @return true if user can create orders
   */
  boolean canUserCreateOrders(User user);

  /**
   * Check if order can be cancelled.
   *
   * @param orderId the order ID to check
   * @return true if order can be cancelled
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   */
  boolean canOrderBeCancelled(UUID orderId);

  /**
   * Check if order belongs to user.
   *
   * @param orderId the order ID to check
   * @param user the user to verify ownership
   * @return true if order belongs to user
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   */
  boolean doesOrderBelongToUser(UUID orderId, User user);

  // ==================== ORDER BUSINESS LOGIC ====================

  /**
   * Calculate total amount for products and quantities. Used for order total calculation before
   * creation.
   *
   * @param productQuantities map of product IDs to quantities
   * @return the total amount for the order
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if product not found
   */
  BigDecimal calculateOrderTotal(Map<UUID, Integer> productQuantities);

  /**
   * Get available quantity for a product. Returns the number of available stock items for a
   * product.
   *
   * @param productId the product ID to check
   * @return the available quantity
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if product not found
   */
  int getAvailableQuantityForProduct(UUID productId);

  /**
   * Reserve stock items for an order. Internal method for stock reservation during order creation.
   *
   * @param order the order to reserve stock for
   * @param productQuantities map of product IDs to quantities
   * @throws com.accountselling.platform.exception.InsufficientStockException if insufficient stock
   */
  void reserveStockForOrder(Order order, Map<UUID, Integer> productQuantities);

  /**
   * Release stock reservations for cancelled order. Releases all stock items reserved for a
   * cancelled order.
   *
   * @param orderId the order ID to release stock for
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   */
  void releaseStockReservations(UUID orderId);

  /**
   * Mark stock items as sold for completed order. Marks all stock items in an order as sold when
   * payment is completed.
   *
   * @param orderId the order ID to mark stock as sold
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   */
  void markStockItemsAsSold(UUID orderId);

  // ==================== ORDER SEARCH AND REPORTING ====================

  /**
   * Search orders with multiple criteria.
   *
   * @param orderNumber order number pattern (can be null)
   * @param username username pattern (can be null)
   * @param status order status (can be null)
   * @param startDate start date (can be null)
   * @param endDate end date (can be null)
   * @param minAmount minimum amount (can be null)
   * @param maxAmount maximum amount (can be null)
   * @param pageable pagination parameters
   * @return page of orders matching the criteria
   */
  Page<Order> searchOrders(
      String orderNumber,
      String username,
      OrderStatus status,
      LocalDateTime startDate,
      LocalDateTime endDate,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      Pageable pageable);

  /**
   * Get order statistics for dashboard.
   *
   * @return order statistics including counts by status
   */
  OrderStatistics getOrderStatistics();

  /**
   * Get order statistics within date range.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return order statistics for the specified period
   */
  OrderStatistics getOrderStatistics(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Get daily order statistics within date range.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of daily order statistics
   */
  List<DailyOrderStatistics> getDailyOrderStatistics(
      LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Get top customers by order count.
   *
   * @param limit maximum number of customers to return
   * @return list of top customers by order count
   */
  List<User> getTopCustomersByOrderCount(int limit);

  /**
   * Get recent orders for dashboard.
   *
   * @param limit maximum number of orders to return
   * @return list of recent orders
   */
  List<Order> getRecentOrders(int limit);

  // ==================== ORDER INTEGRATION ====================

  /**
   * Process order completion from payment service. Called when payment is successfully completed.
   *
   * @param orderId the order ID to complete
   * @param paymentTransactionId the payment transaction ID
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   * @throws com.accountselling.platform.exception.InvalidOrderStatusException if order cannot be
   *     completed
   */
  void processOrderCompletion(UUID orderId, String paymentTransactionId);

  /**
   * Process order failure from payment service. Called when payment fails.
   *
   * @param orderId the order ID to mark as failed
   * @param failureReason the reason for failure
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   */
  void processOrderFailure(UUID orderId, String failureReason);

  /**
   * Get order download information for completed order. Returns stock credentials and information
   * for completed orders.
   *
   * @param orderId the order ID to get download info for
   * @param user the user requesting download (for authorization)
   * @return map of product names to their credentials/info
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if order not found
   * @throws com.accountselling.platform.exception.UnauthorizedException if user doesn't own the
   *     order
   * @throws com.accountselling.platform.exception.InvalidOrderStatusException if order is not
   *     completed
   */
  Map<String, String> getOrderDownloadInfo(UUID orderId, User user);
}
