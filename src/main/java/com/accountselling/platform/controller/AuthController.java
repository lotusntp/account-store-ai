package com.accountselling.platform.controller;

import com.accountselling.platform.dto.auth.*;
import com.accountselling.platform.exception.AuthenticationFailedException;
import com.accountselling.platform.exception.TokenRefreshException;
import com.accountselling.platform.exception.UsernameAlreadyExistsException;
import com.accountselling.platform.model.User;
import com.accountselling.platform.service.JwtTokenService;
import com.accountselling.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication operations. Handles user registration, login, token refresh,
 * and logout.
 *
 * <p>Controller สำหรับการจัดการการยืนยันตัวตน รวมถึงการลงทะเบียน เข้าสู่ระบบ และการจัดการ token
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

  private final UserService userService;
  private final JwtTokenService jwtTokenService;
  private final AuthenticationManager authenticationManager;

  /**
   * Register a new user account. Creates a new user with provided credentials and profile
   * information.
   *
   * @param request the registration request containing user information
   * @return authentication response with tokens and user information
   */
  @PostMapping("/register")
  @Operation(
      summary = "Register new user",
      description = "Register a new user account with username and password")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Username already exists")
      })
  public ResponseEntity<AuthResponseDto> register(
      @Valid @RequestBody RegisterRequestDto request) {
    log.info("Processing user registration for username: {}", request.getUsername());

    // Check if username already exists
    if (userService.existsByUsername(request.getUsername())) {
      log.warn("Registration failed: Username already exists: {}", request.getUsername());
      throw new UsernameAlreadyExistsException(request.getUsername());
    }

    // Register new user with username and password only
    User user = userService.registerUser(request.getUsername(), request.getPassword());

    // Load user details for token generation
    UserDetails userDetails = userService.loadUserByUsername(user.getUsername());

    // Generate tokens
    String accessToken = jwtTokenService.generateAccessToken(userDetails);
    String refreshToken = jwtTokenService.generateRefreshToken(userDetails);

    // Create response
    AuthResponseDto response =
        AuthResponseDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .accessTokenExpiresIn(jwtTokenService.getAccessTokenExpiration())
            .refreshTokenExpiresIn(jwtTokenService.getRefreshTokenExpiration())
            .user(createUserInfoDto(user))
            .build();

    log.info("User registration successful for username: {}", request.getUsername());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Authenticate user and generate tokens. Validates credentials and returns JWT tokens for
   * successful authentication.
   *
   * @param request the login request containing username and password
   * @return authentication response with tokens and user information
   */
  @PostMapping("/login")
  @Operation(
      summary = "User login",
      description = "Authenticate user credentials and generate JWT tokens")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  public ResponseEntity<AuthResponseDto> login(
      @Valid @RequestBody LoginRequestDto request) {
    log.info("Processing login request for username: {}", request.getUsername());

    try {
      // Authenticate user
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  request.getUsername(), request.getPassword()));

      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      User user =
          userService
              .findByUsername(userDetails.getUsername())
              .orElseThrow(
                  () -> new AuthenticationFailedException("User not found after authentication"));

      // Generate tokens
      String accessToken = jwtTokenService.generateAccessToken(userDetails);
      String refreshToken = jwtTokenService.generateRefreshToken(userDetails);

      // Create response
      AuthResponseDto response =
          AuthResponseDto.builder()
              .accessToken(accessToken)
              .refreshToken(refreshToken)
              .tokenType("Bearer")
              .accessTokenExpiresIn(jwtTokenService.getAccessTokenExpiration())
              .refreshTokenExpiresIn(jwtTokenService.getRefreshTokenExpiration())
              .user(createUserInfoDto(user))
              .build();

      log.info("Login successful for username: {}", request.getUsername());
      return ResponseEntity.ok(response);

    } catch (BadCredentialsException e) {
      log.error("Login failed for username: {} - invalid credentials", request.getUsername());
      throw new AuthenticationFailedException("Invalid username or password");
    }
  }

  /**
   * Refresh access token using refresh token. Generates a new access token if the refresh token is
   * valid.
   *
   * @param request the refresh token request
   * @return new authentication response with fresh tokens
   */
  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh access token",
      description = "Generate new access token using valid refresh token")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  public ResponseEntity<AuthResponseDto> refresh(
      @Valid @RequestBody RefreshTokenRequestDto request) {
    log.info("Processing token refresh request");

    String refreshToken = request.getRefreshToken();

    // Extract username from refresh token
    String username = jwtTokenService.extractUsername(refreshToken);

    // Load user details
    UserDetails userDetails = userService.loadUserByUsername(username);

    // Validate refresh token
    if (!jwtTokenService.isTokenValid(refreshToken, userDetails)) {
      log.warn("Token refresh failed: Invalid refresh token for user: {}", username);
      throw new TokenRefreshException("Invalid refresh token");
    }

    // Get user entity
    User user =
        userService
            .findByUsername(username)
            .orElseThrow(() -> new TokenRefreshException("User not found: " + username));

    // Generate new tokens
    String newAccessToken = jwtTokenService.generateAccessToken(userDetails);
    String newRefreshToken = jwtTokenService.generateRefreshToken(userDetails);

    // Create response
    AuthResponseDto response =
        AuthResponseDto.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .accessTokenExpiresIn(jwtTokenService.getAccessTokenExpiration())
            .refreshTokenExpiresIn(jwtTokenService.getRefreshTokenExpiration())
            .user(createUserInfoDto(user))
            .build();

    log.info("Token refresh successful for user: {}", username);
    return ResponseEntity.ok(response);
  }

  /**
   * Logout user and invalidate tokens. Currently returns success status as token invalidation is
   * handled client-side.
   *
   * @return logout confirmation response
   */
  @PostMapping("/logout")
  @Operation(summary = "User logout", description = "Logout user and invalidate tokens")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  public ResponseEntity<Map<String, String>> logout() {
    log.info("Processing logout request");

    // Note: In a stateless JWT implementation, logout is typically handled client-side
    // by removing tokens from storage. Server-side token blacklisting could be implemented
    // for enhanced security but requires additional infrastructure.

    Map<String, String> response = new HashMap<>();
    response.put("message", "Logout successful");
    response.put("timestamp", LocalDateTime.now().toString());

    log.info("Logout completed successfully");
    return ResponseEntity.ok(response);
  }

  /**
   * Create user information DTO from User entity. Maps user entity fields to DTO for API response.
   *
   * @param user the user entity
   * @return user information DTO
   */
  private AuthResponseDto.UserInfoDto createUserInfoDto(User user) {
    Set<String> roleNames =
        user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet());

    return AuthResponseDto.UserInfoDto.builder()
        .username(user.getUsername())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .fullName(user.getFullName())
        .roles(roleNames)
        .enabled(user.getEnabled())
        .createdAt(user.getCreatedAt())
        .lastLoginAt(LocalDateTime.now()) // Current login time
        .build();
  }
}
