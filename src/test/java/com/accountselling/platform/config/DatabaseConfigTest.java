package com.accountselling.platform.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DatabaseConfigTest {

  @Autowired private DataSource dataSource;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testDatabaseConnection() {
    assertNotNull(dataSource);
    assertNotNull(jdbcTemplate);

    // Test that we can execute a query
    String dbVersion = jdbcTemplate.queryForObject("SELECT H2VERSION() FROM DUAL", String.class);
    assertNotNull(dbVersion);
    System.out.println("Database version: " + dbVersion);
  }
}
