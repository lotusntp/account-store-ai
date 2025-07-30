package com.accountselling.platform.security;

import static org.junit.jupiter.api.Assertions.*;

import com.accountselling.platform.exception.security.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for EncryptionService.
 *
 * <p>Unit tests สำหรับ EncryptionService ทดสอบการเข้ารหัสและถอดรหัสข้อมูลที่สำคัญ
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionService Unit Tests")
class EncryptionServiceTest {

  private EncryptionService encryptionService;

  @BeforeEach
  void setUp() {
    // Use auto-generated key for testing
    encryptionService = new EncryptionService("");
  }

  @Nested
  @DisplayName("Basic Encryption/Decryption Tests")
  class BasicEncryptionDecryptionTests {

    @Test
    @DisplayName("Should encrypt and decrypt simple text successfully")
    void shouldEncryptAndDecryptSimpleText() {
      String plaintext = "Hello, World!";

      String encrypted = encryptionService.encrypt(plaintext);
      String decrypted = encryptionService.decrypt(encrypted);

      assertNotNull(encrypted);
      assertNotEquals(plaintext, encrypted);
      assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Should encrypt and decrypt complex sensitive data")
    void shouldEncryptAndDecryptComplexSensitiveData() {
      String sensitiveData =
          "username:admin\npassword:P@ssw0rd123!\nemail:admin@example.com\nserver:game-server-01";

      String encrypted = encryptionService.encrypt(sensitiveData);
      String decrypted = encryptionService.decrypt(encrypted);

      assertNotNull(encrypted);
      assertNotEquals(sensitiveData, encrypted);
      assertEquals(sensitiveData, decrypted);
    }

    @Test
    @DisplayName("Should encrypt and decrypt Unicode characters")
    void shouldEncryptAndDecryptUnicodeCharacters() {
      String unicodeText = "สวัสดี世界 🎮 Ñiño café";

      String encrypted = encryptionService.encrypt(unicodeText);
      String decrypted = encryptionService.decrypt(encrypted);

      assertEquals(unicodeText, decrypted);
    }

    @Test
    @DisplayName("Should encrypt and decrypt empty string")
    void shouldHandleEmptyString() {
      String emptyString = "";

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            encryptionService.encrypt(emptyString);
          });
    }

    @Test
    @DisplayName("Should encrypt and decrypt very long text")
    void shouldEncryptAndDecryptLongText() {
      StringBuilder longText = new StringBuilder();
      for (int i = 0; i < 1000; i++) {
        longText.append("This is a very long text for testing encryption performance. ");
      }
      String plaintext = longText.toString();

      String encrypted = encryptionService.encrypt(plaintext);
      String decrypted = encryptionService.decrypt(encrypted);

      assertEquals(plaintext, decrypted);
    }
  }

  @Nested
  @DisplayName("Security Tests")
  class SecurityTests {

    @Test
    @DisplayName("Should generate different encrypted values for same input")
    void shouldGenerateDifferentEncryptedValues() {
      String plaintext = "test data";

      String encrypted1 = encryptionService.encrypt(plaintext);
      String encrypted2 = encryptionService.encrypt(plaintext);

      assertNotEquals(
          encrypted1,
          encrypted2,
          "Same plaintext should produce different encrypted values due to random IV");

      // But both should decrypt to the same value
      assertEquals(plaintext, encryptionService.decrypt(encrypted1));
      assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Should fail to decrypt with tampered data")
    void shouldFailToDecryptTamperedData() {
      String plaintext = "sensitive information";
      String encrypted = encryptionService.encrypt(plaintext);

      // Tamper with the encrypted data
      String tamperedEncrypted = encrypted.substring(0, encrypted.length() - 4) + "XXXX";

      assertThrows(
          EncryptionException.class,
          () -> {
            encryptionService.decrypt(tamperedEncrypted);
          });
    }

    @Test
    @DisplayName("Should fail to decrypt with invalid Base64")
    void shouldFailToDecryptInvalidBase64() {
      String invalidBase64 = "This is not valid Base64!@#$%";

      assertThrows(
          EncryptionException.class,
          () -> {
            encryptionService.decrypt(invalidBase64);
          });
    }

    @Test
    @DisplayName("Should fail to decrypt with too short data")
    void shouldFailToDecryptTooShortData() {
      String tooShortData = "SGVsbG8="; // Valid Base64 but too short for our encryption format

      assertThrows(
          EncryptionException.class,
          () -> {
            encryptionService.decrypt(tooShortData);
          });
    }
  }

  @Nested
  @DisplayName("SecureString Integration Tests")
  class SecureStringIntegrationTests {

    @Test
    @DisplayName("Should encrypt to SecureString and decrypt back")
    void shouldEncryptToSecureStringAndDecryptBack() {
      String plaintext = "sensitive account data";

      try (SecureString secureEncrypted = encryptionService.encryptToSecureString(plaintext)) {
        String decrypted = encryptionService.decryptFromSecureString(secureEncrypted);
        assertEquals(plaintext, decrypted);
      }
    }

    @Test
    @DisplayName("Should handle SecureString with complex data")
    void shouldHandleSecureStringWithComplexData() {
      String complexData =
          "{\n"
              + "  \"username\": \"player123\",\n"
              + "  \"password\": \"SecureP@ss!\",\n"
              + "  \"server\": \"asia-server-01\"\n"
              + "}";

      try (SecureString secureEncrypted = encryptionService.encryptToSecureString(complexData)) {
        assertFalse(secureEncrypted.isEmpty());
        assertFalse(secureEncrypted.isCleared());

        String decrypted = encryptionService.decryptFromSecureString(secureEncrypted);
        assertEquals(complexData, decrypted);
      }
    }
  }

  @Nested
  @DisplayName("Health Check Tests")
  class HealthCheckTests {

    @Test
    @DisplayName("Should pass health check with valid service")
    void shouldPassHealthCheckWithValidService() {
      assertTrue(encryptionService.isHealthy());
    }

    @Test
    @DisplayName("Should pass health check multiple times")
    void shouldPassHealthCheckMultipleTimes() {
      for (int i = 0; i < 10; i++) {
        assertTrue(encryptionService.isHealthy(), "Health check should pass on iteration " + i);
      }
    }
  }

  @Nested
  @DisplayName("Key Management Tests")
  class KeyManagementTests {

    @Test
    @DisplayName("Should generate new encryption key")
    void shouldGenerateNewEncryptionKey() {
      String newKey = encryptionService.generateNewKey();

      assertNotNull(newKey);
      assertFalse(newKey.isEmpty());
      assertTrue(
          newKey.length() > 40, "Base64 encoded 256-bit key should be longer than 40 characters");
    }

    @Test
    @DisplayName("Should generate different keys each time")
    void shouldGenerateDifferentKeysEachTime() {
      String key1 = encryptionService.generateNewKey();
      String key2 = encryptionService.generateNewKey();

      assertNotEquals(key1, key2);
    }

    @Test
    @DisplayName("Should create new service with generated key")
    void shouldCreateNewServiceWithGeneratedKey() {
      String newKey = encryptionService.generateNewKey();
      EncryptionService newService = new EncryptionService(newKey);

      assertTrue(newService.isHealthy());

      String plaintext = "test with new key";
      String encrypted = newService.encrypt(plaintext);
      String decrypted = newService.decrypt(encrypted);

      assertEquals(plaintext, decrypted);
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw IllegalArgumentException for null plaintext")
    void shouldThrowExceptionForNullPlaintext() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            encryptionService.encrypt(null);
          });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null encrypted data")
    void shouldThrowExceptionForNullEncryptedData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            encryptionService.decrypt(null);
          });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for empty plaintext")
    void shouldThrowExceptionForEmptyPlaintext() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            encryptionService.encrypt("");
          });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for empty encrypted data")
    void shouldThrowExceptionForEmptyEncryptedData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            encryptionService.decrypt("");
          });
    }
  }

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should handle multiple concurrent encryptions")
    void shouldHandleConcurrentEncryptions() {
      String plaintext = "concurrent test data";
      int threadCount = 10;
      int operationsPerThread = 100;

      Thread[] threads = new Thread[threadCount];
      boolean[] results = new boolean[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    for (int j = 0; j < operationsPerThread; j++) {
                      String encrypted =
                          encryptionService.encrypt(plaintext + "_" + threadIndex + "_" + j);
                      String decrypted = encryptionService.decrypt(encrypted);
                      if (!decrypted.equals(plaintext + "_" + threadIndex + "_" + j)) {
                        results[threadIndex] = false;
                        return;
                      }
                    }
                    results[threadIndex] = true;
                  } catch (Exception e) {
                    results[threadIndex] = false;
                  }
                });
      }

      // Start all threads
      for (Thread thread : threads) {
        thread.start();
      }

      // Wait for all threads to complete
      for (Thread thread : threads) {
        try {
          thread.join(10000); // 10 second timeout
        } catch (InterruptedException e) {
          fail("Thread was interrupted");
        }
      }

      // Check all results
      for (int i = 0; i < threadCount; i++) {
        assertTrue(results[i], "Thread " + i + " failed");
      }
    }
  }
}
