package com.accountselling.platform.repository;

import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Order entity operations. Provides comprehensive data access methods for
 * order management including user orders, status tracking, reporting, and analytics.
 *
 * <p>รีพอสิทอรี่สำหรับจัดการข้อมูลคำสั่งซื้อ รองรับการจัดการคำสั่งซื้อของผู้ใช้ การติดตามสถานะ
 * การรายงาน และการวิเคราะห์
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

  // ==================== BASIC ORDER QUERIES ====================

  /**
   * Find order by order number. Used for order lookup and tracking.
   *
   * @param orderNumber the order number to search for
   * @return Optional containing the order if found
   */
  Optional<Order> findByOrderNumber(String orderNumber);

  /**
   * Check if order number exists. Used for order number validation and uniqueness checks.
   *
   * @param orderNumber the order number to check
   * @return true if order number exists, false otherwise
   */
  boolean existsByOrderNumber(String orderNumber);

  /**
   * Find orders by user. Used for displaying user's order history.
   *
   * @param user the user to search orders for
   * @return list of orders for the user
   */
  List<Order> findByUser(User user);

  /**
   * Find orders by user with pagination. Used for paginated user order history.
   *
   * @param user the user to search orders for
   * @param pageable pagination parameters
   * @return page of orders for the user
   */
  Page<Order> findByUser(User user, Pageable pageable);

  /**
   * Find orders by user ID. Used for user-specific order management.
   *
   * @param userId the user ID to search orders for
   * @return list of orders for the user
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
  List<Order> findByUserId(@Param("userId") UUID userId);

  /**
   * Find orders by user ID with pagination. Used for paginated user order management.
   *
   * @param userId the user ID to search orders for
   * @param pageable pagination parameters
   * @return page of orders for the user
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
  Page<Order> findByUserId(@Param("userId") UUID userId, Pageable pageable);

  // ==================== STATUS-BASED QUERIES ====================

  /**
   * Find orders by status. Used for status-based order management and reporting.
   *
   * @param status the order status to filter by
   * @return list of orders with the specified status
   */
  List<Order> findByStatus(OrderStatus status);

  /**
   * Find orders by status with pagination. Used for paginated status-based order management.
   *
   * @param status the order status to filter by
   * @param pageable pagination parameters
   * @return page of orders with the specified status
   */
  Page<Order> findByStatus(OrderStatus status, Pageable pageable);

  /**
   * Find orders by user and status. Used for user-specific status-based order filtering.
   *
   * @param user the user to search orders for
   * @param status the order status to filter by
   * @return list of orders for the user with the specified status
   */
  List<Order> findByUserAndStatus(User user, OrderStatus status);

  /**
   * Find orders by user and status with pagination. Used for paginated user-specific status-based
   * order filtering.
   *
   * @param user the user to search orders for
   * @param status the order status to filter by
   * @param pageable pagination parameters
   * @return page of orders for the user with the specified status
   */
  Page<Order> findByUserAndStatus(User user, OrderStatus status, Pageable pageable);

  /**
   * Find pending orders. Used for processing pending orders and payment tracking.
   *
   * @return list of pending orders
   */
  @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
  List<Order> findPendingOrders();

  /**
   * Find completed orders. Used for sales reporting and analytics.
   *
   * @return list of completed orders
   */
  @Query("SELECT o FROM Order o WHERE o.status = 'COMPLETED' ORDER BY o.createdAt DESC")
  List<Order> findCompletedOrders();

  /**
   * Find failed orders. Used for failure analysis and customer support.
   *
   * @return list of failed orders
   */
  @Query("SELECT o FROM Order o WHERE o.status = 'FAILED' ORDER BY o.createdAt DESC")
  List<Order> findFailedOrders();

  // ==================== DATE-BASED QUERIES ====================

  /**
   * Find orders created within date range. Used for reporting and analytics within specific
   * periods.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of orders created within the date range
   */
  @Query(
      "SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY"
          + " o.createdAt DESC")
  List<Order> findOrdersBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Find orders created within date range with pagination. Used for paginated reporting within
   * specific periods.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @param pageable pagination parameters
   * @return page of orders created within the date range
   */
  @Query(
      "SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY"
          + " o.createdAt DESC")
  Page<Order> findOrdersBetweenDates(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  /**
   * Find completed orders within date range. Used for sales reporting within specific periods.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of completed orders within the date range
   */
  @Query(
      "SELECT o FROM Order o WHERE o.status = 'COMPLETED' AND o.createdAt >= :startDate AND"
          + " o.createdAt <= :endDate ORDER BY o.createdAt DESC")
  List<Order> findCompletedOrdersBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Find orders by user within date range. Used for user-specific order history within periods.
   *
   * @param user the user to search orders for
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of orders for the user within the date range
   */
  @Query(
      "SELECT o FROM Order o WHERE o.user = :user AND o.createdAt >= :startDate AND o.createdAt <="
          + " :endDate ORDER BY o.createdAt DESC")
  List<Order> findOrdersByUserBetweenDates(
      @Param("user") User user,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // ==================== AMOUNT-BASED QUERIES ====================

  /**
   * Find orders by total amount range. Used for amount-based filtering and analysis.
   *
   * @param minAmount minimum total amount (inclusive)
   * @param maxAmount maximum total amount (inclusive)
   * @return list of orders within the specified amount range
   */
  @Query(
      "SELECT o FROM Order o WHERE o.totalAmount >= :minAmount AND o.totalAmount <= :maxAmount"
          + " ORDER BY o.totalAmount DESC")
  List<Order> findOrdersByAmountRange(
      @Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);

  /**
   * Find orders above specified amount. Used for high-value order tracking.
   *
   * @param amount the minimum amount threshold
   * @return list of orders with total amount above the threshold
   */
  @Query("SELECT o FROM Order o WHERE o.totalAmount > :amount ORDER BY o.totalAmount DESC")
  List<Order> findOrdersAboveAmount(@Param("amount") BigDecimal amount);

  // ==================== COUNTING QUERIES ====================

  /**
   * Count orders by user. Used for user statistics and analytics.
   *
   * @param user the user to count orders for
   * @return count of orders for the user
   */
  long countByUser(User user);

  /**
   * Count orders by status. Used for status-based statistics.
   *
   * @param status the order status to count
   * @return count of orders with the specified status
   */
  long countByStatus(OrderStatus status);

  /**
   * Count orders by user and status. Used for user-specific status statistics.
   *
   * @param user the user to count orders for
   * @param status the order status to count
   * @return count of orders for the user with the specified status
   */
  long countByUserAndStatus(User user, OrderStatus status);

  /**
   * Count orders created within date range. Used for period-based statistics.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return count of orders created within the date range
   */
  @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
  long countOrdersBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Count completed orders within date range. Used for sales statistics within periods.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return count of completed orders within the date range
   */
  @Query(
      "SELECT COUNT(o) FROM Order o WHERE o.status = 'COMPLETED' AND o.createdAt >= :startDate AND"
          + " o.createdAt <= :endDate")
  long countCompletedOrdersBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  // ==================== AGGREGATION QUERIES ====================

  /**
   * Calculate total revenue from completed orders. Used for revenue reporting and analytics.
   *
   * @return total revenue from all completed orders
   */
  @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED'")
  BigDecimal calculateTotalRevenue();

  /**
   * Calculate total revenue within date range. Used for period-based revenue reporting.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return total revenue within the date range
   */
  @Query(
      "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED' AND"
          + " o.createdAt >= :startDate AND o.createdAt <= :endDate")
  BigDecimal calculateRevenueBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Calculate average order value. Used for business analytics.
   *
   * @return average order value from completed orders
   */
  @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED'")
  BigDecimal calculateAverageOrderValue();

  /**
   * Calculate total revenue by user. Used for customer value analysis.
   *
   * @param user the user to calculate revenue for
   * @return total revenue from the user's completed orders
   */
  @Query(
      "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user = :user AND o.status ="
          + " 'COMPLETED'")
  BigDecimal calculateRevenueByUser(@Param("user") User user);

  // ==================== ADVANCED QUERIES ====================

  /**
   * Find recent orders for dashboard. Used for admin dashboard recent activity display.
   *
   * @param limit maximum number of orders to return
   * @return list of recent orders
   */
  @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC LIMIT :limit")
  List<Order> findRecentOrders(@Param("limit") int limit);

  /**
   * Find top customers by order count. Used for customer analytics and loyalty programs.
   *
   * @param limit maximum number of customers to return
   * @return list of users with highest order counts
   */
  @Query(
      "SELECT o.user FROM Order o WHERE o.status = 'COMPLETED' GROUP BY o.user ORDER BY COUNT(o)"
          + " DESC LIMIT :limit")
  List<User> findTopCustomersByOrderCount(@Param("limit") int limit);

  /**
   * Find top customers by revenue. Used for customer value analysis.
   *
   * @param limit maximum number of customers to return
   * @return list of users with highest total revenue
   */
  @Query(
      "SELECT o.user FROM Order o WHERE o.status = 'COMPLETED' GROUP BY o.user ORDER BY"
          + " SUM(o.totalAmount) DESC LIMIT :limit")
  List<User> findTopCustomersByRevenue(@Param("limit") int limit);

  /**
   * Find orders with payment issues. Used for identifying orders that need attention.
   *
   * @return list of orders with payment-related issues
   */
  @Query(
      "SELECT o FROM Order o WHERE o.status IN ('PENDING', 'PROCESSING') AND o.createdAt <"
          + " :threshold ORDER BY o.createdAt ASC")
  List<Order> findOrdersWithPaymentIssues(@Param("threshold") LocalDateTime threshold);

  /**
   * Find orders by username. Used for admin order search functionality.
   *
   * @param username the username to search orders for
   * @return list of orders for the specified username
   */
  @Query("SELECT o FROM Order o WHERE o.user.username = :username ORDER BY o.createdAt DESC")
  List<Order> findOrdersByUsername(@Param("username") String username);

  /**
   * Find orders by username with pagination. Used for paginated admin order search.
   *
   * @param username the username to search orders for
   * @param pageable pagination parameters
   * @return page of orders for the specified username
   */
  @Query("SELECT o FROM Order o WHERE o.user.username = :username ORDER BY o.createdAt DESC")
  Page<Order> findOrdersByUsername(@Param("username") String username, Pageable pageable);

  /**
   * Search orders by multiple criteria. Used for advanced order search with multiple filters.
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
  @Query(
      """
      SELECT o FROM Order o
      WHERE (:orderNumber IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%')))
      AND (:username IS NULL OR LOWER(o.user.username) LIKE LOWER(CONCAT('%', :username, '%')))
      AND (:status IS NULL OR o.status = :status)
      AND (:startDate IS NULL OR o.createdAt >= :startDate)
      AND (:endDate IS NULL OR o.createdAt <= :endDate)
      AND (:minAmount IS NULL OR o.totalAmount >= :minAmount)
      AND (:maxAmount IS NULL OR o.totalAmount <= :maxAmount)
      ORDER BY o.createdAt DESC
      """)
  Page<Order> searchOrders(
      @Param("orderNumber") String orderNumber,
      @Param("username") String username,
      @Param("status") OrderStatus status,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("minAmount") BigDecimal minAmount,
      @Param("maxAmount") BigDecimal maxAmount,
      Pageable pageable);

  /**
   * Get order statistics for dashboard. Used for admin dashboard statistics display.
   *
   * @return object array containing [total, pending, completed, failed] counts
   */
  @Query(
      """
      SELECT
          COUNT(o) as total,
          SUM(CASE WHEN o.status = 'PENDING' THEN 1 ELSE 0 END) as pending,
          SUM(CASE WHEN o.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
          SUM(CASE WHEN o.status = 'FAILED' THEN 1 ELSE 0 END) as failed
      FROM Order o
      """)
  Object[] getOrderStatistics();

  /**
   * Get daily order statistics within date range. Used for daily sales reporting and charts.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of daily statistics [date, count, revenue]
   */
  @Query(
      """
      SELECT
          DATE(o.createdAt) as date,
          COUNT(o) as orderCount,
          COALESCE(SUM(CASE WHEN o.status = 'COMPLETED' THEN o.totalAmount ELSE 0 END), 0) as revenue
      FROM Order o
      WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate
      GROUP BY DATE(o.createdAt)
      ORDER BY DATE(o.createdAt)
      """)
  List<Object[]> getDailyOrderStatistics(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  // ==================== ADDITIONAL METHODS FOR TESTS ====================

  /**
   * Find orders by status in list. Used for multi-status filtering.
   *
   * @param statuses list of order statuses to filter by
   * @return list of orders with any of the specified statuses
   */
  List<Order> findByStatusIn(List<OrderStatus> statuses);

  /**
   * Find orders created between dates. Used for date range filtering.
   *
   * @param startDate start date
   * @param endDate end date
   * @return list of orders created between dates
   */
  List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Find orders by user and created between dates. Used for user-specific date range filtering.
   *
   * @param user the user
   * @param startDate start date
   * @param endDate end date
   * @return list of orders for user created between dates
   */
  List<Order> findByUserAndCreatedAtBetween(
      User user, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Find orders by status and created between dates. Used for status-specific date range filtering.
   *
   * @param status the order status
   * @param startDate start date
   * @param endDate end date
   * @return list of orders with status created between dates
   */
  List<Order> findByStatusAndCreatedAtBetween(
      OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Find orders by total amount between range. Used for amount range filtering.
   *
   * @param minAmount minimum amount
   * @param maxAmount maximum amount
   * @return list of orders with total amount in range
   */
  List<Order> findByTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

  /**
   * Find orders by total amount greater than. Used for minimum amount filtering.
   *
   * @param amount minimum amount threshold
   * @return list of orders with total amount greater than threshold
   */
  List<Order> findByTotalAmountGreaterThan(BigDecimal amount);

  /**
   * Find orders by total amount less than. Used for maximum amount filtering.
   *
   * @param amount maximum amount threshold
   * @return list of orders with total amount less than threshold
   */
  List<Order> findByTotalAmountLessThan(BigDecimal amount);

  /**
   * Count orders created between dates. Used for date range statistics.
   *
   * @param startDate start date
   * @param endDate end date
   * @return count of orders created between dates
   */
  long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Calculate total amount by user. Used for user spending analysis.
   *
   * @param user the user
   * @return total amount from user's orders
   */
  @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user = :user")
  BigDecimal calculateTotalAmountByUser(@Param("user") User user);

  /**
   * Calculate completed amount by user. Used for user completed spending analysis.
   *
   * @param user the user
   * @return total amount from user's completed orders
   */
  @Query(
      "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user = :user AND o.status ="
          + " 'COMPLETED'")
  BigDecimal calculateCompletedAmountByUser(@Param("user") User user);

  /**
   * Calculate total amount by status. Used for status-based financial analysis.
   *
   * @param status the order status
   * @return total amount from orders with specified status
   */
  @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
  BigDecimal calculateTotalAmountByStatus(@Param("status") OrderStatus status);

  /**
   * Calculate total amount between dates. Used for period-based financial analysis.
   *
   * @param startDate start date
   * @param endDate end date
   * @return total amount from orders created between dates
   */
  @Query(
      "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :startDate AND"
          + " o.createdAt <= :endDate")
  BigDecimal calculateTotalAmountBetween(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Calculate average amount by user. Used for user spending pattern analysis.
   *
   * @param user the user
   * @return average amount from user's orders
   */
  @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.user = :user")
  BigDecimal calculateAverageAmountByUser(@Param("user") User user);

  /**
   * Get order statistics by status. Used for comprehensive status analysis.
   *
   * @return list of statistics [status, count, totalAmount]
   */
  @Query(
      """
      SELECT
          o.status as status,
          COUNT(o) as count,
          COALESCE(SUM(o.totalAmount), 0) as totalAmount
      FROM Order o
      GROUP BY o.status
      ORDER BY COUNT(o) DESC
      """)
  List<Object[]> getOrderStatisticsByStatus();

  /**
   * Get order statistics by status between dates. Used for period-based status analysis.
   *
   * @param startDate start date
   * @param endDate end date
   * @return list of statistics [status, count, totalAmount]
   */
  @Query(
      """
      SELECT
          o.status as status,
          COUNT(o) as count,
          COALESCE(SUM(o.totalAmount), 0) as totalAmount
      FROM Order o
      WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate
      GROUP BY o.status
      ORDER BY COUNT(o) DESC
      """)
  List<Object[]> getOrderStatisticsByStatusBetween(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Get top spending users. Used for customer value analysis.
   *
   * @param pageable pagination parameters
   * @return page of top spending users [user, orderCount, totalSpent]
   */
  @Query(
      """
      SELECT
          o.user as user,
          COUNT(o) as orderCount,
          COALESCE(SUM(o.totalAmount), 0) as totalSpent
      FROM Order o
      WHERE o.status = 'COMPLETED'
      GROUP BY o.user
      ORDER BY SUM(o.totalAmount) DESC
      """)
  Page<Object[]> getTopSpendingUsers(Pageable pageable);

  /**
   * Find recent orders with pagination. Used for dashboard recent activity.
   *
   * @param pageable pagination parameters
   * @return page of recent orders
   */
  @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
  Page<Order> findRecentOrders(Pageable pageable);

  /**
   * Find orders by username. Used for admin search functionality.
   *
   * @param username the username to search for
   * @return list of orders for the username
   */
  List<Order> findByUserUsername(String username);

  /**
   * Find orders by username with pagination. Used for paginated admin search.
   *
   * @param username the username to search for
   * @param pageable pagination parameters
   * @return page of orders for the username
   */
  Page<Order> findByUserUsername(String username, Pageable pageable);
}
