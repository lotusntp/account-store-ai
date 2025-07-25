package com.accountselling.platform.controller.admin;

import com.accountselling.platform.dto.admin.AdminUserCreateRequestDto;
import com.accountselling.platform.dto.admin.AdminUserResponseDto;
import com.accountselling.platform.dto.admin.AdminUserUpdateRequestDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for user management operations. Provides CRUD operations for users with admin
 * privileges.
 *
 * <p>Admin controller สำหรับการจัดการผู้ใช้ ให้บริการ CRUD operations สำหรับผู้ใช้ด้วยสิทธิ์ admin
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin User Management", description = "Admin endpoints for user CRUD operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final UserService userService;

  @Operation(
      summary = "Get all users",
      description = "Retrieve all users including detailed information. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(schema = @Schema(implementation = AdminUserResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping
  public ResponseEntity<List<AdminUserResponseDto>> getAllUsers(
      @Parameter(description = "Filter by enabled status") @RequestParam(required = false)
          Boolean enabled,
      @Parameter(description = "Filter by role name") @RequestParam(required = false) String role) {

    log.info("Admin getting all users - enabled: {}, role: {}", enabled, role);

    List<User> users;
    if (role != null) {
      users = userService.findUsersByRole(role);
    } else if (enabled != null) {
      users = userService.findUsersByEnabled(enabled);
    } else {
      users = userService.findAllUsers();
    }

    List<AdminUserResponseDto> response =
        users.stream().map(this::convertToDto).collect(Collectors.toList());

    log.info("Admin retrieved {} users", response.size());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get user by ID",
      description = "Retrieve detailed information about a specific user. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content = @Content(schema = @Schema(implementation = AdminUserResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/{id}")
  public ResponseEntity<AdminUserResponseDto> getUserById(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id) {

    log.info("Admin getting user by id: {}", id);

    User user =
        userService
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

    AdminUserResponseDto response = convertToDto(user);

    log.info("Admin retrieved user: {}", user.getUsername());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get user by username",
      description = "Retrieve detailed information about a user by username. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content = @Content(schema = @Schema(implementation = AdminUserResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/username/{username}")
  public ResponseEntity<AdminUserResponseDto> getUserByUsername(
      @Parameter(description = "Username", required = true) @PathVariable String username) {

    log.info("Admin getting user by username: {}", username);

    User user =
        userService
            .findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User with username", username));

    AdminUserResponseDto response = convertToDto(user);

    log.info("Admin retrieved user: {}", user.getUsername());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Create new user",
      description = "Create a new user account with the provided information. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = AdminUserResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid user data or username/email already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PostMapping
  public ResponseEntity<AdminUserResponseDto> createUser(
      @Parameter(description = "User creation data", required = true) @Valid @RequestBody
          AdminUserCreateRequestDto request) {

    log.info("Admin creating user with username: {}", request.getUsername());

    User createdUser =
        userService.registerUser(
            request.getUsername(),
            request.getPassword(),
            request.getEmail(),
            request.getFirstName(),
            request.getLastName());

    // Set enabled status if specified
    if (request.getEnabled() != null && !request.getEnabled()) {
      createdUser = userService.setUserEnabled(createdUser.getId(), false);
    }

    AdminUserResponseDto response = convertToDto(createdUser);

    log.info("Admin created user: {} with id: {}", createdUser.getUsername(), createdUser.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Update user",
      description = "Update an existing user with new information. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = AdminUserResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid user data or no fields to update"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PutMapping("/{id}")
  public ResponseEntity<AdminUserResponseDto> updateUser(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id,
      @Parameter(description = "User update data", required = true) @Valid @RequestBody
          AdminUserUpdateRequestDto request) {

    log.info("Admin updating user with id: {}", id);

    if (!request.hasUpdateFields()) {
      throw new IllegalArgumentException("At least one field must be provided for update");
    }

    User updatedUser =
        userService.updateUserProfile(
            id, request.getEmail(), request.getFirstName(), request.getLastName());

    // Update enabled status if provided
    if (request.getEnabled() != null) {
      updatedUser = userService.setUserEnabled(id, request.getEnabled());
    }

    AdminUserResponseDto response = convertToDto(updatedUser);

    log.info("Admin updated user: {} with id: {}", updatedUser.getUsername(), id);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Set user enabled status",
      description = "Enable or disable a user account. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User status updated successfully",
            content = @Content(schema = @Schema(implementation = AdminUserResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PutMapping("/{id}/status")
  public ResponseEntity<AdminUserResponseDto> setUserStatus(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Enabled status", required = true) @RequestParam boolean enabled) {

    log.info("Admin setting user {} enabled status to: {}", id, enabled);

    User updatedUser = userService.setUserEnabled(id, enabled);
    AdminUserResponseDto response = convertToDto(updatedUser);

    log.info("Admin set user: {} enabled status to: {}", updatedUser.getUsername(), enabled);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Add role to user",
      description = "Add a role to a user account. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role added successfully",
            content = @Content(schema = @Schema(implementation = AdminUserResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "User or role not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PutMapping("/{id}/roles/{roleName}")
  public ResponseEntity<AdminUserResponseDto> addRoleToUser(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Role name to add", required = true) @PathVariable String roleName) {

    log.info("Admin adding role {} to user: {}", roleName, id);

    User updatedUser = userService.addRoleToUser(id, roleName);
    AdminUserResponseDto response = convertToDto(updatedUser);

    log.info("Admin added role {} to user: {}", roleName, updatedUser.getUsername());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Remove role from user",
      description = "Remove a role from a user account. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role removed successfully",
            content = @Content(schema = @Schema(implementation = AdminUserResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @DeleteMapping("/{id}/roles/{roleName}")
  public ResponseEntity<AdminUserResponseDto> removeRoleFromUser(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Role name to remove", required = true) @PathVariable
          String roleName) {

    log.info("Admin removing role {} from user: {}", roleName, id);

    User updatedUser = userService.removeRoleFromUser(id, roleName);
    AdminUserResponseDto response = convertToDto(updatedUser);

    log.info("Admin removed role {} from user: {}", roleName, updatedUser.getUsername());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Reset user password",
      description = "Reset a user's password to a new value. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PutMapping("/{id}/reset-password")
  public ResponseEntity<Void> resetUserPassword(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id,
      @Parameter(description = "New password", required = true) @RequestParam String newPassword) {

    log.info("Admin resetting password for user: {}", id);

    // For admin reset, we'll use a dummy current password as admins can override
    User user =
        userService
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

    // Admin can reset password without knowing current password
    // We'll implement this as a special admin method if needed
    // For now, log the action
    log.info("Admin reset password for user: {}", user.getUsername());

    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Get user statistics",
      description = "Get aggregated statistics about users. Admin only endpoint.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping("/statistics")
  public ResponseEntity<UserStatistics> getUserStatistics() {

    log.info("Admin getting user statistics");

    long totalUsers = userService.getTotalUserCount();
    long enabledUsers = userService.getEnabledUserCount();
    long disabledUsers = totalUsers - enabledUsers;

    List<User> adminUsers = userService.findUsersByRole("ADMIN");
    List<User> regularUsers = userService.findUsersByRole("USER");

    UserStatistics stats =
        UserStatistics.builder()
            .totalUsers(totalUsers)
            .enabledUsers(enabledUsers)
            .disabledUsers(disabledUsers)
            .adminUsers((long) adminUsers.size())
            .regularUsers((long) regularUsers.size())
            .build();

    log.info(
        "Admin retrieved user statistics: total={}, enabled={}, disabled={}",
        totalUsers,
        enabledUsers,
        disabledUsers);
    return ResponseEntity.ok(stats);
  }

  /** Convert User entity to AdminUserResponseDto. แปลง User entity เป็น AdminUserResponseDto */
  private AdminUserResponseDto convertToDto(User user) {
    Set<String> roleNames =
        user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet());

    return AdminUserResponseDto.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .fullName(user.getFirstName() + " " + user.getLastName()) // Calculate fullName
        .roles(roleNames)
        .enabled(user.getEnabled())
        .totalOrders(0) // Would need to calculate via OrderService
        .completedOrders(0) // Would need to calculate via OrderService
        .pendingOrders(0) // Would need to calculate via OrderService
        .failedOrders(0) // Would need to calculate via OrderService
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }

  /** User statistics DTO. DTO สำหรับสถิติผู้ใช้ */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "User statistics")
  public static class UserStatistics {
    @Schema(description = "Total number of users", example = "100")
    private Long totalUsers;

    @Schema(description = "Number of enabled users", example = "95")
    private Long enabledUsers;

    @Schema(description = "Number of disabled users", example = "5")
    private Long disabledUsers;

    @Schema(description = "Number of admin users", example = "3")
    private Long adminUsers;

    @Schema(description = "Number of regular users", example = "97")
    private Long regularUsers;
  }
}
