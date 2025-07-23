package com.accountselling.platform.service;

import com.accountselling.platform.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Category management operations.
 * Provides business logic for category hierarchy management, search, and validation.
 * 
 * Interface สำหรับการจัดการหมวดหมู่สินค้า
 * รองรับการจัดการแบบลำดับชั้น การค้นหา และการตรวจสอบข้อมูล
 */
public interface CategoryService {

    // ========== Read Operations ==========

    /**
     * Find category by ID.
     * ค้นหาหมวดหมู่ตาม ID
     * 
     * @param id the category ID
     * @return Optional containing the category if found
     */
    Optional<Category> findById(UUID id);

    /**
     * Find category by name.
     * ค้นหาหมวดหมู่ตามชื่อ
     * 
     * @param name the category name
     * @return Optional containing the category if found
     */
    Optional<Category> findByName(String name);

    /**
     * Find all categories.
     * ดึงหมวดหมู่ทั้งหมด
     * 
     * @return list of all categories
     */
    List<Category> findAllCategories();

    /**
     * Find all active categories ordered by sort order and name.
     * ดึงหมวดหมู่ที่เปิดใช้งานทั้งหมด เรียงตาม sort order และชื่อ
     * 
     * @return list of active categories
     */
    List<Category> findActiveCategories();

    /**
     * Find root categories (categories without parent).
     * ดึงหมวดหมู่รากทั้งหมด (หมวดหมู่ที่ไม่มี parent)
     * 
     * @return list of root categories
     */
    List<Category> findRootCategories();

    /**
     * Find active root categories.
     * ดึงหมวดหมู่รากที่เปิดใช้งาน
     * 
     * @return list of active root categories
     */
    List<Category> findActiveRootCategories();

    /**
     * Find subcategories of a parent category.
     * ดึงหมวดหมู่ย่อยของหมวดหมู่แม่
     * 
     * @param parentId the parent category ID
     * @return list of subcategories
     */
    List<Category> findSubcategories(UUID parentId);

    /**
     * Find active subcategories of a parent category.
     * ดึงหมวดหมู่ย่อยที่เปิดใช้งานของหมวดหมู่แม่
     * 
     * @param parentId the parent category ID
     * @return list of active subcategories
     */
    List<Category> findActiveSubcategories(UUID parentId);

    /**
     * Search categories by name (case-insensitive).
     * ค้นหาหมวดหมู่ตามชื่อ (ไม่สนใจตัวพิมพ์เล็ก-ใหญ่)
     * 
     * @param name the name pattern to search for
     * @param activeOnly whether to search only active categories
     * @return list of categories matching the name pattern
     */
    List<Category> searchCategoriesByName(String name, boolean activeOnly);

    /**
     * Get category hierarchy tree starting from root categories.
     * ดึงโครงสร้างต้นไม้หมวดหมู่เริ่มจากหมวดหมู่รากทั้งหมด
     * 
     * @param activeOnly whether to include only active categories
     * @return list of root categories with their complete hierarchy
     */
    List<Category> getCategoryHierarchy(boolean activeOnly);

    /**
     * Get all descendant categories (recursive) of a given category.
     * ดึงหมวดหมู่ลูกหลานทั้งหมด (แบบ recursive) ของหมวดหมู่ที่กำหนด
     * 
     * @param categoryId the parent category ID
     * @param includeInactive whether to include inactive categories
     * @return list of all descendant categories
     */
    List<Category> getAllDescendants(UUID categoryId, boolean includeInactive);

    /**
     * Check if category exists by name.
     * ตรวจสอบว่ามีหมวดหมู่ชื่อนี้อยู่หรือไม่
     * 
     * @param name the category name to check
     * @return true if category exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Count products in a category.
     * นับจำนวนสินค้าในหมวดหมู่
     * 
     * @param categoryId the category ID
     * @param activeOnly whether to count only active products
     * @return count of products in the category
     */
    long countProductsInCategory(UUID categoryId, boolean activeOnly);

    // ========== Write Operations ==========

    /**
     * Create a new category.
     * สร้างหมวดหมู่ใหม่
     * 
     * @param name the category name
     * @param description the category description
     * @param parentId the parent category ID (null for root category)
     * @return the created category
     * @throws com.accountselling.platform.exception.ResourceAlreadyExistsException if category name already exists
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if parent category not found
     */
    Category createCategory(String name, String description, UUID parentId);

    /**
     * Create a new category with sort order.
     * สร้างหมวดหมู่ใหม่พร้อม sort order
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
     * อัปเดตข้อมูลหมวดหมู่
     * 
     * @param categoryId the category ID to update
     * @param name the new category name
     * @param description the new category description
     * @param sortOrder the new sort order
     * @return the updated category
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
     * @throws com.accountselling.platform.exception.ResourceAlreadyExistsException if new name already exists
     */
    Category updateCategory(UUID categoryId, String name, String description, Integer sortOrder);

    /**
     * Move category to a new parent.
     * ย้ายหมวดหมู่ไปยัง parent ใหม่
     * 
     * @param categoryId the category ID to move
     * @param newParentId the new parent category ID (null to make it root)
     * @return the updated category
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if category or parent not found
     * @throws IllegalArgumentException if moving would create circular dependency
     */
    Category moveCategory(UUID categoryId, UUID newParentId);

    /**
     * Set category active status.
     * กำหนดสถานะการเปิดใช้งานหมวดหมู่
     * 
     * @param categoryId the category ID
     * @param active the new active status
     * @return the updated category
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
     */
    Category setCategoryActive(UUID categoryId, boolean active);

    /**
     * Delete category by ID.
     * ลบหมวดหมู่ตาม ID
     * 
     * @param categoryId the category ID to delete
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
     * @throws IllegalStateException if category has products or subcategories
     */
    void deleteCategory(UUID categoryId);

    /**
     * Force delete category and move its products to another category.
     * บังคับลบหมวดหมู่และย้ายสินค้าไปยังหมวดหมู่อื่น
     * 
     * @param categoryId the category ID to delete
     * @param targetCategoryId the target category ID for moving products
     * @throws com.accountselling.platform.exception.ResourceNotFoundException if category not found
     */
    void deleteCategoryAndMoveProducts(UUID categoryId, UUID targetCategoryId);

    // ========== Validation & Utility Methods ==========

    /**
     * Validate category hierarchy (check for circular dependencies).
     * ตรวจสอบโครงสร้างหมวดหมู่ (ตรวจสอบการเกิด circular dependency)
     * 
     * @param categoryId the category ID
     * @param proposedParentId the proposed parent category ID
     * @return true if valid, false if would create circular dependency
     */
    boolean isValidHierarchy(UUID categoryId, UUID proposedParentId);

    /**
     * Check if category can be deleted safely.
     * ตรวจสอบว่าสามารถลบหมวดหมู่ได้อย่างปลอดภัยหรือไม่
     * 
     * @param categoryId the category ID to check
     * @return true if can be deleted safely, false otherwise
     */
    boolean canDeleteCategory(UUID categoryId);

    /**
     * Get category breadcrumb path.
     * ดึง breadcrumb path ของหมวดหมู่
     * 
     * @param categoryId the category ID
     * @return list of categories from root to the specified category
     */
    List<Category> getCategoryPath(UUID categoryId);

    /**
     * Reorder categories within the same parent.
     * จัดเรียงหมวดหมู่ใหม่ภายใต้ parent เดียวกัน
     * 
     * @param parentId the parent category ID (null for root categories)
     * @param categoryIds list of category IDs in the desired order
     */
    void reorderCategories(UUID parentId, List<UUID> categoryIds);
}