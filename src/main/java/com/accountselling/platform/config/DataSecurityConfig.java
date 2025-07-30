package com.accountselling.platform.config;

import com.accountselling.platform.security.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration class for data security and encryption key management.
 *
 * <p>คลาสการกำหนดค่าสำหรับความปลอดภัยของข้อมูลและการจัดการ encryption keys
 * รวมถึงการตั้งค่าความปลอดภัยในการเก็บข้อมูลสำคัญ
 */
@Configuration
@Slf4j
public class DataSecurityConfig {

  private final Environment environment;

  public DataSecurityConfig(Environment environment) {
    this.environment = environment;
  }

  /**
   * Creates and configures the main encryption service bean.
   *
   * @param encryptionKey The encryption key from application properties
   * @return Configured EncryptionService instance
   */
  @Bean
  public EncryptionService encryptionService(
      @Value("${app.encryption.key:}") String encryptionKey) {
    validateSecurityConfiguration();

    EncryptionService service = new EncryptionService(encryptionKey);

    // Verify the service is working correctly
    if (!service.isHealthy()) {
      throw new RuntimeException(
          "EncryptionService failed health check - cannot start application");
    }

    log.info("EncryptionService configured and validated successfully");
    return service;
  }

  /**
   * Validates the security configuration to ensure proper setup. Logs warnings for development/test
   * environments with weak security.
   */
  private void validateSecurityConfiguration() {
    String[] activeProfiles = environment.getActiveProfiles();
    boolean isProduction = false;
    boolean isTest = false;

    for (String profile : activeProfiles) {
      if ("prod".equals(profile) || "production".equals(profile)) {
        isProduction = true;
      }
      if ("test".equals(profile)) {
        isTest = true;
      }
    }

    String encryptionKey = environment.getProperty("app.encryption.key", "");

    if (isProduction) {
      // Strict validation for production
      if (encryptionKey.trim().isEmpty()) {
        throw new RuntimeException(
            "Production environment requires explicit encryption key configuration");
      }

      if (encryptionKey.length() < 32) {
        throw new RuntimeException(
            "Production encryption key must be at least 32 characters (Base64 encoded 256-bit"
                + " key)");
      }

      // Check if using default/example keys
      if (encryptionKey.contains("example")
          || encryptionKey.contains("default")
          || encryptionKey.equals("changeme")) {
        throw new RuntimeException(
            "Production environment cannot use example/default encryption keys");
      }

      log.info("Production encryption configuration validated");

    } else if (isTest) {
      // Allow auto-generation for test environment
      if (encryptionKey.trim().isEmpty()) {
        log.info("Test environment: Using auto-generated encryption key");
      } else {
        log.info("Test environment: Using provided encryption key");
      }

    } else {
      // Development environment warnings
      if (encryptionKey.trim().isEmpty()) {
        log.warn(
            "Development environment: Using auto-generated encryption key - data will not be"
                + " portable between restarts");
      } else {
        log.info("Development environment: Using provided encryption key");
      }
    }

    // General security warnings
    if (!isTest) {
      logSecurityRecommendations(isProduction);
    }
  }

  /** Logs security recommendations based on the environment. */
  private void logSecurityRecommendations(boolean isProduction) {
    if (isProduction) {
      log.info("Production Security Checklist:");
      log.info("✓ Encryption key validation completed");
      log.info(
          "ℹ Ensure encryption keys are stored securely (e.g., AWS Secrets Manager, HashiCorp"
              + " Vault)");
      log.info("ℹ Consider implementing key rotation policies");
      log.info("ℹ Monitor encryption service health and performance");
      log.info("ℹ Ensure database encryption at rest is enabled");
      log.info("ℹ Regular security audits of encrypted data storage");
    } else {
      log.info("Development Security Notes:");
      log.info(
          "ℹ Use app.encryption.key property to set a consistent encryption key for development");
      log.info("ℹ Never commit encryption keys to version control");
      log.info("ℹ Test encryption/decryption with realistic data volumes");
      log.info("ℹ Verify encrypted data storage in development database");
    }
  }

  /**
   * Configuration properties class for encryption settings. This allows for future expansion of
   * encryption-related configuration.
   */
  public static class EncryptionProperties {
    private String key;
    private boolean keyRotationEnabled = false;
    private int keyRotationDays = 90;
    private boolean healthCheckEnabled = true;

    // Getters and setters
    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public boolean isKeyRotationEnabled() {
      return keyRotationEnabled;
    }

    public void setKeyRotationEnabled(boolean keyRotationEnabled) {
      this.keyRotationEnabled = keyRotationEnabled;
    }

    public int getKeyRotationDays() {
      return keyRotationDays;
    }

    public void setKeyRotationDays(int keyRotationDays) {
      this.keyRotationDays = keyRotationDays;
    }

    public boolean isHealthCheckEnabled() {
      return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
      this.healthCheckEnabled = healthCheckEnabled;
    }
  }
}
