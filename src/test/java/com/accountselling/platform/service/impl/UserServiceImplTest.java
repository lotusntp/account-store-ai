package com.accountselling.platform.service.impl;

import com.accountselling.platform.exception.InvalidCredentialsException;
import com.accountselling.platform.exception.ResourceAlreadyExistsException;
import com.accountselling.platform.exception.ResourceNotFoundException;
import com.accountselling.platform.model.Role;
import com.accountselling.platform.model.User;
import com.accountselling.platform.repository.RoleRepository;
import com.accountselling.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl.
 * Tests user registration, authentication, and management functionality.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role("USER", "Default user role");
        userRole.setId(1L);

        adminRole = new Role("ADMIN", "Administrator role");
        adminRole.setId(2L);

        testUser = new User("testuser", "hashedpassword", "test@example.com");
        testUser.setId(UUID.randomUUID());
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);
        testUser.addRole(userRole);
    }

    @Test
    void loadUserByUsername_WithExistingUser_ShouldReturnUserDetails() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("testuser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("hashedpassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsername_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: nonexistent");
    }

    @Test
    void registerUser_WithValidData_ShouldCreateUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User result = userService.registerUser("newuser", "password123", "new@example.com", "New", "User");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getPassword()).isEqualTo("hashedpassword123");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getRoles()).contains(userRole);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WithExistingUsername_ShouldThrowException() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser("existinguser", "password123", "new@example.com"))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Username already exists: existinguser");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WithExistingEmail_ShouldThrowException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser("newuser", "password123", "existing@example.com"))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Email already exists: existing@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WithoutDefaultRole_ShouldThrowException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.registerUser("newuser", "password123", "new@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Default role not found: USER");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_WithValidCredentials_ShouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);

        User result = userService.authenticateUser("testuser", "password123");

        assertThat(result).isEqualTo(testUser);
        verify(passwordEncoder).matches("password123", "hashedpassword");
    }

    @Test
    void authenticateUser_WithInvalidUsername_ShouldThrowException() {
        when(userRepository.findByUsername("invaliduser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.authenticateUser("invaliduser", "password123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void authenticateUser_WithInvalidPassword_ShouldThrowException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.authenticateUser("testuser", "wrongpassword"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void authenticateUser_WithDisabledUser_ShouldThrowException() {
        testUser.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);

        assertThatThrownBy(() -> userService.authenticateUser("testuser", "password123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("User account is disabled");
    }

    @Test
    void authenticateUser_WithLockedUser_ShouldThrowException() {
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);

        assertThatThrownBy(() -> userService.authenticateUser("testuser", "password123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("User account is locked");
    }

    @Test
    void findByUsername_WithExistingUser_ShouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
    }

    @Test
    void findByUsername_WithNonExistentUser_ShouldReturnEmpty() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void updateUserProfile_WithValidData_ShouldUpdateUser() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUserProfile(userId, "newemail@example.com", "NewFirst", "NewLast");

        assertThat(result.getEmail()).isEqualTo("newemail@example.com");
        assertThat(result.getFirstName()).isEqualTo("NewFirst");
        assertThat(result.getLastName()).isEqualTo("NewLast");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfile_WithExistingEmail_ShouldThrowException() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserProfile(userId, "existing@example.com", "NewFirst", "NewLast"))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Email already exists: existing@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_WithValidCurrentPassword_ShouldUpdatePassword() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentpassword", "hashedpassword")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("hashednewpassword");

        userService.changePassword(userId, "currentpassword", "newpassword");

        verify(passwordEncoder).matches("currentpassword", "hashedpassword");
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(testUser);
        assertThat(testUser.getPassword()).isEqualTo("hashednewpassword");
    }

    @Test
    void changePassword_WithInvalidCurrentPassword_ShouldThrowException() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(userId, "wrongpassword", "newpassword"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void setUserEnabled_WithValidUser_ShouldUpdateStatus() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.setUserEnabled(userId, false);

        assertThat(result.isEnabled()).isFalse();
        verify(userRepository).save(testUser);
    }

    @Test
    void addRoleToUser_WithValidUserAndRole_ShouldAddRole() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.addRoleToUser(userId, "ADMIN");

        assertThat(result.getRoles()).contains(adminRole);
        verify(userRepository).save(testUser);
    }

    @Test
    void addRoleToUser_WithNonExistentRole_ShouldThrowException() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addRoleToUser(userId, "NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found: NONEXISTENT");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeRoleFromUser_WithExistingRole_ShouldRemoveRole() {
        UUID userId = testUser.getId();
        testUser.addRole(adminRole);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.removeRoleFromUser(userId, "ADMIN");

        assertThat(result.getRoles()).doesNotContain(adminRole);
        verify(userRepository).save(testUser);
    }

    @Test
    void existsByUsername_WithExistingUsername_ShouldReturnTrue() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        boolean result = userService.existsByUsername("testuser");

        assertThat(result).isTrue();
    }

    @Test
    void existsByEmail_WithExistingEmail_ShouldReturnTrue() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        boolean result = userService.existsByEmail("test@example.com");

        assertThat(result).isTrue();
    }

    @Test
    void findAllUsers_ShouldReturnAllUsers() {
        List<User> users = Arrays.asList(testUser, new User("user2", "pass2", "user2@example.com"));
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(users);
    }

    @Test
    void getTotalUserCount_ShouldReturnCount() {
        when(userRepository.count()).thenReturn(5L);

        long result = userService.getTotalUserCount();

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void getEnabledUserCount_ShouldReturnEnabledCount() {
        when(userRepository.countByEnabled(true)).thenReturn(3L);

        long result = userService.getEnabledUserCount();

        assertThat(result).isEqualTo(3L);
    }
}