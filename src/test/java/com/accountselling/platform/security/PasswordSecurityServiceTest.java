package com.accountselling.platform.security;

import static org.junit.jupiter.api.Assertions.*;

import com.accountselling.platform.security.PasswordSecurityService.PasswordStrength;
import com.accountselling.platform.security.PasswordSecurityService.PasswordStrengthLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PasswordSecurityService.
 *
 * <p>Unit tests สำหรับ PasswordSecurityService ทดสอบการจัดการรหัสผ่านอย่างปลอดภัย
 */
@DisplayName("PasswordSecurityService Unit Tests")
class PasswordSecurityServiceTest {

  private PasswordSecurityService passwordSecurityService;

  @BeforeEach
  void setUp() {
    passwordSecurityService = new PasswordSecurityService();
  }

  @Nested
  @DisplayName("BCrypt Password Hashing Tests")
  class BCryptPasswordHashingTests {

    @Test
    @DisplayName("Should hash password with BCrypt successfully")
    void shouldHashPasswordWithBCryptSuccessfully() {
      String rawPassword = "TestPassword123!";

      String hashedPassword = passwordSecurityService.hashPasswordWithBCrypt(rawPassword);

      assertNotNull(hashedPassword);
      assertNotEquals(rawPassword, hashedPassword);
      assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"));
    }

    @Test
    @DisplayName("Should generate different hashes for same password")
    void shouldGenerateDifferentHashesForSamePassword() {
      String rawPassword = "SamePassword123!";

      String hash1 = passwordSecurityService.hashPasswordWithBCrypt(rawPassword);
      String hash2 = passwordSecurityService.hashPasswordWithBCrypt(rawPassword);

      assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should verify BCrypt password successfully")
    void shouldVerifyBCryptPasswordSuccessfully() {
      String rawPassword = "VerifyTest123!";
      String hashedPassword = passwordSecurityService.hashPasswordWithBCrypt(rawPassword);

      assertTrue(passwordSecurityService.verifyBCryptPassword(rawPassword, hashedPassword));
      assertFalse(passwordSecurityService.verifyBCryptPassword("WrongPassword", hashedPassword));
    }

    @Test
    @DisplayName("Should handle null values in BCrypt verification")
    void shouldHandleNullValuesInBCryptVerification() {
      assertFalse(passwordSecurityService.verifyBCryptPassword(null, "hash"));
      assertFalse(passwordSecurityService.verifyBCryptPassword("password", null));
      assertFalse(passwordSecurityService.verifyBCryptPassword(null, null));
    }

    @Test
    @DisplayName("Should throw exception for null password in BCrypt hashing")
    void shouldThrowExceptionForNullPasswordInBCryptHashing() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            passwordSecurityService.hashPasswordWithBCrypt(null);
          });
    }
  }

  @Nested
  @DisplayName("Argon2 Password Hashing Tests")
  class Argon2PasswordHashingTests {

    @Test
    @DisplayName("Should hash password with Argon2 successfully")
    void shouldHashPasswordWithArgon2Successfully() {
      String rawPassword = "TestPassword123!";

      String hashedPassword = passwordSecurityService.hashPasswordWithArgon2(rawPassword);

      assertNotNull(hashedPassword);
      assertNotEquals(rawPassword, hashedPassword);
      assertTrue(hashedPassword.startsWith("$argon2"));
    }

    @Test
    @DisplayName("Should verify Argon2 password successfully")
    void shouldVerifyArgon2PasswordSuccessfully() {
      String rawPassword = "VerifyTest123!";
      String hashedPassword = passwordSecurityService.hashPasswordWithArgon2(rawPassword);

      assertTrue(passwordSecurityService.verifyArgon2Password(rawPassword, hashedPassword));
      assertFalse(passwordSecurityService.verifyArgon2Password("WrongPassword", hashedPassword));
    }

    @Test
    @DisplayName("Should handle null values in Argon2 verification")
    void shouldHandleNullValuesInArgon2Verification() {
      assertFalse(passwordSecurityService.verifyArgon2Password(null, "hash"));
      assertFalse(passwordSecurityService.verifyArgon2Password("password", null));
      assertFalse(passwordSecurityService.verifyArgon2Password(null, null));
    }
  }

  @Nested
  @DisplayName("Password Strength Assessment Tests")
  class PasswordStrengthAssessmentTests {

    @Test
    @DisplayName("Should assess very weak passwords correctly")
    void shouldAssessVeryWeakPasswordsCorrectly() {
      PasswordStrength result = passwordSecurityService.assessPasswordStrength("123");
      assertEquals(PasswordStrengthLevel.VERY_WEAK, result.getLevel());
      assertFalse(result.meetsMinimumRequirements());
    }

    @Test
    @DisplayName("Should assess weak passwords correctly")
    void shouldAssessWeakPasswordsCorrectly() {
      PasswordStrength result = passwordSecurityService.assessPasswordStrength("password");
      assertEquals(PasswordStrengthLevel.VERY_WEAK, result.getLevel());
      assertFalse(result.meetsMinimumRequirements());
    }

    @Test
    @DisplayName("Should assess moderate passwords correctly")
    void shouldAssessModeratePasswordsCorrectly() {
      PasswordStrength result = passwordSecurityService.assessPasswordStrength("Password123");
      assertEquals(PasswordStrengthLevel.WEAK, result.getLevel());
      assertFalse(result.meetsMinimumRequirements()); // Missing special characters makes it weak
    }

    @Test
    @DisplayName("Should assess strong passwords correctly")
    void shouldAssessStrongPasswordsCorrectly() {
      PasswordStrength result = passwordSecurityService.assessPasswordStrength("Password123!");
      assertEquals(
          PasswordStrengthLevel.MODERATE,
          result.getLevel()); // Still moderate due to length and patterns
      assertTrue(result.meetsMinimumRequirements());
    }

    @Test
    @DisplayName("Should assess very strong passwords correctly")
    void shouldAssessVeryStrongPasswordsCorrectly() {
      PasswordStrength result =
          passwordSecurityService.assessPasswordStrength("MyVerySecureP@ssw0rd2024!");
      assertEquals(PasswordStrengthLevel.VERY_STRONG, result.getLevel());
      assertTrue(result.meetsMinimumRequirements());
    }

    @Test
    @DisplayName("Should detect common password patterns")
    void shouldDetectCommonPasswordPatterns() {
      PasswordStrength result1 = passwordSecurityService.assessPasswordStrength("password123");
      assertTrue(result1.getFeedback().contains("common"));

      PasswordStrength result2 = passwordSecurityService.assessPasswordStrength("admin123");
      assertTrue(result2.getFeedback().contains("common"));
    }

    @Test
    @DisplayName("Should detect repetitive characters")
    void shouldDetectRepetitiveCharacters() {
      PasswordStrength result = passwordSecurityService.assessPasswordStrength("Passsswooord123!");
      assertTrue(result.getFeedback().contains("repetitive"));
    }

    @Test
    @DisplayName("Should detect sequential characters")
    void shouldDetectSequentialCharacters() {
      PasswordStrength result1 = passwordSecurityService.assessPasswordStrength("Password123abc!");
      assertTrue(result1.getFeedback().contains("sequential"));

      PasswordStrength result2 = passwordSecurityService.assessPasswordStrength("Password12321!");
      assertTrue(result2.getFeedback().contains("sequential"));
    }

    @Test
    @DisplayName("Should handle null password in strength assessment")
    void shouldHandleNullPasswordInStrengthAssessment() {
      PasswordStrength result = passwordSecurityService.assessPasswordStrength(null);
      assertEquals(PasswordStrengthLevel.VERY_WEAK, result.getLevel());
      assertTrue(result.getFeedback().contains("cannot be null"));
    }

    @Test
    @DisplayName("Should provide helpful feedback for weak passwords")
    void shouldProvideHelpfulFeedbackForWeakPasswords() {
      PasswordStrength result = passwordSecurityService.assessPasswordStrength("short");

      String feedback = result.getFeedback();
      assertTrue(
          feedback.contains("8 characters")
              || feedback.contains("uppercase")
              || feedback.contains("numbers")
              || feedback.contains("special"));
    }
  }

  @Nested
  @DisplayName("Password Generation Tests")
  class PasswordGenerationTests {

    @Test
    @DisplayName("Should generate secure password with specified length")
    void shouldGenerateSecurePasswordWithSpecifiedLength() {
      String password = passwordSecurityService.generateSecurePassword(12, true);

      assertEquals(12, password.length());
      assertNotNull(password);
    }

    @Test
    @DisplayName("Should generate password with special characters when requested")
    void shouldGeneratePasswordWithSpecialCharactersWhenRequested() {
      String password = passwordSecurityService.generateSecurePassword(16, true);

      // Should contain at least one special character
      assertTrue(password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*"));
    }

    @Test
    @DisplayName("Should generate password without special characters when not requested")
    void shouldGeneratePasswordWithoutSpecialCharactersWhenNotRequested() {
      String password = passwordSecurityService.generateSecurePassword(16, false);

      // Should not contain special characters but should have letters and numbers
      assertTrue(password.matches(".*[A-Z].*")); // Uppercase
      assertTrue(password.matches(".*[a-z].*")); // Lowercase
      assertTrue(password.matches(".*\\d.*")); // Digits
      assertFalse(password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")); // No special chars
    }

    @Test
    @DisplayName("Should generate different passwords each time")
    void shouldGenerateDifferentPasswordsEachTime() {
      String password1 = passwordSecurityService.generateSecurePassword(12, true);
      String password2 = passwordSecurityService.generateSecurePassword(12, true);

      assertNotEquals(password1, password2);
    }

    @Test
    @DisplayName("Should throw exception for invalid password length")
    void shouldThrowExceptionForInvalidPasswordLength() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            passwordSecurityService.generateSecurePassword(7, true); // Too short
          });

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            passwordSecurityService.generateSecurePassword(129, true); // Too long
          });
    }

    @Test
    @DisplayName("Generated password should have high strength")
    void generatedPasswordShouldHaveHighStrength() {
      String password = passwordSecurityService.generateSecurePassword(16, true);
      PasswordStrength strength = passwordSecurityService.assessPasswordStrength(password);

      assertTrue(
          strength.getLevel() == PasswordStrengthLevel.STRONG
              || strength.getLevel() == PasswordStrengthLevel.VERY_STRONG);
      assertTrue(strength.meetsMinimumRequirements());
    }
  }

  @Nested
  @DisplayName("Memory Security Tests")
  class MemorySecurityTests {

    @Test
    @DisplayName("Should clear password character array safely")
    void shouldClearPasswordCharArraySafely() {
      char[] password = "SecretPassword123!".toCharArray();

      // Verify password has content
      assertTrue(password[0] != '\0');

      passwordSecurityService.clearPassword(password);

      // Verify all characters are cleared
      for (char c : password) {
        assertEquals('\0', c);
      }
    }

    @Test
    @DisplayName("Should handle null password array in clear method")
    void shouldHandleNullPasswordArrayInClearMethod() {
      assertDoesNotThrow(
          () -> {
            passwordSecurityService.clearPassword(null);
          });
    }

    @Test
    @DisplayName("Should handle empty password array in clear method")
    void shouldHandleEmptyPasswordArrayInClearMethod() {
      char[] emptyArray = new char[0];
      assertDoesNotThrow(
          () -> {
            passwordSecurityService.clearPassword(emptyArray);
          });
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle moderately long passwords in hashing")
    void shouldHandleModeratelyLongPasswordsInHashing() {
      // Create a password that's long but within limits (under 128 chars)
      String longPassword =
          "VeryLongButValidPassword123!@#$%^&*()_+[]{}|;:,.<>?".repeat(2); // ~100 chars

      // Trim to ensure it's under 128 characters
      final String password =
          longPassword.length() > 120 ? longPassword.substring(0, 120) + "!" : longPassword;

      assertDoesNotThrow(
          () -> {
            String hashed = passwordSecurityService.hashPasswordWithBCrypt(password);
            assertTrue(passwordSecurityService.verifyBCryptPassword(password, hashed));
          });
    }

    @Test
    @DisplayName("Should reject extremely long passwords")
    void shouldRejectExtremelyLongPasswords() {
      StringBuilder extremelyLongPassword = new StringBuilder();
      for (int i = 0; i < 1000; i++) {
        extremelyLongPassword.append("VeryLongPassword123!");
      }

      String password = extremelyLongPassword.toString();

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            passwordSecurityService.hashPasswordWithBCrypt(password);
          });
    }

    @Test
    @DisplayName("Should handle passwords with only Unicode characters")
    void shouldHandlePasswordsWithOnlyUnicodeCharacters() {
      String unicodePassword = "密码世界🔐🎮";

      String hashed = passwordSecurityService.hashPasswordWithBCrypt(unicodePassword);
      assertTrue(passwordSecurityService.verifyBCryptPassword(unicodePassword, hashed));

      PasswordStrength strength = passwordSecurityService.assessPasswordStrength(unicodePassword);
      assertNotNull(strength);
    }

    @Test
    @DisplayName("Should handle malformed BCrypt hashes gracefully")
    void shouldHandleMalformedBCryptHashesGracefully() {
      String malformedHash = "not-a-valid-bcrypt-hash";

      assertFalse(passwordSecurityService.verifyBCryptPassword("password", malformedHash));
    }
  }

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should handle multiple concurrent password operations")
    void shouldHandleMultipleConcurrentPasswordOperations() {
      int threadCount = 10;
      Thread[] threads = new Thread[threadCount];
      boolean[] results = new boolean[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    String password = "TestPassword" + threadIndex + "!";
                    String hashed = passwordSecurityService.hashPasswordWithBCrypt(password);
                    boolean verified =
                        passwordSecurityService.verifyBCryptPassword(password, hashed);
                    PasswordStrength strength =
                        passwordSecurityService.assessPasswordStrength(password);

                    results[threadIndex] = verified && strength != null;
                  } catch (Exception e) {
                    results[threadIndex] = false;
                  }
                });
      }

      // Start all threads
      for (Thread thread : threads) {
        thread.start();
      }

      // Wait for completion
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
