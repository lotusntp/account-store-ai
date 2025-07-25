package com.accountselling.platform.repository;

import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.OrderItem;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
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
 * Repository interface for OrderItem entity operations. Provides comprehensive data access methods
 * for order item management including order details, product sales tracking, stock item
 * relationships, and analytics.
 *
 * <p>รีพอสิทอรี่สำหรับจัดการข้อมูลรายการสินค้าในคำสั่งซื้อ รองรับการจัดการรายละเอียดคำสั่งซื้อ
 * การติดตามการขายสินค้า ความสัมพันธ์กับสต็อก และการวิเคราะห์
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

  // ==================== BASIC ORDER ITEM QUERIES ====================

  /**
   * Find order items by order. Used for displaying order details and item breakdown.
   *
   * @param order the order to search items for
   * @return list of order items for the order
   */
  List<OrderItem> findByOrder(Order order);

  /**
   * Find order items by order with pagination. Used for paginated order item display.
   *
   * @param order the order to search items for
   * @param pageable pagination parameters
   * @return page of order items for the order
   */
  Page<OrderItem> findByOrder(Order order, Pageable pageable);

  /**
   * Find order items by order ID. Used for order item management by order ID.
   *
   * @param orderId the order ID to search items for
   * @return list of order items for the order
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId ORDER BY oi.createdAt ASC")
  List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);

  /**
   * Find order item by stock item. Used for tracking which order item contains a specific stock
   * item.
   *
   * @param stockItem the stock item to search for
   * @return Optional containing the order item if found
   */
  Optional<OrderItem> findByStockItem(Stock stockItem);

  /**
   * Find order item by stock item ID. Used for stock item to order item mapping.
   *
   * @param stockItemId the stock item ID to search for
   * @return Optional containing the order item if found
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.stockItem.id = :stockItemId")
  Optional<OrderItem> findByStockItemId(@Param("stockItemId") UUID stockItemId);

  // ==================== PRODUCT-BASED QUERIES ====================

  /**
   * Find order items by product. Used for product sales tracking and analytics.
   *
   * @param product the product to search order items for
   * @return list of order items for the product
   */
  List<OrderItem> findByProduct(Product product);

  /**
   * Find order items by product with pagination. Used for paginated product sales tracking.
   *
   * @param product the product to search order items for
   * @param pageable pagination parameters
   * @return page of order items for the product
   */
  Page<OrderItem> findByProduct(Product product, Pageable pageable);

  /**
   * Find order items by product ID. Used for product-specific sales analysis.
   *
   * @param productId the product ID to search order items for
   * @return list of order items for the product
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId ORDER BY oi.createdAt DESC")
  List<OrderItem> findByProductId(@Param("productId") UUID productId);

  /**
   * Find order items by product within date range. Used for product sales reporting within specific
   * periods.
   *
   * @param product the product to search order items for
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of order items for the product within the date range
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.product = :product AND oi.createdAt >= :startDate AND"
          + " oi.createdAt <= :endDate ORDER BY oi.createdAt DESC")
  List<OrderItem> findByProductBetweenDates(
      @Param("product") Product product,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // ==================== USER-BASED QUERIES ====================

  /**
   * Find order items by user. Used for user purchase history and analytics.
   *
   * @param user the user to search order items for
   * @return list of order items for the user
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.user = :user ORDER BY oi.createdAt DESC")
  List<OrderItem> findByUser(@Param("user") User user);

  /**
   * Find order items by user with pagination. Used for paginated user purchase history.
   *
   * @param user the user to search order items for
   * @param pageable pagination parameters
   * @return page of order items for the user
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.user = :user ORDER BY oi.createdAt DESC")
  Page<OrderItem> findByUser(@Param("user") User user, Pageable pageable);

  /**
   * Find order items by user ID. Used for user-specific purchase tracking.
   *
   * @param userId the user ID to search order items for
   * @return list of order items for the user
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.user.id = :userId ORDER BY oi.createdAt DESC")
  List<OrderItem> findByUserId(@Param("userId") UUID userId);

  /**
   * Find order items by user and product. Used for tracking user purchases of specific products.
   *
   * @param user the user to search order items for
   * @param product the product to filter by
   * @return list of order items for the user and product
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.order.user = :user AND oi.product = :product ORDER BY"
          + " oi.createdAt DESC")
  List<OrderItem> findByUserAndProduct(@Param("user") User user, @Param("product") Product product);

  // ==================== ORDER STATUS-BASED QUERIES ====================

  /**
   * Find order items by order status. Used for status-based order item filtering.
   *
   * @param status the order status to filter by
   * @return list of order items with orders of the specified status
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.status = :status ORDER BY oi.createdAt DESC")
  List<OrderItem> findByOrderStatus(@Param("status") OrderStatus status);

  /**
   * Find order items by order status with pagination. Used for paginated status-based order item
   * filtering.
   *
   * @param status the order status to filter by
   * @param pageable pagination parameters
   * @return page of order items with orders of the specified status
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.status = :status ORDER BY oi.createdAt DESC")
  Page<OrderItem> findByOrderStatus(@Param("status") OrderStatus status, Pageable pageable);

  /**
   * Find completed order items. Used for sales analytics and reporting.
   *
   * @return list of order items from completed orders
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.order.status = 'COMPLETED' ORDER BY oi.createdAt DESC")
  List<OrderItem> findCompletedOrderItems();

  /**
   * Find completed order items by product. Used for product-specific sales analytics.
   *
   * @param product the product to search completed order items for
   * @return list of completed order items for the product
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.product = :product AND oi.order.status = 'COMPLETED'"
          + " ORDER BY oi.createdAt DESC")
  List<OrderItem> findCompletedOrderItemsByProduct(@Param("product") Product product);

  // ==================== DATE-BASED QUERIES ====================

  /**
   * Find order items created within date range. Used for reporting and analytics within specific
   * periods.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of order items created within the date range
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.createdAt >= :startDate AND oi.createdAt <= :endDate"
          + " ORDER BY oi.createdAt DESC")
  List<OrderItem> findOrderItemsBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Find completed order items within date range. Used for sales reporting within specific periods.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of completed order items within the date range
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.order.status = 'COMPLETED' AND oi.createdAt >="
          + " :startDate AND oi.createdAt <= :endDate ORDER BY oi.createdAt DESC")
  List<OrderItem> findCompletedOrderItemsBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  // ==================== PRICE-BASED QUERIES ====================

  /**
   * Find order items by price range. Used for price-based analysis and filtering.
   *
   * @param minPrice minimum price (inclusive)
   * @param maxPrice maximum price (inclusive)
   * @return list of order items within the specified price range
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.price >= :minPrice AND oi.price <= :maxPrice ORDER BY"
          + " oi.price DESC")
  List<OrderItem> findByPriceRange(
      @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

  /**
   * Find order items above specified price. Used for high-value item tracking.
   *
   * @param price the minimum price threshold
   * @return list of order items with price above the threshold
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.price > :price ORDER BY oi.price DESC")
  List<OrderItem> findOrderItemsAbovePrice(@Param("price") BigDecimal price);

  // ==================== COUNTING QUERIES ====================

  /**
   * Count order items by order. Used for order item count statistics.
   *
   * @param order the order to count items for
   * @return count of order items for the order
   */
  long countByOrder(Order order);

  /**
   * Count order items by product. Used for product sales count statistics.
   *
   * @param product the product to count order items for
   * @return count of order items for the product
   */
  long countByProduct(Product product);

  /**
   * Count completed order items by product. Used for product sales statistics.
   *
   * @param product the product to count completed order items for
   * @return count of completed order items for the product
   */
  @Query(
      "SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product = :product AND oi.order.status ="
          + " 'COMPLETED'")
  long countCompletedByProduct(@Param("product") Product product);

  /**
   * Count order items by user. Used for user purchase statistics.
   *
   * @param user the user to count order items for
   * @return count of order items for the user
   */
  @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.user = :user")
  long countByUser(@Param("user") User user);

  /**
   * Count order items by order status. Used for status-based statistics.
   *
   * @param status the order status to count items for
   * @return count of order items with orders of the specified status
   */
  @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.status = :status")
  long countByOrderStatus(@Param("status") OrderStatus status);

  // ==================== AGGREGATION QUERIES ====================

  /**
   * Calculate total revenue from completed order items. Used for revenue reporting and analytics.
   *
   * @return total revenue from all completed order items
   */
  @Query("SELECT COALESCE(SUM(oi.price), 0) FROM OrderItem oi WHERE oi.order.status = 'COMPLETED'")
  BigDecimal calculateTotalRevenue();

  /**
   * Calculate total revenue by product. Used for product-specific revenue analysis.
   *
   * @param product the product to calculate revenue for
   * @return total revenue from the product's completed order items
   */
  @Query(
      "SELECT COALESCE(SUM(oi.price), 0) FROM OrderItem oi WHERE oi.product = :product AND"
          + " oi.order.status = 'COMPLETED'")
  BigDecimal calculateRevenueByProduct(@Param("product") Product product);

  /**
   * Calculate total revenue within date range. Used for period-based revenue reporting.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return total revenue within the date range
   */
  @Query(
      "SELECT COALESCE(SUM(oi.price), 0) FROM OrderItem oi WHERE oi.order.status = 'COMPLETED' AND"
          + " oi.createdAt >= :startDate AND oi.createdAt <= :endDate")
  BigDecimal calculateRevenueBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Calculate average item price. Used for pricing analytics.
   *
   * @return average price from completed order items
   */
  @Query("SELECT COALESCE(AVG(oi.price), 0) FROM OrderItem oi WHERE oi.order.status = 'COMPLETED'")
  BigDecimal calculateAverageItemPrice();

  // ==================== ADVANCED QUERIES ====================

  /**
   * Find top-selling products by quantity. Used for product popularity analysis.
   *
   * @param limit maximum number of products to return
   * @return list of products ordered by sales quantity
   */
  @Query(
      "SELECT oi.product FROM OrderItem oi WHERE oi.order.status = 'COMPLETED' GROUP BY oi.product"
          + " ORDER BY COUNT(oi) DESC LIMIT :limit")
  List<Product> findTopSellingProductsByQuantity(@Param("limit") int limit);

  /**
   * Find top-selling products by revenue. Used for revenue-based product analysis.
   *
   * @param limit maximum number of products to return
   * @return list of products ordered by total revenue
   */
  @Query(
      "SELECT oi.product FROM OrderItem oi WHERE oi.order.status = 'COMPLETED' GROUP BY oi.product"
          + " ORDER BY SUM(oi.price) DESC LIMIT :limit")
  List<Product> findTopSellingProductsByRevenue(@Param("limit") int limit);

  /**
   * Find recent order items for dashboard. Used for admin dashboard recent activity display.
   *
   * @param limit maximum number of order items to return
   * @return list of recent order items
   */
  @Query("SELECT oi FROM OrderItem oi ORDER BY oi.createdAt DESC LIMIT :limit")
  List<OrderItem> findRecentOrderItems(@Param("limit") int limit);

  /**
   * Find order items by category. Used for category-based sales analysis.
   *
   * @param categoryName the category name to filter by
   * @return list of order items for products in the specified category
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.product.category.name = :categoryName ORDER BY"
          + " oi.createdAt DESC")
  List<OrderItem> findByCategory(@Param("categoryName") String categoryName);

  /**
   * Find completed order items by category. Used for category-based sales reporting.
   *
   * @param categoryName the category name to filter by
   * @return list of completed order items for products in the specified category
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.product.category.name = :categoryName AND"
          + " oi.order.status = 'COMPLETED' ORDER BY oi.createdAt DESC")
  List<OrderItem> findCompletedByCategory(@Param("categoryName") String categoryName);

  /**
   * Search order items by multiple criteria. Used for advanced order item search with multiple
   * filters.
   *
   * @param productName product name pattern (can be null)
   * @param categoryName category name (can be null)
   * @param username username pattern (can be null)
   * @param orderStatus order status (can be null)
   * @param startDate start date (can be null)
   * @param endDate end date (can be null)
   * @param minPrice minimum price (can be null)
   * @param maxPrice maximum price (can be null)
   * @param pageable pagination parameters
   * @return page of order items matching the criteria
   */
  @Query(
      """
      SELECT oi FROM OrderItem oi
      WHERE (:productName IS NULL OR LOWER(oi.product.name) LIKE LOWER(CONCAT('%', :productName, '%')))
      AND (:categoryName IS NULL OR oi.product.category.name = :categoryName)
      AND (:username IS NULL OR LOWER(oi.order.user.username) LIKE LOWER(CONCAT('%', :username, '%')))
      AND (:orderStatus IS NULL OR oi.order.status = :orderStatus)
      AND (:startDate IS NULL OR oi.createdAt >= :startDate)
      AND (:endDate IS NULL OR oi.createdAt <= :endDate)
      AND (:minPrice IS NULL OR oi.price >= :minPrice)
      AND (:maxPrice IS NULL OR oi.price <= :maxPrice)
      ORDER BY oi.createdAt DESC
      """)
  Page<OrderItem> searchOrderItems(
      @Param("productName") String productName,
      @Param("categoryName") String categoryName,
      @Param("username") String username,
      @Param("orderStatus") OrderStatus orderStatus,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice,
      Pageable pageable);

  /**
   * Get product sales statistics. Used for comprehensive product performance analysis.
   *
   * @param productId the product ID to get statistics for
   * @return object array containing [totalSold, totalRevenue, averagePrice] for completed orders
   */
  @Query(
      """
      SELECT
          COUNT(oi) as totalSold,
          COALESCE(SUM(oi.price), 0) as totalRevenue,
          COALESCE(AVG(oi.price), 0) as averagePrice
      FROM OrderItem oi
      WHERE oi.product.id = :productId AND oi.order.status = 'COMPLETED'
      """)
  Object[] getProductSalesStatistics(@Param("productId") UUID productId);

  /**
   * Get daily sales statistics within date range. Used for daily sales reporting and charts.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of daily statistics [date, itemCount, revenue]
   */
  @Query(
      """
      SELECT
          DATE(oi.createdAt) as date,
          COUNT(oi) as itemCount,
          COALESCE(SUM(oi.price), 0) as revenue
      FROM OrderItem oi
      WHERE oi.order.status = 'COMPLETED'
      AND oi.createdAt >= :startDate AND oi.createdAt <= :endDate
      GROUP BY DATE(oi.createdAt)
      ORDER BY DATE(oi.createdAt)
      """)
  List<Object[]> getDailySalesStatistics(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Check if stock item is already used in an order item. Used for preventing duplicate stock item
   * usage.
   *
   * @param stockItem the stock item to check
   * @return true if stock item is already used in an order item
   */
  boolean existsByStockItem(Stock stockItem);

  /**
   * Find order items with invalid stock relationships. Used for data integrity checks and cleanup.
   *
   * @return list of order items where stock item product doesn't match order item product
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.stockItem.product != oi.product")
  List<OrderItem> findOrderItemsWithInvalidStockRelationships();

  // ==================== ADDITIONAL METHODS FOR TESTS ====================

  /**
   * Find order items by product name containing (case-insensitive). Used for product name search.
   *
   * @param productName the product name pattern
   * @return list of order items with matching product names
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE LOWER(oi.productName) LIKE LOWER(CONCAT('%', :productName,"
          + " '%'))")
  List<OrderItem> findByProductNameContainingIgnoreCase(@Param("productName") String productName);

  /**
   * Find order items by category name. Used for category-based filtering.
   *
   * @param categoryName the category name
   * @return list of order items in the specified category
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.categoryName = :categoryName")
  List<OrderItem> findByCategoryName(@Param("categoryName") String categoryName);

  /**
   * Find order items by server. Used for server-based filtering.
   *
   * @param server the server name
   * @return list of order items for the specified server
   */
  @Query("SELECT oi FROM OrderItem oi WHERE oi.server = :server")
  List<OrderItem> findByServer(@Param("server") String server);

  /**
   * Find order items by username. Used for user-based search.
   *
   * @param username the username
   * @return list of order items for the specified user
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.order.user.username = :username ORDER BY oi.createdAt"
          + " DESC")
  List<OrderItem> findByUsername(@Param("username") String username);

  /**
   * Find order items created between dates. Used for date range filtering.
   *
   * @param startDate start date
   * @param endDate end date
   * @return list of order items created between dates
   */
  List<OrderItem> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Find order items by product and created between dates. Used for product-specific date range
   * analysis.
   *
   * @param product the product
   * @param startDate start date
   * @param endDate end date
   * @return list of order items for product created between dates
   */
  List<OrderItem> findByProductAndCreatedAtBetween(
      Product product, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Find order items by price between range. Used for price range filtering.
   *
   * @param minPrice minimum price
   * @param maxPrice maximum price
   * @return list of order items with price in range
   */
  List<OrderItem> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

  /**
   * Find order items by price greater than. Used for minimum price filtering.
   *
   * @param price minimum price threshold
   * @return list of order items with price greater than threshold
   */
  List<OrderItem> findByPriceGreaterThan(BigDecimal price);

  /**
   * Calculate total revenue by product. Used for product revenue analysis.
   *
   * @param product the product
   * @return total revenue from the product
   */
  @Query(
      "SELECT COALESCE(SUM(oi.price), 0) FROM OrderItem oi WHERE oi.product = :product AND"
          + " oi.order.status = 'COMPLETED'")
  BigDecimal calculateTotalRevenueByProduct(@Param("product") Product product);

  /**
   * Calculate total spent by user. Used for user spending analysis.
   *
   * @param user the user
   * @return total amount spent by the user
   */
  @Query(
      "SELECT COALESCE(SUM(oi.price), 0) FROM OrderItem oi WHERE oi.order.user = :user AND"
          + " oi.order.status = 'COMPLETED'")
  BigDecimal calculateTotalSpentByUser(@Param("user") User user);

  /**
   * Calculate total revenue by category name. Used for category revenue analysis.
   *
   * @param categoryName the category name
   * @return total revenue from the category
   */
  @Query(
      "SELECT COALESCE(SUM(oi.price), 0) FROM OrderItem oi WHERE oi.categoryName = :categoryName"
          + " AND oi.order.status = 'COMPLETED'")
  BigDecimal calculateTotalRevenueByCategoryName(@Param("categoryName") String categoryName);

  /**
   * Calculate average price by product. Used for product pricing analysis.
   *
   * @param product the product
   * @return average price for the product
   */
  @Query(
      "SELECT COALESCE(AVG(oi.price), 0) FROM OrderItem oi WHERE oi.product = :product AND"
          + " oi.order.status = 'COMPLETED'")
  BigDecimal calculateAveragePriceByProduct(@Param("product") Product product);

  /**
   * Get top selling products. Used for product popularity analysis.
   *
   * @param pageable pagination parameters
   * @return page of top selling products [product, salesCount]
   */
  @Query(
      """
      SELECT
          oi.product as product,
          COUNT(oi) as salesCount
      FROM OrderItem oi
      WHERE oi.order.status = 'COMPLETED'
      GROUP BY oi.product
      ORDER BY COUNT(oi) DESC
      """)
  Page<Object[]> getTopSellingProducts(Pageable pageable);

  /**
   * Get sales statistics by category. Used for category performance analysis.
   *
   * @return list of category statistics [categoryName, salesCount, totalRevenue]
   */
  @Query(
      """
      SELECT
          oi.categoryName as categoryName,
          COUNT(oi) as salesCount,
          COALESCE(SUM(oi.price), 0) as totalRevenue
      FROM OrderItem oi
      WHERE oi.order.status = 'COMPLETED'
      GROUP BY oi.categoryName
      ORDER BY COUNT(oi) DESC
      """)
  List<Object[]> getSalesStatisticsByCategory();

  /**
   * Find order items by completed orders. Used for completed sales analysis.
   *
   * @return list of order items from completed orders
   */
  @Query(
      "SELECT oi FROM OrderItem oi WHERE oi.order.status = 'COMPLETED' ORDER BY oi.createdAt DESC")
  List<OrderItem> findByCompletedOrders();

  /**
   * Find distinct category names. Used for category filter options.
   *
   * @return list of distinct category names
   */
  @Query(
      "SELECT DISTINCT oi.categoryName FROM OrderItem oi WHERE oi.categoryName IS NOT NULL ORDER BY"
          + " oi.categoryName")
  List<String> findDistinctCategoryNames();

  /**
   * Find distinct servers. Used for server filter options.
   *
   * @return list of distinct server names
   */
  @Query(
      "SELECT DISTINCT oi.server FROM OrderItem oi WHERE oi.server IS NOT NULL ORDER BY oi.server")
  List<String> findDistinctServers();
}
