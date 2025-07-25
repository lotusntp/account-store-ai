package com.accountselling.platform.controller;

import com.accountselling.platform.dto.product.ProductResponseDto;
import com.accountselling.platform.dto.product.ProductSearchRequestDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for product management operations. Provides endpoints for browsing products,
 * product search, and filtering.
 *
 * <p>REST Controller สำหรับการจัดการสินค้า ให้บริการ endpoints สำหรับการเรียกดูสินค้า
 * การค้นหาสินค้า และการกรอง
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Product Management",
    description = "Endpoints for product browsing, search, and filtering operations")
public class ProductController {

  private final ProductService productService;

  @Operation(
      summary = "Get all products",
      description =
          "Retrieve paginated list of products with optional filtering by active status and stock"
              + " availability.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping
  public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
      @Parameter(description = "Page number (0-based)")
          @RequestParam(value = "page", defaultValue = "0")
          int page,
      @Parameter(description = "Page size (1-100)")
          @RequestParam(value = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Sort field")
          @RequestParam(value = "sortBy", defaultValue = "sortOrder")
          String sortBy,
      @Parameter(description = "Sort direction (asc/desc)")
          @RequestParam(value = "sortDirection", defaultValue = "asc")
          String sortDirection,
      @Parameter(description = "Include only active products")
          @RequestParam(value = "activeOnly", defaultValue = "true")
          boolean activeOnly,
      @Parameter(description = "Include only products with available stock")
          @RequestParam(value = "inStockOnly", defaultValue = "false")
          boolean inStockOnly) {

    log.info(
        "Getting all products - page: {}, size: {}, activeOnly: {}, inStockOnly: {}",
        page,
        size,
        activeOnly,
        inStockOnly);

    Pageable pageable = createPageable(page, size, sortBy, sortDirection);

    Page<Product> productPage;
    if (inStockOnly) {
      productPage = productService.findProductsWithAvailableStock(pageable);
    } else if (activeOnly) {
      productPage = productService.findActiveProducts(pageable);
    } else {
      productPage = productService.findAllProducts(pageable);
    }

    Page<ProductResponseDto> response = productPage.map(this::convertToDto);

    log.info(
        "Retrieved {} products out of {} total",
        response.getNumberOfElements(),
        response.getTotalElements());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get product by ID",
      description =
          "Retrieve detailed information about a specific product including stock information.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/{id}")
  public ResponseEntity<ProductResponseDto> getProductById(
      @Parameter(description = "Product ID", required = true) @PathVariable UUID id) {

    log.info("Getting product by id: {}", id);

    Product product =
        productService
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

    ProductResponseDto response = convertToDetailedDto(product);

    log.info("Retrieved product: {}", product.getName());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get products by category",
      description =
          "Retrieve paginated list of products within a specific category with optional subcategory"
              + " inclusion.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/category/{categoryId}")
  public ResponseEntity<Page<ProductResponseDto>> getProductsByCategory(
      @Parameter(description = "Category ID", required = true) @PathVariable UUID categoryId,
      @Parameter(description = "Include products from subcategories")
          @RequestParam(value = "includeSubcategories", defaultValue = "false")
          boolean includeSubcategories,
      @Parameter(description = "Page number (0-based)")
          @RequestParam(value = "page", defaultValue = "0")
          int page,
      @Parameter(description = "Page size (1-100)")
          @RequestParam(value = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Sort field")
          @RequestParam(value = "sortBy", defaultValue = "sortOrder")
          String sortBy,
      @Parameter(description = "Sort direction (asc/desc)")
          @RequestParam(value = "sortDirection", defaultValue = "asc")
          String sortDirection,
      @Parameter(description = "Include only active products")
          @RequestParam(value = "activeOnly", defaultValue = "true")
          boolean activeOnly,
      @Parameter(description = "Include only products with available stock")
          @RequestParam(value = "inStockOnly", defaultValue = "false")
          boolean inStockOnly) {

    log.info(
        "Getting products by category: {} with includeSubcategories: {}, activeOnly: {},"
            + " inStockOnly: {}",
        categoryId,
        includeSubcategories,
        activeOnly,
        inStockOnly);

    Pageable pageable = createPageable(page, size, sortBy, sortDirection);

    Page<Product> productPage;
    if (inStockOnly) {
      productPage =
          productService.findProductsWithAvailableStockByCategory(
              categoryId, includeSubcategories, pageable);
    } else {
      productPage =
          productService.findProductsByCategory(
              categoryId, includeSubcategories, activeOnly, pageable);
    }

    Page<ProductResponseDto> response = productPage.map(this::convertToDto);

    log.info("Retrieved {} products for category: {}", response.getNumberOfElements(), categoryId);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Search products",
      description =
          "Advanced product search with multiple filtering criteria including name, category, price"
              + " range, server, and stock status.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/search")
  public ResponseEntity<Page<ProductResponseDto>> searchProducts(
      @Valid ProductSearchRequestDto searchRequest) {

    log.info(
        "Searching products with criteria: name={}, categoryId={}, server={}, priceRange=[{}-{}],"
            + " inStock={}, activeOnly={}",
        searchRequest.getName(),
        searchRequest.getCategoryId(),
        searchRequest.getServer(),
        searchRequest.getMinPrice(),
        searchRequest.getMaxPrice(),
        searchRequest.getInStock(),
        searchRequest.getActiveOnly());

    // Validate price range
    if (!searchRequest.isValidPriceRange()) {
      throw new IllegalArgumentException(
          "Maximum price must be greater than or equal to minimum price");
    }

    Pageable pageable =
        createPageable(
            searchRequest.getPage(),
            searchRequest.getSize(),
            searchRequest.getSortBy(),
            searchRequest.getSortDirection());

    // Convert DTO to service criteria
    ProductService.ProductSearchCriteria criteria = new ProductService.ProductSearchCriteria();
    criteria.setName(searchRequest.getName());
    criteria.setCategoryId(searchRequest.getCategoryId());
    criteria.setIncludeSubcategories(searchRequest.getIncludeSubcategories());
    criteria.setServer(searchRequest.getServer());
    criteria.setMinPrice(searchRequest.getMinPrice());
    criteria.setMaxPrice(searchRequest.getMaxPrice());
    criteria.setInStock(searchRequest.getInStock());
    criteria.setActiveOnly(searchRequest.getActiveOnly());

    Page<Product> productPage = productService.searchProducts(criteria, pageable);
    Page<ProductResponseDto> response = productPage.map(this::convertToDto);

    log.info(
        "Search found {} products out of {} total",
        response.getNumberOfElements(),
        response.getTotalElements());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Search products by name",
      description =
          "Simple product search by name with support for partial matching and Thai language.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/search/name")
  public ResponseEntity<Page<ProductResponseDto>> searchProductsByName(
      @Parameter(description = "Product name to search for (minimum 2 characters)", required = true)
          @RequestParam
          @Size(min = 2, message = "Search query must be at least 2 characters")
          String name,
      @Parameter(description = "Page number (0-based)")
          @RequestParam(value = "page", defaultValue = "0")
          int page,
      @Parameter(description = "Page size (1-100)")
          @RequestParam(value = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Sort field")
          @RequestParam(value = "sortBy", defaultValue = "sortOrder")
          String sortBy,
      @Parameter(description = "Sort direction (asc/desc)")
          @RequestParam(value = "sortDirection", defaultValue = "asc")
          String sortDirection,
      @Parameter(description = "Include only active products")
          @RequestParam(value = "activeOnly", defaultValue = "true")
          boolean activeOnly) {

    log.info("Searching products by name: '{}' with activeOnly={}", name, activeOnly);

    Pageable pageable = createPageable(page, size, sortBy, sortDirection);

    Page<Product> productPage =
        productService.searchProductsByName(name.trim(), activeOnly, pageable);
    Page<ProductResponseDto> response = productPage.map(this::convertToDto);

    log.info("Found {} products matching name search: '{}'", response.getNumberOfElements(), name);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get products by server",
      description = "Retrieve products filtered by server name with pagination support.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/server/{server}")
  public ResponseEntity<Page<ProductResponseDto>> getProductsByServer(
      @Parameter(description = "Server name", required = true) @PathVariable String server,
      @Parameter(description = "Page number (0-based)")
          @RequestParam(value = "page", defaultValue = "0")
          int page,
      @Parameter(description = "Page size (1-100)")
          @RequestParam(value = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Sort field")
          @RequestParam(value = "sortBy", defaultValue = "sortOrder")
          String sortBy,
      @Parameter(description = "Sort direction (asc/desc)")
          @RequestParam(value = "sortDirection", defaultValue = "asc")
          String sortDirection,
      @Parameter(description = "Include only active products")
          @RequestParam(value = "activeOnly", defaultValue = "true")
          boolean activeOnly) {

    log.info("Getting products by server: '{}' with activeOnly={}", server, activeOnly);

    Pageable pageable = createPageable(page, size, sortBy, sortDirection);

    Page<Product> productPage = productService.findProductsByServer(server, activeOnly, pageable);
    Page<ProductResponseDto> response = productPage.map(this::convertToDto);

    log.info("Retrieved {} products for server: '{}'", response.getNumberOfElements(), server);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get products by price range",
      description = "Retrieve products within specified price range with pagination support.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid price parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/price-range")
  public ResponseEntity<Page<ProductResponseDto>> getProductsByPriceRange(
      @Parameter(description = "Minimum price (inclusive)", required = true)
          @RequestParam
          @DecimalMin(value = "0.01", message = "Minimum price must be greater than 0")
          BigDecimal minPrice,
      @Parameter(description = "Maximum price (inclusive)", required = true)
          @RequestParam
          @DecimalMin(value = "0.01", message = "Maximum price must be greater than 0")
          BigDecimal maxPrice,
      @Parameter(description = "Page number (0-based)")
          @RequestParam(value = "page", defaultValue = "0")
          int page,
      @Parameter(description = "Page size (1-100)")
          @RequestParam(value = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Sort field") @RequestParam(value = "sortBy", defaultValue = "price")
          String sortBy,
      @Parameter(description = "Sort direction (asc/desc)")
          @RequestParam(value = "sortDirection", defaultValue = "asc")
          String sortDirection,
      @Parameter(description = "Include only active products")
          @RequestParam(value = "activeOnly", defaultValue = "true")
          boolean activeOnly) {

    log.info(
        "Getting products by price range: [{} - {}] with activeOnly={}",
        minPrice,
        maxPrice,
        activeOnly);

    // Validate price range
    if (maxPrice.compareTo(minPrice) < 0) {
      throw new IllegalArgumentException(
          "Maximum price must be greater than or equal to minimum price");
    }

    Pageable pageable = createPageable(page, size, sortBy, sortDirection);

    Page<Product> productPage =
        productService.findProductsByPriceRange(minPrice, maxPrice, activeOnly, pageable);
    Page<ProductResponseDto> response = productPage.map(this::convertToDto);

    log.info(
        "Retrieved {} products in price range: [{} - {}]",
        response.getNumberOfElements(),
        minPrice,
        maxPrice);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get available servers",
      description = "Retrieve list of distinct server names from all products.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Servers retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/servers")
  public ResponseEntity<List<String>> getAvailableServers() {

    log.info("Getting available servers");

    List<String> servers = productService.getAvailableServers();

    log.info("Retrieved {} available servers", servers.size());
    return ResponseEntity.ok(servers);
  }

  /**
   * Create pageable object from request parameters. สร้าง pageable object จาก request parameters
   */
  private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
    // Validate pagination parameters
    if (page < 0) {
      throw new IllegalArgumentException("Page number cannot be negative");
    }
    if (size < 1 || size > 100) {
      throw new IllegalArgumentException("Page size must be between 1 and 100");
    }

    Sort.Direction direction =
        "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
    Sort sort = Sort.by(direction, sortBy);

    return PageRequest.of(page, size, sort);
  }

  /**
   * Convert Product entity to basic ProductResponseDto. แปลง Product entity เป็น ProductResponseDto
   * พื้นฐาน
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
        .category(
            ProductResponseDto.CategoryInfo.builder()
                .id(product.getCategory().getId())
                .name(product.getCategory().getName())
                .fullPath(buildCategoryPath(product.getCategory()))
                .active(product.getCategory().getActive())
                .build())
        .stock(
            ProductResponseDto.StockInfo.builder()
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
   * Convert Product entity to detailed ProductResponseDto. แปลง Product entity เป็น
   * ProductResponseDto แบบรายละเอียด
   */
  private ProductResponseDto convertToDetailedDto(Product product) {
    return convertToDto(product); // Same as basic for now, can be extended later
  }

  /** Format price for display. จัดรูปแบบราคาสำหรับการแสดงผล */
  private String formatPrice(BigDecimal price) {
    if (price == null) {
      return "0.00";
    }
    return String.format("%.2f", price);
  }

  /** Build category path string. สร้าง path ของหมวดหมู่เป็น string */
  private String buildCategoryPath(com.accountselling.platform.model.Category category) {
    if (category.getParentCategory() == null) {
      return category.getName();
    }
    return buildCategoryPath(category.getParentCategory()) + " > " + category.getName();
  }
}
