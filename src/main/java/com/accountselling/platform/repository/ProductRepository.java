package com.accountselling.platform.repository;

import com.accountselling.platform.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Product entity operations.
 * Provides data access methods for product management functionality including
 * search, filtering, and inventory-related operations.
 * 
 * รีพอสิทอรี่สำหรับจัดการข้อมูลสินค้า
 * รองรับการค้นหา การกรอง และการจัดการสต็อก
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Find product by name.
     * Used for product lookup and validation.
     * 
     * @param name the product name to search for
     * @return Optional containing the product if found
     */
    Optional<Product> findByName(String name);

    /**
     * Check if product name exists.
     * Used for product creation validation.
     * 
     * @param name the product name to check
     * @return true if product name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find products by active status.
     * Used for displaying active products to customers.
     * 
     * @param active the active status to filter by
     * @return list of products with the specified active status
     */
    List<Product> findByActive(Boolean active);

    /**
     * Find active products with pagination.
     * Used for customer-facing product listings.
     * 
     * @param pageable pagination parameters
     * @return page of active products
     */
    Page<Product> findByActive(Boolean active, Pageable pageable);

    /**
     * Find products by category.
     * Used for category-based product browsing.
     * 
     * @param categoryId the category ID to filter by
     * @return list of products in the specified category
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId ORDER BY p.sortOrder ASC, p.name ASC")
    List<Product> findByCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Find active products by category with pagination.
     * Used for customer-facing category browsing.
     * 
     * @param categoryId the category ID to filter by
     * @param pageable pagination parameters
     * @return page of active products in the specified category
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.active = true ORDER BY p.sortOrder ASC, p.name ASC")
    Page<Product> findActiveByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Find products by category hierarchy (including subcategories).
     * Used for browsing products in a category and all its subcategories.
     * 
     * @param categoryIds list of category IDs (parent and all descendants)
     * @return list of products in the specified categories
     */
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds ORDER BY p.sortOrder ASC, p.name ASC")
    List<Product> findByCategoryIdIn(@Param("categoryIds") List<UUID> categoryIds);

    /**
     * Find active products by category hierarchy with pagination.
     * Used for customer-facing hierarchical category browsing.
     * 
     * @param categoryIds list of category IDs (parent and all descendants)
     * @param pageable pagination parameters
     * @return page of active products in the specified categories
     */
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds AND p.active = true ORDER BY p.sortOrder ASC, p.name ASC")
    Page<Product> findActiveByCategoryIdIn(@Param("categoryIds") List<UUID> categoryIds, Pageable pageable);

    /**
     * Find products by name containing (case-insensitive).
     * Used for product search functionality.
     * 
     * @param name the name pattern to search for
     * @return list of products with names containing the pattern
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Find active products by name containing with pagination.
     * Used for customer-facing product search.
     * 
     * @param name the name pattern to search for
     * @param pageable pagination parameters
     * @return page of active products with names containing the pattern
     */
    Page<Product> findByNameContainingIgnoreCaseAndActive(String name, Boolean active, Pageable pageable);

    /**
     * Find products by server.
     * Used for server-specific product filtering.
     * 
     * @param server the server name to filter by
     * @return list of products for the specified server
     */
    List<Product> findByServer(String server);

    /**
     * Find active products by server with pagination.
     * Used for customer-facing server-based filtering.
     * 
     * @param server the server name to filter by
     * @param pageable pagination parameters
     * @return page of active products for the specified server
     */
    Page<Product> findByServerAndActive(String server, Boolean active, Pageable pageable);

    /**
     * Find products by price range.
     * Used for price-based filtering.
     * 
     * @param minPrice minimum price (inclusive)
     * @param maxPrice maximum price (inclusive)
     * @return list of products within the specified price range
     */
    @Query("SELECT p FROM Product p WHERE p.price >= :minPrice AND p.price <= :maxPrice ORDER BY p.price ASC")
    List<Product> findByPriceBetween(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    /**
     * Find active products by price range with pagination.
     * Used for customer-facing price filtering.
     * 
     * @param minPrice minimum price (inclusive)
     * @param maxPrice maximum price (inclusive)
     * @param pageable pagination parameters
     * @return page of active products within the specified price range
     */
    @Query("SELECT p FROM Product p WHERE p.price >= :minPrice AND p.price <= :maxPrice AND p.active = true ORDER BY p.price ASC")
    Page<Product> findActiveByPriceBetween(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

    /**
     * Search products by multiple criteria.
     * Used for advanced product search with multiple filters.
     * 
     * @param namePattern name pattern to search for (can be null)
     * @param categoryId category ID to filter by (can be null)
     * @param server server name to filter by (can be null)
     * @param minPrice minimum price (can be null)
     * @param maxPrice maximum price (can be null)
     * @param active active status to filter by
     * @param pageable pagination parameters
     * @return page of products matching the criteria
     */
    @Query("""
        SELECT p FROM Product p 
        WHERE (:namePattern IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :namePattern, '%')))
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:server IS NULL OR p.server = :server)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND p.active = :active
        ORDER BY p.sortOrder ASC, p.name ASC
        """)
    Page<Product> searchProducts(
        @Param("namePattern") String namePattern,
        @Param("categoryId") UUID categoryId,
        @Param("server") String server,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("active") Boolean active,
        Pageable pageable
    );

    /**
     * Find products with low stock.
     * Used for inventory management and alerts.
     * 
     * @return list of products with stock below their low stock threshold
     */
    @Query("""
        SELECT p FROM Product p 
        WHERE (SELECT COUNT(s) FROM Stock s WHERE s.product = p AND s.sold = false AND s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP) <= p.lowStockThreshold
        AND p.active = true
        """)
    List<Product> findProductsWithLowStock();

    /**
     * Find products that are out of stock.
     * Used for inventory management.
     * 
     * @return list of products with no available stock
     */
    @Query("""
        SELECT p FROM Product p 
        WHERE (SELECT COUNT(s) FROM Stock s WHERE s.product = p AND s.sold = false AND s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP) = 0
        AND p.active = true
        """)
    List<Product> findOutOfStockProducts();

    /**
     * Find products with available stock.
     * Used for customer-facing product listings.
     * 
     * @return list of products with at least one available stock item
     */
    @Query("""
        SELECT p FROM Product p 
        WHERE (SELECT COUNT(s) FROM Stock s WHERE s.product = p AND s.sold = false AND s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP) > 0
        AND p.active = true
        """)
    List<Product> findProductsWithAvailableStock();

    /**
     * Find products with available stock by category with pagination.
     * Used for customer-facing category browsing with stock availability.
     * 
     * @param categoryId the category ID to filter by
     * @param pageable pagination parameters
     * @return page of products with available stock in the specified category
     */
    @Query("""
        SELECT p FROM Product p 
        WHERE p.category.id = :categoryId
        AND (SELECT COUNT(s) FROM Stock s WHERE s.product = p AND s.sold = false AND s.reservedUntil IS NULL OR s.reservedUntil < CURRENT_TIMESTAMP) > 0
        AND p.active = true
        ORDER BY p.sortOrder ASC, p.name ASC
        """)
    Page<Product> findProductsWithAvailableStockByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Get distinct servers from all products.
     * Used for server filter options.
     * 
     * @return list of distinct server names
     */
    @Query("SELECT DISTINCT p.server FROM Product p WHERE p.server IS NOT NULL AND p.active = true ORDER BY p.server")
    List<String> findDistinctServers();

    /**
     * Find products ordered by name for admin interfaces.
     * Used for consistent product listing in admin panels.
     * 
     * @return list of all products ordered by name
     */
    List<Product> findAllByOrderByNameAsc();

    /**
     * Find products ordered by sort order and name.
     * Used for organized product display.
     * 
     * @return list of all products ordered by sort order and name
     */
    List<Product> findAllByOrderBySortOrderAscNameAsc();

    /**
     * Find products by category ordered by sort order and name.
     * Used for organized category-based product display.
     * 
     * @param categoryId the category ID to filter by
     * @return list of products in the category ordered by sort order and name
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId ORDER BY p.sortOrder ASC, p.name ASC")
    List<Product> findByCategoryIdOrderBySortOrderAscNameAsc(@Param("categoryId") UUID categoryId);

    /**
     * Count products by category.
     * Used for category statistics.
     * 
     * @param categoryId the category ID to count products for
     * @return count of products in the specified category
     */
    long countByCategoryId(UUID categoryId);

    /**
     * Count active products by category.
     * Used for customer-facing category statistics.
     * 
     * @param categoryId the category ID to count active products for
     * @return count of active products in the specified category
     */
    long countByCategoryIdAndActive(UUID categoryId, Boolean active);
}