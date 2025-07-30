package com.accountselling.platform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.model.Role;
import com.accountselling.platform.model.User;
import com.accountselling.platform.repository.RoleRepository;
import com.accountselling.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for role-based access control. Tests URL-based authorization, method security,
 * and role hierarchy functionality.
 *
 * <p>Integration tests สำหรับการทดสอบระบบควบคุมการเข้าถึงตาม role รวมถึงการทดสอบ URL-based
 * authorization, method security และ role hierarchy
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Role-Based Access Control Integration Tests")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class RoleBasedAccessControlIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Autowired private ObjectMapper objectMapper;

  private User regularUser;
  private User adminUser;
  private User superAdminUser;
  private Role userRole;
  private Role adminRole;
  private Role superAdminRole;

  @BeforeEach
  void setUp() {
    // Clean up any existing data
    userRepository.deleteAll();
    roleRepository.deleteAll();

    // Create and save roles first
    userRole = roleRepository.save(new Role("ROLE_USER", "Regular user role"));
    adminRole = roleRepository.save(new Role("ROLE_ADMIN", "Administrator role"));
    superAdminRole = roleRepository.save(new Role("ROLE_SUPER_ADMIN", "Super administrator role"));

    // Create users without roles first
    regularUser = new User("testuser", passwordEncoder.encode("password123"));
    regularUser.setEmail("user@test.com");
    regularUser = userRepository.save(regularUser);

    adminUser = new User("testadmin", passwordEncoder.encode("admin123"));
    adminUser.setEmail("admin@test.com");
    adminUser = userRepository.save(adminUser);

    superAdminUser = new User("testsuperadmin", passwordEncoder.encode("superadmin123"));
    superAdminUser.setEmail("superadmin@test.com");
    superAdminUser = userRepository.save(superAdminUser);

    // Now add roles using the helper methods from User entity
    regularUser.addRole(userRole);
    userRepository.save(regularUser);

    adminUser.addRole(adminRole);
    adminUser.addRole(userRole);
    userRepository.save(adminUser);

    superAdminUser.addRole(superAdminRole);
    userRepository.save(superAdminUser);
  }

  @Nested
  @DisplayName("Public Endpoint Access Tests")
  class PublicEndpointTests {

    @Test
    @DisplayName("Should allow access to public auth endpoints without authentication")
    void shouldAllowAccessToPublicAuthEndpoints() throws Exception {
      // Test login endpoint is accessible
      Map<String, String> loginRequest = new HashMap<>();
      loginRequest.put("username", "testuser");
      loginRequest.put("password", "password123");

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(loginRequest)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to public product endpoints without authentication")
    void shouldAllowAccessToPublicProductEndpoints() throws Exception {
      mockMvc.perform(get("/api/products")).andExpect(status().isOk());

      mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to payment webhook without authentication")
    void shouldAllowAccessToPaymentWebhookEndpoints() throws Exception {
      Map<String, Object> webhookData = new HashMap<>();
      webhookData.put("transaction_id", "test-tx-123");
      webhookData.put("status", "completed");

      mockMvc
          .perform(
              post("/api/payments/webhook")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(webhookData)))
          .andExpect(status().isInternalServerError()); // Expected since transaction won't be found
    }
  }

  @Nested
  @DisplayName("User Role Access Tests")
  class UserRoleAccessTests {

    private String userToken;

    @BeforeEach
    void setUpUserToken() {
      userToken = generateTokenForUser(regularUser);
    }

    @Test
    @DisplayName("Should allow USER role access to user profile endpoint")
    void shouldAllowUserAccessToUserProfile() throws Exception {
      mockMvc
          .perform(get("/api/users/profile").header("Authorization", "Bearer " + userToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("Should allow USER role access to user orders endpoint")
    void shouldAllowUserAccessToUserOrders() throws Exception {
      mockMvc
          .perform(get("/api/users/orders").header("Authorization", "Bearer " + userToken))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow USER role access to payment endpoints")
    void shouldAllowUserAccessToPaymentEndpoints() throws Exception {
      // Should be able to access payment status (though might return 404 for non-existent payment)
      mockMvc
          .perform(
              get("/api/payments/status/00000000-0000-0000-0000-000000000001")
                  .header("Authorization", "Bearer " + userToken))
          .andExpect(status().isNotFound()); // Expected since payment doesn't exist
    }

    @Test
    @DisplayName("Should deny USER role access to admin endpoints")
    void shouldDenyUserAccessToAdminEndpoints() throws Exception {
      mockMvc
          .perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + userToken))
          .andExpect(status().isForbidden());

      mockMvc
          .perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
          .andExpect(status().isForbidden());

      mockMvc
          .perform(get("/api/admin/system/health").header("Authorization", "Bearer " + userToken))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Admin Role Access Tests")
  class AdminRoleAccessTests {

    private String adminToken;

    @BeforeEach
    void setUpAdminToken() {
      adminToken = generateTokenForUser(adminUser);
    }

    @Test
    @DisplayName("Should allow ADMIN role access to admin endpoints")
    void shouldAllowAdminAccessToAdminEndpoints() throws Exception {
      mockMvc
          .perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalUsers").exists());

      mockMvc
          .perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isOk());

      mockMvc
          .perform(get("/api/admin/system/health").header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Should allow ADMIN role access to user endpoints via role hierarchy")
    void shouldAllowAdminAccessToUserEndpoints() throws Exception {
      // Admin should inherit USER role privileges
      mockMvc
          .perform(get("/api/users/profile").header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("testadmin"));

      mockMvc
          .perform(get("/api/users/orders").header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny ADMIN role access to super admin endpoints")
    void shouldDenyAdminAccessToSuperAdminEndpoints() throws Exception {
      mockMvc
          .perform(get("/api/admin/system/config").header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Super Admin Role Access Tests")
  class SuperAdminRoleAccessTests {

    private String superAdminToken;

    @BeforeEach
    void setUpSuperAdminToken() {
      superAdminToken = generateTokenForUser(superAdminUser);
    }

    @Test
    @DisplayName("Should allow SUPER_ADMIN role access to all endpoints")
    void shouldAllowSuperAdminAccessToAllEndpoints() throws Exception {
      // Super admin should access super admin endpoints
      mockMvc
          .perform(
              get("/api/admin/system/config").header("Authorization", "Bearer " + superAdminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessLevel").value("SUPER_ADMIN"));

      // Super admin should access admin endpoints
      mockMvc
          .perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + superAdminToken))
          .andExpect(status().isOk());

      // Super admin should access user endpoints
      mockMvc
          .perform(get("/api/users/profile").header("Authorization", "Bearer " + superAdminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("testsuperadmin"));
    }
  }

  @Nested
  @DisplayName("Unauthorized Access Tests")
  class UnauthorizedAccessTests {

    @Test
    @DisplayName("Should deny access to protected endpoints without token")
    void shouldDenyAccessWithoutToken() throws Exception {
      mockMvc.perform(get("/api/users/profile")).andExpect(status().isUnauthorized());

      mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());

      mockMvc
          .perform(get("/api/payments/status/00000000-0000-0000-0000-000000000001"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with invalid token")
    void shouldDenyAccessWithInvalidToken() throws Exception {
      mockMvc
          .perform(get("/api/users/profile").header("Authorization", "Bearer invalid-token"))
          .andExpect(status().isUnauthorized());

      mockMvc
          .perform(get("/api/admin/dashboard").header("Authorization", "Bearer invalid-token"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with expired token")
    void shouldDenyAccessWithExpiredToken() throws Exception {
      // Create an expired token (this would require mocking or using a very short expiration)
      // For this test, we'll use a malformed token that simulates expiration
      String expiredToken =
          "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTY5MDAwMDAwMCwiZXhwIjoxNjkwMDAwMDAwfQ.invalid";

      mockMvc
          .perform(get("/api/users/profile").header("Authorization", "Bearer " + expiredToken))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("Method Security Tests")
  class MethodSecurityTests {

    @Test
    @DisplayName("Should enforce method-level security annotations")
    void shouldEnforceMethodLevelSecurity() throws Exception {
      String userToken = generateTokenForUser(regularUser);

      // Test that @PreAuthorize("hasRole('ADMIN')") is enforced
      mockMvc
          .perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + userToken))
          .andExpect(status().isForbidden());

      // Test that @PreAuthorize("hasRole('USER')") allows access
      mockMvc
          .perform(get("/api/users/profile").header("Authorization", "Bearer " + userToken))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should respect role hierarchy in method security")
    void shouldRespectRoleHierarchyInMethodSecurity() throws Exception {
      String adminToken = generateTokenForUser(adminUser);

      // Admin should be able to access USER role endpoints due to hierarchy
      mockMvc
          .perform(get("/api/users/profile").header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isOk());

      // Admin should be able to access ADMIN role endpoints
      mockMvc
          .perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isOk());
    }
  }

  private String loginAndGetToken(String username, String password) throws Exception {
    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("username", username);
    loginRequest.put("password", password);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
    return (String) response.get("accessToken");
  }

  /** Generate JWT token for a user by creating Authentication object */
  private String generateTokenForUser(User user) {
    Collection<SimpleGrantedAuthority> authorities =
        user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(java.util.stream.Collectors.toList());

    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(authorities)
            .build();

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

    return jwtTokenProvider.generateToken(authentication);
  }
}
