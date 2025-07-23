package com.accountselling.platform.repository;

import com.accountselling.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Repository interface for User entity operations.
 * Provides data access methods for user management functionality.
 * 
 * รีพอสิทอรี่สำหรับจัดการข้อมูลผู้ใช้
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username.
     * Used for authentication and user lookup.
     * 
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     * Used for email-based operations and validation.
     * 
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists.
     * Used for registration validation.
     * 
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists.
     * Used for registration validation.
     * 
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find users by enabled status.
     * Used for admin user management.
     * 
     * @param enabled the enabled status to filter by
     * @return list of users with the specified enabled status
     */
    List<User> findByEnabled(Boolean enabled);

    /**
     * Find users by role name.
     * Used for admin operations and role-based queries.
     * 
     * @param roleName the role name to filter by
     * @return list of users with the specified role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Find users with orders.
     * Used for customer analytics and reporting.
     * 
     * @return list of users who have placed orders
     */
    @Query("SELECT DISTINCT u FROM User u WHERE SIZE(u.orders) > 0")
    List<User> findUsersWithOrders();

    /**
     * Count users by enabled status.
     * Used for dashboard statistics.
     * 
     * @param enabled the enabled status to count
     * @return count of users with the specified enabled status
     */
    long countByEnabled(Boolean enabled);

    /**
     * Find users by username containing (case-insensitive).
     * Used for admin user search functionality.
     * 
     * @param username the username pattern to search for
     * @return list of users with usernames containing the pattern
     */
    List<User> findByUsernameContainingIgnoreCase(String username);

    /**
     * Find users by email containing (case-insensitive).
     * Used for admin user search functionality.
     * 
     * @param email the email pattern to search for
     * @return list of users with emails containing the pattern
     */
    List<User> findByEmailContainingIgnoreCase(String email);
}