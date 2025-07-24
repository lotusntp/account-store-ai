package com.accountselling.platform.controller;

import com.accountselling.platform.dto.auth.LoginRequestDto;
import com.accountselling.platform.dto.auth.RefreshTokenRequestDto;
import com.accountselling.platform.dto.auth.RegisterRequestDto;
import com.accountselling.platform.model.Role;
import com.accountselling.platform.model.User;
import com.accountselling.platform.repository.RoleRepository;
import com.accountselling.platform.repository.UserRepository;
import com.accountselling.platform.service.JwtTokenService;
import com.accountselling.platform.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests authentication endpoints including registration, login, token refresh, and logout.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    private Role userRole;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create USER role if it doesn't exist
        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    return roleRepository.save(role);
                });
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("testuser", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("testuser")))
                .andExpect(jsonPath("$.user.roles", hasItem("USER")));
    }

    @Test
    @DisplayName("Should reject registration with existing username")
    void shouldRejectRegistrationWithExistingUsername() throws Exception {
        // Create existing user
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setRoles(Set.of(userRole));
        userRepository.save(existingUser);

        RegisterRequestDto request = new RegisterRequestDto("existinguser", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should reject registration with invalid input")
    void shouldRejectRegistrationWithInvalidInput() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("", "123"); // Invalid username and password

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        // Create test user
        User testUser = new User();
        testUser.setUsername("loginuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRoles(Set.of(userRole));
        userRepository.save(testUser);

        LoginRequestDto request = new LoginRequestDto("loginuser", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("loginuser")));
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto("nonexistent", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject login with blank credentials")
    void shouldRejectLoginWithBlankCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void shouldRefreshTokenSuccessfullyWithValidRefreshToken() throws Exception {
        // Create test user
        User testUser = new User();
        testUser.setUsername("refreshuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRoles(Set.of(userRole));
        testUser = userRepository.save(testUser);

        // Generate refresh token using UserDetails
        UserDetails userDetails = userService.loadUserByUsername(testUser.getUsername());
        String refreshToken = jwtTokenService.generateRefreshToken(userDetails);

        RefreshTokenRequestDto request = new RefreshTokenRequestDto(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("refreshuser")));
    }

    @Test
    @DisplayName("Should reject refresh with invalid token")
    void shouldRejectRefreshWithInvalidToken() throws Exception {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("invalid.token.here");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("JWT")))
                .andExpect(jsonPath("$.path", is("/api/auth/refresh")));
    }

    @Test
    @DisplayName("Should reject refresh with blank token")
    void shouldRejectRefreshWithBlankToken() throws Exception {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logout successful")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}