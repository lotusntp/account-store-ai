package com.accountselling.platform.service.impl;

import com.accountselling.platform.exception.InvalidCredentialsException;
import com.accountselling.platform.exception.ResourceAlreadyExistsException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Role;
import com.accountselling.platform.model.User;
import com.accountselling.platform.repository.RoleRepository;
import com.accountselling.platform.repository.UserRepository;
import com.accountselling.platform.service.UserService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Implementation of UserService for user management operations. Handles user registration,
 * authentication, and profile management with BCrypt password encoding.
 *
 * <p>การใช้งาน UserService สำหรับการจัดการผู้ใช้ รวมถึงการลงทะเบียน การยืนยันตัวตน
 * และการจัดการโปรไฟล์
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  private static final String DEFAULT_USER_ROLE = "USER";

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("Loading user by username: {}", username);

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    return buildUserDetails(user);
  }

  @Override
  @Transactional
  public User registerUser(String username, String password) {
    return registerUser(username, password, null, null, null);
  }

  @Override
  @Transactional
  public User registerUser(String username, String password, String email) {
    return registerUser(username, password, email, null, null);
  }

  @Override
  @Transactional
  public User registerUser(
      String username, String password, String email, String firstName, String lastName) {
    log.info("Attempting to register new user with username: {}", username);

    validateRegistrationData(username, password, email);

    if (userRepository.existsByUsername(username)) {
      throw new ResourceAlreadyExistsException("Username already exists: " + username);
    }

    if (StringUtils.hasText(email) && userRepository.existsByEmail(email)) {
      throw new ResourceAlreadyExistsException("Email already exists: " + email);
    }

    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setEmail(email);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEnabled(true);
    user.setAccountNonExpired(true);
    user.setAccountNonLocked(true);
    user.setCredentialsNonExpired(true);

    Role userRole =
        roleRepository
            .findByName(DEFAULT_USER_ROLE)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Default role not found: " + DEFAULT_USER_ROLE));

    user.addRole(userRole);

    User savedUser = userRepository.save(user);
    log.info("Successfully registered new user with ID: {}", savedUser.getId());

    return savedUser;
  }

  @Override
  @Transactional(readOnly = true)
  public User authenticateUser(String username, String password) {
    log.debug("Authenticating user: {}", username);

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

    if (!passwordEncoder.matches(password, user.getPassword())) {
      log.warn("Invalid password attempt for user: {}", username);
      throw new InvalidCredentialsException("Invalid username or password");
    }

    if (!user.isEnabled()) {
      throw new InvalidCredentialsException("User account is disabled");
    }

    if (!user.isAccountNonLocked()) {
      throw new InvalidCredentialsException("User account is locked");
    }

    if (!user.isAccountNonExpired()) {
      throw new InvalidCredentialsException("User account is expired");
    }

    if (!user.isCredentialsNonExpired()) {
      throw new InvalidCredentialsException("User credentials are expired");
    }

    log.debug("User authentication successful: {}", username);
    return user;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findById(UUID id) {
    return userRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  @Override
  @Transactional
  public User updateUserProfile(UUID userId, String email, String firstName, String lastName) {
    log.debug("Updating profile for user ID: {}", userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

    if (StringUtils.hasText(email) && !email.equals(user.getEmail())) {
      if (userRepository.existsByEmail(email)) {
        throw new ResourceAlreadyExistsException("Email already exists: " + email);
      }
      user.setEmail(email);
    }

    if (StringUtils.hasText(firstName)) {
      user.setFirstName(firstName);
    }

    if (StringUtils.hasText(lastName)) {
      user.setLastName(lastName);
    }

    User updatedUser = userRepository.save(user);
    log.info("Successfully updated profile for user ID: {}", userId);

    return updatedUser;
  }

  @Override
  @Transactional
  public void changePassword(UUID userId, String currentPassword, String newPassword) {
    log.debug("Changing password for user ID: {}", userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new InvalidCredentialsException("Current password is incorrect");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    log.info("Successfully changed password for user ID: {}", userId);
  }

  @Override
  @Transactional
  public User setUserEnabled(UUID userId, boolean enabled) {
    log.debug("Setting enabled status to {} for user ID: {}", enabled, userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

    user.setEnabled(enabled);
    User updatedUser = userRepository.save(user);

    log.info("Successfully set enabled status to {} for user ID: {}", enabled, userId);
    return updatedUser;
  }

  @Override
  @Transactional
  public User addRoleToUser(UUID userId, String roleName) {
    log.debug("Adding role {} to user ID: {}", roleName, userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

    user.addRole(role);
    User updatedUser = userRepository.save(user);

    log.info("Successfully added role {} to user ID: {}", roleName, userId);
    return updatedUser;
  }

  @Override
  @Transactional
  public User removeRoleFromUser(UUID userId, String roleName) {
    log.debug("Removing role {} from user ID: {}", roleName, userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

    Role role = roleRepository.findByName(roleName).orElse(null);

    if (role != null) {
      user.removeRole(role);
      userRepository.save(user);
      log.info("Successfully removed role {} from user ID: {}", roleName, userId);
    } else {
      log.warn("Role {} not found when trying to remove from user ID: {}", roleName, userId);
    }

    return user;
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findAllUsers() {
    return userRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findUsersByEnabled(boolean enabled) {
    return userRepository.findByEnabled(enabled);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findUsersByRole(String roleName) {
    return userRepository.findByRoleName(roleName);
  }

  @Override
  @Transactional(readOnly = true)
  public long getTotalUserCount() {
    return userRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public long getEnabledUserCount() {
    return userRepository.countByEnabled(true);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<User> findAllUsers(Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  @Override
  @Transactional
  public User updateUserStatus(String userId, boolean enabled) {
    UUID userUuid;
    try {
      userUuid = UUID.fromString(userId);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid user ID format: " + userId);
    }

    User user =
        userRepository
            .findById(userUuid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

    user.setEnabled(enabled);
    User updatedUser = userRepository.save(user);

    log.info(
        "User status updated - ID: {}, Username: {}, Enabled: {}",
        userId,
        user.getUsername(),
        enabled);

    return updatedUser;
  }

  @Override
  @Transactional(readOnly = true)
  public long countAllUsers() {
    return userRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public long countActiveUsers() {
    return userRepository.countByEnabled(true);
  }

  private UserDetails buildUserDetails(User user) {
    Collection<SimpleGrantedAuthority> authorities =
        user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(authorities)
        .accountExpired(!user.isAccountNonExpired())
        .accountLocked(!user.isAccountNonLocked())
        .credentialsExpired(!user.isCredentialsNonExpired())
        .disabled(!user.isEnabled())
        .build();
  }

  private void validateRegistrationData(String username, String password, String email) {
    if (!StringUtils.hasText(username)) {
      throw new IllegalArgumentException("Username cannot be blank");
    }
    if (!StringUtils.hasText(password)) {
      throw new IllegalArgumentException("Password cannot be blank");
    }
    if (username.length() < 3 || username.length() > 50) {
      throw new IllegalArgumentException("Username must be between 3 and 50 characters");
    }
    if (password.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters");
    }
    if (StringUtils.hasText(email) && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
      throw new IllegalArgumentException("Invalid email format");
    }
  }
}
