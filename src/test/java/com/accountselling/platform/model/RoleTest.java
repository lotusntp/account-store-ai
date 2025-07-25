package com.accountselling.platform.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Role entity. Tests role creation, validation, and user relationship management.
 */
@DisplayName("Role Entity Tests")
class RoleTest {

  private Role role;
  private User user;

  @BeforeEach
  void setUp() {
    role = new Role();
    user = new User();
  }

  @Test
  @DisplayName("Should create role with default constructor")
  void shouldCreateRoleWithDefaultConstructor() {
    assertNotNull(role);
    assertNull(role.getId());
    assertNull(role.getName());
    assertNull(role.getDescription());
    assertNotNull(role.getUsers());
    assertTrue(role.getUsers().isEmpty());
  }

  @Test
  @DisplayName("Should create role with name constructor")
  void shouldCreateRoleWithNameConstructor() {
    Role roleWithName = new Role("USER");

    assertNotNull(roleWithName);
    assertEquals("USER", roleWithName.getName());
    assertNull(roleWithName.getDescription());
    assertNotNull(roleWithName.getUsers());
    assertTrue(roleWithName.getUsers().isEmpty());
  }

  @Test
  @DisplayName("Should create role with name and description constructor")
  void shouldCreateRoleWithNameAndDescriptionConstructor() {
    Role roleWithDetails = new Role("ADMIN", "Administrator role");

    assertNotNull(roleWithDetails);
    assertEquals("ADMIN", roleWithDetails.getName());
    assertEquals("Administrator role", roleWithDetails.getDescription());
    assertNotNull(roleWithDetails.getUsers());
    assertTrue(roleWithDetails.getUsers().isEmpty());
  }

  @Test
  @DisplayName("Should set and get role properties")
  void shouldSetAndGetRoleProperties() {
    role.setId(1L);
    role.setName("USER");
    role.setDescription("Regular user role");

    assertEquals(1L, role.getId());
    assertEquals("USER", role.getName());
    assertEquals("Regular user role", role.getDescription());
  }

  @Test
  @DisplayName("Should add user to role")
  void shouldAddUserToRole() {
    user.setUsername("testuser");

    role.addUser(user);

    assertTrue(role.getUsers().contains(user));
    assertTrue(user.getRoles().contains(role));
    assertEquals(1, role.getUsers().size());
    assertEquals(1, user.getRoles().size());
  }

  @Test
  @DisplayName("Should remove user from role")
  void shouldRemoveUserFromRole() {
    user.setUsername("testuser");
    role.addUser(user);

    role.removeUser(user);

    assertFalse(role.getUsers().contains(user));
    assertFalse(user.getRoles().contains(role));
    assertEquals(0, role.getUsers().size());
    assertEquals(0, user.getRoles().size());
  }

  @Test
  @DisplayName("Should handle multiple users")
  void shouldHandleMultipleUsers() {
    User user1 = new User("user1", "password1");
    User user2 = new User("user2", "password2");

    role.addUser(user1);
    role.addUser(user2);

    assertEquals(2, role.getUsers().size());
    assertTrue(role.getUsers().contains(user1));
    assertTrue(role.getUsers().contains(user2));
  }

  @Test
  @DisplayName("Should implement equals correctly")
  void shouldImplementEqualsCorrectly() {
    Role role1 = new Role("USER");
    Role role2 = new Role("USER");
    Role role3 = new Role("ADMIN");

    assertEquals(role1, role2);
    assertNotEquals(role1, role3);
    assertNotEquals(role1, null);
    assertNotEquals(role1, "not a role");
  }

  @Test
  @DisplayName("Should implement hashCode correctly")
  void shouldImplementHashCodeCorrectly() {
    Role role1 = new Role("USER");
    Role role2 = new Role("USER");

    assertEquals(role1.hashCode(), role2.hashCode());
  }

  @Test
  @DisplayName("Should implement toString correctly")
  void shouldImplementToStringCorrectly() {
    role.setId(1L);
    role.setName("USER");
    role.setDescription("Regular user");

    String toString = role.toString();

    // Lombok generates toString in format: ClassName(field1=value1, field2=value2)
    assertTrue(toString.contains("Role("));
    assertTrue(toString.contains("id=1"));
    assertTrue(toString.contains("name=USER"));
    assertTrue(toString.contains("description=Regular user"));
  }
}
