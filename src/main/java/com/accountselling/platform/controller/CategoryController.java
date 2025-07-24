package com.accountselling.platform.controller;

import com.accountselling.platform.dto.category.CategoryResponseDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for category management operations.
 * Provides endpoints for browsing categories, category hierarchy, and category search.
 * 
 * REST Controller สำหรับการจัดการหมวดหมู่สินค้า
 * ให้บริการ endpoints สำหรับการเรียกดูหมวดหมู่ ลำดับชั้นหมวดหมู่ และการค้นหาหมวดหมู่
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Category Management", description = "Endpoints for category browsing and search operations")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
        summary = "Get all categories",
        description = "Retrieve all categories with optional filtering by active status. Returns hierarchical category structure."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories(
            @Parameter(description = "Include only active categories")
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        
        log.info("Getting all categories with activeOnly={}", activeOnly);
        
        List<Category> categories;
        if (activeOnly) {
            categories = categoryService.findActiveCategories();
        } else {
            categories = categoryService.findAllCategories();
        }
        
        List<CategoryResponseDto> response = categories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Retrieved {} categories", response.size());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get category by ID",
        description = "Retrieve detailed information about a specific category including its subcategories and product count."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Getting category by id: {}", id);
        
        Category category = categoryService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
        
        CategoryResponseDto response = convertToDetailedDto(category);
        
        log.info("Retrieved category: {}", category.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get category hierarchy",
        description = "Retrieve complete category hierarchy starting from root categories. Includes all subcategories in tree structure."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category hierarchy retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/hierarchy")
    public ResponseEntity<List<CategoryResponseDto>> getCategoryHierarchy(
            @Parameter(description = "Include only active categories")
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        
        log.info("Getting category hierarchy with activeOnly={}", activeOnly);
        
        List<Category> rootCategories = categoryService.getCategoryHierarchy(activeOnly);
        
        List<CategoryResponseDto> response = rootCategories.stream()
                .map(this::convertToHierarchicalDto)
                .collect(Collectors.toList());
        
        log.info("Retrieved category hierarchy with {} root categories", response.size());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get root categories",
        description = "Retrieve only root categories (categories without parent). Useful for navigation menus."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Root categories retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/root")
    public ResponseEntity<List<CategoryResponseDto>> getRootCategories(
            @Parameter(description = "Include only active categories")
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        
        log.info("Getting root categories with activeOnly={}", activeOnly);
        
        List<Category> rootCategories;
        if (activeOnly) {
            rootCategories = categoryService.findActiveRootCategories();
        } else {
            rootCategories = categoryService.findRootCategories();
        }
        
        List<CategoryResponseDto> response = rootCategories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Retrieved {} root categories", response.size());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get subcategories",
        description = "Retrieve direct subcategories of a specific parent category."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subcategories retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Parent category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<List<CategoryResponseDto>> getSubcategories(
            @Parameter(description = "Parent category ID", required = true)
            @PathVariable UUID parentId,
            @Parameter(description = "Include only active subcategories")
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        
        log.info("Getting subcategories for parent: {} with activeOnly={}", parentId, activeOnly);
        
        // Verify parent category exists
        categoryService.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", parentId.toString()));
        
        List<Category> subcategories;
        if (activeOnly) {
            subcategories = categoryService.findActiveSubcategories(parentId);
        } else {
            subcategories = categoryService.findSubcategories(parentId);
        }
        
        List<CategoryResponseDto> response = subcategories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Retrieved {} subcategories for parent: {}", response.size(), parentId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Search categories",
        description = "Search categories by name using case-insensitive pattern matching. Supports partial matches."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories search completed successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<List<CategoryResponseDto>> searchCategories(
            @Parameter(description = "Category name to search for (minimum 2 characters)", required = true)
            @RequestParam @Size(min = 2, message = "Search query must be at least 2 characters") String name,
            @Parameter(description = "Include only active categories")
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        
        log.info("Searching categories with name: '{}' and activeOnly={}", name, activeOnly);
        
        List<Category> categories = categoryService.searchCategoriesByName(name.trim(), activeOnly);
        
        List<CategoryResponseDto> response = categories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Found {} categories matching search query: '{}'", response.size(), name);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get category path",
        description = "Get breadcrumb path from root to specified category. Returns list of categories in hierarchy order."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category path retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}/path")
    public ResponseEntity<List<CategoryResponseDto>> getCategoryPath(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Getting category path for id: {}", id);
        
        // Verify category exists
        categoryService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
        
        List<Category> categoryPath = categoryService.getCategoryPath(id);
        
        List<CategoryResponseDto> response = categoryPath.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Retrieved category path with {} levels for category: {}", response.size(), id);
        return ResponseEntity.ok(response);
    }

    /**
     * Convert Category entity to basic CategoryResponseDto.
     * แปลง Category entity เป็น CategoryResponseDto พื้นฐาน
     */
    private CategoryResponseDto convertToDto(Category category) {
        CategoryResponseDto.CategoryResponseDtoBuilder builder = CategoryResponseDto.builder()
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
            builder.parent(CategoryResponseDto.ParentInfo.builder()
                    .id(category.getParentCategory().getId())
                    .name(category.getParentCategory().getName())
                    .fullPath(buildFullPath(category.getParentCategory()))
                    .build());
        }

        return builder.build();
    }

    /**
     * Convert Category entity to detailed CategoryResponseDto with subcategories.
     * แปลง Category entity เป็น CategoryResponseDto แบบรายละเอียดพร้อมหมวดหมู่ย่อย
     */
    private CategoryResponseDto convertToDetailedDto(Category category) {
        CategoryResponseDto dto = convertToDto(category);
        
        // Add subcategories
        List<Category> subcategories = categoryService.findActiveSubcategories(category.getId());
        if (!subcategories.isEmpty()) {
            List<CategoryResponseDto> subcategoryDtos = subcategories.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            dto.setSubcategories(subcategoryDtos);
        }
        
        return dto;
    }

    /**
     * Convert Category entity to hierarchical CategoryResponseDto with all descendants.
     * แปลง Category entity เป็น CategoryResponseDto แบบลำดับชั้นพร้อมลูกหลานทั้งหมด
     */
    private CategoryResponseDto convertToHierarchicalDto(Category category) {
        CategoryResponseDto dto = convertToDto(category);
        
        // Recursively add subcategories
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            List<CategoryResponseDto> subcategoryDtos = category.getSubCategories().stream()
                    .filter(sub -> sub.getActive()) // Only include active subcategories
                    .map(this::convertToHierarchicalDto)
                    .collect(Collectors.toList());
            dto.setSubcategories(subcategoryDtos);
        }
        
        return dto;
    }

    /**
     * Calculate category level in hierarchy (0 for root categories).
     * คำนวณระดับของหมวดหมู่ในลำดับชั้น (0 สำหรับหมวดหมู่ราก)
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
     * Build full path string for category (e.g., "Games > Action > FPS").
     * สร้าง path แบบเต็มสำหรับหมวดหมู่ (เช่น "เกมส์ > แอ็คชั่น > FPS")
     */
    private String buildFullPath(Category category) {
        if (category.getParentCategory() == null) {
            return category.getName();
        }
        return buildFullPath(category.getParentCategory()) + " > " + category.getName();
    }
}