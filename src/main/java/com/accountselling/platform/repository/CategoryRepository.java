package com.accountselling.platform.repository;

import com.accountselling.platform.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Category entity operations.
 * Provides data access methods for category management functionality including
 * hierarchical operations and search capabilities.
 * 
 * รีพอสิทอรี่สำหรับจัดการข้อมูลหมวดหมู่สินค้า
 * รองรับการจัดการแบบลำดับชั้นและการค้นหา
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find category by name.
     * Used for category lookup and validation.
     * 
     * @param name the category name to search for
     * @return Optional containing the category if found
     */
    Optional<Category> findByName(String name);

    /**
     * Check if category name exists.
     * Used for category creation validation.
     * 
     * @param name the category name to check
     * @return true if category name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find categories by active status.
     * Used for displaying active categories to customers.
     * 
     * @param active the active status to filter by
     * @return list of categories with the specified active status
     */
    List<Category> findByActive(Boolean active);

    /**
     * Find all active categories ordered by sort order and name.
     * Used for category listing in customer-facing interfaces.
     * 
     * @return list of active categories ordered by sort order and name
     */
    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findActiveOrderedBySort();

    /**
     * Find root categories (categories without parent).
     * Used for building category hierarchy navigation.
     * 
     * @return list of root categories
     */
    @Query("SELECT c FROM Category c WHERE c.parentCategory IS NULL ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findRootCategories();

    /**
     * Find active root categories.
     * Used for customer-facing category navigation.
     * 
     * @return list of active root categories
     */
    @Query("SELECT c FROM Category c WHERE c.parentCategory IS NULL AND c.active = true ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findActiveRootCategories();

    /**
     * Find subcategories of a parent category.
     * Used for building category hierarchy.
     * 
     * @param parentId the parent category ID
     * @return list of subcategories ordered by sort order and name
     */
    @Query("SELECT c FROM Category c WHERE c.parentCategory.id = :parentId ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findByParentCategoryId(@Param("parentId") UUID parentId);

    /**
     * Find active subcategories of a parent category.
     * Used for customer-facing category navigation.
     * 
     * @param parentId the parent category ID
     * @return list of active subcategories ordered by sort order and name
     */
    @Query("SELECT c FROM Category c WHERE c.parentCategory.id = :parentId AND c.active = true ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findActiveByParentCategoryId(@Param("parentId") UUID parentId);

    /**
     * Find categories by name containing (case-insensitive).
     * Used for category search functionality.
     * 
     * @param name the name pattern to search for
     * @return list of categories with names containing the pattern
     */
    List<Category> findByNameContainingIgnoreCase(String name);

    /**
     * Find active categories by name containing (case-insensitive).
     * Used for customer-facing category search.
     * 
     * @param name the name pattern to search for
     * @return list of active categories with names containing the pattern
     */
    List<Category> findByNameContainingIgnoreCaseAndActive(String name, Boolean active);

    /**
     * Find categories with products.
     * Used for category management and analytics.
     * 
     * @return list of categories that have at least one product
     */
    @Query("SELECT DISTINCT c FROM Category c WHERE SIZE(c.products) > 0")
    List<Category> findCategoriesWithProducts();

    /**
     * Find categories without products.
     * Used for category cleanup and management.
     * 
     * @return list of categories that have no products
     */
    @Query("SELECT c FROM Category c WHERE SIZE(c.products) = 0")
    List<Category> findCategoriesWithoutProducts();

    /**
     * Count products in a category.
     * Used for category statistics and display.
     * 
     * @param categoryId the category ID to count products for
     * @return count of products in the specified category
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Count active products in a category.
     * Used for customer-facing category display.
     * 
     * @param categoryId the category ID to count active products for
     * @return count of active products in the specified category
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    long countActiveProductsByCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Find all direct and indirect subcategories of a category.
     * Used for bulk operations on category hierarchies.
     * Note: This is a simplified version that finds direct subcategories.
     * For full hierarchy traversal, use programmatic approach.
     * 
     * @param rootCategoryId the root category ID
     * @return list of direct subcategories
     */
    @Query("SELECT c FROM Category c WHERE c.parentCategory.id = :rootCategoryId ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findDirectSubcategories(@Param("rootCategoryId") UUID rootCategoryId);

    /**
     * Find categories by parent category (null for root categories).
     * Used for level-based category retrieval.
     * 
     * @param parentId the parent category ID (null for root categories)
     * @return list of categories with the specified parent
     */
    @Query("SELECT c FROM Category c WHERE (:parentId IS NULL AND c.parentCategory IS NULL) OR c.parentCategory.id = :parentId ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findByParentId(@Param("parentId") UUID parentId);

    /**
     * Find categories ordered by name for admin interfaces.
     * Used for consistent category listing in admin panels.
     * 
     * @return list of all categories ordered by name
     */
    List<Category> findAllByOrderByNameAsc();

    /**
     * Find categories ordered by sort order and name.
     * Used for organized category display.
     * 
     * @return list of all categories ordered by sort order and name
     */
    List<Category> findAllByOrderBySortOrderAscNameAsc();
}