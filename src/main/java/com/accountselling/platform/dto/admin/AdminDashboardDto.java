package com.accountselling.platform.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for admin dashboard data.
 * DTO สำหรับข้อมูลแดชบอร์ด admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin dashboard overview data")
public class AdminDashboardDto {

    @Schema(description = "User statistics")
    private UserStats userStats;

    @Schema(description = "Order statistics")
    private OrderStats orderStats;

    @Schema(description = "Product and category statistics")
    private ProductStats productStats;

    @Schema(description = "Stock and inventory statistics")
    private StockStats stockStats;

    @Schema(description = "Revenue statistics")
    private RevenueStats revenueStats;

    @Schema(description = "System information")
    private SystemInfo systemInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User statistics")
    public static class UserStats {
        @Schema(description = "Total number of users", example = "1250")
        private Long totalUsers;

        @Schema(description = "Number of active users", example = "1200")
        private Long activeUsers;

        @Schema(description = "Number of new users today", example = "15")
        private Long newUsersToday;

        @Schema(description = "Number of new users this week", example = "87")
        private Long newUsersThisWeek;

        @Schema(description = "Number of admin users", example = "5")
        private Long adminUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Order statistics")
    public static class OrderStats {
        @Schema(description = "Total number of orders", example = "2350")
        private Long totalOrders;

        @Schema(description = "Number of pending orders", example = "25")
        private Long pendingOrders;

        @Schema(description = "Number of completed orders", example = "2200")
        private Long completedOrders;

        @Schema(description = "Number of failed orders", example = "125")
        private Long failedOrders;

        @Schema(description = "Number of orders today", example = "45")
        private Long ordersToday;

        @Schema(description = "Number of orders this week", example = "285")
        private Long ordersThisWeek;

        @Schema(description = "Average order value", example = "89.50")
        private BigDecimal averageOrderValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Product and category statistics")
    public static class ProductStats {
        @Schema(description = "Total number of products", example = "450")
        private Long totalProducts;

        @Schema(description = "Number of active products", example = "425")
        private Long activeProducts;

        @Schema(description = "Number of categories", example = "25")
        private Long totalCategories;

        @Schema(description = "Number of active categories", example = "23")
        private Long activeCategories;

        @Schema(description = "Products with low stock", example = "12")
        private Long lowStockProducts;

        @Schema(description = "Out of stock products", example = "3")
        private Long outOfStockProducts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Stock and inventory statistics")
    public static class StockStats {
        @Schema(description = "Total stock items", example = "15240")
        private Long totalStockItems;

        @Schema(description = "Available stock items", example = "12890")
        private Long availableStockItems;

        @Schema(description = "Reserved stock items", example = "150")
        private Long reservedStockItems;

        @Schema(description = "Sold stock items", example = "2200")
        private Long soldStockItems;

        @Schema(description = "Stock turnover rate", example = "0.85")
        private Double stockTurnoverRate;

        @Schema(description = "Expired reservations", example = "8")
        private Long expiredReservations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Revenue statistics")
    public static class RevenueStats {
        @Schema(description = "Total revenue", example = "195420.50")
        private BigDecimal totalRevenue;

        @Schema(description = "Revenue today", example = "2450.00")
        private BigDecimal revenueToday;

        @Schema(description = "Revenue this week", example = "18750.50")
        private BigDecimal revenueThisWeek;

        @Schema(description = "Revenue this month", example = "67890.25")
        private BigDecimal revenueThisMonth;

        @Schema(description = "Revenue growth rate (month over month)", example = "12.5")
        private Double revenueGrowthRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "System information")
    public static class SystemInfo {
        @Schema(description = "System uptime in seconds", example = "345600")
        private Long uptimeSeconds;

        @Schema(description = "Database connection status", example = "HEALTHY")
        private String databaseStatus;

        @Schema(description = "API response time average (ms)", example = "125")
        private Long averageResponseTime;

        @Schema(description = "Active user sessions", example = "89")
        private Long activeSessions;

        @Schema(description = "Last backup time")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastBackupTime;
    }
}