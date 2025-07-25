package com.accountselling.platform.service;

import com.accountselling.platform.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Category management operations. Provides business logic for category
 * hierarchy management, search, and validation.
 *
 * <p>Interface for category management Supports hierarchical management, search, and data
 * validation
 */
public interface CategoryService {

  // ========== Read Operations ==========

  /**
   * Find category by ID.
   *
   * @param id the category ID
   * @return Optional containing the category if found
   */
  Optional<Category> findById(UUID id);

  /**
   * Find category by name.
   *
   * @param name the category name
   * @return Optional containing the category if found
   */
  Optional<Category> findByName(String name);

  /**
   * Find all categories.
   *
   * @return list of all categories
   */
  List<Category> findAllCategories();

  /**
   * Find all active categories ordered by sort order and name.
   *
   * @return list of active categories
   */
  List<Category> findActiveCategories();

  /**
   * Find root categories (categories without parent).
   *
   * @return list of root categories
   */
  List<Category> findRootCategories();

  /**
   * Find active root categories.
   *
   * @return list of active root categories
   */
  List<Category> findActiveRootCategories();

  /**
   * Find subcategories of a parent category.
   *
   * @param parentId the parent category ID
   * @return list of subcategories
   */
  List<Category> findSubcategories(UUID parentId);

  /**
   * Find active subcategories of a parent category.
   *
   * @param parentId the parent category ID
   * @return list of active subcategories
   */
  List<Category> findActiveSubcategories(UUID parentId);

  /**
   * Search categories by name (case-insensitive).
   *
   * @param name the name pattern to search for
   * @param activeOnly whether to search only active categories
   * @return list of categories matching the name pattern
   */
  List<Category> searchCategoriesByName(String name, boolean activeOnly);

  /**
   * Get category hierarchy tree starting from root categories.
   *
   * @param activeOnly whether to include only active categories
   * @return list of root categories with their complete hierarchy
   */
  List<Category> getCategoryHierarchy(boolean activeOnly);

  /**
   * Get all descendant categories (recursive) of a given category.
   *
   * @param categoryId the parent category ID
   * @param includeInactive whether to include inactive categories
   * @return list of all descendant categories
   */
  List<Category> getAllDescendants(UUID categoryId, boolean includeInactive);

  /**
   * Check if category exists by name.
   *
   * @param name the category name to check
   * @return true if category exists, false otherwise
   */
  boolean existsByName(String name);

  /**
   * Count products in a category.
   *
   * @param categoryId the category ID
   * @param activeOnly whether to count only active products
   * @return count of products in the category
   */
  long countProductsInCategory(UUID categoryId, boolean activeOnly);

  // ========== Write Operations ==========

  /**
   * Create a new category.
   *
   * @param name the category name
   * @param description the category description
   * @param parentId the parent category ID (null for root category)
   * @return the created category
   * @throws com.accountselling.platform.exception.ResourceAlreadyExistsException if category name
   *     already exists
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if parent category not
   *     found
   */
  Category createCategory(String name, String description, UUID parentId);

  /**
   * Create a new category with sort order.
   *
   * @param name the category name
   * @param description the category description
   * @param parentId the parent category ID
   * @param sortOrder the sort order
   * @return the created category
   */
  Category createCategory(String name, String description, UUID parentId, Integer sortOrder);

  /**
   * Update category information.
   *
   * @param categoryId the category ID to update
   * @param name the new category name
   * @param description the new category description
   * @param sortOrder the new sort order
   * @return the updated category
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
   * @throws com.accountselling.platform.exception.ResourceAlreadyExistsException if new name
   *     already exists
   */
  Category updateCategory(UUID categoryId, String name, String description, Integer sortOrder);

  /**
   * Move category to a new parent.
   *
   * @param categoryId the category ID to move
   * @param newParentId the new parent category ID (null to make it root)
   * @return the updated category
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if category or parent
   *     not found
   * @throws IllegalArgumentException if moving would create circular dependency
   */
  Category moveCategory(UUID categoryId, UUID newParentId);

  /**
   * Set category active status.
   *
   * @param categoryId the category ID
   * @param active the new active status
   * @return the updated category
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
   */
  Category setCategoryActive(UUID categoryId, boolean active);

  /**
   * Delete category by ID.
   *
   * @param categoryId the category ID to delete
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
   * @throws IllegalStateException if category has products or subcategories
   */
  void deleteCategory(UUID categoryId);

  /**
   * Force delete category and move its products to another category.
   *
   * @param categoryId the category ID to delete
   * @param targetCategoryId the target category ID for moving products
   * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
   */
  void deleteCategoryAndMoveProducts(UUID categoryId, UUID targetCategoryId);

  // ========== Validation & Utility Methods ==========

  /**
   * Validate category hierarchy (check for circular dependencies).
   *
   * @param categoryId the category ID
   * @param proposedParentId the proposed parent category ID
   * @return true if valid, false if would create circular dependency
   */
  boolean isValidHierarchy(UUID categoryId, UUID proposedParentId);

  /**
   * Check if category can be deleted safely.
   *
   * @param categoryId the category ID to check
   * @return true if can be deleted safely, false otherwise
   */
  boolean canDeleteCategory(UUID categoryId);

  /**
   * Get category breadcrumb path.
   *
   * @param categoryId the category ID
   * @return list of categories from root to the specified category
   */
  List<Category> getCategoryPath(UUID categoryId);

  /**
   * Reorder categories within the same parent.
   *
   * @param parentId the parent category ID (null for root categories)
   * @param categoryIds list of category IDs in the desired order
   */
  void reorderCategories(UUID parentId, List<UUID> categoryIds);
}
