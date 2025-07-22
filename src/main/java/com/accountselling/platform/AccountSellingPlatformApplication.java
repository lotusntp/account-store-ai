package com.accountselling.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Account Selling Platform. This Spring Boot application provides a
 * secure marketplace for selling digital accounts.
 *
 * <p>Includes OpenTelemetry configuration for observability and tracing.
 */
@SpringBootApplication
public class AccountSellingPlatformApplication {

  public static void main(String[] args) {
    SpringApplication.run(AccountSellingPlatformApplication.class, args);
  }
}
