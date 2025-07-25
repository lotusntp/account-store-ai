package com.accountselling.platform.controller.admin;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.config.TestSecurityConfig;
import com.accountselling.platform.dto.admin.AdminUserCreateRequestDto;
import com.accountselling.platform.dto.admin.AdminUserUpdateRequestDto;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Role;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive unit tests for AdminUserController. Tests all user management operations,
 * validation, authorization, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(AdminUserController.class)
@Import(TestSecurityConfig.class)
class AdminUserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserService userService;

  @Autowired private ObjectMapper objectMapper;

  private User testUser;
  private User adminUser;
  private Role userRole;
  private Role adminRole;
  private AdminUserCreateRequestDto createRequest;
  private AdminUserUpdateRequestDto updateRequest;

  @BeforeEach
  void setUp() {
    // Setup roles
    userRole = new Role();
    userRole.setId(1L);
    userRole.setName("USER");
    userRole.setDescription("Standard user role");

    adminRole = new Role();
    adminRole.setId(2L);
    adminRole.setName("ADMIN");
    adminRole.setDescription("Administrator role");

    // Setup test user
    testUser = new User();
    testUser.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174002"));
    testUser.setUsername("johndoe");
    testUser.setEmail("john.doe@example.com");
    testUser.setFirstName("John");
    testUser.setLastName("Doe");
    testUser.setEnabled(true);
    testUser.setRoles(Set.of(userRole));
    testUser.setCreatedAt(LocalDateTime.now().minusDays(30));
    testUser.setUpdatedAt(LocalDateTime.now().minusDays(5));

    // Setup admin user
    adminUser = new User();
    adminUser.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174003"));
    adminUser.setUsername("admin");
    adminUser.setEmail("admin@example.com");
    adminUser.setFirstName("Admin");
    adminUser.setLastName("User");
    adminUser.setEnabled(true);
    adminUser.setRoles(Set.of(adminRole, userRole));
    adminUser.setCreatedAt(LocalDateTime.now().minusDays(100));
    adminUser.setUpdatedAt(LocalDateTime.now().minusDays(10));

    // Setup DTOs
    createRequest = new AdminUserCreateRequestDto();
    createRequest.setUsername("newuser");
    createRequest.setPassword("password123");
    createRequest.setEmail("newuser@example.com");
    createRequest.setFirstName("New");
    createRequest.setLastName("User");
    createRequest.setEnabled(true);

    updateRequest = new AdminUserUpdateRequestDto();
    updateRequest.setEmail("updated@example.com");
    updateRequest.setFirstName("Updated");
    updateRequest.setLastName("Name");
    updateRequest.setEnabled(false);
  }

  // ==================== GET ALL USERS TESTS ====================

  @Test
  @DisplayName("Get all users - Success")
  @WithMockUser(roles = "ADMIN")
  void getAllUsers_Success() throws Exception {
    // Arrange
    List<User> users = Arrays.asList(testUser, adminUser);
    when(userService.findAllUsers()).thenReturn(users);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(testUser.getId().toString()))
        .andExpect(jsonPath("$[0].username").value("johndoe"))
        .andExpect(jsonPath("$[0].email").value("john.doe@example.com"))
        .andExpect(jsonPath("$[0].firstName").value("John"))
        .andExpect(jsonPath("$[0].lastName").value("Doe"))
        .andExpect(jsonPath("$[0].enabled").value(true))
        .andExpect(jsonPath("$[0].roles", hasItem("USER")))
        .andExpect(jsonPath("$[1].id").value(adminUser.getId().toString()))
        .andExpect(jsonPath("$[1].username").value("admin"))
        .andExpect(jsonPath("$[1].roles", hasItems("ADMIN", "USER")));

    verify(userService).findAllUsers();
  }

  @Test
  @DisplayName("Get users by enabled status - Success")
  @WithMockUser(roles = "ADMIN")
  void getUsersByEnabled_Success() throws Exception {
    // Arrange
    List<User> enabledUsers = Arrays.asList(testUser, adminUser);
    when(userService.findUsersByEnabled(true)).thenReturn(enabledUsers);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/users").param("enabled", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].enabled").value(true))
        .andExpect(jsonPath("$[1].enabled").value(true));

    verify(userService).findUsersByEnabled(true);
    verify(userService, never()).findAllUsers();
  }

  @Test
  @DisplayName("Get users by role - Success")
  @WithMockUser(roles = "ADMIN")
  void getUsersByRole_Success() throws Exception {
    // Arrange
    List<User> adminUsers = Arrays.asList(adminUser);
    when(userService.findUsersByRole("ADMIN")).thenReturn(adminUsers);

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/users").param("role", "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(adminUser.getId().toString()))
        .andExpect(jsonPath("$[0].roles", hasItem("ADMIN")));

    verify(userService).findUsersByRole("ADMIN");
    verify(userService, never()).findAllUsers();
  }

  @Test
  @DisplayName("Get all users - Unauthorized")
  void getAllUsers_Unauthorized() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());

    verify(userService, never()).findAllUsers();
  }

  @Test
  @DisplayName("Get all users - Forbidden for non-admin")
  @WithMockUser(roles = "USER")
  void getAllUsers_Forbidden() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());

    verify(userService, never()).findAllUsers();
  }

  // ==================== GET USER BY ID TESTS ====================

  @Test
  @DisplayName("Get user by ID - Success")
  @WithMockUser(roles = "ADMIN")
  void getUserById_Success() throws Exception {
    // Arrange
    when(userService.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/users/{id}", testUser.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
        .andExpect(jsonPath("$.username").value("johndoe"))
        .andExpect(jsonPath("$.email").value("john.doe@example.com"))
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"))
        .andExpect(jsonPath("$.fullName").value("John Doe"))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.roles", hasItem("USER")))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());

    verify(userService).findById(testUser.getId());
  }

  @Test
  @DisplayName("Get user by ID - Not found")
  @WithMockUser(roles = "ADMIN")
  void getUserById_NotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(userService.findById(nonExistentId)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc.perform(get("/api/admin/users/{id}", nonExistentId)).andExpect(status().isNotFound());

    verify(userService).findById(nonExistentId);
  }

  // ==================== GET USER BY USERNAME TESTS ====================

  @Test
  @DisplayName("Get user by username - Success")
  @WithMockUser(roles = "ADMIN")
  void getUserByUsername_Success() throws Exception {
    // Arrange
    when(userService.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/users/username/{username}", "johndoe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
        .andExpect(jsonPath("$.username").value("johndoe"))
        .andExpect(jsonPath("$.email").value("john.doe@example.com"));

    verify(userService).findByUsername("johndoe");
  }

  @Test
  @DisplayName("Get user by username - Not found")
  @WithMockUser(roles = "ADMIN")
  void getUserByUsername_NotFound() throws Exception {
    // Arrange
    when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/users/username/{username}", "nonexistent"))
        .andExpect(status().isNotFound());

    verify(userService).findByUsername("nonexistent");
  }

  // ==================== CREATE USER TESTS ====================

  @Test
  @DisplayName("Create user - Success")
  @WithMockUser(roles = "ADMIN")
  void createUser_Success() throws Exception {
    // Arrange
    when(userService.registerUser(
            createRequest.getUsername(),
            createRequest.getPassword(),
            createRequest.getEmail(),
            createRequest.getFirstName(),
            createRequest.getLastName()))
        .thenReturn(testUser);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
        .andExpect(jsonPath("$.username").value("johndoe"))
        .andExpect(jsonPath("$.email").value("john.doe@example.com"))
        .andExpect(jsonPath("$.enabled").value(true));

    verify(userService)
        .registerUser(
            createRequest.getUsername(),
            createRequest.getPassword(),
            createRequest.getEmail(),
            createRequest.getFirstName(),
            createRequest.getLastName());
  }

  @Test
  @DisplayName("Create user - Create disabled user")
  @WithMockUser(roles = "ADMIN")
  void createUser_CreateDisabledUser() throws Exception {
    // Arrange
    createRequest.setEnabled(false);
    User disabledUser = new User();
    disabledUser.setId(UUID.randomUUID());
    disabledUser.setUsername(createRequest.getUsername());
    disabledUser.setEnabled(false);

    when(userService.registerUser(any(), any(), any(), any(), any())).thenReturn(testUser);
    when(userService.setUserEnabled(testUser.getId(), false)).thenReturn(disabledUser);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.enabled").value(false));

    verify(userService).setUserEnabled(testUser.getId(), false);
  }

  @Test
  @DisplayName("Create user - Invalid input")
  @WithMockUser(roles = "ADMIN")
  void createUser_InvalidInput() throws Exception {
    // Arrange
    AdminUserCreateRequestDto invalidRequest = new AdminUserCreateRequestDto();
    invalidRequest.setUsername(""); // Invalid: empty username
    invalidRequest.setPassword("123"); // Invalid: password too short
    invalidRequest.setEmail("invalid-email"); // Invalid: invalid email format

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    verify(userService, never()).registerUser(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Create user - Username already exists")
  @WithMockUser(roles = "ADMIN")
  void createUser_UsernameAlreadyExists() throws Exception {
    // Arrange
    when(userService.registerUser(any(), any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Username already exists"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isBadRequest());

    verify(userService).registerUser(any(), any(), any(), any(), any());
  }

  // ==================== UPDATE USER TESTS ====================

  @Test
  @DisplayName("Update user - Success")
  @WithMockUser(roles = "ADMIN")
  void updateUser_Success() throws Exception {
    // Arrange
    User updatedUser = new User();
    updatedUser.setId(testUser.getId());
    updatedUser.setUsername(testUser.getUsername());
    updatedUser.setEmail("updated@example.com");
    updatedUser.setFirstName("Updated");
    updatedUser.setLastName("Name");
    updatedUser.setEnabled(false);

    when(userService.updateUserProfile(
            testUser.getId(),
            updateRequest.getEmail(),
            updateRequest.getFirstName(),
            updateRequest.getLastName()))
        .thenReturn(updatedUser);

    when(userService.setUserEnabled(testUser.getId(), updateRequest.getEnabled()))
        .thenReturn(updatedUser);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/users/{id}", testUser.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
        .andExpect(jsonPath("$.username").value(testUser.getUsername()))
        .andExpect(jsonPath("$.email").value("updated@example.com"))
        .andExpect(jsonPath("$.firstName").value("Updated"))
        .andExpect(jsonPath("$.lastName").value("Name"))
        .andExpect(jsonPath("$.enabled").value(false));

    verify(userService)
        .updateUserProfile(
            testUser.getId(),
            updateRequest.getEmail(),
            updateRequest.getFirstName(),
            updateRequest.getLastName());
    verify(userService).setUserEnabled(testUser.getId(), updateRequest.getEnabled());
  }

  @Test
  @DisplayName("Update user - No fields to update")
  @WithMockUser(roles = "ADMIN")
  void updateUser_NoFieldsToUpdate() throws Exception {
    // Arrange
    AdminUserUpdateRequestDto emptyRequest = new AdminUserUpdateRequestDto();

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/users/{id}", testUser.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
        .andExpect(status().isBadRequest());

    verify(userService, never()).updateUserProfile(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Update user - User not found")
  @WithMockUser(roles = "ADMIN")
  void updateUser_UserNotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(userService.updateUserProfile(any(), any(), any(), any()))
        .thenThrow(new ResourceNotFoundException("User", nonExistentId.toString()));

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/users/{id}", nonExistentId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());

    verify(userService).updateUserProfile(any(), any(), any(), any());
  }

  // ==================== SET USER STATUS TESTS ====================

  @Test
  @DisplayName("Set user status - Success")
  @WithMockUser(roles = "ADMIN")
  void setUserStatus_Success() throws Exception {
    // Arrange
    User updatedUser = new User();
    updatedUser.setId(testUser.getId());
    updatedUser.setUsername(testUser.getUsername());
    updatedUser.setEnabled(false);

    when(userService.setUserEnabled(testUser.getId(), false)).thenReturn(updatedUser);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/users/{id}/status", testUser.getId())
                .with(csrf())
                .param("enabled", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
        .andExpect(jsonPath("$.enabled").value(false));

    verify(userService).setUserEnabled(testUser.getId(), false);
  }

  // ==================== ADD ROLE TO USER TESTS ====================

  @Test
  @DisplayName("Add role to user - Success")
  @WithMockUser(roles = "ADMIN")
  void addRoleToUser_Success() throws Exception {
    // Arrange
    User userWithAdminRole = new User();
    userWithAdminRole.setId(testUser.getId());
    userWithAdminRole.setUsername(testUser.getUsername());
    userWithAdminRole.setRoles(Set.of(userRole, adminRole));

    when(userService.addRoleToUser(testUser.getId(), "ADMIN")).thenReturn(userWithAdminRole);

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/users/{id}/roles/{roleName}", testUser.getId(), "ADMIN").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
        .andExpect(jsonPath("$.roles", hasItems("USER", "ADMIN")));

    verify(userService).addRoleToUser(testUser.getId(), "ADMIN");
  }

  @Test
  @DisplayName("Add role to user - Role not found")
  @WithMockUser(roles = "ADMIN")
  void addRoleToUser_RoleNotFound() throws Exception {
    // Arrange
    when(userService.addRoleToUser(testUser.getId(), "NONEXISTENT"))
        .thenThrow(new ResourceNotFoundException("Role", "NONEXISTENT"));

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/users/{id}/roles/{roleName}", testUser.getId(), "NONEXISTENT")
                .with(csrf()))
        .andExpect(status().isNotFound());

    verify(userService).addRoleToUser(testUser.getId(), "NONEXISTENT");
  }

  // ==================== REMOVE ROLE FROM USER TESTS ====================

  @Test
  @DisplayName("Remove role from user - Success")
  @WithMockUser(roles = "ADMIN")
  void removeRoleFromUser_Success() throws Exception {
    // Arrange
    User userWithoutAdminRole = new User();
    userWithoutAdminRole.setId(adminUser.getId());
    userWithoutAdminRole.setUsername(adminUser.getUsername());
    userWithoutAdminRole.setRoles(Set.of(userRole));

    when(userService.removeRoleFromUser(adminUser.getId(), "ADMIN"))
        .thenReturn(userWithoutAdminRole);

    // Act & Assert
    mockMvc
        .perform(
            delete("/api/admin/users/{id}/roles/{roleName}", adminUser.getId(), "ADMIN")
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(adminUser.getId().toString()))
        .andExpect(jsonPath("$.roles", hasItem("USER")))
        .andExpect(jsonPath("$.roles", not(hasItem("ADMIN"))));

    verify(userService).removeRoleFromUser(adminUser.getId(), "ADMIN");
  }

  // ==================== RESET PASSWORD TESTS ====================

  @Test
  @DisplayName("Reset user password - Success")
  @WithMockUser(roles = "ADMIN")
  void resetUserPassword_Success() throws Exception {
    // Arrange
    when(userService.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/users/{id}/reset-password", testUser.getId())
                .with(csrf())
                .param("newPassword", "newpassword123"))
        .andExpect(status().isOk());

    verify(userService).findById(testUser.getId());
  }

  @Test
  @DisplayName("Reset user password - User not found")
  @WithMockUser(roles = "ADMIN")
  void resetUserPassword_UserNotFound() throws Exception {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    when(userService.findById(nonExistentId)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc
        .perform(
            put("/api/admin/users/{id}/reset-password", nonExistentId)
                .with(csrf())
                .param("newPassword", "newpassword123"))
        .andExpect(status().isNotFound());

    verify(userService).findById(nonExistentId);
  }

  // ==================== GET USER STATISTICS TESTS ====================

  @Test
  @DisplayName("Get user statistics - Success")
  @WithMockUser(roles = "ADMIN")
  void getUserStatistics_Success() throws Exception {
    // Arrange
    when(userService.getTotalUserCount()).thenReturn(100L);
    when(userService.getEnabledUserCount()).thenReturn(95L);
    when(userService.findUsersByRole("ADMIN")).thenReturn(Arrays.asList(adminUser));
    when(userService.findUsersByRole("USER")).thenReturn(Arrays.asList(testUser, adminUser));

    // Act & Assert
    mockMvc
        .perform(get("/api/admin/users/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsers").value(100))
        .andExpect(jsonPath("$.enabledUsers").value(95))
        .andExpect(jsonPath("$.disabledUsers").value(5))
        .andExpect(jsonPath("$.adminUsers").value(1))
        .andExpect(jsonPath("$.regularUsers").value(2));

    verify(userService).getTotalUserCount();
    verify(userService).getEnabledUserCount();
    verify(userService).findUsersByRole("ADMIN");
    verify(userService).findUsersByRole("USER");
  }
}
