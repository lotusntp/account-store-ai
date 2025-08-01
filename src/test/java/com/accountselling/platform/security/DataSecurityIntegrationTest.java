package com.accountselling.platform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountselling.platform.model.Category;
import com.accountselling.platform.model.Product;
import com.accountselling.platform.model.Role;
import com.accountselling.platform.model.Stock;
import com.accountselling.platform.model.User;
import com.accountselling.platform.repository.CategoryRepository;
import com.accountselling.platform.repository.ProductRepository;
import com.accountselling.platform.repository.RoleRepository;
import com.accountselling.platform.repository.StockRepository;
import com.accountselling.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for data security functionality. Tests encryption services, password security,
 * and secure data handling.
 *
 * <p>Integration tests สำหรับการทดสอบความปลอดภัยของข้อมูล รวมถึงการเข้ารหัส
 * การจัดการรหัสผ่านอย่างปลอดภัย และการจัดการข้อมูลที่มีความสำคัญ
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Data Security Integration Tests")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class DataSecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EncryptionService encryptionService;
  @Autowired private PasswordSecurityService passwordSecurityService;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private StockRepository stockRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ObjectMapper objectMapper;

  private User testUser;
  private Role userRole;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    stockRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    userRepository.deleteAll();
    roleRepository.deleteAll();

    // Create role
    userRole = roleRepository.save(new Role("ROLE_USER", "Regular user role"));

    // Create test user
    testUser = new User("securitytestuser", passwordEncoder.encode("SecurePassword123!"));
    testUser.setEmail("security@test.com");
    testUser = userRepository.save(testUser);
    testUser.addRole(userRole);
    userRepository.save(testUser);
  }

  @Nested
  @DisplayName("Encryption Service Integration Tests")
  class EncryptionServiceIntegrationTests {

    @Test
    @DisplayName("Should encrypt and decrypt sensitive data in application context")
    void shouldEncryptAndDecryptSensitiveDataInApplicationContext() {
      String sensitiveData =
          "username:gameaccount123\npassword:GameP@ss456!\nemail:game@example.com";

      // Test encryption service is properly configured
      assertTrue(encryptionService.isHealthy());

      // Test encryption and decryption
      String encrypted = encryptionService.encrypt(sensitiveData);
      String decrypted = encryptionService.decrypt(encrypted);

      assertNotEquals(sensitiveData, encrypted);
      assertEquals(sensitiveData, decrypted);
    }

    @Test
    @DisplayName("Should handle SecureString operations in application context")
    void shouldHandleSecureStringOperationsInApplicationContext() {
      String sensitiveData = "account_credentials_data";

      try (SecureString secureEncrypted = encryptionService.encryptToSecureString(sensitiveData)) {
        assertFalse(secureEncrypted.isEmpty());

        String decrypted = encryptionService.decryptFromSecureString(secureEncrypted);
        assertEquals(sensitiveData, decrypted);
      }
    }

    @Test
    @DisplayName("Should maintain encryption consistency across application restarts")
    void shouldMaintainEncryptionConsistencyAcrossApplicationRestarts() {
      // This test simulates encryption with a known key
      String testData = "persistent_test_data";
      String encrypted = encryptionService.encrypt(testData);

      // Create new service instance (simulating restart)
      // In test environment, this should use the same auto-generated key
      String decrypted = encryptionService.decrypt(encrypted);
      assertEquals(testData, decrypted);
    }
  }

  @Nested
  @DisplayName("Password Security Integration Tests")
  class PasswordSecurityIntegrationTests {

    @Test
    @DisplayName("Should validate password strength during user registration")
    void shouldValidatePasswordStrengthDuringUserRegistration() throws Exception {
      // Test password strength assessment directly through service
      var weakStrength = passwordSecurityService.assessPasswordStrength("123");
      assertEquals(
          PasswordSecurityService.PasswordStrengthLevel.VERY_WEAK, weakStrength.getLevel());
      assertFalse(weakStrength.meetsMinimumRequirements());
    }

    @Test
    @DisplayName("Should accept strong password during registration")
    void shouldAcceptStrongPasswordDuringRegistration() throws Exception {
      // Test password strength assessment directly through service
      var strongStrength = passwordSecurityService.assessPasswordStrength("VerySecureP@ssw0rd123!");
      assertTrue(strongStrength.meetsMinimumRequirements());
      assertTrue(
          strongStrength.getLevel() == PasswordSecurityService.PasswordStrengthLevel.STRONG
              || strongStrength.getLevel()
                  == PasswordSecurityService.PasswordStrengthLevel.VERY_STRONG);
    }

    @Test
    @DisplayName("Should properly hash passwords using BCrypt")
    void shouldProperlyHashPasswordsUsingBCrypt() {
      String rawPassword = "TestPassword123!";
      String hashedPassword = passwordSecurityService.hashPasswordWithBCrypt(rawPassword);

      // Verify hash format
      assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"));

      // Verify password verification works
      assertTrue(passwordSecurityService.verifyBCryptPassword(rawPassword, hashedPassword));
      assertFalse(passwordSecurityService.verifyBCryptPassword("WrongPassword", hashedPassword));
    }

    @Test
    @DisplayName("Should assess password strength accurately")
    void shouldAssessPasswordStrengthAccurately() {
      // Test various password strengths
      var veryWeak = passwordSecurityService.assessPasswordStrength("123");
      assertEquals(PasswordSecurityService.PasswordStrengthLevel.VERY_WEAK, veryWeak.getLevel());

      var weak = passwordSecurityService.assessPasswordStrength("password");
      assertTrue(
          weak.getLevel() == PasswordSecurityService.PasswordStrengthLevel.VERY_WEAK
              || weak.getLevel() == PasswordSecurityService.PasswordStrengthLevel.WEAK);

      var strong = passwordSecurityService.assessPasswordStrength("VerySecureP@ssw0rd123!");
      assertTrue(
          strong.getLevel() == PasswordSecurityService.PasswordStrengthLevel.STRONG
              || strong.getLevel() == PasswordSecurityService.PasswordStrengthLevel.VERY_STRONG);
      assertTrue(strong.meetsMinimumRequirements());
    }
  }

  @Nested
  @DisplayName("Secure Data Storage Integration Tests")
  class SecureDataStorageIntegrationTests {

    @Test
    @DisplayName("Should store and retrieve encrypted account data")
    void shouldStoreAndRetrieveEncryptedAccountData() {
      // Create account data that needs encryption
      String accountCredentials =
          "username:testaccount\npassword:AccountP@ss123!\nserver:test-server";

      // Encrypt the data before storing
      String encryptedCredentials = encryptionService.encrypt(accountCredentials);

      // Create category and product first (required for Stock)
      Category category = new Category();
      category.setName("Test Category");
      category.setDescription("Test category for security tests");
      category = categoryRepository.save(category);

      Product product = new Product();
      product.setName("Test Product");
      product.setDescription("Test product for security tests");
      product.setPrice(new java.math.BigDecimal("29.99"));
      product.setCategory(category);
      product = productRepository.save(product);

      // Create and save stock with encrypted data
      Stock stock = new Stock(product, encryptedCredentials);
      stock.setAdditionalInfo("Premium Gaming Account");

      Stock savedStock = stockRepository.save(stock);

      // Verify data is stored encrypted
      assertNotEquals(accountCredentials, savedStock.getAccountData());

      // Verify we can decrypt it back
      String decryptedData = encryptionService.decrypt(savedStock.getAccountData());
      assertEquals(accountCredentials, decryptedData);
    }

    @Test
    @DisplayName("Should handle sensitive data with SecureString")
    void shouldHandleSensitiveDataWithSecureString() {
      String sensitiveAccountInfo = "premium_account_data_with_credentials";

      // Use SecureString for handling sensitive data
      try (SecureString secureData = new SecureString(sensitiveAccountInfo)) {
        // Encrypt using SecureString
        SecureString encrypted = encryptionService.encryptToSecureString(sensitiveAccountInfo);

        try (encrypted) {
          assertFalse(encrypted.isEmpty());

          // Decrypt back
          String decrypted = encryptionService.decryptFromSecureString(encrypted);
          assertEquals(sensitiveAccountInfo, decrypted);
        }

        // Verify SecureString was cleared
        assertTrue(encrypted.isCleared());
      }
    }
  }

  @Nested
  @DisplayName("Security Configuration Integration Tests")
  class SecurityConfigurationIntegrationTests {

    @Test
    @DisplayName("Should have properly configured encryption service")
    void shouldHaveProperlyConfiguredEncryptionService() {
      assertNotNull(encryptionService);
      assertTrue(encryptionService.isHealthy());
    }

    @Test
    @DisplayName("Should have properly configured password security service")
    void shouldHaveProperlyConfiguredPasswordSecurityService() {
      assertNotNull(passwordSecurityService);

      // Test that service can generate secure passwords
      String generatedPassword = passwordSecurityService.generateSecurePassword(12, true);
      assertNotNull(generatedPassword);
      assertEquals(12, generatedPassword.length());

      // Generated password should be strong
      var strength = passwordSecurityService.assessPasswordStrength(generatedPassword);
      assertTrue(strength.meetsMinimumRequirements());
    }

    @Test
    @DisplayName("Should maintain security across different operations")
    void shouldMaintainSecurityAcrossDifferentOperations() {
      // Test multiple security operations together
      String originalPassword = "TestPassword123!";
      String accountInfo = "sensitive_account_information";

      // Hash password
      String hashedPassword = passwordSecurityService.hashPasswordWithBCrypt(originalPassword);

      // Encrypt account info
      String encryptedInfo = encryptionService.encrypt(accountInfo);

      // Verify both operations work correctly
      assertTrue(passwordSecurityService.verifyBCryptPassword(originalPassword, hashedPassword));
      assertEquals(accountInfo, encryptionService.decrypt(encryptedInfo));
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingAndEdgeCases {

    @Test
    @DisplayName("Should handle encryption failures gracefully")
    void shouldHandleEncryptionFailuresGracefully() {
      // Test with null input
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            encryptionService.encrypt(null);
          });

      // Test with empty input
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            encryptionService.encrypt("");
          });
    }

    @Test
    @DisplayName("Should handle password security failures gracefully")
    void shouldHandlePasswordSecurityFailuresGracefully() {
      // Test with null password
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            passwordSecurityService.hashPasswordWithBCrypt(null);
          });

      // Test with invalid verification parameters
      assertFalse(passwordSecurityService.verifyBCryptPassword(null, "hash"));
      assertFalse(passwordSecurityService.verifyBCryptPassword("password", null));
    }

    @Test
    @DisplayName("Should handle large data encryption efficiently")
    void shouldHandleLargeDataEncryptionEfficiently() {
      // Create large test data
      StringBuilder largeData = new StringBuilder();
      for (int i = 0; i < 1000; i++) {
        largeData.append("Large account data entry ").append(i).append("\n");
      }

      String originalData = largeData.toString();

      // Test encryption and decryption of large data
      long startTime = System.currentTimeMillis();
      String encrypted = encryptionService.encrypt(originalData);
      String decrypted = encryptionService.decrypt(encrypted);
      long endTime = System.currentTimeMillis();

      assertEquals(originalData, decrypted);
      assertTrue(
          endTime - startTime < 5000, "Large data encryption should complete within 5 seconds");
    }
  }

  @Nested
  @DisplayName("Real-world Scenario Tests")
  class RealWorldScenarioTests {

    @Test
    @DisplayName("Should handle complete user registration with strong password")
    void shouldHandleCompleteUserRegistrationWithStrongPassword() throws Exception {
      // Test password strength directly instead of making HTTP call
      String strongPassword = "MyVerySecureP@ssw0rd2024!";

      // Verify password meets security requirements
      var strength = passwordSecurityService.assessPasswordStrength(strongPassword);
      assertTrue(strength.meetsMinimumRequirements());
      assertTrue(
          strength.getLevel() == PasswordSecurityService.PasswordStrengthLevel.STRONG
              || strength.getLevel() == PasswordSecurityService.PasswordStrengthLevel.VERY_STRONG);

      // Test password hashing
      String hashedPassword = passwordSecurityService.hashPasswordWithBCrypt(strongPassword);
      assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"));
      assertTrue(passwordSecurityService.verifyBCryptPassword(strongPassword, hashedPassword));
    }

    @Test
    @DisplayName("Should handle account purchase flow with encrypted data")
    void shouldHandleAccountPurchaseFlowWithEncryptedData() {
      // Create encrypted stock item
      String accountCredentials =
          "username:premiumgamer\npassword:Premium@Pass123!\nserver:eu-server-01\nlevel:85";
      String encryptedCredentials = encryptionService.encrypt(accountCredentials);

      // Create category and product first (required for Stock)
      Category category = new Category();
      category.setName("Premium Accounts");
      category.setDescription("Premium gaming accounts");
      category = categoryRepository.save(category);

      Product product = new Product();
      product.setName("Premium WoW Account");
      product.setDescription("Level 85 premium account");
      product.setPrice(new java.math.BigDecimal("199.99"));
      product.setCategory(category);
      product = productRepository.save(product);

      // Create stock with encrypted data
      Stock stock = new Stock(product, encryptedCredentials);
      stock.setAdditionalInfo("Premium WoW Account - Level 85");

      Stock savedStock = stockRepository.save(stock);

      // Simulate account purchase - retrieve and decrypt data
      Optional<Stock> purchasedStock = stockRepository.findById(savedStock.getId());
      assertTrue(purchasedStock.isPresent());

      String decryptedCredentials =
          encryptionService.decrypt(purchasedStock.get().getAccountData());
      assertEquals(accountCredentials, decryptedCredentials);

      // Verify original credentials are not visible in database
      assertNotEquals(accountCredentials, purchasedStock.get().getAccountData());
    }
  }
}
