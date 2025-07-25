package com.accountselling.platform.controller.admin;

import com.accountselling.platform.dto.stock.*;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.service.ProductService;
import com.accountselling.platform.service.StockService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin controller for stock inventory management operations.
 * Provides CRUD operations for stock items with admin privileges.
 * 
 * Admin controller สำหรับการจัดการสต็อกสินค้า
 * ให้บริการ CRUD operations สำหรับสต็อกด้วยสิทธิ์ admin
 */
@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Stock Management", description = "Admin endpoints for stock inventory CRUD operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStockController {

    private final StockService stockService;
    private final ProductService productService;

    @Operation(
        summary = "Get stock by product",
        description = "Retrieve all stock items for a specific product with pagination. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock items retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StockResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<StockResponseDto>> getStockByProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID productId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection) {
        
        log.info("Admin getting stock for product: {} - page: {}, size: {}", productId, page, size);
        
        // Verify product exists
        productService.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));
        
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<Stock> stockPage = stockService.getStockByProduct(productId, pageable);
        
        Page<StockResponseDto> response = stockPage.map(this::convertToDto);
        
        log.info("Admin retrieved {} stock items for product: {}", response.getNumberOfElements(), productId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get stock by ID",
        description = "Retrieve detailed information about a specific stock item. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock item retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StockResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Stock item not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StockResponseDto> getStockById(
            @Parameter(description = "Stock ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin getting stock by id: {}", id);
        
        Stock stock = stockService.getStockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", id.toString()));
        
        StockResponseDto response = convertToDto(stock);
        
        log.info("Admin retrieved stock item for product: {}", stock.getProduct().getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Create new stock item",
        description = "Create a new stock item for a product. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Stock item created successfully",
                    content = @Content(schema = @Schema(implementation = StockResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid stock data or credentials already exist"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<StockResponseDto> createStock(
            @Parameter(description = "Stock creation data", required = true)
            @Valid @RequestBody StockCreateRequestDto request) {
        
        log.info("Admin creating stock for product: {}", request.getProductId());
        
        Stock createdStock = stockService.createStock(
                request.getProductId(),
                request.getCredentials(),
                request.getAdditionalInfo()
        );
        
        StockResponseDto response = convertToDto(createdStock);
        
        log.info("Admin created stock item for product: {} with id: {}", 
                createdStock.getProduct().getName(), createdStock.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Create multiple stock items in bulk",
        description = "Create multiple stock items for a product in a single operation. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Stock items created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid bulk stock data"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<StockResponseDto>> createBulkStock(
            @Parameter(description = "Bulk stock creation data", required = true)
            @Valid @RequestBody StockBulkCreateRequestDto request) {
        
        log.info("Admin creating {} stock items for product: {}", 
                request.getCredentialsList().size(), request.getProductId());
        
        List<Stock> createdStocks = stockService.createBulkStock(
                request.getProductId(),
                request.getCredentialsList()
        );
        
        List<StockResponseDto> response = createdStocks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Admin created {} stock items for product: {}", 
                response.size(), request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Update stock item",
        description = "Update additional information of a stock item. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock item updated successfully",
                    content = @Content(schema = @Schema(implementation = StockResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Stock item not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<StockResponseDto> updateStock(
            @Parameter(description = "Stock ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Stock update data", required = true)
            @Valid @RequestBody StockUpdateRequestDto request) {
        
        log.info("Admin updating stock with id: {}", id);
        
        Stock updatedStock = stockService.updateStockAdditionalInfo(id, request.getAdditionalInfo());
        StockResponseDto response = convertToDto(updatedStock);
        
        log.info("Admin updated stock item for product: {}", updatedStock.getProduct().getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Mark stock as sold",
        description = "Mark a stock item as sold. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock item marked as sold successfully",
                    content = @Content(schema = @Schema(implementation = StockResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Stock item already sold"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Stock item not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/sold")
    public ResponseEntity<StockResponseDto> markAsSold(
            @Parameter(description = "Stock ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin marking stock as sold: {}", id);
        
        Stock soldStock = stockService.markAsSold(id);
        StockResponseDto response = convertToDto(soldStock);
        
        log.info("Admin marked stock as sold for product: {}", soldStock.getProduct().getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Release stock reservation",
        description = "Release reservation of a stock item. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock reservation released successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Stock item not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/release")
    public ResponseEntity<Void> releaseReservation(
            @Parameter(description = "Stock ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin releasing reservation for stock: {}", id);
        
        boolean released = stockService.releaseReservation(id);
        
        if (released) {
            log.info("Admin released reservation for stock: {}", id);
            return ResponseEntity.ok().build();
        } else {
            log.warn("Admin attempted to release reservation for stock that was not reserved: {}", id);
            return ResponseEntity.ok().build(); // Still return OK as the end state is achieved
        }
    }

    @Operation(
        summary = "Delete stock item",
        description = "Delete a stock item if it's not sold or reserved. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Stock item deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Stock item cannot be deleted (sold or reserved)"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Stock item not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(
            @Parameter(description = "Stock ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin deleting stock with id: {}", id);
        
        // Get stock for logging before deletion
        Stock stock = stockService.getStockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", id.toString()));
        
        stockService.deleteStock(id);
        
        log.info("Admin deleted stock item for product: {}", stock.getProduct().getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Get stock statistics",
        description = "Get stock statistics for a specific product. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StockStatistics.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/product/{productId}/statistics")
    public ResponseEntity<StockStatistics> getStockStatistics(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID productId) {
        
        log.info("Admin getting stock statistics for product: {}", productId);
        
        // Verify product exists
        productService.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));
        
        StockStatistics statistics = stockService.getStockStatistics(productId);
        
        log.info("Admin retrieved stock statistics for product: {}", productId);
        return ResponseEntity.ok(statistics);
    }

    @Operation(
        summary = "Get products with low stock",
        description = "Get list of products that have low stock levels. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Low stock products retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/low-stock")
    public ResponseEntity<List<UUID>> getProductsWithLowStock(
            @Parameter(description = "Custom threshold (uses product's threshold if not specified)")
            @RequestParam(required = false) Integer threshold) {
        
        log.info("Admin getting products with low stock, threshold: {}", threshold);
        
        List<Product> lowStockProducts = stockService.getProductsWithLowStock(threshold);
        List<UUID> productIds = lowStockProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        
        log.info("Admin found {} products with low stock", productIds.size());
        return ResponseEntity.ok(productIds);
    }

    @Operation(
        summary = "Get expired reservations",
        description = "Get list of stock items with expired reservations. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Expired reservations retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StockResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/expired-reservations")
    public ResponseEntity<List<StockResponseDto>> getExpiredReservations() {
        
        log.info("Admin getting expired reservations");
        
        List<Stock> expiredReservations = stockService.getExpiredReservations();
        List<StockResponseDto> response = expiredReservations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Admin found {} expired reservations", response.size());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Clean up expired reservations",
        description = "Clean up all expired reservations and make stock available again. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Expired reservations cleaned up successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/cleanup-expired")
    public ResponseEntity<Integer> cleanupExpiredReservations() {
        
        log.info("Admin cleaning up expired reservations");
        
        int cleanedUp = stockService.cleanupExpiredReservations();
        
        log.info("Admin cleaned up {} expired reservations", cleanedUp);
        return ResponseEntity.ok(cleanedUp);
    }

    @Operation(
        summary = "Get sold stock",
        description = "Get list of sold stock items with optional date filtering. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sold stock retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StockResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/sold")
    public ResponseEntity<List<StockResponseDto>> getSoldStock(
            @Parameter(description = "Product ID (optional - gets sold stock for all products if not specified)")
            @RequestParam(required = false) UUID productId,
            @Parameter(description = "Start date (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Admin getting sold stock - productId: {}, startDate: {}, endDate: {}", 
                productId, startDate, endDate);
        
        List<Stock> soldStock = stockService.getSoldStock(productId, startDate, endDate);
        List<StockResponseDto> response = soldStock.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Admin retrieved {} sold stock items", response.size());
        return ResponseEntity.ok(response);
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
     * Convert Stock entity to StockResponseDto.
     * แปลง Stock entity เป็น StockResponseDto
     */
    private StockResponseDto convertToDto(Stock stock) {
        return StockResponseDto.builder()
                .id(stock.getId())
                .product(StockResponseDto.ProductInfo.builder()
                        .id(stock.getProduct().getId())
                        .name(stock.getProduct().getName())
                        .server(stock.getProduct().getServer())
                        .categoryName(stock.getProduct().getCategory().getName())
                        .active(stock.getProduct().getActive())
                        .build())
                .maskedCredentials(maskCredentials(stock.getCredentials()))
                .additionalInfo(stock.getAdditionalInfo())
                .sold(stock.getSold())
                .soldAt(stock.getSoldAt())
                .reservedUntil(stock.getReservedUntil())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }

    /**
     * Mask sensitive credentials for display.
     * ปิดบังข้อมูลสำคัญสำหรับการแสดงผล
     */
    private String maskCredentials(String credentials) {
        if (credentials == null || credentials.isEmpty()) {
            return "";
        }
        
        // Simple masking - replace password values with asterisks
        return credentials.replaceAll("(?i)(password|pass|pwd)\\s*[:|=]\\s*[^\\n\\r]+", "$1:***");
    }
}