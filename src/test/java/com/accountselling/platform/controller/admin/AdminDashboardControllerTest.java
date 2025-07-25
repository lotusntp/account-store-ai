package com.accountselling.platform.controller.admin;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.config.TestSecurityConfig;
import com.accountselling.platform.dto.statistics.DailyOrderStatistics;
import com.accountselling.platform.dto.statistics.OrderStatistics;
import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive unit tests for AdminDashboardController. Tests all dashboard operations,
 * validation, authorization, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(AdminDashboardController.class)
@Import(TestSecurityConfig.class)
class AdminDashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserService userService;

  @MockBean private OrderService orderService;

  @MockBean private ProductService productService;

  @MockBean private CategoryService categoryService;

  @MockBean private StockService stockService;

  @Autowired private ObjectMapper objectMapper;

  private User testUser;
  private Product testProduct;
  private Category testCategory;
  private Stock testStock;
  private OrderStatistics testOrderStatistics;
  private DailyOrderStatistics testDailyStatistics;

  @BeforeEach
  void setUp() {
    // Setup test user
    testUser = new User();
    testUser.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    testUser.setUsername("johndoe");
    testUser.setEmail("john.doe@example.com");
    testUser.setEnabled(true);

    // Setup test category (avoid circular reference)
    testCategory = new Category();
    testCategory.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    testCategory.setName("Gaming");
    testCategory.setActive(true);
    // Don't set products collection to avoid circular reference

    // Setup test product
    testProduct = new Product();
    testProduct.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174002"));
    testProduct.setName("Premium Game Account");
    testProduct.setPrice(new BigDecimal("99.99"));
    testProduct.setCategory(testCategory);
    testProduct.setActive(true);

    // Setup test stock
    testStock = new Stock();
    testStock.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174003"));
    testStock.setProduct(testProduct);
    testStock.setSold(false);

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

  // ==================== GET DASHBOARD OVERVIEW TESTS ====================

  @Test
  @DisplayName("Get dashboard overview - Success")
  @WithMockUser(roles = "ADMIN")
  void getDashboardOverview_Success() throws Exception {
    // Arrange
    // User stats
    when(userService.getTotalUserCount()).thenReturn(1000L);
    when(userService.getEnabledUserCount()).thenReturn(950L);
    when(userService.findUsersByRole("ADMIN")).thenReturn(Arrays.asList(testUser));

    // Order stats
    when(orderService.getOrderStatistics()).thenReturn(testOrderStatistics);
    when(orderService.getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(testOrderStatistics);

    // Product stats - simulate count via pagination and active product filtering
    List<Product> allProducts = Arrays.asList(testProduct); // testProduct is active=true
    when(productService.findAllProducts(any()))
        .thenReturn(
            new org.springframework.data.domain.PageImpl<>(
                allProducts, org.springframework.data.domain.PageRequest.of(0, 1), 500L));
    when(categoryService.findAllCategories()).thenReturn(Arrays.asList(testCategory));
    when(categoryService.findActiveCategories()).thenReturn(Arrays.asList(testCategory));

    // Stock stats
    when(stockService.getExpiredReservations()).thenReturn(Arrays.asList(testStock));
    when(stockService.getTotalStockCount(any(UUID.class))).thenReturn(100L);
    when(stockService.getAvailableStockCount(any(UUID.class))).thenReturn(80L);
    when(stockService.getReservedStockCount(any(UUID.class))).thenReturn(10L);
    when(stockService.getSoldStockCount(any(UUID.class))).thenReturn(10L);
    when(stockService.getProductsWithLowStock(null)).thenReturn(Arrays.asList(testProduct));

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/overview"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userStats").exists())
        .andExpect(jsonPath("$.userStats.totalUsers").value(1000))
        .andExpect(jsonPath("$.userStats.activeUsers").value(950))
        .andExpect(jsonPath("$.userStats.adminUsers").value(1))
        .andExpect(jsonPath("$.orderStats").exists())
        .andExpect(jsonPath("$.orderStats.totalOrders").value(100))
        .andExpect(jsonPath("$.orderStats.pendingOrders").value(20))
        .andExpect(jsonPath("$.orderStats.completedOrders").value(70))
        .andExpect(jsonPath("$.orderStats.failedOrders").value(10))
        .andExpect(jsonPath("$.productStats").exists())
        .andExpect(jsonPath("$.productStats.totalProducts").value(500))
        .andExpect(jsonPath("$.productStats.activeProducts").value(1))
        .andExpect(jsonPath("$.productStats.totalCategories").value(1))
        .andExpect(jsonPath("$.productStats.activeCategories").value(1))
        .andExpect(jsonPath("$.stockStats").exists())
        .andExpect(jsonPath("$.stockStats.totalStockItems").value(100))
        .andExpect(jsonPath("$.stockStats.availableStockItems").value(80))
        .andExpect(jsonPath("$.stockStats.reservedStockItems").value(10))
        .andExpect(jsonPath("$.stockStats.soldStockItems").value(10))
        .andExpect(jsonPath("$.revenueStats").exists())
        .andExpect(jsonPath("$.revenueStats.totalRevenue").value(9999.00))
        .andExpect(jsonPath("$.systemInfo").exists());

    verify(userService).getTotalUserCount();
    verify(userService).getEnabledUserCount();
    verify(userService).findUsersByRole("ADMIN");
    verify(orderService, times(6))
        .getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class));
    verify(orderService, times(2)).getOrderStatistics();
    verify(productService, times(2)).findAllProducts(any());
    verify(categoryService).findAllCategories();
    verify(categoryService).findActiveCategories();
  }

  @Test
  @DisplayName("Get dashboard overview - Unauthorized")
  void getDashboardOverview_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/dashboard/overview")).andExpect(status().isForbidden());

    verify(userService, never()).getTotalUserCount();
  }

  @Test
  @DisplayName("Get dashboard overview - Forbidden for non-admin")
  @WithMockUser(roles = "USER")
  void getDashboardOverview_Forbidden() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/dashboard/overview")).andExpect(status().isForbidden());

    verify(userService, never()).getTotalUserCount();
  }

  // ==================== GET USER STATISTICS TESTS ====================

  @Test
  @DisplayName("Get user statistics - Success")
  @WithMockUser(roles = "ADMIN")
  void getUserStats_Success() throws Exception {
    // Arrange
    when(userService.getTotalUserCount()).thenReturn(1000L);
    when(userService.getEnabledUserCount()).thenReturn(950L);
    when(userService.findUsersByRole("ADMIN")).thenReturn(Arrays.asList(testUser));

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/users/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsers").value(1000))
        .andExpect(jsonPath("$.activeUsers").value(950))
        .andExpect(jsonPath("$.newUsersToday").value(0))
        .andExpect(jsonPath("$.newUsersThisWeek").value(0))
        .andExpect(jsonPath("$.adminUsers").value(1));

    verify(userService).getTotalUserCount();
    verify(userService).getEnabledUserCount();
    verify(userService).findUsersByRole("ADMIN");
  }

  // ==================== GET ORDER STATISTICS TESTS ====================

  @Test
  @DisplayName("Get order statistics - Success")
  @WithMockUser(roles = "ADMIN")
  void getOrderStats_Success() throws Exception {
    // Arrange
    when(orderService.getOrderStatistics()).thenReturn(testOrderStatistics);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/orders/stats"))
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
  void getOrderStats_WithDateRange() throws Exception {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(30);
    LocalDateTime endDate = LocalDateTime.now();

    when(orderService.getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(testOrderStatistics);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/dashboard/orders/stats")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(100));

    verify(orderService).getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class));
    verify(orderService, never()).getOrderStatistics();
  }

  @Test
  @DisplayName("Get order statistics - Only start date provided")
  @WithMockUser(roles = "ADMIN")
  void getOrderStats_OnlyStartDate() throws Exception {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(30);

    when(orderService.getOrderStatistics()).thenReturn(testOrderStatistics);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/orders/stats").param("startDate", startDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(100));

    verify(orderService).getOrderStatistics();
    verify(orderService, never())
        .getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  // ==================== GET DAILY TRENDS TESTS ====================

  @Test
  @DisplayName("Get daily trends - Success")
  @WithMockUser(roles = "ADMIN")
  void getDailyTrends_Success() throws Exception {
    // Arrange
    List<DailyOrderStatistics> dailyTrends = Arrays.asList(testDailyStatistics);
    LocalDateTime startDate = LocalDateTime.now().minusDays(7);
    LocalDateTime endDate = LocalDateTime.now();

    when(orderService.getDailyOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(dailyTrends);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/dashboard/trends/daily")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].orderCount").value(5))
        .andExpect(jsonPath("$[0].revenue").value(499.95));

    verify(orderService)
        .getDailyOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Get daily trends - Missing required parameters")
  @WithMockUser(roles = "ADMIN")
  void getDailyTrends_MissingParameters() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/dashboard/trends/daily")).andExpect(status().isBadRequest());

    verify(orderService, never()).getDailyOrderStatistics(any(), any());
  }

  @Test
  @DisplayName("Get daily trends - Invalid date format")
  @WithMockUser(roles = "ADMIN")
  void getDailyTrends_InvalidDateFormat() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/dashboard/trends/daily")
                .param("startDate", "invalid-date")
                .param("endDate", "invalid-date"))
        .andExpect(status().isBadRequest());

    verify(orderService, never()).getDailyOrderStatistics(any(), any());
  }

  // ==================== GET SYSTEM HEALTH TESTS ====================

  @Test
  @DisplayName("Get system health - Success")
  @WithMockUser(roles = "ADMIN")
  void getSystemHealth_Success() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/system/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uptimeSeconds").exists())
        .andExpect(jsonPath("$.databaseStatus").value("HEALTHY"))
        .andExpect(jsonPath("$.averageResponseTime").value(125))
        .andExpect(jsonPath("$.activeSessions").value(0))
        .andExpect(jsonPath("$.lastBackupTime").exists());
  }

  // ==================== GET LOW STOCK ALERTS TESTS ====================

  @Test
  @DisplayName("Get low stock alerts - Success")
  @WithMockUser(roles = "ADMIN")
  void getLowStockAlerts_Success() throws Exception {
    // Arrange - Create a simple product without circular references
    Product simpleProduct = new Product();
    simpleProduct.setId(testProduct.getId());
    simpleProduct.setName("Premium Game Account");

    List<Product> lowStockProducts = Arrays.asList(simpleProduct);
    when(stockService.getProductsWithLowStock(null)).thenReturn(lowStockProducts);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/alerts/low-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(testProduct.getId().toString()))
        .andExpect(jsonPath("$[0].name").value("Premium Game Account"));

    verify(stockService).getProductsWithLowStock(null);
  }

  @Test
  @DisplayName("Get low stock alerts - No low stock products")
  @WithMockUser(roles = "ADMIN")
  void getLowStockAlerts_NoLowStockProducts() throws Exception {
    // Arrange
    when(stockService.getProductsWithLowStock(isNull())).thenReturn(Arrays.asList());

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/alerts/low-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    verify(stockService).getProductsWithLowStock(null);
  }

  // ==================== EDGE CASES AND ERROR HANDLING TESTS ====================

  @Test
  @DisplayName("Get dashboard overview - Service throws exception")
  @WithMockUser(roles = "ADMIN")
  void getDashboardOverview_ServiceException() throws Exception {
    // Arrange
    when(userService.getTotalUserCount())
        .thenThrow(new RuntimeException("Database connection failed"));

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/overview"))
        .andExpect(status().isInternalServerError());

    verify(userService).getTotalUserCount();
  }

  @Test
  @DisplayName("Get user statistics - Service returns null")
  @WithMockUser(roles = "ADMIN")
  void getUserStats_ServiceReturnsNull() throws Exception {
    // Arrange
    when(userService.getTotalUserCount()).thenReturn(0L);
    when(userService.getEnabledUserCount()).thenReturn(0L);
    when(userService.findUsersByRole("ADMIN")).thenReturn(Arrays.asList());

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/users/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsers").value(0))
        .andExpect(jsonPath("$.activeUsers").value(0))
        .andExpect(jsonPath("$.adminUsers").value(0));

    verify(userService).getTotalUserCount();
    verify(userService).getEnabledUserCount();
    verify(userService).findUsersByRole("ADMIN");
  }

  @Test
  @DisplayName("Get order statistics - Service returns empty statistics")
  @WithMockUser(roles = "ADMIN")
  void getOrderStats_EmptyStatistics() throws Exception {
    // Arrange
    OrderStatistics emptyStats =
        new OrderStatistics(
            0L, // total
            0L, // pending
            0L, // processing
            0L, // completed
            0L, // failed
            0L, // cancelled
            BigDecimal.ZERO, // totalRevenue
            BigDecimal.ZERO // averageOrderValue
            );

    when(orderService.getOrderStatistics()).thenReturn(emptyStats);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/dashboard/orders/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.pending").value(0))
        .andExpect(jsonPath("$.completed").value(0))
        .andExpect(jsonPath("$.failed").value(0))
        .andExpect(jsonPath("$.totalRevenue").value(0))
        .andExpect(jsonPath("$.averageOrderValue").value(0));

    verify(orderService).getOrderStatistics();
  }

  @Test
  @DisplayName("Get daily trends - Empty results")
  @WithMockUser(roles = "ADMIN")
  void getDailyTrends_EmptyResults() throws Exception {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(7);
    LocalDateTime endDate = LocalDateTime.now();

    when(orderService.getDailyOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Arrays.asList());

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/dashboard/trends/daily")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    verify(orderService)
        .getDailyOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Get daily trends - Large date range")
  @WithMockUser(roles = "ADMIN")
  void getDailyTrends_LargeDateRange() throws Exception {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(365);
    LocalDateTime endDate = LocalDateTime.now();

    List<DailyOrderStatistics> largeTrendList =
        Arrays.asList(testDailyStatistics, testDailyStatistics, testDailyStatistics);

    when(orderService.getDailyOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(largeTrendList);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/admin/dashboard/trends/daily")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)));

    verify(orderService)
        .getDailyOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  // ==================== AUTHORIZATION TESTS ====================

  @Test
  @DisplayName("All endpoints require admin role - User stats")
  @WithMockUser(roles = "USER")
  void allEndpointsRequireAdminRole_UserStats() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/users/stats")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("All endpoints require admin role - Order stats")
  @WithMockUser(roles = "USER")
  void allEndpointsRequireAdminRole_OrderStats() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/orders/stats")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("All endpoints require admin role - Daily trends")
  @WithMockUser(roles = "USER")
  void allEndpointsRequireAdminRole_DailyTrends() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/dashboard/trends/daily")
                .param("startDate", LocalDateTime.now().toString())
                .param("endDate", LocalDateTime.now().toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("All endpoints require admin role - System health")
  @WithMockUser(roles = "USER")
  void allEndpointsRequireAdminRole_SystemHealth() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/system/health")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("All endpoints require admin role - Low stock alerts")
  @WithMockUser(roles = "USER")
  void allEndpointsRequireAdminRole_LowStockAlerts() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/alerts/low-stock")).andExpect(status().isForbidden());
  }

  // ==================== INTEGRATION-STYLE TESTS ====================

  @Test
  @DisplayName("Dashboard overview includes all required sections")
  @WithMockUser(roles = "ADMIN")
  void dashboardOverview_IncludesAllSections() throws Exception {
    // Arrange - Setup minimal mocks for all services
    when(userService.getTotalUserCount()).thenReturn(100L);
    when(userService.getEnabledUserCount()).thenReturn(95L);
    when(userService.findUsersByRole("ADMIN")).thenReturn(Arrays.asList(testUser));
    when(orderService.getOrderStatistics()).thenReturn(testOrderStatistics);
    when(orderService.getOrderStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(testOrderStatistics);
    when(productService.findAllProducts(any()))
        .thenReturn(
            new org.springframework.data.domain.PageImpl<>(
                Arrays.asList(testProduct),
                org.springframework.data.domain.PageRequest.of(0, 1),
                500L));
    when(categoryService.findAllCategories()).thenReturn(Arrays.asList(testCategory));
    when(categoryService.findActiveCategories()).thenReturn(Arrays.asList(testCategory));
    when(stockService.getExpiredReservations()).thenReturn(Arrays.asList());
    when(stockService.getTotalStockCount(any())).thenReturn(100L);
    when(stockService.getAvailableStockCount(any())).thenReturn(80L);
    when(stockService.getReservedStockCount(any())).thenReturn(10L);
    when(stockService.getSoldStockCount(any())).thenReturn(10L);
    when(stockService.getProductsWithLowStock(null)).thenReturn(Arrays.asList());

    // Act & Assert - Verify all major sections are present
    mockMvc
        .perform(get("/api/admin/dashboard/overview"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userStats").exists())
        .andExpect(jsonPath("$.orderStats").exists())
        .andExpect(jsonPath("$.productStats").exists())
        .andExpect(jsonPath("$.stockStats").exists())
        .andExpect(jsonPath("$.revenueStats").exists())
        .andExpect(jsonPath("$.systemInfo").exists());
  }
}
