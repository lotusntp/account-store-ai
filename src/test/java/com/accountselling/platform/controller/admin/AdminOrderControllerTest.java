package com.accountselling.platform.controller.admin;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.config.TestSecurityConfig;
import com.accountselling.platform.dto.statistics.DailyOrderStatistics;
import com.accountselling.platform.dto.statistics.OrderStatistics;
import com.accountselling.platform.enums.OrderStatus;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.*;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive unit tests for AdminOrderController. Tests all order management operations,
 * validation, authorization, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(AdminOrderController.class)
@Import(TestSecurityConfig.class)
class AdminOrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OrderService orderService;

  @MockBean private UserService userService;

  @Autowired private ObjectMapper objectMapper;

  private Order testOrder;
  private User testUser;
  private Product testProduct;
  private Category testCategory;
  private OrderItem testOrderItem;
  private OrderStatistics testOrderStatistics;
  private DailyOrderStatistics testDailyStatistics;

  @BeforeEach
  void setUp() {
    // Setup test category
    testCategory = new Category();
    testCategory.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    testCategory.setName("Gaming");
    testCategory.setActive(true);

    // Setup test product
    testProduct = new Product();
    testProduct.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    testProduct.setName("Premium Game Account");
    testProduct.setPrice(new BigDecimal("99.99"));
    testProduct.setCategory(testCategory);
    testProduct.setServer("US-West");
    testProduct.setActive(true);

    // Setup test user
    testUser = new User();
    testUser.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174002"));
    testUser.setUsername("johndoe");
    testUser.setEmail("john.doe@example.com");
    testUser.setFirstName("John");
    testUser.setLastName("Doe");
    testUser.setEnabled(true);

    // Setup test order item
    testOrderItem = new OrderItem();
    testOrderItem.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174003"));
    testOrderItem.setProduct(testProduct);
    testOrderItem.setPrice(testProduct.getPrice());
    testOrderItem.setStockItem(new Stock()); // Simple stock item

    // Setup test order
    testOrder = new Order();
    testOrder.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174004"));
    testOrder.setOrderNumber("ORD-20240101-001");
    testOrder.setUser(testUser);
    testOrder.setStatus(OrderStatus.PENDING);
    testOrder.setTotalAmount(new BigDecimal("99.99"));
    testOrder.setOrderItems(Set.of(testOrderItem));
    testOrder.setCreatedAt(LocalDateTime.now().minusDays(1));
    testOrder.setUpdatedAt(LocalDateTime.now().minusDays(1));

    testOrderItem.setOrder(testOrder);

    // Setup order statistics
    testOrderStatistics =
        new OrderStatistics(
            100L, // total
            20L, // pending
            10L, // processing
            70L, // completed
            10L, // failed
            0L, // cancelled
            new BigDecimal("9999.00"), // totalRevenue
            new BigDecimal("99.99") // averageOrderValue
            );

    // Setup daily statistics
    testDailyStatistics =
        new DailyOrderStatistics(
            LocalDateTime.now().minusDays(1), // date
            5L, // orderCount
            new BigDecimal("499.95") // revenue
            );
  }

  // ==================== SEARCH ORDERS TESTS ====================

  @Test
  @DisplayName("Search orders - Success")
  @WithMockUser(roles = "ADMIN")
  void searchOrders_Success() throws Exception {
    // Arrange
    List<Order> orders = Arrays.asList(testOrder);
    Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

    when(orderService.searchOrders(
            eq("ORD-20240101"),
            eq("johndoe"),
            eq(OrderStatus.PENDING),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(Pageable.class)))
        .thenReturn(orderPage);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/orders")
                .param("orderNumber", "ORD-20240101")
                .param("username", "johndoe")
                .param("status", "PENDING")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-01-02T00:00:00")
                .param("minAmount", "50.00")
                .param("maxAmount", "150.00")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "createdAt")
                .param("sortDirection", "desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-20240101-001"))
        .andExpect(jsonPath("$.content[0].status").value("PENDING"))
        .andExpect(jsonPath("$.content[0].totalAmount").value(99.99))
        .andExpect(jsonPath("$.content[0].username").value("johndoe"))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(orderService)
        .searchOrders(
            eq("ORD-20240101"),
            eq("johndoe"),
            eq(OrderStatus.PENDING),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(Pageable.class));
  }

  @Test
  @DisplayName("Search orders - No filters")
  @WithMockUser(roles = "ADMIN")
  void searchOrders_NoFilters() throws Exception {
    // Arrange
    List<Order> orders = Arrays.asList(testOrder);
    Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

    when(orderService.searchOrders(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            any(Pageable.class)))
        .thenReturn(orderPage);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)));

    verify(orderService)
        .searchOrders(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            any(Pageable.class));
  }

  @Test
  @DisplayName("Search orders - Invalid pagination")
  @WithMockUser(roles = "ADMIN")
  void searchOrders_InvalidPagination() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders").param("page", "-1").param("size", "200"))
        .andExpect(status().isBadRequest());

    verify(orderService, never())
        .searchOrders(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Search orders - Unauthorized")
  void searchOrders_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/orders")).andExpect(status().isForbidden());

    verify(orderService, never())
        .searchOrders(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Search orders - Forbidden for non-admin")
  @WithMockUser(roles = "USER")
  void searchOrders_Forbidden() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/orders")).andExpect(status().isForbidden());

    verify(orderService, never())
        .searchOrders(any(), any(), any(), any(), any(), any(), any(), any());
  }

  // ==================== GET ORDER BY ID TESTS ====================

  @Test
  @DisplayName("Get order by ID - Success")
  @WithMockUser(roles = "ADMIN")
  void getOrderById_Success() throws Exception {
    // Arrange
    when(orderService.findById(testOrder.getId())).thenReturn(testOrder);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders/{id}", testOrder.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.orderNumber").value("ORD-20240101-001"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.totalAmount").value(99.99))
        .andExpect(jsonPath("$.username").value("johndoe"))
        .andExpect(jsonPath("$.userDisplayName").value("John Doe"))
        .andExpect(jsonPath("$.orderItems", hasSize(1)))
        .andExpect(jsonPath("$.orderItems[0].productName").value("Premium Game Account"))
        .andExpect(jsonPath("$.orderItems[0].categoryName").value("Gaming"))
        .andExpect(jsonPath("$.orderItems[0].server").value("US-West"))
        .andExpect(jsonPath("$.orderItems[0].price").value(99.99));

    verify(orderService).findById(testOrder.getId());
  }

  @Test
  @DisplayName("Get order by ID - Not found")
  @WithMockUser(roles = "ADMIN")
  void getOrderById_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(orderService.findById(nonExistentId))
        .thenThrow(new ResourceNotFoundException("Order", nonExistentId.toString()));

    // Act & Assert
    mockMvc.perform(get("/api/admin/orders/{id}", nonExistentId)).andExpect(status().isNotFound());

    verify(orderService).findById(nonExistentId);
  }

  // ==================== GET ORDERS BY USER TESTS ====================

  @Test
  @DisplayName("Get orders by user - Success")
  @WithMockUser(roles = "ADMIN")
  void getOrdersByUser_Success() throws Exception {
    // Arrange
    List<Order> orders = Arrays.asList(testOrder);
    Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

    when(userService.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(orderService.getOrdersByUser(eq(testUser), any(Pageable.class))).thenReturn(orderPage);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/orders/user/{userId}", testUser.getId())
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.content[0].username").value("johndoe"));

    verify(userService).findById(testUser.getId());
    verify(orderService).getOrdersByUser(eq(testUser), any(Pageable.class));
  }

  @Test
  @DisplayName("Get orders by user - User not found")
  @WithMockUser(roles = "ADMIN")
  void getOrdersByUser_UserNotFound() throws Exception {
    // Arrange
    UUID nonExistentUserId = UUID.randomUUID();
    when(userService.findById(nonExistentUserId)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders/user/{userId}", nonExistentUserId))
        .andExpect(status().isNotFound());

    verify(userService).findById(nonExistentUserId);
    verify(orderService, never()).getOrdersByUser(any(), any());
  }

  // ==================== GET ORDERS BY STATUS TESTS ====================

  @Test
  @DisplayName("Get orders by status - Success")
  @WithMockUser(roles = "ADMIN")
  void getOrdersByStatus_Success() throws Exception {
    // Arrange
    List<Order> orders = Arrays.asList(testOrder);
    Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

    when(orderService.getOrdersByStatus(eq(OrderStatus.PENDING), any(Pageable.class)))
        .thenReturn(orderPage);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/orders/status/{status}", "PENDING")
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].status").value("PENDING"));

    verify(orderService).getOrdersByStatus(eq(OrderStatus.PENDING), any(Pageable.class));
  }

  // ==================== MARK ORDER AS PROCESSING TESTS ====================

  @Test
  @DisplayName("Mark order as processing - Success")
  @WithMockUser(roles = "ADMIN")
  void markOrderAsProcessing_Success() throws Exception {
    // Arrange
    Order processingOrder = new Order();
    processingOrder.setId(testOrder.getId());
    processingOrder.setOrderNumber(testOrder.getOrderNumber());
    processingOrder.setUser(testUser);
    processingOrder.setStatus(OrderStatus.PROCESSING);
    processingOrder.setTotalAmount(testOrder.getTotalAmount());
    processingOrder.setOrderItems(testOrder.getOrderItems());

    when(orderService.markOrderAsProcessing(testOrder.getId())).thenReturn(processingOrder);

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/orders/{id}/processing", testOrder.getId()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.status").value("PROCESSING"));

    verify(orderService).markOrderAsProcessing(testOrder.getId());
  }

  @Test
  @DisplayName("Mark order as processing - Order not found")
  @WithMockUser(roles = "ADMIN")
  void markOrderAsProcessing_OrderNotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(orderService.markOrderAsProcessing(nonExistentId))
        .thenThrow(new ResourceNotFoundException("Order", nonExistentId.toString()));

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/orders/{id}/processing", nonExistentId).with(csrf()))
        .andExpect(status().isNotFound());

    verify(orderService).markOrderAsProcessing(nonExistentId);
  }

  @Test
  @DisplayName("Mark order as processing - Invalid status transition")
  @WithMockUser(roles = "ADMIN")
  void markOrderAsProcessing_InvalidStatusTransition() throws Exception {
    // Arrange
    when(orderService.markOrderAsProcessing(testOrder.getId()))
        .thenThrow(new IllegalStateException("Invalid status transition"));

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/orders/{id}/processing", testOrder.getId()).with(csrf()))
        .andExpect(status().isBadRequest());

    verify(orderService).markOrderAsProcessing(testOrder.getId());
  }

  // ==================== MARK ORDER AS COMPLETED TESTS ====================

  @Test
  @DisplayName("Mark order as completed - Success")
  @WithMockUser(roles = "ADMIN")
  void markOrderAsCompleted_Success() throws Exception {
    // Arrange
    Order completedOrder = new Order();
    completedOrder.setId(testOrder.getId());
    completedOrder.setOrderNumber(testOrder.getOrderNumber());
    completedOrder.setUser(testUser);
    completedOrder.setStatus(OrderStatus.COMPLETED);
    completedOrder.setTotalAmount(testOrder.getTotalAmount());
    completedOrder.setOrderItems(testOrder.getOrderItems());

    when(orderService.markOrderAsCompleted(testOrder.getId())).thenReturn(completedOrder);

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/orders/{id}/completed", testOrder.getId()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    verify(orderService).markOrderAsCompleted(testOrder.getId());
  }

  // ==================== MARK ORDER AS FAILED TESTS ====================

  @Test
  @DisplayName("Mark order as failed - Success")
  @WithMockUser(roles = "ADMIN")
  void markOrderAsFailed_Success() throws Exception {
    // Arrange
    Order failedOrder = new Order();
    failedOrder.setId(testOrder.getId());
    failedOrder.setOrderNumber(testOrder.getOrderNumber());
    failedOrder.setUser(testUser);
    failedOrder.setStatus(OrderStatus.FAILED);
    failedOrder.setTotalAmount(testOrder.getTotalAmount());
    failedOrder.setOrderItems(testOrder.getOrderItems());

    when(orderService.markOrderAsFailed(testOrder.getId(), "Payment failed"))
        .thenReturn(failedOrder);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/orders/{id}/failed", testOrder.getId())
                .with(csrf())
                .param("reason", "Payment failed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.status").value("FAILED"));

    verify(orderService).markOrderAsFailed(testOrder.getId(), "Payment failed");
  }

  @Test
  @DisplayName("Mark order as failed - Default reason")
  @WithMockUser(roles = "ADMIN")
  void markOrderAsFailed_DefaultReason() throws Exception {
    // Arrange
    Order failedOrder = new Order();
    failedOrder.setId(testOrder.getId());
    failedOrder.setStatus(OrderStatus.FAILED);
    failedOrder.setUser(testUser);
    failedOrder.setOrderItems(testOrder.getOrderItems());

    when(orderService.markOrderAsFailed(testOrder.getId(), "Order marked as failed by admin"))
        .thenReturn(failedOrder);

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/orders/{id}/failed", testOrder.getId()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"));

    verify(orderService).markOrderAsFailed(testOrder.getId(), "Order marked as failed by admin");
  }

  // ==================== CANCEL ORDER TESTS ====================

  @Test
  @DisplayName("Cancel order - Success")
  @WithMockUser(roles = "ADMIN")
  void cancelOrder_Success() throws Exception {
    // Arrange
    Order cancelledOrder = new Order();
    cancelledOrder.setId(testOrder.getId());
    cancelledOrder.setOrderNumber(testOrder.getOrderNumber());
    cancelledOrder.setUser(testUser);
    cancelledOrder.setStatus(OrderStatus.CANCELLED);
    cancelledOrder.setTotalAmount(testOrder.getTotalAmount());
    cancelledOrder.setOrderItems(testOrder.getOrderItems());

    when(orderService.cancelOrder(testOrder.getId(), "Out of stock")).thenReturn(cancelledOrder);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/orders/{id}/cancel", testOrder.getId())
                .with(csrf())
                .param("reason", "Out of stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    verify(orderService).cancelOrder(testOrder.getId(), "Out of stock");
  }

  @Test
  @DisplayName("Cancel order - Cannot cancel")
  @WithMockUser(roles = "ADMIN")
  void cancelOrder_CannotCancel() throws Exception {
    // Arrange
    when(orderService.cancelOrder(testOrder.getId(), "Order cancelled by admin"))
        .thenThrow(new IllegalStateException("Order cannot be cancelled"));

    // Act & Assert
    mockMvc
        .perform(put("/api/admin/orders/{id}/cancel", testOrder.getId()).with(csrf()))
        .andExpect(status().isBadRequest());

    verify(orderService).cancelOrder(testOrder.getId(), "Order cancelled by admin");
  }

  // ==================== GET ORDER STATISTICS TESTS ====================

  @Test
  @DisplayName("Get order statistics - Success")
  @WithMockUser(roles = "ADMIN")
  void getOrderStatistics_Success() throws Exception {
    // Arrange
    when(orderService.getOrderStatistics()).thenReturn(testOrderStatistics);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(100))
        .andExpect(jsonPath("$.pending").value(20))
        .andExpect(jsonPath("$.completed").value(70))
        .andExpect(jsonPath("$.failed").value(10))
        .andExpect(jsonPath("$.totalRevenue").value(9999.00))
        .andExpect(jsonPath("$.averageOrderValue").value(99.99));

    verify(orderService).getOrderStatistics();
  }

  @Test
  @DisplayName("Get order statistics - With date range")
  @WithMockUser(roles = "ADMIN")
  void getOrderStatistics_WithDateRange() throws Exception {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(30);
    LocalDateTime endDate = LocalDateTime.now();

    when(orderService.getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(testOrderStatistics);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/orders/statistics")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(100));

    verify(orderService).getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class));
    verify(orderService, never()).getOrderStatistics();
  }

  // ==================== GET DAILY ORDER STATISTICS TESTS ====================

  @Test
  @DisplayName("Get daily order statistics - Success")
  @WithMockUser(roles = "ADMIN")
  void getDailyOrderStatistics_Success() throws Exception {
    // Arrange
    List<DailyOrderStatistics> dailyStats = Arrays.asList(testDailyStatistics);
    LocalDateTime startDate = LocalDateTime.now().minusDays(7);
    LocalDateTime endDate = LocalDateTime.now();

    when(orderService.getDailyOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(dailyStats);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/orders/statistics/daily")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].orderCount").value(5))
        .andExpect(jsonPath("$[0].revenue").value(499.95));

    verify(orderService)
        .getDailyOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  // ==================== GET TOP CUSTOMERS TESTS ====================

  @Test
  @DisplayName("Get top customers - Success")
  @WithMockUser(roles = "ADMIN")
  void getTopCustomers_Success() throws Exception {
    // Arrange
    List<User> topCustomers = Arrays.asList(testUser);
    when(orderService.getTopCustomersByOrderCount(10)).thenReturn(topCustomers);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders/top-customers").param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0]").value(testUser.getId().toString()));

    verify(orderService).getTopCustomersByOrderCount(10);
  }

  @Test
  @DisplayName("Get top customers - Default limit")
  @WithMockUser(roles = "ADMIN")
  void getTopCustomers_DefaultLimit() throws Exception {
    // Arrange
    List<User> topCustomers = Arrays.asList(testUser);
    when(orderService.getTopCustomersByOrderCount(10)).thenReturn(topCustomers);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders/top-customers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));

    verify(orderService).getTopCustomersByOrderCount(10);
  }

  // ==================== GET RECENT ORDERS TESTS ====================

  @Test
  @DisplayName("Get recent orders - Success")
  @WithMockUser(roles = "ADMIN")
  void getRecentOrders_Success() throws Exception {
    // Arrange
    List<Order> recentOrders = Arrays.asList(testOrder);
    when(orderService.getRecentOrders(20)).thenReturn(recentOrders);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders/recent").param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(testOrder.getId().toString()))
        .andExpect(jsonPath("$[0].orderNumber").value("ORD-20240101-001"))
        .andExpect(jsonPath("$[0].status").value("PENDING"));

    verify(orderService).getRecentOrders(20);
  }

  @Test
  @DisplayName("Get recent orders - Default limit")
  @WithMockUser(roles = "ADMIN")
  void getRecentOrders_DefaultLimit() throws Exception {
    // Arrange
    List<Order> recentOrders = Arrays.asList(testOrder);
    when(orderService.getRecentOrders(20)).thenReturn(recentOrders);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders/recent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));

    verify(orderService).getRecentOrders(20);
  }

  // ==================== EDGE CASES AND VALIDATION TESTS ====================

  @Test
  @DisplayName("Search orders - Invalid sort direction")
  @WithMockUser(roles = "ADMIN")
  void searchOrders_InvalidSortDirection() throws Exception {
    // Arrange
    List<Order> orders = Arrays.asList(testOrder);
    Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

    when(orderService.searchOrders(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(orderPage);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders").param("sortDirection", "invalid"))
        .andExpect(status().isOk()); // Should default to ASC

    verify(orderService).searchOrders(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Search orders - Empty results")
  @WithMockUser(roles = "ADMIN")
  void searchOrders_EmptyResults() throws Exception {
    // Arrange
    Page<Order> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);

    when(orderService.searchOrders(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(emptyPage);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.totalElements").value(0));

    verify(orderService).searchOrders(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Get orders by user - Custom sorting")
  @WithMockUser(roles = "ADMIN")
  void getOrdersByUser_CustomSorting() throws Exception {
    // Arrange
    List<Order> orders = Arrays.asList(testOrder);
    Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

    when(userService.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(orderService.getOrdersByUser(eq(testUser), any(Pageable.class))).thenReturn(orderPage);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/orders/user/{userId}", testUser.getId())
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "totalAmount")
                .param("sortDirection", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)));

    verify(orderService).getOrdersByUser(eq(testUser), any(Pageable.class));
  }
}
