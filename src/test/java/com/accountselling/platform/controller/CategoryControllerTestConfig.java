package com.accountselling.platform.controller;

import com.accountselling.platform.exception.handler.GlobalExceptionHandler;
import com.accountselling.platform.security.JwtAuthenticationFilter;
import com.accountselling.platform.security.JwtTokenProvider;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test configuration for CategoryControllerTest to provide mock beans for security components.
 *
 * <p>Test configuration สำหรับ CategoryControllerTest เพื่อให้ mock beans สำหรับ security
 * components
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test")
public class CategoryControllerTestConfig {

  @Bean
  @Primary
  public JwtTokenProvider jwtTokenProvider() {
    return Mockito.mock(JwtTokenProvider.class);
  }

  @Bean
  @Primary
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    return Mockito.mock(JwtAuthenticationFilter.class);
  }

  @Bean
  @Primary
  public GlobalExceptionHandler globalExceptionHandler() {
    return new GlobalExceptionHandler();
  }

  @Bean
  @Primary
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }
}
