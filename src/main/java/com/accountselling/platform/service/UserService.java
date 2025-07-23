package com.accountselling.platform.service;

import com.accountselling.platform.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for user management operations.
 * Extends UserDetailsService for Spring Security integration.
 * 
 * บริการสำหรับการจัดการผู้ใช้และการทำงานร่วมกับ Spring Security
 */
public interface UserService extends UserDetailsService {

    /**
     * Register a new user account.
     * 
     * @param username the username for the new account
     * @param password the plain text password (will be encoded)
     * @param email the email address
     * @return the created user
     * @throws IllegalArgumentException if username or email already exists
     */
    User registerUser(String username, String password, String email);

    /**
     * Register a new user with additional profile information.
     * 
     * @param username the username for the new account
     * @param password the plain text password (will be encoded)
     * @param email the email address
     * @param firstName the first name
     * @param lastName the last name
     * @return the created user
     * @throws IllegalArgumentException if username or email already exists
     */
    User registerUser(String username, String password, String email, String firstName, String lastName);

    /**
     * Authenticate user login credentials.
     * 
     * @param username the username
     * @param password the plain text password
     * @return the authenticated user if credentials are valid
     * @throws IllegalArgumentException if credentials are invalid
     */
    User authenticateUser(String username, String password);

    /**
     * Find user by username.
     * 
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     * 
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by ID.
     * 
     * @param id the user ID
     * @return Optional containing the user if found
     */
    Optional<User> findById(UUID id);

    /**
     * Check if username exists.
     * 
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists.
     * 
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Update user profile information.
     * 
     * @param userId the user ID
     * @param email the new email (optional)
     * @param firstName the new first name (optional)
     * @param lastName the new last name (optional)
     * @return the updated user
     * @throws IllegalArgumentException if user not found or email already exists
     */
    User updateUserProfile(UUID userId, String email, String firstName, String lastName);

    /**
     * Change user password.
     * 
     * @param userId the user ID
     * @param currentPassword the current password for verification
     * @param newPassword the new password
     * @throws IllegalArgumentException if user not found or current password is incorrect
     */
    void changePassword(UUID userId, String currentPassword, String newPassword);

    /**
     * Enable or disable user account.
     * 
     * @param userId the user ID
     * @param enabled the enabled status
     * @return the updated user
     * @throws IllegalArgumentException if user not found
     */
    User setUserEnabled(UUID userId, boolean enabled);

    /**
     * Add role to user.
     * 
     * @param userId the user ID
     * @param roleName the role name to add
     * @return the updated user
     * @throws IllegalArgumentException if user or role not found
     */
    User addRoleToUser(UUID userId, String roleName);

    /**
     * Remove role from user.
     * 
     * @param userId the user ID
     * @param roleName the role name to remove
     * @return the updated user
     * @throws IllegalArgumentException if user not found
     */
    User removeRoleFromUser(UUID userId, String roleName);

    /**
     * Find all users with pagination support.
     * 
     * @return list of all users
     */
    List<User> findAllUsers();

    /**
     * Find users by enabled status.
     * 
     * @param enabled the enabled status to filter by
     * @return list of users with the specified enabled status
     */
    List<User> findUsersByEnabled(boolean enabled);

    /**
     * Find users by role name.
     * 
     * @param roleName the role name to filter by
     * @return list of users with the specified role
     */
    List<User> findUsersByRole(String roleName);

    /**
     * Get total user count.
     * 
     * @return total number of users
     */
    long getTotalUserCount();

    /**
     * Get count of enabled users.
     * 
     * @return number of enabled users
     */
    long getEnabledUserCount();
}