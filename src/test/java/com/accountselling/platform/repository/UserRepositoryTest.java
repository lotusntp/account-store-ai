package com.accountselling.platform.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountselling.platform.model.Role;
import com.accountselling.platform.model.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** Unit tests for UserRepository. Tests repository methods for user management functionality. */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private UserRepository userRepository;

  @Autowired private RoleRepository roleRepository;

  private User testUser;
  private Role userRole;
  private Role adminRole;

  private final String password = "$2a$10$12345678901234567890123456789012345678901234567890122";

  @BeforeEach
  void setUp() {
    // Create test roles
    userRole = new Role("USER", "Regular user role");
    adminRole = new Role("ADMIN", "Administrator role");
    entityManager.persistAndFlush(userRole);
    entityManager.persistAndFlush(adminRole);

    // Create test user
    testUser = new User("testuser", password, "test@example.com");
    testUser.setFirstName("Test");
    testUser.setLastName("User");
    testUser.setEnabled(true);
    testUser.addRole(userRole);
  }

  @Test
  void findByUsername_WhenUserExists_ShouldReturnUser() {
    // Given
    entityManager.persistAndFlush(testUser);

    // When
    Optional<User> result = userRepository.findByUsername("testuser");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getUsername()).isEqualTo("testuser");
    assertThat(result.get().getEmail()).isEqualTo("test@example.com");
  }

  @Test
  void findByUsername_WhenUserDoesNotExist_ShouldReturnEmpty() {
    // When
    Optional<User> result = userRepository.findByUsername("nonexistent");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void findByEmail_WhenUserExists_ShouldReturnUser() {
    // Given
    entityManager.persistAndFlush(testUser);

    // When
    Optional<User> result = userRepository.findByEmail("test@example.com");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    assertThat(result.get().getUsername()).isEqualTo("testuser");
  }

  @Test
  void findByEmail_WhenUserDoesNotExist_ShouldReturnEmpty() {
    // When
    Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void existsByUsername_WhenUserExists_ShouldReturnTrue() {
    // Given
    entityManager.persistAndFlush(testUser);

    // When
    boolean exists = userRepository.existsByUsername("testuser");

    // Then
    assertThat(exists).isTrue();
  }

  @Test
  void existsByUsername_WhenUserDoesNotExist_ShouldReturnFalse() {
    // When
    boolean exists = userRepository.existsByUsername("nonexistent");

    // Then
    assertThat(exists).isFalse();
  }

  @Test
  void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
    // Given
    entityManager.persistAndFlush(testUser);

    // When
    boolean exists = userRepository.existsByEmail("test@example.com");

    // Then
    assertThat(exists).isTrue();
  }

  @Test
  void existsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
    // When
    boolean exists = userRepository.existsByEmail("nonexistent@example.com");

    // Then
    assertThat(exists).isFalse();
  }

  @Test
  void findByEnabled_ShouldReturnUsersWithSpecifiedStatus() {
    // Given
    User enabledUser = new User("enabled", password);
    enabledUser.setEnabled(true);

    User disabledUser = new User("disabled", password);
    disabledUser.setEnabled(false);

    entityManager.persistAndFlush(enabledUser);
    entityManager.persistAndFlush(disabledUser);

    // When
    List<User> enabledUsers = userRepository.findByEnabled(true);
    List<User> disabledUsers = userRepository.findByEnabled(false);

    // Then
    assertThat(enabledUsers).hasSize(1);
    assertThat(enabledUsers.get(0).getUsername()).isEqualTo("enabled");

    assertThat(disabledUsers).hasSize(1);
    assertThat(disabledUsers.get(0).getUsername()).isEqualTo("disabled");
  }

  @Test
  void findByRoleName_ShouldReturnUsersWithSpecifiedRole() {
    // Given
    User regularUser = new User("regular", password);
    regularUser.addRole(userRole);

    User adminUser = new User("admin", password);
    adminUser.addRole(adminRole);

    entityManager.persistAndFlush(regularUser);
    entityManager.persistAndFlush(adminUser);

    // When
    List<User> usersWithUserRole = userRepository.findByRoleName("USER");
    List<User> usersWithAdminRole = userRepository.findByRoleName("ADMIN");

    // Then
    assertThat(usersWithUserRole).hasSize(1);
    assertThat(usersWithUserRole.get(0).getUsername()).isEqualTo("regular");

    assertThat(usersWithAdminRole).hasSize(1);
    assertThat(usersWithAdminRole.get(0).getUsername()).isEqualTo("admin");
  }

  @Test
  void findUsersWithOrders_WhenNoUsersHaveOrders_ShouldReturnEmptyList() {
    // Given
    entityManager.persistAndFlush(testUser);

    // When
    List<User> usersWithOrders = userRepository.findUsersWithOrders();

    // Then
    assertThat(usersWithOrders).isEmpty();
  }

  @Test
  void countByEnabled_ShouldReturnCorrectCount() {
    // Given
    User enabledUser1 = new User("enabled1", password);
    enabledUser1.setEnabled(true);

    User enabledUser2 = new User("enabled2", password);
    enabledUser2.setEnabled(true);

    User disabledUser = new User("disabled", password);
    disabledUser.setEnabled(false);

    entityManager.persistAndFlush(enabledUser1);
    entityManager.persistAndFlush(enabledUser2);
    entityManager.persistAndFlush(disabledUser);

    // When
    long enabledCount = userRepository.countByEnabled(true);
    long disabledCount = userRepository.countByEnabled(false);

    // Then
    assertThat(enabledCount).isEqualTo(2);
    assertThat(disabledCount).isEqualTo(1);
  }

  @Test
  void findByUsernameContainingIgnoreCase_ShouldReturnMatchingUsers() {
    // Given
    User user1 = new User("TestUser1", password);
    User user2 = new User("AnotherTestUser", password);
    User user3 = new User("DifferentUser", password);

    entityManager.persistAndFlush(user1);
    entityManager.persistAndFlush(user2);
    entityManager.persistAndFlush(user3);

    // When
    List<User> results = userRepository.findByUsernameContainingIgnoreCase("test");

    // Then
    assertThat(results).hasSize(2);
    assertThat(results)
        .extracting(User::getUsername)
        .containsExactlyInAnyOrder("TestUser1", "AnotherTestUser");
  }

  @Test
  void findByEmailContainingIgnoreCase_ShouldReturnMatchingUsers() {
    // Given
    User user1 = new User("user1", password, "test1@EXAMPLE.com");
    User user2 = new User("user2", password, "test2@example.COM");
    User user3 = new User("user3", password, "different@other.com");

    entityManager.persistAndFlush(user1);
    entityManager.persistAndFlush(user2);
    entityManager.persistAndFlush(user3);

    // When
    List<User> results = userRepository.findByEmailContainingIgnoreCase("example");

    // Then
    assertThat(results).hasSize(2);
    assertThat(results)
        .extracting(User::getEmail)
        .containsExactlyInAnyOrder("test1@EXAMPLE.com", "test2@example.COM");
  }

  @Test
  void save_ShouldPersistUserWithRoles() {
    // Given
    testUser.addRole(adminRole);

    // When
    User savedUser = entityManager.persistAndFlush(testUser);

    // Then
    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getUsername()).isEqualTo("testuser");
    assertThat(savedUser.getRoles()).hasSize(2);
    assertThat(savedUser.getRoles())
        .extracting(Role::getName)
        .containsExactlyInAnyOrder("USER", "ADMIN");
  }

  @Test
  void delete_ShouldRemoveUserButKeepRoles() {
    // Given
    User savedUser = entityManager.persistAndFlush(testUser);
    Long roleId = userRole.getId();

    // When
    userRepository.delete(savedUser);
    entityManager.flush();

    // Then
    assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    assertThat(roleRepository.findById(roleId)).isPresent();
  }
}
