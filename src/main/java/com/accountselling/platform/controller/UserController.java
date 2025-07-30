package com.accountselling.platform.controller;

import com.accountselling.platform.dto.user.UserOrderHistoryDto;
import com.accountselling.platform.dto.user.UserProfileResponseDto;
import com.accountselling.platform.exception.OrderAccessDeniedException;
import com.accountselling.platform.exception.OrderNotCompletedException;
import com.accountselling.platform.exception.UserNotFoundException;
import com.accountselling.platform.model.Order;
import com.accountselling.platform.model.OrderItem;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.StockService;
import com.accountselling.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user-related operations. Handles user profile management, order history, and
 * account downloads.
 *
 * <p>Controller สำหรับจัดการข้อมูลผู้ใช้ รวมถึงโปรไฟล์ ประวัติคำสั่งซื้อ และการดาวน์โหลดข้อมูลบัญชี
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User management and profile endpoints")
public class UserController {

  private final UserService userService;
  private final OrderService orderService;
  private final StockService stockService;

  /**
   * Get current user's profile information. Returns comprehensive profile data for the
   * authenticated user.
   *
   * @return user profile response with personal information and statistics
   */
  @GetMapping("/profile")
  @PreAuthorize("hasRole('USER')")
  @Operation(
      summary = "Get user profile",
      description = "Get current authenticated user's profile information")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  public ResponseEntity<UserProfileResponseDto> getUserProfile() {
    log.info("Processing user profile request");

    User user = getCurrentAuthenticatedUser();

    // Build profile response with statistics
    UserProfileResponseDto profile =
        UserProfileResponseDto.builder()
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFullName())
            .roles(user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()))
            .enabled(user.getEnabled())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .totalOrders(user.getOrderCount())
            .completedOrders(user.getCompletedOrders().size())
            .pendingOrders(user.getPendingOrders().size())
            .build();

    log.info("User profile retrieved successfully for user: {}", user.getUsername());
    return ResponseEntity.ok(profile);
  }

  /**
   * Get current user's order history. Returns paginated list of user's orders with detailed
   * information.
   *
   * @param page page number (default: 0)
   * @param size page size (default: 20)
   * @param sort sort criteria (default: createdAt,desc)
   * @return list of user's orders with items and status information
   */
  @GetMapping("/orders")
  @PreAuthorize("hasRole('USER')")
  @Operation(
      summary = "Get user order history",
      description = "Get current authenticated user's order history")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Order history retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  public ResponseEntity<List<UserOrderHistoryDto>> getUserOrders(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    log.info("Processing user orders request - page: {}, size: {}", page, size);

    User user = getCurrentAuthenticatedUser();

    // Create pageable with sorting
    String[] sortParams = sort.split(",");
    Sort.Direction direction =
        sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

    // Get user's orders and convert to DTOs
    Page<Order> orderPage = orderService.getOrdersByUser(user, pageable);
    List<Order> orders = orderPage.getContent();

    List<UserOrderHistoryDto> orderHistory =
        orders.stream().map(this::convertToOrderHistoryDto).collect(Collectors.toList());

    log.info(
        "User order history retrieved successfully for user: {} - {} orders found (page {} of {})",
        user.getUsername(),
        orders.size(),
        page + 1,
        orderPage.getTotalPages());
    return ResponseEntity.ok(orderHistory);
  }

  /**
   * Download account data for a specific completed order. Returns account information as a text
   * file for orders that have been completed.
   *
   * @param orderId the ID of the completed order
   * @return account data as downloadable text file
   */
  @GetMapping("/download/{orderId}")
  @PreAuthorize("hasRole('USER')")
  @Operation(
      summary = "Download order account data",
      description = "Download account information for a completed order")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Account data downloaded successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - order does not belong to user"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(
            responseCode = "400",
            description = "Order not completed or no downloadable content")
      })
  public ResponseEntity<String> downloadOrderAccountData(@PathVariable UUID orderId) {
    log.info("Processing account data download request for order: {}", orderId);

    User user = getCurrentAuthenticatedUser();

    // Get order and verify ownership
    Order order = orderService.findById(orderId);

    if (!order.getUser().getId().equals(user.getId())) {
      log.warn(
          "User {} attempted to download order {} belonging to another user",
          user.getUsername(),
          orderId);
      throw new OrderAccessDeniedException(orderId.toString(), user.getUsername());
    }

    // Check if order is completed
    if (!order.isCompleted()) {
      log.warn("User {} attempted to download uncompleted order {}", user.getUsername(), orderId);
      throw new OrderNotCompletedException(orderId.toString());
    }

    // Build account data content
    StringBuilder accountData = new StringBuilder();
    accountData.append("=== ACCOUNT INFORMATION ===\n");
    accountData.append("Order ID: ").append(order.getId()).append("\n");
    accountData
        .append("Order Date: ")
        .append(
            order.getCreatedAt() != null
                ? order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : "N/A")
        .append("\n");
    accountData.append("Customer: ").append(user.getUsername()).append("\n");
    accountData.append("Total Amount: $").append(order.getTotalAmount()).append("\n\n");

    // Add account details for each order item
    for (OrderItem item : order.getOrderItems()) {
      accountData.append("=== ").append(item.getProduct().getName()).append(" ===\n");
      accountData
          .append("Category: ")
          .append(item.getProduct().getCategory().getName())
          .append("\n");
      accountData.append("Quantity: 1\n");
      accountData.append("Unit Price: $").append(item.getPrice()).append("\n\n");

      // Get account information from stock
      // Get sold stock items for this product
      List<Stock> stockItems = java.util.Arrays.asList(item.getStockItem());

      int accountCount = 0;
      for (Stock stock : stockItems) {
        if (accountCount >= 1) break;

        accountData.append("Account #").append(++accountCount).append(":\n");
        accountData.append(stock.getAccountData()).append("\n\n");
      }
    }

    accountData.append("=== IMPORTANT NOTES ===\n");
    accountData.append("- Please change passwords immediately after first login\n");
    accountData.append("- Keep this information secure and confidential\n");
    accountData.append("- Contact support if you encounter any issues\n");
    accountData
        .append("\nDownloaded on: ")
        .append(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    // Set response headers for file download
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.valueOf("text/plain;charset=UTF-8"));
    headers.add(
        "Content-Disposition", "attachment; filename=\"accounts_order_" + orderId + ".txt\"");

    log.info(
        "Account data download completed for user: {}, order: {}", user.getUsername(), orderId);

    return ResponseEntity.ok().headers(headers).body(accountData.toString());
  }

  /**
   * Get current authenticated user from security context.
   *
   * @return the current authenticated user
   * @throws IllegalStateException if user is not authenticated or not found
   */
  private User getCurrentAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new UserNotFoundException("User is not authenticated");
    }

    String username = authentication.getName();
    return userService
        .findByUsername(username)
        .orElseThrow(() -> new UserNotFoundException("Authenticated user not found: " + username));
  }

  /**
   * Convert Order entity to UserOrderHistoryDto.
   *
   * @param order the order entity to convert
   * @return the converted order history DTO
   */
  private UserOrderHistoryDto convertToOrderHistoryDto(Order order) {
    List<UserOrderHistoryDto.OrderItemDto> itemDtos =
        order.getOrderItems().stream()
            .map(
                item ->
                    UserOrderHistoryDto.OrderItemDto.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .categoryName(item.getProduct().getCategory().getName())
                        .quantity(1)
                        .unitPrice(item.getPrice())
                        .totalPrice(item.getPrice())
                        .build())
            .collect(Collectors.toList());

    return UserOrderHistoryDto.builder()
        .orderId(order.getId())
        .status(order.getStatus())
        .totalAmount(order.getTotalAmount())
        .createdAt(order.getCreatedAt())
        .completedAt(order.isCompleted() ? order.getUpdatedAt() : null)
        .totalItems(order.getOrderItems().size())
        .items(itemDtos)
        .build();
  }
}
