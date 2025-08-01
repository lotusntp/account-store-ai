package com.accountselling.platform.security;

import com.accountselling.platform.exception.security.EncryptionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Comprehensive data security service that integrates encryption, password security, and secure
 * data handling capabilities.
 *
 * <p>บริการความปลอดภัยข้อมูลแบบครบวงจรที่รวมการเข้ารหัส การจัดการรหัสผ่านอย่างปลอดภัย
 * และการจัดการข้อมูลที่มีความสำคัญ
 */
@Service
@Slf4j
public class DataSecurityService {

  private final EncryptionService encryptionService;
  private final PasswordSecurityService passwordSecurityService;

  // Security monitoring
  private final AtomicLong encryptionOperations = new AtomicLong(0);
  private final AtomicLong decryptionOperations = new AtomicLong(0);
  private final AtomicLong passwordHashingOperations = new AtomicLong(0);
  private final AtomicLong securityViolations = new AtomicLong(0);

  // Cache for frequently accessed security configurations
  private final ConcurrentHashMap<String, Object> securityConfigCache = new ConcurrentHashMap<>();

  @Value("${app.security.data.encrypt-sensitive-fields:true}")
  private boolean encryptSensitiveFields;

  @Value("${app.security.data.secure-memory-handling:true}")
  private boolean secureMemoryHandling;

  @Value("${app.security.data.audit-data-access:true}")
  private boolean auditDataAccess;

  @Autowired
  public DataSecurityService(
      EncryptionService encryptionService, PasswordSecurityService passwordSecurityService) {
    this.encryptionService = encryptionService;
    this.passwordSecurityService = passwordSecurityService;
    log.info(
        "DataSecurityService initialized with encryption={}, secure-memory={}, audit={}",
        encryptSensitiveFields,
        secureMemoryHandling,
        auditDataAccess);
  }

  /**
   * Securely processes sensitive data with encryption and audit logging.
   *
   * @param sensitiveData The sensitive data to process
   * @param dataType Type of data for audit purposes
   * @return Encrypted data
   * @throws EncryptionException if encryption fails
   */
  public String securelyProcessSensitiveData(String sensitiveData, String dataType) {
    if (sensitiveData == null || sensitiveData.trim().isEmpty()) {
      throw new IllegalArgumentException("Sensitive data cannot be null or empty");
    }

    try {
      if (auditDataAccess) {
        log.debug("Processing sensitive data of type: {}", dataType);
      }

      String encryptedData;
      if (encryptSensitiveFields) {
        encryptedData = encryptionService.encrypt(sensitiveData);
        encryptionOperations.incrementAndGet();
      } else {
        log.warn("Sensitive field encryption is disabled - data stored in plain text");
        encryptedData = sensitiveData;
      }

      if (auditDataAccess) {
        log.info(
            "Successfully processed sensitive data of type: {}, length: {}",
            dataType,
            sensitiveData.length());
      }

      return encryptedData;

    } catch (Exception e) {
      securityViolations.incrementAndGet();
      log.error("Failed to process sensitive data of type: {}", dataType, e);
      throw new EncryptionException("Failed to securely process sensitive data", e);
    }
  }

  /**
   * Securely retrieves and decrypts sensitive data with audit logging.
   *
   * @param encryptedData The encrypted data to retrieve
   * @param dataType Type of data for audit purposes
   * @return Decrypted sensitive data
   * @throws EncryptionException if decryption fails
   */
  public String securelyRetrieveSensitiveData(String encryptedData, String dataType) {
    if (encryptedData == null || encryptedData.trim().isEmpty()) {
      throw new IllegalArgumentException("Encrypted data cannot be null or empty");
    }

    try {
      if (auditDataAccess) {
        log.debug("Retrieving sensitive data of type: {}", dataType);
      }

      String decryptedData;
      if (encryptSensitiveFields) {
        decryptedData = encryptionService.decrypt(encryptedData);
        decryptionOperations.incrementAndGet();
      } else {
        decryptedData = encryptedData;
      }

      if (auditDataAccess) {
        log.info("Successfully retrieved sensitive data of type: {}", dataType);
      }

      return decryptedData;

    } catch (Exception e) {
      securityViolations.incrementAndGet();
      log.error("Failed to retrieve sensitive data of type: {}", dataType, e);
      throw new EncryptionException("Failed to securely retrieve sensitive data", e);
    }
  }

  /**
   * Securely processes sensitive data using SecureString for memory safety.
   *
   * @param sensitiveData The sensitive data to process
   * @param dataType Type of data for audit purposes
   * @return SecureString containing encrypted data
   */
  public SecureString securelyProcessWithSecureString(String sensitiveData, String dataType) {
    if (!secureMemoryHandling) {
      // Fallback to regular processing if secure memory handling is disabled
      String encrypted = securelyProcessSensitiveData(sensitiveData, dataType);
      return new SecureString(encrypted);
    }

    try (SecureString secureInput = new SecureString(sensitiveData)) {
      SecureString encryptedSecure = encryptionService.encryptToSecureString(sensitiveData);
      encryptionOperations.incrementAndGet();

      if (auditDataAccess) {
        log.info("Successfully processed sensitive data with SecureString, type: {}", dataType);
      }

      return encryptedSecure;

    } catch (Exception e) {
      securityViolations.incrementAndGet();
      log.error("Failed to process sensitive data with SecureString, type: {}", dataType, e);
      throw new EncryptionException("Failed to securely process data with SecureString", e);
    }
  }

  /**
   * Securely hashes a password with strength validation.
   *
   * @param rawPassword The raw password to hash
   * @return Hashed password
   * @throws IllegalArgumentException if password doesn't meet security requirements
   */
  public String securelyHashPassword(String rawPassword) {
    if (rawPassword == null) {
      throw new IllegalArgumentException("Password cannot be null");
    }

    // Validate password strength
    PasswordSecurityService.PasswordStrength strength =
        passwordSecurityService.assessPasswordStrength(rawPassword);

    if (!strength.meetsMinimumRequirements()) {
      securityViolations.incrementAndGet();
      log.warn("Password does not meet minimum security requirements: {}", strength.getFeedback());
      throw new IllegalArgumentException(
          "Password does not meet security requirements: " + strength.getFeedback());
    }

    try {
      String hashedPassword = passwordSecurityService.hashPasswordWithBCrypt(rawPassword);
      passwordHashingOperations.incrementAndGet();

      if (auditDataAccess) {
        log.info("Successfully hashed password with strength level: {}", strength.getLevel());
      }

      return hashedPassword;

    } catch (Exception e) {
      securityViolations.incrementAndGet();
      log.error("Failed to hash password", e);
      throw new RuntimeException("Failed to securely hash password", e);
    }
  }

  /**
   * Verifies a password against its hash with audit logging.
   *
   * @param rawPassword The raw password to verify
   * @param hashedPassword The hashed password to verify against
   * @return true if password matches
   */
  public boolean securelyVerifyPassword(String rawPassword, String hashedPassword) {
    if (rawPassword == null || hashedPassword == null) {
      if (auditDataAccess) {
        log.warn("Password verification attempted with null values");
      }
      return false;
    }

    try {
      boolean matches = passwordSecurityService.verifyBCryptPassword(rawPassword, hashedPassword);

      if (auditDataAccess) {
        log.info("Password verification result: {}", matches ? "success" : "failed");
      }

      if (!matches) {
        securityViolations.incrementAndGet();
      }

      return matches;

    } catch (Exception e) {
      securityViolations.incrementAndGet();
      log.error("Password verification failed with exception", e);
      return false;
    }
  }

  /**
   * Validates that sensitive data meets security requirements before processing.
   *
   * @param data The data to validate
   * @param dataType Type of data being validated
   * @return true if data meets security requirements
   */
  public boolean validateSensitiveDataSecurity(String data, String dataType) {
    if (data == null || data.trim().isEmpty()) {
      log.warn("Sensitive data validation failed: data is null or empty for type {}", dataType);
      return false;
    }

    // Check for common security issues
    if (data.toLowerCase().contains("password") && !data.contains(":")) {
      log.warn("Potential plain text password detected in data type: {}", dataType);
      return false;
    }

    // Check for minimum data length for certain types
    if ("account_credentials".equals(dataType) && data.length() < 10) {
      log.warn("Account credentials too short for type: {}", dataType);
      return false;
    }

    // Check for suspicious patterns
    if (data.matches(".*[<>\"'&].*")) {
      log.warn("Potentially unsafe characters detected in data type: {}", dataType);
      return false;
    }

    return true;
  }

  /**
   * Clears sensitive data from memory safely.
   *
   * @param sensitiveData Character array containing sensitive data
   */
  public void clearSensitiveData(char[] sensitiveData) {
    if (secureMemoryHandling && sensitiveData != null) {
      passwordSecurityService.clearPassword(sensitiveData);
      if (auditDataAccess) {
        log.debug("Cleared sensitive data from memory");
      }
    }
  }

  /**
   * Gets security health status and metrics.
   *
   * @return SecurityHealthStatus containing current security metrics
   */
  public SecurityHealthStatus getSecurityHealthStatus() {
    boolean encryptionHealthy = encryptionService.isHealthy();

    return SecurityHealthStatus.builder()
        .encryptionServiceHealthy(encryptionHealthy)
        .encryptionOperations(encryptionOperations.get())
        .decryptionOperations(decryptionOperations.get())
        .passwordHashingOperations(passwordHashingOperations.get())
        .securityViolations(securityViolations.get())
        .encryptSensitiveFields(encryptSensitiveFields)
        .secureMemoryHandling(secureMemoryHandling)
        .auditDataAccess(auditDataAccess)
        .build();
  }

  /** Resets security metrics (for testing purposes). */
  public void resetSecurityMetrics() {
    encryptionOperations.set(0);
    decryptionOperations.set(0);
    passwordHashingOperations.set(0);
    securityViolations.set(0);
    log.info("Security metrics reset");
  }

  /** Security health status data class. */
  public static class SecurityHealthStatus {
    private final boolean encryptionServiceHealthy;
    private final long encryptionOperations;
    private final long decryptionOperations;
    private final long passwordHashingOperations;
    private final long securityViolations;
    private final boolean encryptSensitiveFields;
    private final boolean secureMemoryHandling;
    private final boolean auditDataAccess;

    private SecurityHealthStatus(
        boolean encryptionServiceHealthy,
        long encryptionOperations,
        long decryptionOperations,
        long passwordHashingOperations,
        long securityViolations,
        boolean encryptSensitiveFields,
        boolean secureMemoryHandling,
        boolean auditDataAccess) {
      this.encryptionServiceHealthy = encryptionServiceHealthy;
      this.encryptionOperations = encryptionOperations;
      this.decryptionOperations = decryptionOperations;
      this.passwordHashingOperations = passwordHashingOperations;
      this.securityViolations = securityViolations;
      this.encryptSensitiveFields = encryptSensitiveFields;
      this.secureMemoryHandling = secureMemoryHandling;
      this.auditDataAccess = auditDataAccess;
    }

    public static SecurityHealthStatusBuilder builder() {
      return new SecurityHealthStatusBuilder();
    }

    // Getters
    public boolean isEncryptionServiceHealthy() {
      return encryptionServiceHealthy;
    }

    public long getEncryptionOperations() {
      return encryptionOperations;
    }

    public long getDecryptionOperations() {
      return decryptionOperations;
    }

    public long getPasswordHashingOperations() {
      return passwordHashingOperations;
    }

    public long getSecurityViolations() {
      return securityViolations;
    }

    public boolean isEncryptSensitiveFields() {
      return encryptSensitiveFields;
    }

    public boolean isSecureMemoryHandling() {
      return secureMemoryHandling;
    }

    public boolean isAuditDataAccess() {
      return auditDataAccess;
    }

    public static class SecurityHealthStatusBuilder {
      private boolean encryptionServiceHealthy;
      private long encryptionOperations;
      private long decryptionOperations;
      private long passwordHashingOperations;
      private long securityViolations;
      private boolean encryptSensitiveFields;
      private boolean secureMemoryHandling;
      private boolean auditDataAccess;

      public SecurityHealthStatusBuilder encryptionServiceHealthy(
          boolean encryptionServiceHealthy) {
        this.encryptionServiceHealthy = encryptionServiceHealthy;
        return this;
      }

      public SecurityHealthStatusBuilder encryptionOperations(long encryptionOperations) {
        this.encryptionOperations = encryptionOperations;
        return this;
      }

      public SecurityHealthStatusBuilder decryptionOperations(long decryptionOperations) {
        this.decryptionOperations = decryptionOperations;
        return this;
      }

      public SecurityHealthStatusBuilder passwordHashingOperations(long passwordHashingOperations) {
        this.passwordHashingOperations = passwordHashingOperations;
        return this;
      }

      public SecurityHealthStatusBuilder securityViolations(long securityViolations) {
        this.securityViolations = securityViolations;
        return this;
      }

      public SecurityHealthStatusBuilder encryptSensitiveFields(boolean encryptSensitiveFields) {
        this.encryptSensitiveFields = encryptSensitiveFields;
        return this;
      }

      public SecurityHealthStatusBuilder secureMemoryHandling(boolean secureMemoryHandling) {
        this.secureMemoryHandling = secureMemoryHandling;
        return this;
      }

      public SecurityHealthStatusBuilder auditDataAccess(boolean auditDataAccess) {
        this.auditDataAccess = auditDataAccess;
        return this;
      }

      public SecurityHealthStatus build() {
        return new SecurityHealthStatus(
            encryptionServiceHealthy,
            encryptionOperations,
            decryptionOperations,
            passwordHashingOperations,
            securityViolations,
            encryptSensitiveFields,
            secureMemoryHandling,
            auditDataAccess);
      }
    }
  }
}
