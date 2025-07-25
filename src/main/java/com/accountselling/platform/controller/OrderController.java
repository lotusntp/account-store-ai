package com.accountselling.platform.controller;

import com.accountselling.platform.dto.order.OrderCreateRequestDto;
import com.accountselling.platform.dto.order.OrderResponseDto;
import com.accountselling.platform.dto.order.OrderResponseDto.OrderItemResponseDto;
import com.accountselling.platform.exception.OrderAccessDeniedException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.OrderItem;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;

/**
 * REST Controller for order operations. Handles order creation, retrieval, and management
 * for authenticated users.
 *
 * <p>Controller สำหรับจัดการคำสั่งซื้อ รวมถึงการสร้าง ดูรายละเอียด และจัดการคำสั่งซื้อของผู้ใช้
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order management and processing endpoints")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * Create a new order for authenticated user. Creates order with specified products
     * and quantities, reserves stock items, and returns order details.
     *
     * @param request the order creation request containing product quantities
     * @return created order response with order details and reserved items
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Create new order",
        description = "Create a new order with specified products and quantities")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock available")
        })
    public ResponseEntity<OrderResponseDto> createOrder(
        @Valid @RequestBody OrderCreateRequestDto request) {
        log.info("Processing order creation request for {} products", request.getUniqueProductCount());

        User currentUser = getCurrentAuthenticatedUser();

        // Create order with product quantities
        Order order = orderService.createOrder(currentUser, request.productQuantities());

        OrderResponseDto response = convertToOrderResponseDto(order);

        log.info("Order created successfully - ID: {}, User: {}, Total: ${}", 
            order.getId(), currentUser.getUsername(), order.getTotalAmount());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all orders for authenticated user with pagination. Returns user's order history
     * with configurable pagination and sorting.
     *
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @param sort sort criteria (default: createdAt,desc)
     * @return paginated list of user's orders
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get user orders",
        description = "Get paginated list of orders for authenticated user")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
        })
    public ResponseEntity<Page<OrderResponseDto>> getUserOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.info("Processing user orders request - page: {}, size: {}", page, size);

        User currentUser = getCurrentAuthenticatedUser();

        // Create pageable with sorting
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1]) 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        // Get user's orders and convert to DTOs
        Page<Order> orderPage = orderService.getOrdersByUser(currentUser, pageable);
        Page<OrderResponseDto> responsePage = orderPage.map(this::convertToOrderResponseDto);

        log.info("User orders retrieved successfully for user: {} - {} orders found (page {} of {})",
            currentUser.getUsername(), orderPage.getContent().size(), page + 1, orderPage.getTotalPages());

        return ResponseEntity.ok(responsePage);
    }

    /**
     * Get specific order details by order ID. Returns detailed order information
     * if order belongs to authenticated user.
     *
     * @param orderId the order ID to retrieve
     * @return order details with items and status information
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get order details",
        description = "Get detailed information for a specific order")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "Order details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - order does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Order not found")
        })
    public ResponseEntity<OrderResponseDto> getOrderById(
        @PathVariable UUID orderId) {
        log.info("Processing order details request for order: {}", orderId);

        User currentUser = getCurrentAuthenticatedUser();

        // Get order and verify ownership
        Order order = orderService.findById(orderId);

        if (!order.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to access order {} belonging to another user",
                currentUser.getUsername(), orderId);
            throw new OrderAccessDeniedException(orderId.toString(), currentUser.getUsername());
        }

        OrderResponseDto response = convertToOrderResponseDto(order);

        log.info("Order details retrieved successfully - ID: {}, User: {}, Status: {}", 
            orderId, currentUser.getUsername(), order.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an order by order ID. Allows user to cancel their own pending orders,
     * which releases reserved stock and prevents payment processing.
     *
     * @param orderId the order ID to cancel
     * @param reason optional cancellation reason
     * @return updated order response with cancelled status
     */
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Cancel order",
        description = "Cancel a pending order and release reserved stock")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - order does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled")
        })
    public ResponseEntity<OrderResponseDto> cancelOrder(
        @PathVariable UUID orderId,
        @RequestParam(required = false, defaultValue = "Cancelled by user") String reason) {
        log.info("Processing order cancellation request for order: {}", orderId);

        User currentUser = getCurrentAuthenticatedUser();

        // Cancel order by user (includes ownership validation)
        Order cancelledOrder = orderService.cancelOrderByUser(orderId, currentUser, reason);

        OrderResponseDto response = convertToOrderResponseDto(cancelledOrder);

        log.info("Order cancelled successfully - ID: {}, User: {}, Reason: {}", 
            orderId, currentUser.getUsername(), reason);

        return ResponseEntity.ok(response);
    }

    /**
     * Download account credentials for completed order. Returns account information
     * as downloadable text file for orders that have been successfully completed.
     *
     * @param orderId the order ID to download credentials for
     * @return account credentials as downloadable text file
     */
    @GetMapping("/{orderId}/download")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Download order account credentials",
        description = "Download account information for a completed order as text file")
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "Account data downloaded successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - order does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "400", description = "Order not completed or no downloadable content")
        })
    public ResponseEntity<String> downloadOrderCredentials(
        @PathVariable UUID orderId) {
        log.info("Processing account download request for order: {}", orderId);

        User currentUser = getCurrentAuthenticatedUser();

        // Get download information (includes ownership and completion validation)
        Map<String, String> downloadInfo = orderService.getOrderDownloadInfo(orderId, currentUser);

        // Build downloadable content
        StringBuilder accountData = new StringBuilder();
        accountData.append("=== ACCOUNT CREDENTIALS ===\n");
        accountData.append("Order ID: ").append(orderId).append("\n");
        accountData.append("Customer: ").append(currentUser.getUsername()).append("\n");
        accountData.append("Downloaded: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        // Add account credentials for each product
        downloadInfo.forEach((productName, credentials) -> {
            accountData.append("=== ").append(productName).append(" ===\n");
            accountData.append(credentials).append("\n\n");
        });

        accountData.append("=== IMPORTANT NOTES ===\n");
        accountData.append("- Change passwords immediately after first login\n");
        accountData.append("- Keep this information secure and confidential\n");
        accountData.append("- Contact support if you encounter any issues\n");

        // Set response headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain;charset=UTF-8");
        headers.add("Content-Disposition", "attachment; filename=\"accounts_order_" + orderId + ".txt\"");

        log.info("Account credentials downloaded successfully for user: {}, order: {}",
            currentUser.getUsername(), orderId);

        return ResponseEntity.ok()
            .headers(headers)
            .body(accountData.toString());
    }

    /**
     * Get current authenticated user from security context.
     *
     * @return the current authenticated user
     * @throws ResourceNotFoundException if user is not authenticated or not found
     */
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("User is not authenticated");
        }

        String username = authentication.getName();
        return userService.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }

    /**
     * Convert Order entity to OrderResponseDto.
     *
     * @param order the order entity to convert
     * @return the converted order response DTO
     */
    private OrderResponseDto convertToOrderResponseDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getOrderItems().stream()
            .map(this::convertToOrderItemDto)
            .collect(Collectors.toList());

        return new OrderResponseDto(
            order.getId(),
            order.getOrderNumber(),
            order.getUser().getUsername(),
            order.getUser().getFullName(),
            order.getTotalAmount(),
            "$" + order.getTotalAmount().toString(),
            order.getStatus(),
            order.getStatus().getDisplayName(),
            order.getNotes(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            itemDtos,
            null // Payment summary will be added later if needed
        );
    }

    /**
     * Convert OrderItem entity to OrderItemResponseDto.
     *
     * @param item the order item entity to convert
     * @return the converted order item response DTO
     */
    private OrderItemResponseDto convertToOrderItemDto(OrderItem item) {
        return new OrderItemResponseDto(
            item.getId(),
            item.getStockItem() != null ? item.getStockItem().getId() : null,
            item.getProduct().getName(),
            item.getProduct().getDescription(),
            item.getProduct().getCategory().getName(),
            item.getProduct().getServer(),
            item.getPrice(),
            "$" + item.getPrice().toString(),
            item.getNotes(),
            item.getStockItem() != null ? item.getStockItem().getSold() : false
        );
    }
}