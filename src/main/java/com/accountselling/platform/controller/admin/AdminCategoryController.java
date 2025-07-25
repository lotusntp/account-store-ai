package com.accountselling.platform.controller.admin;

import com.accountselling.platform.dto.category.CategoryCreateRequestDto;
import com.accountselling.platform.dto.category.CategoryResponseDto;
import com.accountselling.platform.dto.category.CategoryUpdateRequestDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for category management operations. Provides CRUD operations for categories with
 * admin privileges.
 *
 * <p>Admin controller สำหรับการจัดการหมวดหมู่ ให้บริการ CRUD operations สำหรับหมวดหมู่ด้วยสิทธิ์
 * admin
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Admin Category Management",
    description = "Admin endpoints for category CRUD operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

  private final CategoryService categoryService;

  @Operation(
      summary = "Get all categories (admin)",
      description = "Retrieve all categories including inactive ones. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Categories retrieved successfully",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping
  public ResponseEntity<List<CategoryResponseDto>> getAllCategories(
      @Parameter(description = "Include only active categories")
          @RequestParam(value = "activeOnly", defaultValue = "false")
          boolean activeOnly) {

    log.info("Admin getting all categories with activeOnly={}", activeOnly);

    List<Category> categories;
    if (activeOnly) {
      categories = categoryService.findActiveCategories();
    } else {
      categories = categoryService.findAllCategories();
    }

    List<CategoryResponseDto> response =
        categories.stream().map(this::convertToDto).collect(Collectors.toList());

    log.info("Admin retrieved {} categories", response.size());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get category by ID (admin)",
      description = "Retrieve detailed information about a specific category. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Category retrieved successfully",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/{id}")
  public ResponseEntity<CategoryResponseDto> getCategoryById(
      @Parameter(description = "Category ID", required = true) @PathVariable UUID id) {

    log.info("Admin getting category by id: {}", id);

    Category category =
        categoryService
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));

    CategoryResponseDto response = convertToDetailedDto(category);

    log.info("Admin retrieved category: {}", category.getName());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Create new category",
      description = "Create a new category with the provided information. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Category created successfully",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid category data or category name already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Parent category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PostMapping
  public ResponseEntity<CategoryResponseDto> createCategory(
      @Parameter(description = "Category creation data", required = true) @Valid @RequestBody
          CategoryCreateRequestDto request) {

    log.info("Admin creating category with name: {}", request.getName());

    Category createdCategory =
        categoryService.createCategory(
            request.getName(),
            request.getDescription(),
            request.getParentId(),
            request.getSortOrder());

    CategoryResponseDto response = convertToDto(createdCategory);

    log.info(
        "Admin created category: {} with id: {}",
        createdCategory.getName(),
        createdCategory.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Update category",
      description = "Update an existing category with new information. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Category updated successfully",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid category data or category name already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PutMapping("/{id}")
  public ResponseEntity<CategoryResponseDto> updateCategory(
      @Parameter(description = "Category ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Category update data", required = true) @Valid @RequestBody
          CategoryUpdateRequestDto request) {

    log.info("Admin updating category with id: {}", id);

    Category updatedCategory =
        categoryService.updateCategory(
            id, request.getName(), request.getDescription(), request.getSortOrder());

    // Update active status if provided
    if (request.getActive() != null) {
      updatedCategory = categoryService.setCategoryActive(id, request.getActive());
    }

    CategoryResponseDto response = convertToDto(updatedCategory);

    log.info("Admin updated category: {} with id: {}", updatedCategory.getName(), id);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Move category to new parent",
      description =
          "Move a category to a new parent category or make it a root category. Admin only"
              + " endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Category moved successfully",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid move operation (would create circular dependency)"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Category or parent category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PutMapping("/{id}/move")
  public ResponseEntity<CategoryResponseDto> moveCategory(
      @Parameter(description = "Category ID to move", required = true) @PathVariable UUID id,
      @Parameter(description = "New parent category ID (null to make root category)")
          @RequestParam(required = false)
          UUID parentId) {

    log.info("Admin moving category {} to parent: {}", id, parentId);

    Category movedCategory = categoryService.moveCategory(id, parentId);
    CategoryResponseDto response = convertToDto(movedCategory);

    log.info("Admin moved category: {} to parent: {}", movedCategory.getName(), parentId);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Set category active status",
      description = "Enable or disable a category. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Category status updated successfully",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PutMapping("/{id}/status")
  public ResponseEntity<CategoryResponseDto> setCategoryStatus(
      @Parameter(description = "Category ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Active status", required = true) @RequestParam boolean active) {

    log.info("Admin setting category {} active status to: {}", id, active);

    Category updatedCategory = categoryService.setCategoryActive(id, active);
    CategoryResponseDto response = convertToDto(updatedCategory);

    log.info("Admin set category: {} active status to: {}", updatedCategory.getName(), active);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Delete category",
      description =
          "Delete a category if it has no products or subcategories. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Category cannot be deleted (has products or subcategories)"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCategory(
      @Parameter(description = "Category ID", required = true) @PathVariable UUID id) {

    log.info("Admin deleting category with id: {}", id);

    // Get category name for logging before deletion
    Category category =
        categoryService
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));

    categoryService.deleteCategory(id);

    log.info("Admin deleted category: {} with id: {}", category.getName(), id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Force delete category and move products",
      description =
          "Delete a category and move its products to another category. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Category deleted and products moved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Category or target category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @DeleteMapping("/{id}/force")
  public ResponseEntity<Void> forceDeleteCategory(
      @Parameter(description = "Category ID to delete", required = true) @PathVariable UUID id,
      @Parameter(description = "Target category ID for moving products", required = true)
          @RequestParam
          UUID targetCategoryId) {

    log.info("Admin force deleting category {} and moving products to: {}", id, targetCategoryId);

    // Get category name for logging before deletion
    Category category =
        categoryService
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));

    categoryService.deleteCategoryAndMoveProducts(id, targetCategoryId);

    log.info(
        "Admin force deleted category: {} and moved products to category: {}",
        category.getName(),
        targetCategoryId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Reorder categories",
      description = "Reorder categories within the same parent. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Categories reordered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid reorder request"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Parent category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PutMapping("/reorder")
  public ResponseEntity<Void> reorderCategories(
      @Parameter(description = "Parent category ID (null for root categories)")
          @RequestParam(required = false)
          UUID parentId,
      @Parameter(description = "Ordered list of category IDs", required = true) @RequestBody
          List<UUID> categoryIds) {

    log.info("Admin reordering categories under parent: {} with order: {}", parentId, categoryIds);

    categoryService.reorderCategories(parentId, categoryIds);

    log.info("Admin reordered {} categories under parent: {}", categoryIds.size(), parentId);
    return ResponseEntity.ok().build();
  }

  /**
   * Convert Category entity to basic CategoryResponseDto. แปลง Category entity เป็น
   * CategoryResponseDto พื้นฐาน
   */
  private CategoryResponseDto convertToDto(Category category) {
    CategoryResponseDto.CategoryResponseDtoBuilder builder =
        CategoryResponseDto.builder()
            .id(category.getId())
            .name(category.getName())
            .description(category.getDescription())
            .active(category.getActive())
            .sortOrder(category.getSortOrder())
            .level(calculateLevel(category))
            .fullPath(buildFullPath(category))
            .productCount(categoryService.countProductsInCategory(category.getId(), false))
            .activeProductCount(categoryService.countProductsInCategory(category.getId(), true))
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt());

    // Add parent information if exists
    if (category.getParentCategory() != null) {
      builder.parent(
          CategoryResponseDto.ParentInfo.builder()
              .id(category.getParentCategory().getId())
              .name(category.getParentCategory().getName())
              .fullPath(buildFullPath(category.getParentCategory()))
              .build());
    }

    return builder.build();
  }

  /**
   * Convert Category entity to detailed CategoryResponseDto with subcategories. แปลง Category
   * entity เป็น CategoryResponseDto แบบรายละเอียดพร้อมหมวดหมู่ย่อย
   */
  private CategoryResponseDto convertToDetailedDto(Category category) {
    CategoryResponseDto dto = convertToDto(category);

    // Add subcategories (admin can see all subcategories, not just active ones)
    List<Category> subcategories = categoryService.findSubcategories(category.getId());
    if (!subcategories.isEmpty()) {
      List<CategoryResponseDto> subcategoryDtos =
          subcategories.stream().map(this::convertToDto).collect(Collectors.toList());
      dto.setSubcategories(subcategoryDtos);
    }

    return dto;
  }

  /**
   * Calculate category level in hierarchy (0 for root categories). คำนวณระดับของหมวดหมู่ในลำดับชั้น
   * (0 สำหรับหมวดหมู่ราก)
   */
  private Integer calculateLevel(Category category) {
    int level = 0;
    Category current = category;
    while (current.getParentCategory() != null) {
      level++;
      current = current.getParentCategory();
    }
    return level;
  }

  /**
   * Build full path string for category (e.g., "Games > Action > FPS"). สร้าง path
   * แบบเต็มสำหรับหมวดหมู่ (เช่น "เกมส์ > แอ็คชั่น > FPS")
   */
  private String buildFullPath(Category category) {
    if (category.getParentCategory() == null) {
      return category.getName();
    }
    return buildFullPath(category.getParentCategory()) + " > " + category.getName();
  }
}
