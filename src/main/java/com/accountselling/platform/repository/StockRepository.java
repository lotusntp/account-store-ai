package com.accountselling.platform.repository;

import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for Stock entity operations. Provides comprehensive data access methods for
 * inventory management including stock availability, reservations, sales tracking, and cleanup
 * operations.
 *
 * <p>รีพอสิทอรี่สำหรับจัดการข้อมูลสต็อก รองรับการจัดการสต็อก การจอง การติดตามการขาย
 * และการล้างข้อมูล
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {

  // ==================== BASIC STOCK QUERIES ====================

  /**
   * Find stock items by product. Used for viewing all stock items for a specific product.
   *
   * @param product the product to search stock for
   * @return list of stock items for the product
   */
  List<Stock> findByProduct(Product product);

  /**
   * Find stock items by product ID. Used for product stock management.
   *
   * @param productId the product ID to search stock for
   * @return list of stock items for the product
   */
  @Query("SELECT s FROM Stock s WHERE s.product.id = :productId ORDER BY s.createdAt ASC")
  List<Stock> findByProductId(@Param("productId") UUID productId);

  /**
   * Find stock items by product with pagination. Used for paginated stock listing in admin
   * interfaces.
   *
   * @param product the product to search stock for
   * @param pageable pagination parameters
   * @return page of stock items for the product
   */
  Page<Stock> findByProduct(Product product, Pageable pageable);

  // ==================== AVAILABILITY QUERIES ====================

  /**
   * Find available stock items (not sold and not reserved). Used for identifying stock that can be
   * sold immediately.
   *
   * @return list of available stock items
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.sold = false AND (s.reservedUntil IS NULL OR s.reservedUntil <"
          + " CURRENT_TIMESTAMP) ORDER BY s.createdAt ASC")
  List<Stock> findAvailableStock();

  /**
   * Find available stock items by product. Used for product-specific stock availability checks.
   *
   * @param product the product to search available stock for
   * @return list of available stock items for the product
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.product = :product AND s.sold = false AND (s.reservedUntil IS"
          + " NULL OR s.reservedUntil < CURRENT_TIMESTAMP) ORDER BY s.createdAt ASC")
  List<Stock> findAvailableStockByProduct(@Param("product") Product product);

  /**
   * Find available stock items by product ID. Used for product-specific stock availability checks.
   *
   * @param productId the product ID to search available stock for
   * @return list of available stock items for the product
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.product.id = :productId AND s.sold = false AND"
          + " (s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP) ORDER BY s.createdAt"
          + " ASC")
  List<Stock> findAvailableStockByProductId(@Param("productId") UUID productId);

  /**
   * Find first available stock item by product. Used for immediate stock allocation during
   * purchase.
   *
   * @param product the product to find available stock for
   * @return first available stock item or empty optional
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.product = :product AND s.sold = false AND (s.reservedUntil IS"
          + " NULL OR s.reservedUntil < CURRENT_TIMESTAMP) ORDER BY s.createdAt ASC LIMIT 1")
  Optional<Stock> findFirstAvailableByProduct(@Param("product") Product product);

  /**
   * Find first available stock item by product ID. Used for immediate stock allocation during
   * purchase.
   *
   * @param productId the product ID to find available stock for
   * @return first available stock item or empty optional
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.product.id = :productId AND s.sold = false AND"
          + " (s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP) ORDER BY s.createdAt"
          + " ASC LIMIT 1")
  Optional<Stock> findFirstAvailableByProductId(@Param("productId") UUID productId);

  // ==================== SOLD STOCK QUERIES ====================

  /**
   * Find sold stock items. Used for sales tracking and reporting.
   *
   * @return list of sold stock items
   */
  List<Stock> findBySoldTrue();

  /**
   * Find sold stock items by product. Used for product-specific sales tracking.
   *
   * @param product the product to search sold stock for
   * @return list of sold stock items for the product
   */
  List<Stock> findByProductAndSoldTrue(Product product);

  /**
   * Find sold stock items within date range. Used for sales reporting within specific periods.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of stock items sold within the date range
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.sold = true AND s.soldAt >= :startDate AND s.soldAt <="
          + " :endDate ORDER BY s.soldAt DESC")
  List<Stock> findSoldBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Find sold stock items by product within date range. Used for product-specific sales reporting.
   *
   * @param product the product to search sold stock for
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of stock items sold for the product within the date range
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.product = :product AND s.sold = true AND s.soldAt >="
          + " :startDate AND s.soldAt <= :endDate ORDER BY s.soldAt DESC")
  List<Stock> findSoldByProductBetweenDates(
      @Param("product") Product product,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // ==================== RESERVATION QUERIES ====================

  /**
   * Find reserved stock items. Used for tracking current reservations.
   *
   * @return list of currently reserved stock items
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.sold = false AND s.reservedUntil IS NOT NULL AND"
          + " s.reservedUntil > CURRENT_TIMESTAMP ORDER BY s.reservedUntil ASC")
  List<Stock> findReservedStock();

  /**
   * Find reserved stock items by product. Used for product-specific reservation tracking.
   *
   * @param product the product to search reserved stock for
   * @return list of reserved stock items for the product
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.product = :product AND s.sold = false AND s.reservedUntil IS"
          + " NOT NULL AND s.reservedUntil > CURRENT_TIMESTAMP ORDER BY s.reservedUntil ASC")
  List<Stock> findReservedStockByProduct(@Param("product") Product product);

  /**
   * Find expired reservation stock items. Used for cleanup operations and releasing expired
   * reservations.
   *
   * @return list of stock items with expired reservations
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.sold = false AND s.reservedUntil IS NOT NULL AND"
          + " s.reservedUntil <= CURRENT_TIMESTAMP")
  List<Stock> findExpiredReservations();

  /**
   * Find reservations expiring soon. Used for proactive reservation management and notifications.
   *
   * @param threshold the time threshold for "expiring soon"
   * @return list of stock items with reservations expiring within the threshold
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.sold = false AND s.reservedUntil IS NOT NULL AND"
          + " s.reservedUntil > CURRENT_TIMESTAMP AND s.reservedUntil <= :threshold ORDER BY"
          + " s.reservedUntil ASC")
  List<Stock> findReservationsExpiringSoon(@Param("threshold") LocalDateTime threshold);

  // ==================== STOCK COUNTING QUERIES ====================

  /**
   * Count total stock items by product. Used for inventory reporting.
   *
   * @param product the product to count stock for
   * @return total count of stock items for the product
   */
  long countByProduct(Product product);

  /**
   * Count available stock items by product. Used for stock availability checks.
   *
   * @param product the product to count available stock for
   * @return count of available stock items for the product
   */
  @Query(
      "SELECT COUNT(s) FROM Stock s WHERE s.product = :product AND s.sold = false AND"
          + " (s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP)")
  long countAvailableByProduct(@Param("product") Product product);

  /**
   * Count available stock items by product ID. Used for stock availability checks.
   *
   * @param productId the product ID to count available stock for
   * @return count of available stock items for the product
   */
  @Query(
      "SELECT COUNT(s) FROM Stock s WHERE s.product.id = :productId AND s.sold = false AND"
          + " (s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP)")
  long countAvailableByProductId(@Param("productId") UUID productId);

  /**
   * Count sold stock items by product. Used for sales reporting.
   *
   * @param product the product to count sold stock for
   * @return count of sold stock items for the product
   */
  long countByProductAndSoldTrue(Product product);

  /**
   * Count reserved stock items by product. Used for reservation reporting.
   *
   * @param product the product to count reserved stock for
   * @return count of reserved stock items for the product
   */
  @Query(
      "SELECT COUNT(s) FROM Stock s WHERE s.product = :product AND s.sold = false AND"
          + " s.reservedUntil IS NOT NULL AND s.reservedUntil > CURRENT_TIMESTAMP")
  long countReservedByProduct(@Param("product") Product product);

  // ==================== BULK OPERATIONS ====================

  /**
   * Clear expired reservations in bulk. Used for scheduled cleanup operations.
   *
   * @return number of reservations cleared
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE Stock s SET s.reservedUntil = NULL WHERE s.sold = false AND s.reservedUntil IS NOT"
          + " NULL AND s.reservedUntil <= CURRENT_TIMESTAMP")
  int clearExpiredReservations();

  /**
   * Reserve multiple stock items for a product. Used for bulk reservation operations.
   *
   * @param productId the product ID to reserve stock for
   * @param reservedUntil the reservation end time
   * @param limit maximum number of items to reserve
   * @return number of items reserved
   */
  @Modifying
  @Transactional
  @Query(
      value =
          """
          UPDATE stock SET reserved_until = :reservedUntil
          WHERE id IN (
              SELECT id FROM (
                  SELECT id FROM stock
                  WHERE product_id = :productId
                  AND sold = false
                  AND (reserved_until IS NULL OR reserved_until < CURRENT_TIMESTAMP)
                  ORDER BY created_at ASC
                  LIMIT :limit
              ) AS subquery
          )
          """,
      nativeQuery = true)
  int reserveStockItems(
      @Param("productId") UUID productId,
      @Param("reservedUntil") LocalDateTime reservedUntil,
      @Param("limit") int limit);

  /**
   * Release reservations for specific stock items. Used for canceling reservations.
   *
   * @param stockIds list of stock IDs to release reservations for
   * @return number of reservations released
   */
  @Modifying
  @Transactional
  @Query("UPDATE Stock s SET s.reservedUntil = NULL WHERE s.id IN :stockIds")
  int releaseReservations(@Param("stockIds") List<UUID> stockIds);

  // ==================== ADVANCED QUERIES ====================

  /**
   * Find stock items with low availability by category. Used for inventory management alerts.
   *
   * @param threshold the low stock threshold
   * @return list of products with stock below threshold grouped by category
   */
  @Query(
"""
    SELECT s.product FROM Stock s
    WHERE s.product IN (
        SELECT p FROM Product p WHERE
        (SELECT COUNT(st) FROM Stock st
            WHERE st.product = p
              AND st.sold = false
              AND (st.reservedUntil IS NULL OR st.reservedUntil < CURRENT_TIMESTAMP)
        ) <= :threshold
    )
    GROUP BY s.product, s.product.category.name, s.product.name
    ORDER BY s.product.category.name, s.product.name
""")
  List<Product> findProductsWithLowStock(@Param("threshold") long threshold);

  /**
   * Find stock items by credentials pattern. Used for debugging and administrative searches.
   * WARNING: This should be used carefully due to sensitive data.
   *
   * @param credentialsPattern pattern to search in credentials
   * @return list of stock items matching the credentials pattern
   */
  @Query("SELECT s FROM Stock s WHERE s.credentials LIKE %:credentialsPattern%")
  List<Stock> findByCredentialsContaining(@Param("credentialsPattern") String credentialsPattern);

  /**
   * Find stock statistics grouped by product. Used for comprehensive inventory reporting.
   *
   * @param productId the product ID to get statistics for
   * @return object array containing [total, available, sold, reserved] counts
   */
  @Query(
      """
      SELECT
          COUNT(s) as total,
          SUM(CASE WHEN s.sold = false AND (s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP) THEN 1 ELSE 0 END) as available,
          SUM(CASE WHEN s.sold = true THEN 1 ELSE 0 END) as sold,
          SUM(CASE WHEN s.sold = false AND s.reservedUntil IS NOT NULL AND s.reservedUntil > CURRENT_TIMESTAMP THEN 1 ELSE 0 END) as reserved
      FROM Stock s
      WHERE s.product.id = :productId
      """)
  Object[] getStockStatisticsByProductId(@Param("productId") UUID productId);

  /**
   * Delete sold stock items older than specified date. Used for archival and cleanup operations.
   * WARNING: This permanently deletes data. Use with caution.
   *
   * @param cutoffDate the cutoff date for deletion
   * @return number of records deleted
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM Stock s WHERE s.sold = true AND s.soldAt < :cutoffDate")
  int deleteSoldStockOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

  /**
   * Find duplicate credentials within the same product. Used for data integrity checks.
   *
   * @param productId the product ID to check for duplicates
   * @return list of duplicate credentials
   */
  @Query(
      """
      SELECT s.credentials FROM Stock s
      WHERE s.product.id = :productId
      GROUP BY s.credentials
      HAVING COUNT(s.credentials) > 1
      """)
  List<String> findDuplicateCredentialsByProductId(@Param("productId") UUID productId);

  /**
   * Check if stock item exists with specific credentials for a product. Used for preventing
   * duplicate stock entries.
   *
   * @param productId the product ID to check
   * @param credentials the credentials to check for existence
   * @return true if stock exists with these credentials for the product
   */
  boolean existsByProductIdAndCredentials(UUID productId, String credentials);

  /**
   * Find the oldest unsold stock item by product. Used for FIFO (First In, First Out) stock
   * allocation.
   *
   * @param product the product to find oldest stock for
   * @return oldest unsold stock item or empty optional
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.product = :product AND s.sold = false ORDER BY s.createdAt ASC"
          + " LIMIT 1")
  Optional<Stock> findOldestUnsoldByProduct(@Param("product") Product product);

  /**
   * Find stock items created within date range. Used for tracking stock additions and inventory
   * analysis.
   *
   * @param startDate start date of the range
   * @param endDate end date of the range
   * @return list of stock items created within the date range
   */
  @Query(
      "SELECT s FROM Stock s WHERE s.createdAt >= :startDate AND s.createdAt <= :endDate ORDER BY"
          + " s.createdAt DESC")
  List<Stock> findStockCreatedBetweenDates(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
