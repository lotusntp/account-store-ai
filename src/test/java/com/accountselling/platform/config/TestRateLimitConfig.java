package com.accountselling.platform.config;

import com.accountselling.platform.security.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;

/**
 * Test configuration to disable rate limiting during integration tests. This configuration provides
 * a no-op rate limiting filter for test environment.
 *
 * <p>Configuration สำหรับปิดการทำงานของ rate limiting ในระหว่าง integration tests โดย override
 * RateLimitingFilter ด้วย implementation ที่ไม่ทำงาน
 */
@TestConfiguration
@Slf4j
public class TestRateLimitConfig {

  /**
   * Creates a no-op rate limiting filter for tests. This filter allows all requests to pass through
   * without any rate limiting.
   *
   * <p>สร้าง rate limiting filter ที่ไม่ทำงานสำหรับ tests Filter นี้จะให้ request
   * ทั้งหมดผ่านได้โดยไม่มีการจำกัด rate
   */
  @Bean
  @Primary
  @Profile("test")
  public RateLimitingFilter testRateLimitingFilter() {
    log.info("Creating test rate limiting filter - rate limiting is DISABLED for tests");

    return new RateLimitingFilter() {
      @Override
      protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain)
          throws ServletException, IOException {

        // Always allow requests to pass through in test environment
        log.debug("Test rate limiting filter: allowing request to {}", request.getRequestURI());
        filterChain.doFilter(request, response);
      }
    };
  }
}
