package com.accountselling.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@TestConfiguration
public class NoJpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        // mock หรือ placeholder
        return () -> Optional.of("test-user");
    }
}

