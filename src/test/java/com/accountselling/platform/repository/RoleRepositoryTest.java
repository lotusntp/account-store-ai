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

/** Unit tests for RoleRepository. Tests repository methods for role management functionality. */
@DataJpaTest
@ActiveProfiles("test")
class RoleRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private RoleRepository roleRepository;

  @Autowired private UserRepository userRepository;

  private Role userRole;
  private Role adminRole;

  private final String password = "$2a$10$12345678901234567890123456789012345678901234567890122";

  @BeforeEach
  void setUp() {
    // Create test roles
    userRole = new Role("USER", "Regular user role");
    adminRole = new Role("ADMIN", "Administrator role");
  }

  @Test
  void findByName_WhenRoleExists_ShouldReturnRole() {
    // Given
    entityManager.persistAndFlush(userRole);

    // When
    Optional<Role> result = roleRepository.findByName("USER");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("USER");
    assertThat(result.get().getDescription()).isEqualTo("Regular user role");
  }

  @Test
  void findByName_WhenRoleDoesNotExist_ShouldReturnEmpty() {
    // When
    Optional<Role> result = roleRepository.findByName("NONEXISTENT");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void existsByName_WhenRoleExists_ShouldReturnTrue() {
    // Given
    entityManager.persistAndFlush(userRole);

    // When
    boolean exists = roleRepository.existsByName("USER");

    // Then
    assertThat(exists).isTrue();
  }

  @Test
  void existsByName_WhenRoleDoesNotExist_ShouldReturnFalse() {
    // When
    boolean exists = roleRepository.existsByName("NONEXISTENT");

    // Then
    assertThat(exists).isFalse();
  }

  @Test
  void findByNameContainingIgnoreCase_ShouldReturnMatchingRoles() {
    // Given
    Role moderatorRole = new Role("MODERATOR", "Moderator role");
    Role superAdminRole = new Role("SUPER_ADMIN", "Super administrator role");

    entityManager.persistAndFlush(userRole);
    entityManager.persistAndFlush(adminRole);
    entityManager.persistAndFlush(moderatorRole);
    entityManager.persistAndFlush(superAdminRole);

    // When
    List<Role> results = roleRepository.findByNameContainingIgnoreCase("admin");

    // Then
    assertThat(results).hasSize(2);
    assertThat(results).extracting(Role::getName).containsExactlyInAnyOrder("ADMIN", "SUPER_ADMIN");
  }

  @Test
  void findRolesWithUsers_WhenSomeRolesHaveUsers_ShouldReturnOnlyRolesWithUsers() {
    // Given
    entityManager.persistAndFlush(userRole);
    entityManager.persistAndFlush(adminRole);

    User testUser = new User("testuser", password);
    testUser.addRole(userRole);
    entityManager.persistAndFlush(testUser);

    // When
    List<Role> rolesWithUsers = roleRepository.findRolesWithUsers();

    // Then
    assertThat(rolesWithUsers).hasSize(1);
    assertThat(rolesWithUsers.get(0).getName()).isEqualTo("USER");
  }

  @Test
  void findRolesWithUsers_WhenNoRolesHaveUsers_ShouldReturnEmptyList() {
    // Given
    entityManager.persistAndFlush(userRole);
    entityManager.persistAndFlush(adminRole);

    // When
    List<Role> rolesWithUsers = roleRepository.findRolesWithUsers();

    // Then
    assertThat(rolesWithUsers).isEmpty();
  }

  @Test
  void findRolesWithoutUsers_WhenSomeRolesHaveNoUsers_ShouldReturnRolesWithoutUsers() {
    // Given
    entityManager.persistAndFlush(userRole);
    entityManager.persistAndFlush(adminRole);

    User testUser = new User("testuser", password);
    testUser.addRole(userRole);
    entityManager.persistAndFlush(testUser);

    // When
    List<Role> rolesWithoutUsers = roleRepository.findRolesWithoutUsers();

    // Then
    assertThat(rolesWithoutUsers).hasSize(1);
    assertThat(rolesWithoutUsers.get(0).getName()).isEqualTo("ADMIN");
  }

  @Test
  void findRolesWithoutUsers_WhenAllRolesHaveUsers_ShouldReturnEmptyList() {
    // Given
    entityManager.persistAndFlush(userRole);
    entityManager.persistAndFlush(adminRole);

    User user1 = new User("user1", password);
    user1.addRole(userRole);

    User user2 = new User("user2", password);
    user2.addRole(adminRole);

    entityManager.persistAndFlush(user1);
    entityManager.persistAndFlush(user2);

    // When
    List<Role> rolesWithoutUsers = roleRepository.findRolesWithoutUsers();

    // Then
    assertThat(rolesWithoutUsers).isEmpty();
  }

  @Test
  void countUsersByRoleName_ShouldReturnCorrectCount() {
    // Given
    entityManager.persistAndFlush(userRole);
    entityManager.persistAndFlush(adminRole);

    User user1 = new User("user1", password);
    user1.addRole(userRole);

    User user2 = new User("user2", password);
    user2.addRole(userRole);

    User admin = new User("admin", password);
    admin.addRole(adminRole);

    entityManager.persistAndFlush(user1);
    entityManager.persistAndFlush(user2);
    entityManager.persistAndFlush(admin);

    // When
    long userRoleCount = roleRepository.countUsersByRoleName("USER");
    long adminRoleCount = roleRepository.countUsersByRoleName("ADMIN");
    long nonExistentRoleCount = roleRepository.countUsersByRoleName("NONEXISTENT");

    // Then
    assertThat(userRoleCount).isEqualTo(2);
    assertThat(adminRoleCount).isEqualTo(1);
    assertThat(nonExistentRoleCount).isEqualTo(0);
  }

  @Test
  void findAllByOrderByNameAsc_ShouldReturnRolesOrderedByName() {
    // Given
    Role zRole = new Role("Z_ROLE", "Z role");
    Role aRole = new Role("A_ROLE", "A role");
    Role mRole = new Role("M_ROLE", "M role");

    entityManager.persistAndFlush(zRole);
    entityManager.persistAndFlush(aRole);
    entityManager.persistAndFlush(mRole);

    // When
    List<Role> orderedRoles = roleRepository.findAllByOrderByNameAsc();

    // Then
    assertThat(orderedRoles).hasSize(3);
    assertThat(orderedRoles)
        .extracting(Role::getName)
        .containsExactly("A_ROLE", "M_ROLE", "Z_ROLE");
  }

  @Test
  void save_ShouldPersistRole() {
    // When
    Role savedRole = entityManager.persistAndFlush(userRole);

    // Then
    assertThat(savedRole.getId()).isNotNull();
    assertThat(savedRole.getName()).isEqualTo("USER");
    assertThat(savedRole.getDescription()).isEqualTo("Regular user role");
  }

  @Test
  void save_ShouldPersistRoleWithUsers() {
    // Given
    Role savedRole = entityManager.persistAndFlush(userRole);

    User testUser = new User("testuser", password);
    testUser.addRole(savedRole);
    entityManager.persistAndFlush(testUser);

    // When
    Role roleWithUsers = roleRepository.findById(savedRole.getId()).orElseThrow();

    // Then
    assertThat(roleWithUsers.getUsers()).hasSize(1);
    assertThat(roleWithUsers.getUsers().iterator().next().getUsername()).isEqualTo("testuser");
  }

  @Test
  void delete_ShouldRemoveRoleButKeepUsers() {
    // Given
    Role savedRole = roleRepository.save(userRole);

    User testUser = new User("testuser", password);
    testUser.addRole(savedRole);
    User savedUser = userRepository.save(testUser);

    // Remove role from user
    savedUser.removeRole(savedRole);
    userRepository.save(savedUser);

    // Then delete role
    roleRepository.delete(savedRole);

    // Verify role gone
    assertThat(roleRepository.findById(savedRole.getId())).isEmpty();

    // Verify user still exists and has no roles
    User userAfterRoleDeletion = userRepository.findById(savedUser.getId()).orElseThrow();
    assertThat(userAfterRoleDeletion.getRoles()).isEmpty();
  }

  @Test
  void findAll_ShouldReturnAllRoles() {
    // Given
    roleRepository.saveAll(List.of(userRole, adminRole));

    // When
    List<Role> allRoles = roleRepository.findAll();

    // Then
    assertThat(allRoles).hasSize(2);
    assertThat(allRoles).extracting(Role::getName).containsExactlyInAnyOrder("USER", "ADMIN");
  }
}
