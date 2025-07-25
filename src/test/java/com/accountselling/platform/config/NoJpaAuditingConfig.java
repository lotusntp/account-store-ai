package com.accountselling.platform.config;

import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

@TestConfiguration
public class NoJpaAuditingConfig {

  @Bean
  public AuditorAware<String> auditorAware() {
    // mock or placeholder
    return () -> Optional.of("test-user");
  }
}
