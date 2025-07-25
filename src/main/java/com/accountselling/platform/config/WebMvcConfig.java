package com.accountselling.platform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web MVC configuration for registering interceptors */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final RequestContextInterceptor requestContextInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // Add RequestContextInterceptor to all paths
    registry.addInterceptor(requestContextInterceptor).addPathPatterns("/**");
  }
}
