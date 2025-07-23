package com.accountselling.platform.service.impl;

import com.accountselling.platform.exception.ResourceAlreadyExistsException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.repository.CategoryRepository;
import com.accountselling.platform.service.CategoryService;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Implementation of CategoryService for category management operations. Handles category hierarchy
 * management, search, validation, and CRUD operations.
 *
 * <p>การใช้งาน CategoryService สำหรับการจัดการหมวดหมู่ รองรับการจัดการแบบลำดับชั้น การค้นหา
 * การตรวจสอบ และการจัดการข้อมูล
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  private final CategoryRepository categoryRepository;

  // ========== Read Operations ==========

  @Override
  @Transactional(readOnly = true)
  public Optional<Category> findById(UUID id) {
    log.debug("Finding category by ID: {}", id);
    return categoryRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Category> findByName(String name) {
    log.debug("Finding category by name: {}", name);
    return categoryRepository.findByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> findAllCategories() {
    log.debug("Finding all categories");
    return categoryRepository.findAllByOrderBySortOrderAscNameAsc();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> findActiveCategories() {
    log.debug("Finding all active categories");
    return categoryRepository.findActiveOrderedBySort();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> findRootCategories() {
    log.debug("Finding root categories");
    return categoryRepository.findRootCategories();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> findActiveRootCategories() {
    log.debug("Finding active root categories");
    return categoryRepository.findActiveRootCategories();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> findSubcategories(UUID parentId) {
    log.debug("Finding subcategories for parent ID: {}", parentId);
    return categoryRepository.findByParentCategoryId(parentId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> findActiveSubcategories(UUID parentId) {
    log.debug("Finding active subcategories for parent ID: {}", parentId);
    return categoryRepository.findActiveByParentCategoryId(parentId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> searchCategoriesByName(String name, boolean activeOnly) {
    log.debug("Searching categories by name: {}, activeOnly: {}", name, activeOnly);

    if (!StringUtils.hasText(name)) {
      return Collections.emptyList();
    }

    if (activeOnly) {
      return categoryRepository.findByNameContainingIgnoreCaseAndActive(name, true);
    } else {
      return categoryRepository.findByNameContainingIgnoreCase(name);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getCategoryHierarchy(boolean activeOnly) {
    log.debug("Getting category hierarchy, activeOnly: {}", activeOnly);

    List<Category> rootCategories =
        activeOnly
            ? categoryRepository.findActiveRootCategories()
            : categoryRepository.findRootCategories();

    // Load subcategories for each root category
    for (Category root : rootCategories) {
      loadSubcategoriesRecursively(root, activeOnly);
    }

    return rootCategories;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getAllDescendants(UUID categoryId, boolean includeInactive) {
    log.debug(
        "Getting all descendants for category ID: {}, includeInactive: {}",
        categoryId,
        includeInactive);

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

    List<Category> descendants = new ArrayList<>();
    collectDescendantsRecursively(category, descendants, includeInactive);

    return descendants;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByName(String name) {
    return categoryRepository.existsByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public long countProductsInCategory(UUID categoryId, boolean activeOnly) {
    log.debug("Counting products in category ID: {}, activeOnly: {}", categoryId, activeOnly);

    if (activeOnly) {
      return categoryRepository.countActiveProductsByCategoryId(categoryId);
    } else {
      return categoryRepository.countProductsByCategoryId(categoryId);
    }
  }

  // ========== Write Operations ==========

  @Override
  @Transactional
  public Category createCategory(String name, String description, UUID parentId) {
    return createCategory(name, description, parentId, 0);
  }

  @Override
  @Transactional
  public Category createCategory(
      String name, String description, UUID parentId, Integer sortOrder) {
    log.info("Creating category with name: {}, parentId: {}", name, parentId);

    validateCategoryName(name);

    if (categoryRepository.existsByName(name)) {
      throw new ResourceAlreadyExistsException("Category name already exists: " + name);
    }

    Category parentCategory = null;
    if (parentId != null) {
      parentCategory =
          categoryRepository
              .findById(parentId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Parent category not found with ID: " + parentId));
    }

    Category category = new Category(name, description, parentCategory);
    category.setSortOrder(sortOrder != null ? sortOrder : 0);
    category.setActive(true);

    Category savedCategory = categoryRepository.save(category);
    log.info("Successfully created category with ID: {}", savedCategory.getId());

    return savedCategory;
  }

  @Override
  @Transactional
  public Category updateCategory(
      UUID categoryId, String name, String description, Integer sortOrder) {
    log.info("Updating category ID: {}", categoryId);

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

    if (StringUtils.hasText(name) && !name.equals(category.getName())) {
      validateCategoryName(name);

      if (categoryRepository.existsByName(name)) {
        throw new ResourceAlreadyExistsException("Category name already exists: " + name);
      }
      category.setName(name);
    }

    if (description != null) {
      category.setDescription(description);
    }

    if (sortOrder != null) {
      category.setSortOrder(sortOrder);
    }

    Category updatedCategory = categoryRepository.save(category);
    log.info("Successfully updated category ID: {}", categoryId);

    return updatedCategory;
  }

  @Override
  @Transactional
  public Category moveCategory(UUID categoryId, UUID newParentId) {
    log.info("Moving category ID: {} to new parent ID: {}", categoryId, newParentId);

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

    Category newParent = null;
    if (newParentId != null) {
      newParent =
          categoryRepository
              .findById(newParentId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Parent category not found with ID: " + newParentId));

      // Check for circular dependency
      if (!isValidHierarchy(categoryId, newParentId)) {
        throw new IllegalArgumentException("Moving category would create circular dependency");
      }
    }

    category.setParentCategory(newParent);
    Category movedCategory = categoryRepository.save(category);

    log.info("Successfully moved category ID: {} to new parent", categoryId);
    return movedCategory;
  }

  @Override
  @Transactional
  public Category setCategoryActive(UUID categoryId, boolean active) {
    log.info("Setting category ID: {} active status to: {}", categoryId, active);

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

    category.setActive(active);
    Category updatedCategory = categoryRepository.save(category);

    log.info("Successfully set category ID: {} active status to: {}", categoryId, active);
    return updatedCategory;
  }

  @Override
  @Transactional
  public void deleteCategory(UUID categoryId) {
    log.info("Deleting category ID: {}", categoryId);

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

    if (!canDeleteCategory(categoryId)) {
      throw new IllegalStateException("Cannot delete category that has products or subcategories");
    }

    categoryRepository.delete(category);
    log.info("Successfully deleted category ID: {}", categoryId);
  }

  @Override
  @Transactional
  public void deleteCategoryAndMoveProducts(UUID categoryId, UUID targetCategoryId) {
    log.info(
        "Force deleting category ID: {} and moving products to category ID: {}",
        categoryId,
        targetCategoryId);

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

    Category targetCategory =
        categoryRepository
            .findById(targetCategoryId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Target category not found with ID: " + targetCategoryId));

    // Move all products to target category
    category
        .getProducts()
        .forEach(
            product -> {
              product.setCategory(targetCategory);
              targetCategory.addProduct(product);
            });

    // Move all subcategories to target category
    category
        .getSubCategories()
        .forEach(
            subcategory -> {
              subcategory.setParentCategory(targetCategory);
              targetCategory.addSubCategory(subcategory);
            });

    categoryRepository.delete(category);
    log.info(
        "Successfully force deleted category ID: {} and moved products/subcategories", categoryId);
  }

  // ========== Validation & Utility Methods ==========

  @Override
  @Transactional(readOnly = true)
  public boolean isValidHierarchy(UUID categoryId, UUID proposedParentId) {
    if (categoryId.equals(proposedParentId)) {
      return false; // Category cannot be its own parent
    }

    if (proposedParentId == null) {
      return true; // Root category is always valid
    }

    Category proposedParent = categoryRepository.findById(proposedParentId).orElse(null);
    if (proposedParent == null) {
      return false; // Parent doesn't exist
    }

    // Check if proposed parent is a descendant of the category
    Category current = proposedParent;
    while (current != null) {
      if (current.getId().equals(categoryId)) {
        return false; // Circular dependency detected
      }
      current = current.getParentCategory();
    }

    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canDeleteCategory(UUID categoryId) {
    Category category = categoryRepository.findById(categoryId).orElse(null);
    if (category == null) {
      return false;
    }

    return category.getProducts().isEmpty() && category.getSubCategories().isEmpty();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getCategoryPath(UUID categoryId) {
    log.debug("Getting category path for ID: {}", categoryId);

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

    List<Category> path = new ArrayList<>();
    Category current = category;

    while (current != null) {
      path.add(0, current); // Add to beginning of list
      current = current.getParentCategory();
    }

    return path;
  }

  @Override
  @Transactional
  public void reorderCategories(UUID parentId, List<UUID> categoryIds) {
    log.info("Reordering {} categories under parent ID: {}", categoryIds.size(), parentId);

    List<Category> categories =
        categoryIds.stream()
            .map(
                id ->
                    categoryRepository
                        .findById(id)
                        .orElseThrow(
                            () ->
                                new ResourceNotFoundException("Category not found with ID: " + id)))
            .collect(Collectors.toList());

    // Validate all categories belong to the same parent
    UUID expectedParentId = parentId;
    for (Category category : categories) {
      UUID actualParentId =
          category.getParentCategory() != null ? category.getParentCategory().getId() : null;

      if (!Objects.equals(expectedParentId, actualParentId)) {
        throw new IllegalArgumentException("All categories must belong to the same parent");
      }
    }

    // Update sort orders
    for (int i = 0; i < categories.size(); i++) {
      categories.get(i).setSortOrder(i);
    }

    categoryRepository.saveAll(categories);
    log.info("Successfully reordered {} categories", categories.size());
  }

  // ========== Private Helper Methods ==========

  private void validateCategoryName(String name) {
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("Category name cannot be blank");
    }
    if (name.length() > 100) {
      throw new IllegalArgumentException("Category name cannot exceed 100 characters");
    }
  }

  private void loadSubcategoriesRecursively(Category category, boolean activeOnly) {
    List<Category> subcategories =
        activeOnly
            ? categoryRepository.findActiveByParentCategoryId(category.getId())
            : categoryRepository.findByParentCategoryId(category.getId());

    category.getSubCategories().clear();
    category.getSubCategories().addAll(subcategories);

    // Recursively load subcategories for each child
    for (Category subcategory : subcategories) {
      loadSubcategoriesRecursively(subcategory, activeOnly);
    }
  }

  private void collectDescendantsRecursively(
      Category category, List<Category> descendants, boolean includeInactive) {
    List<Category> subcategories =
        includeInactive
            ? categoryRepository.findByParentCategoryId(category.getId())
            : categoryRepository.findActiveByParentCategoryId(category.getId());

    for (Category subcategory : subcategories) {
      descendants.add(subcategory);
      collectDescendantsRecursively(subcategory, descendants, includeInactive);
    }
  }
}
