package com.accountselling.platform.controller;

import com.accountselling.platform.model.User;
import com.accountselling.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for admin-only operations. Provides administrative endpoints for system
 * management and monitoring.
 *
 * <p>Controller สำหรับฟังก์ชันการจัดการระบบที่เฉพาะแอดมินเท่านั้นที่สามารถเข้าถึงได้
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative endpoints for system management")
public class AdminController {

  private final UserService userService;

  /**
   * Get system dashboard statistics. Provides overview of system usage and metrics.
   *
   * @return dashboard statistics including user counts, order metrics, etc.
   */
  @GetMapping("/dashboard")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get admin dashboard",
      description = "Get system overview and statistics for admin dashboard")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
      })
  public ResponseEntity<Map<String, Object>> getDashboard() {
    log.info("Processing admin dashboard request");

    Map<String, Object> dashboard = new HashMap<>();

    // Get basic statistics
    long totalUsers = userService.countAllUsers();
    long activeUsers = userService.countActiveUsers();

    dashboard.put("totalUsers", totalUsers);
    dashboard.put("activeUsers", activeUsers);
    dashboard.put("inactiveUsers", totalUsers - activeUsers);

    // Add more statistics as needed
    dashboard.put("timestamp", java.time.LocalDateTime.now());
    dashboard.put("systemStatus", "operational");

    log.info("Admin dashboard data retrieved successfully");
    return ResponseEntity.ok(dashboard);
  }

  // Note: User management endpoints have been moved to AdminUserController
  // to avoid mapping conflicts and provide more comprehensive user management features

  /**
   * Enable or disable a user account. Allows admin to manage user account status.
   *
   * @param userId the ID of the user to update
   * @param enabled whether to enable (true) or disable (false) the account
   * @return success message with user status
   */
  @PutMapping("/users/{userId}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Update user account status",
      description = "Enable or disable a user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  public ResponseEntity<Map<String, Object>> updateUserStatus(
      @PathVariable String userId, @RequestParam boolean enabled) {
    log.info(
        "Processing admin update user status request - user: {}, enabled: {}", userId, enabled);

    User user = userService.updateUserStatus(userId, enabled);

    Map<String, Object> response = new HashMap<>();
    response.put("message", "User status updated successfully");
    response.put("userId", userId);
    response.put("username", user.getUsername());
    response.put("enabled", user.getEnabled());
    response.put("updatedAt", user.getUpdatedAt());

    log.info("User status updated successfully - user: {}, enabled: {}", userId, enabled);
    return ResponseEntity.ok(response);
  }

  /**
   * Get system health information. Provides system status and health metrics.
   *
   * @return system health status and metrics
   */
  @GetMapping("/system/health")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get system health",
      description = "Get system health status and metrics for monitoring")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "System health retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
      })
  public ResponseEntity<Map<String, Object>> getSystemHealth() {
    log.info("Processing admin system health request");

    Map<String, Object> health = new HashMap<>();
    health.put("status", "UP");
    health.put("timestamp", java.time.LocalDateTime.now());
    health.put("version", "1.0.0");
    health.put("environment", "development");

    // Add database connectivity check
    try {
      userService.countAllUsers(); // Simple query to test DB connection
      health.put("database", "UP");
    } catch (Exception e) {
      health.put("database", "DOWN");
      health.put("databaseError", e.getMessage());
    }

    log.info("System health check completed");
    return ResponseEntity.ok(health);
  }

  /**
   * Privileged operation that only SUPER_ADMIN can access. Demonstrates role hierarchy.
   *
   * @return system configuration information
   */
  @GetMapping("/system/config")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
      summary = "Get system configuration",
      description = "Get system configuration (SUPER_ADMIN only)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - super admin access required")
      })
  public ResponseEntity<Map<String, Object>> getSystemConfig() {
    log.info("Processing super admin system config request");

    Map<String, Object> config = new HashMap<>();
    config.put("message", "This endpoint requires SUPER_ADMIN role");
    config.put("accessLevel", "SUPER_ADMIN");
    config.put("timestamp", java.time.LocalDateTime.now());

    log.info("System config accessed by super admin");
    return ResponseEntity.ok(config);
  }
}
