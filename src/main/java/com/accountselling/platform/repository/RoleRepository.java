package com.accountselling.platform.repository;

import com.accountselling.platform.model.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Role entity operations. Provides data access methods for role management
 * functionality.
 *
 * <p>รีพอสิทอรี่สำหรับจัดการข้อมูลบทบาทผู้ใช้
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  /**
   * Find role by name. Used for role lookup and assignment.
   *
   * @param name the role name to search for
   * @return Optional containing the role if found
   */
  Optional<Role> findByName(String name);

  /**
   * Check if role name exists. Used for role creation validation.
   *
   * @param name the role name to check
   * @return true if role name exists, false otherwise
   */
  boolean existsByName(String name);

  /**
   * Find roles by name containing (case-insensitive). Used for admin role search functionality.
   *
   * @param name the role name pattern to search for
   * @return list of roles with names containing the pattern
   */
  List<Role> findByNameContainingIgnoreCase(String name);

  /**
   * Find roles assigned to users. Used for role management and cleanup operations.
   *
   * @return list of roles that are assigned to at least one user
   */
  @Query("SELECT DISTINCT r FROM Role r WHERE SIZE(r.users) > 0")
  List<Role> findRolesWithUsers();

  /**
   * Find roles not assigned to any users. Used for role cleanup and management.
   *
   * @return list of roles that are not assigned to any user
   */
  @Query("SELECT r FROM Role r WHERE SIZE(r.users) = 0")
  List<Role> findRolesWithoutUsers();

  /**
   * Count users for a specific role. Used for role statistics and reporting.
   *
   * @param roleName the role name to count users for
   * @return count of users with the specified role
   */
  @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
  long countUsersByRoleName(@Param("roleName") String roleName);

  /**
   * Find all roles ordered by name. Used for consistent role listing in admin interfaces.
   *
   * @return list of all roles ordered by name
   */
  List<Role> findAllByOrderByNameAsc();
}
