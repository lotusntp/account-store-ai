package com.accountselling.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("integration-test")
public class TestContainersConfig {

  @Bean
  public PostgreSQLContainer<?> postgreSQLContainer() {
    PostgreSQLContainer<?> container =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:14"))
            .withDatabaseName("accountselling_test")
            .withUsername("test")
            .withPassword("test");
    container.start();

    // Set system properties for Spring Boot to use
    System.setProperty("spring.datasource.url", container.getJdbcUrl());
    System.setProperty("spring.datasource.username", container.getUsername());
    System.setProperty("spring.datasource.password", container.getPassword());

    return container;
  }
}
