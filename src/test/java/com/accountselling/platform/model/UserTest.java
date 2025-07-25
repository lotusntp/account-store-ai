package com.accountselling.platform.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for User entity. Tests user creation, validation, role management, and business logic.
 */
@DisplayName("User Entity Tests")
class UserTest {

  private User user;
  private Role userRole;
  private Role adminRole;

  @BeforeEach
  void setUp() {
    user = new User();
    userRole = new Role("USER", "Regular user role");
    adminRole = new Role("ADMIN", "Administrator role");
  }

  @Test
  @DisplayName("Should create user with default constructor")
  void shouldCreateUserWithDefaultConstructor() {
    assertNotNull(user);
    assertNull(user.getId());
    assertNull(user.getUsername());
    assertNull(user.getPassword());
    assertNull(user.getEmail());
    assertTrue(user.getEnabled());
    assertTrue(user.getAccountNonExpired());
    assertTrue(user.getAccountNonLocked());
    assertTrue(user.getCredentialsNonExpired());
    assertNotNull(user.getRoles());
    assertTrue(user.getRoles().isEmpty());
  }

  @Test
  @DisplayName("Should create user with username and password constructor")
  void shouldCreateUserWithUsernameAndPasswordConstructor() {
    User userWithCredentials = new User("testuser", "hashedpassword");

    assertNotNull(userWithCredentials);
    assertEquals("testuser", userWithCredentials.getUsername());
    assertEquals("hashedpassword", userWithCredentials.getPassword());
    assertNull(userWithCredentials.getEmail());
    assertTrue(userWithCredentials.getEnabled());
  }

  @Test
  @DisplayName("Should create user with username, password, and email constructor")
  void shouldCreateUserWithFullConstructor() {
    User userWithEmail = new User("testuser", "hashedpassword", "test@example.com");

    assertNotNull(userWithEmail);
    assertEquals("testuser", userWithEmail.getUsername());
    assertEquals("hashedpassword", userWithEmail.getPassword());
    assertEquals("test@example.com", userWithEmail.getEmail());
    assertTrue(userWithEmail.getEnabled());
  }

  @Test
  @DisplayName("Should set and get user properties")
  void shouldSetAndGetUserProperties() {
    UUID id = UUID.randomUUID();
    user.setId(id);
    user.setUsername("testuser");
    user.setPassword("hashedpassword");
    user.setEmail("test@example.com");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEnabled(false);
    user.setAccountNonExpired(false);
    user.setAccountNonLocked(false);
    user.setCredentialsNonExpired(false);

    assertEquals(id, user.getId());
    assertEquals("testuser", user.getUsername());
    assertEquals("hashedpassword", user.getPassword());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("John", user.getFirstName());
    assertEquals("Doe", user.getLastName());
    assertFalse(user.getEnabled());
    assertFalse(user.getAccountNonExpired());
    assertFalse(user.getAccountNonLocked());
    assertFalse(user.getCredentialsNonExpired());
  }

  @Test
  @DisplayName("Should add role to user")
  void shouldAddRoleToUser() {
    user.addRole(userRole);

    assertTrue(user.getRoles().contains(userRole));
    assertTrue(userRole.getUsers().contains(user));
    assertEquals(1, user.getRoles().size());
    assertEquals(1, userRole.getUsers().size());
  }

  @Test
  @DisplayName("Should remove role from user")
  void shouldRemoveRoleFromUser() {
    user.addRole(userRole);

    user.removeRole(userRole);

    assertFalse(user.getRoles().contains(userRole));
    assertFalse(userRole.getUsers().contains(user));
    assertEquals(0, user.getRoles().size());
    assertEquals(0, userRole.getUsers().size());
  }

  @Test
  @DisplayName("Should handle multiple roles")
  void shouldHandleMultipleRoles() {
    user.addRole(userRole);
    user.addRole(adminRole);

    assertEquals(2, user.getRoles().size());
    assertTrue(user.getRoles().contains(userRole));
    assertTrue(user.getRoles().contains(adminRole));
  }

  @Test
  @DisplayName("Should check if user has specific role")
  void shouldCheckIfUserHasSpecificRole() {
    user.addRole(userRole);

    assertTrue(user.hasRole("USER"));
    assertFalse(user.hasRole("ADMIN"));
    assertFalse(user.hasRole("NONEXISTENT"));
  }

  @Test
  @DisplayName("Should check if user has any of specified roles")
  void shouldCheckIfUserHasAnyOfSpecifiedRoles() {
    user.addRole(userRole);

    assertTrue(user.hasAnyRole("USER", "ADMIN"));
    assertTrue(user.hasAnyRole("ADMIN", "USER"));
    assertFalse(user.hasAnyRole("ADMIN", "MODERATOR"));
    assertTrue(user.hasAnyRole("USER"));
    assertFalse(user.hasAnyRole("ADMIN"));
  }

  @Test
  @DisplayName("Should return correct full name")
  void shouldReturnCorrectFullName() {
    user.setUsername("testuser");

    // Test with both first and last name
    user.setFirstName("John");
    user.setLastName("Doe");
    assertEquals("John Doe", user.getFullName());

    // Test with only first name
    user.setLastName(null);
    assertEquals("John", user.getFullName());

    // Test with only last name
    user.setFirstName(null);
    user.setLastName("Doe");
    assertEquals("Doe", user.getFullName());

    // Test with no names
    user.setFirstName(null);
    user.setLastName(null);
    assertEquals("testuser", user.getFullName());
  }

  @Test
  @DisplayName("Should return correct account status methods")
  void shouldReturnCorrectAccountStatusMethods() {
    // Test default values
    assertTrue(user.isEnabled());
    assertTrue(user.isAccountNonExpired());
    assertTrue(user.isAccountNonLocked());
    assertTrue(user.isCredentialsNonExpired());

    // Test modified values
    user.setEnabled(false);
    user.setAccountNonExpired(false);
    user.setAccountNonLocked(false);
    user.setCredentialsNonExpired(false);

    assertFalse(user.isEnabled());
    assertFalse(user.isAccountNonExpired());
    assertFalse(user.isAccountNonLocked());
    assertFalse(user.isCredentialsNonExpired());
  }

  @Test
  @DisplayName("Should implement equals correctly")
  void shouldImplementEqualsCorrectly() {
    User user1 = new User("testuser", "password");
    User user2 = new User("testuser", "differentpassword");
    User user3 = new User("differentuser", "password");

    assertEquals(user1, user2); // Same username
    assertNotEquals(user1, user3); // Different username
    assertNotEquals(user1, null);
    assertNotEquals(user1, "not a user");
  }

  @Test
  @DisplayName("Should implement hashCode correctly")
  void shouldImplementHashCodeCorrectly() {
    User user1 = new User("testuser", "password");
    User user2 = new User("testuser", "differentpassword");

    assertEquals(user1.hashCode(), user2.hashCode());
  }

  @Test
  @DisplayName("Should implement toString correctly")
  void shouldImplementToStringCorrectly() {
    UUID id = UUID.randomUUID();
    user.setId(id);
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.addRole(userRole);

    String toString = user.toString();

    // Lombok generates toString in format: ClassName(field1=value1, field2=value2)
    // Note: User extends BaseEntity so it includes callSuper=true
    assertTrue(toString.contains("User("));
    assertTrue(toString.contains("id=" + id));
    assertTrue(toString.contains("username=testuser"));
    assertTrue(toString.contains("email=test@example.com"));
    assertTrue(toString.contains("firstName=John"));
    assertTrue(toString.contains("lastName=Doe"));
    assertTrue(toString.contains("enabled=true"));
  }

  @Test
  @DisplayName("Should handle null values gracefully")
  void shouldHandleNullValuesGracefully() {
    user.setUsername("testuser");
    user.setEmail(null);
    user.setFirstName(null);
    user.setLastName(null);

    assertNull(user.getEmail());
    assertNull(user.getFirstName());
    assertNull(user.getLastName());
    assertEquals("testuser", user.getFullName());

    assertFalse(user.hasRole(null));
    assertFalse(user.hasAnyRole((String[]) null));
    assertFalse(user.hasAnyRole());
  }
}
