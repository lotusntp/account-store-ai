package com.accountselling.platform.controller.admin;

import com.accountselling.platform.dto.order.OrderResponseDto;
import com.accountselling.platform.dto.statistics.DailyOrderStatistics;
import com.accountselling.platform.dto.statistics.OrderStatistics;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin controller for order management operations.
 * Provides order viewing, status management, and statistics with admin privileges.
 * 
 * Admin controller สำหรับการจัดการคำสั่งซื้อ
 * ให้บริการการดู การจัดการสถานะ และสถิติด้วยสิทธิ์ admin
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Order Management", description = "Admin endpoints for order management and statistics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final UserService userService;

    @Operation(
        summary = "Search orders with filters",
        description = "Search and filter orders with multiple criteria. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> searchOrders(
            @Parameter(description = "Order number pattern")
            @RequestParam(required = false) String orderNumber,
            @Parameter(description = "Username pattern")
            @RequestParam(required = false) String username,
            @Parameter(description = "Order status")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Minimum amount")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum amount")
            @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection) {
        
        log.info("Admin searching orders - orderNumber: {}, username: {}, status: {}, startDate: {}, endDate: {}", 
                orderNumber, username, status, startDate, endDate);
        
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        
        Page<Order> orderPage = orderService.searchOrders(
                orderNumber, username, status, startDate, endDate, minAmount, maxAmount, pageable);
        
        Page<OrderResponseDto> response = orderPage.map(this::convertToDto);
        
        log.info("Admin retrieved {} orders out of {} total", response.getNumberOfElements(), response.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get order by ID",
        description = "Retrieve detailed information about a specific order. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @Parameter(description = "Order ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin getting order by id: {}", id);
        
        Order order = orderService.findById(id);
        OrderResponseDto response = convertToDto(order);
        
        log.info("Admin retrieved order: {} for user: {}", order.getOrderNumber(), order.getUser().getUsername());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get orders by user",
        description = "Retrieve all orders for a specific user. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderResponseDto>> getOrdersByUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection) {
        
        log.info("Admin getting orders for user: {}", userId);
        
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<Order> orderPage = orderService.getOrdersByUser(user, pageable);
        
        Page<OrderResponseDto> response = orderPage.map(this::convertToDto);
        
        log.info("Admin retrieved {} orders for user: {}", response.getNumberOfElements(), user.getUsername());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get orders by status",
        description = "Retrieve all orders with a specific status. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderResponseDto>> getOrdersByStatus(
            @Parameter(description = "Order status", required = true)
            @PathVariable OrderStatus status,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection) {
        
        log.info("Admin getting orders by status: {}", status);
        
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<Order> orderPage = orderService.getOrdersByStatus(status, pageable);
        
        Page<OrderResponseDto> response = orderPage.map(this::convertToDto);
        
        log.info("Admin retrieved {} orders with status: {}", response.getNumberOfElements(), status);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Update order status to processing",
        description = "Mark an order as processing. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/processing")
    public ResponseEntity<OrderResponseDto> markOrderAsProcessing(
            @Parameter(description = "Order ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin marking order as processing: {}", id);
        
        Order updatedOrder = orderService.markOrderAsProcessing(id);
        OrderResponseDto response = convertToDto(updatedOrder);
        
        log.info("Admin marked order: {} as processing", updatedOrder.getOrderNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Update order status to completed",
        description = "Mark an order as completed. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/completed")
    public ResponseEntity<OrderResponseDto> markOrderAsCompleted(
            @Parameter(description = "Order ID", required = true)
            @PathVariable UUID id) {
        
        log.info("Admin marking order as completed: {}", id);
        
        Order updatedOrder = orderService.markOrderAsCompleted(id);
        OrderResponseDto response = convertToDto(updatedOrder);
        
        log.info("Admin marked order: {} as completed", updatedOrder.getOrderNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Update order status to failed",
        description = "Mark an order as failed with reason. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/failed")
    public ResponseEntity<OrderResponseDto> markOrderAsFailed(
            @Parameter(description = "Order ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Failure reason")
            @RequestParam(required = false, defaultValue = "Order marked as failed by admin") String reason) {
        
        log.info("Admin marking order as failed: {} with reason: {}", id, reason);
        
        Order updatedOrder = orderService.markOrderAsFailed(id, reason);
        OrderResponseDto response = convertToDto(updatedOrder);
        
        log.info("Admin marked order: {} as failed", updatedOrder.getOrderNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Cancel order",
        description = "Cancel an order with reason. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Cancellation reason")
            @RequestParam(required = false, defaultValue = "Order cancelled by admin") String reason) {
        
        log.info("Admin cancelling order: {} with reason: {}", id, reason);
        
        Order cancelledOrder = orderService.cancelOrder(id, reason);
        OrderResponseDto response = convertToDto(cancelledOrder);
        
        log.info("Admin cancelled order: {}", cancelledOrder.getOrderNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get order statistics",
        description = "Get comprehensive order statistics. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderStatistics.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/statistics")
    public ResponseEntity<OrderStatistics> getOrderStatistics(
            @Parameter(description = "Start date for statistics")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date for statistics")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Admin getting order statistics - startDate: {}, endDate: {}", startDate, endDate);
        
        OrderStatistics statistics;
        if (startDate != null && endDate != null) {
            statistics = orderService.getOrderStatistics(startDate, endDate);
        } else {
            statistics = orderService.getOrderStatistics();
        }
        
        log.info("Admin retrieved order statistics");
        return ResponseEntity.ok(statistics);
    }

    @Operation(
        summary = "Get daily order statistics",
        description = "Get daily breakdown of order statistics. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Daily order statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/statistics/daily")
    public ResponseEntity<List<DailyOrderStatistics>> getDailyOrderStatistics(
            @Parameter(description = "Start date for daily statistics", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date for daily statistics", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Admin getting daily order statistics from {} to {}", startDate, endDate);
        
        List<DailyOrderStatistics> dailyStats = orderService.getDailyOrderStatistics(startDate, endDate);
        
        log.info("Admin retrieved {} days of order statistics", dailyStats.size());
        return ResponseEntity.ok(dailyStats);
    }

    @Operation(
        summary = "Get top customers",
        description = "Get top customers by order count. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top customers retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/top-customers")
    public ResponseEntity<List<UUID>> getTopCustomers(
            @Parameter(description = "Number of top customers to return")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        log.info("Admin getting top {} customers", limit);
        
        List<User> topCustomers = orderService.getTopCustomersByOrderCount(limit);
        List<UUID> customerIds = topCustomers.stream()
                .map(User::getId)
                .collect(Collectors.toList());
        
        log.info("Admin retrieved {} top customers", customerIds.size());
        return ResponseEntity.ok(customerIds);
    }

    @Operation(
        summary = "Get recent orders",
        description = "Get most recent orders for dashboard. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<OrderResponseDto>> getRecentOrders(
            @Parameter(description = "Number of recent orders to return")
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
        log.info("Admin getting {} recent orders", limit);
        
        List<Order> recentOrders = orderService.getRecentOrders(limit);
        List<OrderResponseDto> response = recentOrders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Admin retrieved {} recent orders", response.size());
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
     * Convert Order entity to OrderResponseDto.
     * แปลง Order entity เป็น OrderResponseDto
     */
    private OrderResponseDto convertToDto(Order order) {
        List<OrderResponseDto.OrderItemResponseDto> orderItems = order.getOrderItems().stream()
                .map(item -> new OrderResponseDto.OrderItemResponseDto(
                        item.getId(),
                        item.getStockItem() != null ? item.getStockItem().getId() : null,
                        item.getProduct().getName(),
                        item.getProduct().getDescription(),
                        item.getProduct().getCategory().getName(),
                        item.getProduct().getServer(),
                        item.getPrice(),
                        String.format("$%.2f", item.getPrice()),
                        null, // notes - assuming this doesn't exist in OrderItem
                        item.getStockItem() != null ? item.getStockItem().getSold() : false
                ))
                .collect(Collectors.toList());

        // Payment summary would need to be retrieved separately if needed
        OrderResponseDto.PaymentSummaryDto paymentSummary = null; // Simplified for now

        return new OrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getUsername(),
                order.getUser().getFirstName() + " " + order.getUser().getLastName(),
                order.getTotalAmount(),
                String.format("$%.2f", order.getTotalAmount()),
                order.getStatus(),
                order.getStatus().toString(), // This might need localization
                null, // notes - assuming this doesn't exist in Order
                order.getCreatedAt(),
                order.getUpdatedAt(),
                orderItems,
                paymentSummary
        );
    }
}