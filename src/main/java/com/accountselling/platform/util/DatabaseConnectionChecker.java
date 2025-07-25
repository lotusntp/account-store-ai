package com.accountselling.platform.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Utility class to check database connection on application startup. Only runs in development and
 * test profiles.
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class DatabaseConnectionChecker implements CommandLineRunner {

  private final JdbcTemplate jdbcTemplate;

  public DatabaseConnectionChecker(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(String... args) {
    try {
      String dbVersion = jdbcTemplate.queryForObject("SELECT version()", String.class);
      log.info("Successfully connected to the database. Database version: {}", dbVersion);

      // Check if the account_selling schema exists
      Integer schemaExists =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name ="
                  + " 'account_selling'",
              Integer.class);

      if (schemaExists != null && schemaExists > 0) {
        log.info("The 'account_selling' schema exists.");
      } else {
        log.warn("The 'account_selling' schema does not exist. It will be created by Flyway.");
      }

    } catch (Exception e) {
      log.error("Failed to connect to the database: {}", e.getMessage(), e);
    }
  }
}
