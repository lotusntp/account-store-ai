package com.accountselling.platform.controller.admin;

import com.accountselling.platform.dto.product.ProductCreateRequestDto;
import com.accountselling.platform.dto.product.ProductResponseDto;
import com.accountselling.platform.dto.product.ProductUpdateRequestDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Admin controller for product management operations.
 * Provides CRUD operations for products with admin privileges.
 * 
 * Admin controller สำหรับการจัดการสินค้า
 * ให้บริการ CRUD operations สำหรับสินค้าด้วยสิทธิ์ admin
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Product Management", description = "Admin endpoints for product CRUD operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @Operation(
        summary = "Get all products (admin)",
        description = "Retrieve all products including inactive ones with pagination. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(value = "sortBy", defaultValue = "sortOrder") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(value = "sortDirection", defaultValue = "asc") String sortDirection,
            @Parameter(description = "Include only active products")
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        
        log.info("Admin getting all products - page: {}, size: {}, activeOnly: {}", page, size, activeOnly);
        
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        
        Page<Product> productPage;
        if (activeOnly) {
            productPage = productService.findActiveProducts(pageable);
        } else {
            productPage = productService.findAllProducts(pageable);
        }
        
        Page<ProductResponseDto> response = productPage.map(this::convertToDto);
        
        log.info("Admin retrieved {} products out of {} total", response.getNumberOfElements(), response.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get product by ID (admin)",
        description = "Retrieve detailed information about a specific product. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin getting product by id: {}", id);
        
        Product product = productService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        
        ProductResponseDto response = convertToDto(product);
        
        log.info("Admin retrieved product: {}", product.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Create new product",
        description = "Create a new product with the provided information. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid product data"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @Parameter(description = "Product creation data", required = true)
            @Valid @RequestBody ProductCreateRequestDto request) {
        
        log.info("Admin creating product with name: {}", request.getName());
        
        Product createdProduct = productService.createProduct(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategoryId(),
                request.getServer(),
                request.getImageUrl(),
                request.getSortOrder(),
                request.getLowStockThreshold()
        );
        
        ProductResponseDto response = convertToDto(createdProduct);
        
        log.info("Admin created product: {} with id: {}", createdProduct.getName(), createdProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Update product",
        description = "Update an existing product with new information. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid product data or no fields to update"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product or category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Product update data", required = true)
            @Valid @RequestBody ProductUpdateRequestDto request) {
        
        log.info("Admin updating product with id: {}", id);
        
        if (!request.hasUpdateFields()) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }
        
        Product updatedProduct = productService.updateProduct(
                id,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategoryId(),
                request.getServer(),
                request.getImageUrl()
        );
        ProductResponseDto response = convertToDto(updatedProduct);
        
        log.info("Admin updated product: {} with id: {}", updatedProduct.getName(), id);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Set product active status",
        description = "Enable or disable a product. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product status updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ProductResponseDto> setProductStatus(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Active status", required = true)
            @RequestParam boolean active) {
        
        log.info("Admin setting product {} active status to: {}", id, active);
        
        Product updatedProduct = productService.setProductActive(id, active);
        ProductResponseDto response = convertToDto(updatedProduct);
        
        log.info("Admin set product: {} active status to: {}", updatedProduct.getName(), active);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Move product to different category",
        description = "Move a product to a different category. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product moved successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product or category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/category")
    public ResponseEntity<ProductResponseDto> moveProductToCategory(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New category ID", required = true)
            @RequestParam UUID categoryId) {
        
        log.info("Admin moving product {} to category: {}", id, categoryId);
        
        Product updatedProduct = productService.moveProductToCategory(id, categoryId);
        ProductResponseDto response = convertToDto(updatedProduct);
        
        log.info("Admin moved product: {} to category: {}", updatedProduct.getName(), categoryId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Delete product",
        description = "Delete a product if it has no pending orders. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Product cannot be deleted (has pending orders or stock)"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin deleting product with id: {}", id);
        
        // Get product name for logging before deletion
        Product product = productService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        
        productService.deleteProduct(id);
        
        log.info("Admin deleted product: {} with id: {}", product.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Force delete product",
        description = "Force delete a product even if it has stock or orders. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product force deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}/force")
    public ResponseEntity<Void> forceDeleteProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin force deleting product with id: {}", id);
        
        // Get product name for logging before deletion
        Product product = productService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        
        productService.forceDeleteProduct(id);
        
        log.info("Admin force deleted product: {} with id: {}", product.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Get products by category (admin)",
        description = "Retrieve products in a specific category including inactive ones. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponseDto>> getProductsByCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID categoryId,
            @Parameter(description = "Include products from subcategories")
            @RequestParam(value = "includeSubcategories", defaultValue = "false") boolean includeSubcategories,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(value = "sortBy", defaultValue = "sortOrder") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(value = "sortDirection", defaultValue = "asc") String sortDirection,
            @Parameter(description = "Include only active products")
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        
        log.info("Admin getting products by category: {} with includeSubcategories: {}, activeOnly: {}", 
                categoryId, includeSubcategories, activeOnly);
        
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        
        Page<Product> productPage = productService.findProductsByCategory(categoryId, includeSubcategories, activeOnly, pageable);
        Page<ProductResponseDto> response = productPage.map(this::convertToDto);
        
        log.info("Admin retrieved {} products for category: {}", response.getNumberOfElements(), categoryId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get available servers",
        description = "Retrieve list of distinct server names from all products. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Servers retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/servers")
    public ResponseEntity<List<String>> getAvailableServers() {
        
        log.info("Admin getting available servers");
        
        List<String> servers = productService.getAvailableServers();
        
        log.info("Admin retrieved {} available servers", servers.size());
        return ResponseEntity.ok(servers);
    }

    /**
     * Create pageable object from request parameters.
     * สร้าง pageable object จาก request parameters
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        // Validate pagination parameters
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        
        return PageRequest.of(page, size, sort);
    }

    /**
     * Convert Product entity to ProductResponseDto.
     * แปลง Product entity เป็น ProductResponseDto
     */
    private ProductResponseDto convertToDto(Product product) {
        ProductService.ProductStockInfo stockInfo = productService.getProductStockInfo(product.getId());
        
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .formattedPrice(formatPrice(product.getPrice()))
                .imageUrl(product.getImageUrl())
                .server(product.getServer())
                .active(product.getActive())
                .sortOrder(product.getSortOrder())
                .category(ProductResponseDto.CategoryInfo.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .fullPath(buildCategoryPath(product.getCategory()))
                        .active(product.getCategory().getActive())
                        .build())
                .stock(ProductResponseDto.StockInfo.builder()
                        .totalStock(stockInfo.getTotalStock())
                        .availableStock(stockInfo.getAvailableStock())
                        .soldStock(stockInfo.getSoldStock())
                        .reservedStock(stockInfo.getReservedStock())
                        .inStock(stockInfo.isInStock())
                        .lowStock(stockInfo.isLowStock())
                        .lowStockThreshold(product.getLowStockThreshold())
                        .build())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Format price for display.
     * จัดรูปแบบราคาสำหรับการแสดงผล
     */
    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0.00";
        }
        return String.format("%.2f", price);
    }

    /**
     * Build category path string.
     * สร้าง path ของหมวดหมู่เป็น string
     */
    private String buildCategoryPath(com.accountselling.platform.model.Category category) {
        if (category.getParentCategory() == null) {
            return category.getName();
        }
        return buildCategoryPath(category.getParentCategory()) + " > " + category.getName();
    }
}