package com.accountselling.platform.config;

import java.util.Properties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.accountselling.platform.repository")
@EntityScan(basePackages = "com.accountselling.platform.model")
public class DatabaseConfig {

  // The actual DataSource will be configured by Spring Boot
  // This class is for additional JPA configuration if needed

  @Bean
  public Properties hibernateProperties() {
    Properties properties = new Properties();
    properties.put(
        "hibernate.physical_naming_strategy",
        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
    return properties;
  }
}
