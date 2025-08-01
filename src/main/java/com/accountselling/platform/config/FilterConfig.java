package com.accountselling.platform.config;

import com.accountselling.platform.filter.SimpleRequestResponseLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Configuration for HTTP filters. */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

  private final SimpleRequestResponseLoggingFilter simpleRequestResponseLoggingFilter;

  @Bean
  public FilterRegistrationBean<SimpleRequestResponseLoggingFilter> loggingFilter() {
    FilterRegistrationBean<SimpleRequestResponseLoggingFilter> registrationBean =
        new FilterRegistrationBean<>();

    registrationBean.setFilter(simpleRequestResponseLoggingFilter);
    registrationBean.addUrlPatterns("/api/*");
    registrationBean.setName("simpleRequestResponseLoggingFilter");
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);

    return registrationBean;
  }
}
