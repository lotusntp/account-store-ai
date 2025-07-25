package com.accountselling.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Account Selling Platform. This Spring Boot application provides a
 * secure marketplace for selling digital accounts.
 *
 * <p>Includes OpenTelemetry configuration for observability and tracing.
 *
 * <p>Enables scheduling for automated inventory management tasks.
 */
@SpringBootApplication
@EnableScheduling
public class AccountSellingPlatformApplication {

  public static void main(String[] args) {
    SpringApplication.run(AccountSellingPlatformApplication.class, args);
  }
}
