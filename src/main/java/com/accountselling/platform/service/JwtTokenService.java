package com.accountselling.platform.service;

import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Service interface for JWT token operations. Handles generation, validation, and extraction of JWT
 * tokens.
 *
 * <p>บริการสำหรับการจัดการ JWT token รวมถึงการสร้าง ตรวจสอบ และดึงข้อมูลจาก token
 */
public interface JwtTokenService {

  /**
   * Generate access token for user authentication.
   *
   * @param userDetails the user details to encode in token
   * @return the generated access token
   */
  String generateAccessToken(UserDetails userDetails);

  /**
   * Generate access token with additional claims.
   *
   * @param extraClaims additional claims to include in token
   * @param userDetails the user details to encode in token
   * @return the generated access token
   */
  String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails);

  /**
   * Generate refresh token for token renewal.
   *
   * @param userDetails the user details to encode in token
   * @return the generated refresh token
   */
  String generateRefreshToken(UserDetails userDetails);

  /**
   * Extract username from JWT token.
   *
   * @param token the JWT token
   * @return the extracted username
   */
  String extractUsername(String token);

  /**
   * Check if token is valid for the given user.
   *
   * @param token the JWT token to validate
   * @param userDetails the user details to validate against
   * @return true if token is valid, false otherwise
   */
  boolean isTokenValid(String token, UserDetails userDetails);

  /**
   * Check if token is expired.
   *
   * @param token the JWT token to check
   * @return true if token is expired, false otherwise
   */
  boolean isTokenExpired(String token);

  /**
   * Get access token expiration time in milliseconds.
   *
   * @return access token expiration time
   */
  long getAccessTokenExpiration();

  /**
   * Get refresh token expiration time in milliseconds.
   *
   * @return refresh token expiration time
   */
  long getRefreshTokenExpiration();
}
