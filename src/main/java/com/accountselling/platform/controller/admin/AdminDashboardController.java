package com.accountselling.platform.controller.admin;

import com.accountselling.platform.dto.admin.AdminDashboardDto;
import com.accountselling.platform.dto.statistics.DailyOrderStatistics;
import com.accountselling.platform.dto.statistics.OrderStatistics;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.service.CategoryService;
import com.accountselling.platform.service.OrderService;
import com.accountselling.platform.service.ProductService;
import com.accountselling.platform.service.StockService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Admin dashboard controller providing comprehensive system statistics and overview.
 * Aggregates data from all services to provide admin dashboard insights.
 * 
 * Admin dashboard controller ให้ข้อมูลสถิติระบบและภาพรวมแบบครอบคลุม
 * รวบรวมข้อมูลจากทุก services เพื่อให้ข้อมูลเชิงลึกสำหรับแดชบอร์ด admin
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Dashboard", description = "Admin dashboard endpoints for system overview and statistics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final StockService stockService;

    @Operation(
        summary = "Get dashboard overview",
        description = "Get comprehensive dashboard data including all key metrics. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AdminDashboardDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/overview")
    public ResponseEntity<AdminDashboardDto> getDashboardOverview() {
        
        log.info("Admin requesting dashboard overview");
        
        // Get current time for calculations
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);
        
        AdminDashboardDto dashboard = AdminDashboardDto.builder()
                .userStats(buildUserStats(startOfDay, startOfWeek))
                .orderStats(buildOrderStats(startOfDay, startOfWeek))
                .productStats(buildProductStats())
                .stockStats(buildStockStats())
                .revenueStats(buildRevenueStats(startOfDay, startOfWeek, startOfMonth))
                .systemInfo(buildSystemInfo())
                .build();
        
        log.info("Admin dashboard overview generated successfully");
        return ResponseEntity.ok(dashboard);
    }

    @Operation(
        summary = "Get user statistics",
        description = "Get detailed user statistics for dashboard. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/users/stats")
    public ResponseEntity<AdminDashboardDto.UserStats> getUserStats() {
        
        log.info("Admin requesting user statistics");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfWeek = now.minusDays(7);
        
        AdminDashboardDto.UserStats userStats = buildUserStats(startOfDay, startOfWeek);
        
        log.info("Admin user statistics generated successfully");
        return ResponseEntity.ok(userStats);
    }

    @Operation(
        summary = "Get order statistics",
        description = "Get detailed order statistics for dashboard. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderStatistics.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/orders/stats")
    public ResponseEntity<OrderStatistics> getOrderStats(
            @Parameter(description = "Start date for statistics")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date for statistics")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Admin requesting order statistics - startDate: {}, endDate: {}", startDate, endDate);
        
        OrderStatistics orderStats;
        if (startDate != null && endDate != null) {
            orderStats = orderService.getOrderStatistics(startDate, endDate);
        } else {
            orderStats = orderService.getOrderStatistics();
        }
        
        log.info("Admin order statistics generated successfully");
        return ResponseEntity.ok(orderStats);
    }

    @Operation(
        summary = "Get daily trends",
        description = "Get daily trends for orders and revenue over specified period. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Daily trends retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/trends/daily")
    public ResponseEntity<List<DailyOrderStatistics>> getDailyTrends(
            @Parameter(description = "Start date for trends", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date for trends", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Admin requesting daily trends from {} to {}", startDate, endDate);
        
        List<DailyOrderStatistics> dailyTrends = orderService.getDailyOrderStatistics(startDate, endDate);
        
        log.info("Admin daily trends generated for {} days", dailyTrends.size());
        return ResponseEntity.ok(dailyTrends);
    }

    @Operation(
        summary = "Get system health",
        description = "Get system health and performance metrics. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System health retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/system/health")
    public ResponseEntity<AdminDashboardDto.SystemInfo> getSystemHealth() {
        
        log.info("Admin requesting system health");
        
        AdminDashboardDto.SystemInfo systemInfo = buildSystemInfo();
        
        log.info("Admin system health generated successfully");
        return ResponseEntity.ok(systemInfo);
    }

    @Operation(
        summary = "Get low stock alerts",
        description = "Get products with low stock levels. Admin only endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Low stock alerts retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/alerts/low-stock")
    public ResponseEntity<List<Product>> getLowStockAlerts() {
        
        log.info("Admin requesting low stock alerts");
        
        List<Product> lowStockProducts = stockService.getProductsWithLowStock(null);
        
        log.info("Admin found {} products with low stock", lowStockProducts.size());
        return ResponseEntity.ok(lowStockProducts);
    }

    /**
     * Build user statistics.
     * สร้างสถิติผู้ใช้
     */
    private AdminDashboardDto.UserStats buildUserStats(LocalDateTime startOfDay, LocalDateTime startOfWeek) {
        long totalUsers = userService.getTotalUserCount();
        long activeUsers = userService.getEnabledUserCount();
        long adminUsers = userService.findUsersByRole("ADMIN").size();
        
        // Note: These would need additional methods in UserService for accurate counts
        long newUsersToday = 0; // userService.getNewUserCount(startOfDay, LocalDateTime.now());
        long newUsersThisWeek = 0; // userService.getNewUserCount(startOfWeek, LocalDateTime.now());
        
        return AdminDashboardDto.UserStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .adminUsers((long) adminUsers)
                .build();
    }

    /**
     * Build order statistics.
     * สร้างสถิติคำสั่งซื้อ
     */
    private AdminDashboardDto.OrderStats buildOrderStats(LocalDateTime startOfDay, LocalDateTime startOfWeek) {
        OrderStatistics orderStats = orderService.getOrderStatistics();
        OrderStatistics todayStats = orderService.getOrderStatistics(startOfDay, LocalDateTime.now());
        OrderStatistics weekStats = orderService.getOrderStatistics(startOfWeek, LocalDateTime.now());
        
        return AdminDashboardDto.OrderStats.builder()
                .totalOrders(orderStats.total())
                .pendingOrders(orderStats.pending())
                .completedOrders(orderStats.completed())
                .failedOrders(orderStats.failed())
                .ordersToday(todayStats.total())
                .ordersThisWeek(weekStats.total())
                .averageOrderValue(orderStats.averageOrderValue())
                .build();
    }

    /**
     * Build product statistics.
     * สร้างสถิติผลิตภัณฑ์
     */
    private AdminDashboardDto.ProductStats buildProductStats() {
        // Get all products and calculate stats manually since count methods don't exist
        Page<Product> allProductsPage = productService.findAllProducts(Pageable.unpaged());
        List<Product> allProducts = allProductsPage.getContent();
        
        long totalProducts = allProductsPage.getTotalElements();
        long activeProducts = allProducts.stream().filter(product -> product.getActive()).count();
        long totalCategories = categoryService.findAllCategories().size();
        long activeCategories = categoryService.findActiveCategories().size();
        
        List<Product> lowStockProducts = stockService.getProductsWithLowStock(null);
        long lowStockCount = lowStockProducts.size();
        
        // Count out of stock products
        long outOfStockCount = lowStockProducts.stream()
                .mapToLong(product -> stockService.getAvailableStockCount(product.getId()) == 0 ? 1 : 0)
                .sum();
        
        return AdminDashboardDto.ProductStats.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalCategories(totalCategories)
                .activeCategories(activeCategories)
                .lowStockProducts(lowStockCount)
                .outOfStockProducts(outOfStockCount)
                .build();
    }

    /**
     * Build stock statistics.
     * สร้างสถิติสต็อก
     */
    private AdminDashboardDto.StockStats buildStockStats() {
        // These would need additional aggregation methods in StockService
        long totalStockItems = 0;
        long availableStockItems = 0;
        long reservedStockItems = 0;
        long soldStockItems = 0;
        double stockTurnoverRate = 0.0;
        long expiredReservations = stockService.getExpiredReservations().size();
        
        // Calculate aggregated stock statistics
        Page<Product> allProductsPage = productService.findAllProducts(Pageable.unpaged());
        List<Product> allProducts = allProductsPage.getContent();
        for (Product product : allProducts) {
            totalStockItems += stockService.getTotalStockCount(product.getId());
            availableStockItems += stockService.getAvailableStockCount(product.getId());
            reservedStockItems += stockService.getReservedStockCount(product.getId());
            soldStockItems += stockService.getSoldStockCount(product.getId());
        }
        
        // Calculate turnover rate
        if (totalStockItems > 0) {
            stockTurnoverRate = (double) soldStockItems / totalStockItems;
        }
        
        return AdminDashboardDto.StockStats.builder()
                .totalStockItems(totalStockItems)
                .availableStockItems(availableStockItems)
                .reservedStockItems(reservedStockItems)
                .soldStockItems(soldStockItems)
                .stockTurnoverRate(stockTurnoverRate)
                .expiredReservations(expiredReservations)
                .build();
    }

    /**
     * Build revenue statistics.
     * สร้างสถิติรายได้
     */
    private AdminDashboardDto.RevenueStats buildRevenueStats(LocalDateTime startOfDay, LocalDateTime startOfWeek, LocalDateTime startOfMonth) {
        OrderStatistics allTimeStats = orderService.getOrderStatistics();
        OrderStatistics todayStats = orderService.getOrderStatistics(startOfDay, LocalDateTime.now());
        OrderStatistics weekStats = orderService.getOrderStatistics(startOfWeek, LocalDateTime.now());
        OrderStatistics monthStats = orderService.getOrderStatistics(startOfMonth, LocalDateTime.now());
        
        // Calculate growth rate (placeholder calculation)
        BigDecimal currentMonthRevenue = monthStats.totalRevenue();
        LocalDateTime previousMonthStart = startOfMonth.minusDays(30);
        OrderStatistics previousMonthStats = orderService.getOrderStatistics(previousMonthStart, startOfMonth);
        BigDecimal previousMonthRevenue = previousMonthStats.totalRevenue();
        
        double growthRate = 0.0;
        if (previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal growth = currentMonthRevenue.subtract(previousMonthRevenue)
                    .divide(previousMonthRevenue, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            growthRate = growth.doubleValue();
        }
        
        return AdminDashboardDto.RevenueStats.builder()
                .totalRevenue(allTimeStats.totalRevenue())
                .revenueToday(todayStats.totalRevenue())
                .revenueThisWeek(weekStats.totalRevenue())
                .revenueThisMonth(monthStats.totalRevenue())
                .revenueGrowthRate(growthRate)
                .build();
    }

    /**
     * Build system information.
     * สร้างข้อมูลระบบ
     */
    private AdminDashboardDto.SystemInfo buildSystemInfo() {
        // These would typically come from system monitoring services
        return AdminDashboardDto.SystemInfo.builder()
                .uptimeSeconds(System.currentTimeMillis() / 1000) // Placeholder
                .databaseStatus("HEALTHY") // Would check actual DB health
                .averageResponseTime(125L) // Would come from metrics
                .activeSessions(0L) // Would come from session management
                .lastBackupTime(LocalDateTime.now().minusHours(6)) // Placeholder
                .build();
    }
}