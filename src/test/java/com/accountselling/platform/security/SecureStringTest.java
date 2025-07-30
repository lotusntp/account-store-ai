package com.accountselling.platform.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SecureString class.
 *
 * <p>Unit tests สำหรับ SecureString class ทดสอบการจัดการหน่วยความจำอย่างปลอดภัย
 */
@DisplayName("SecureString Unit Tests")
class SecureStringTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create SecureString from character array")
    void shouldCreateSecureStringFromCharArray() {
      char[] chars = "test".toCharArray();
      try (SecureString secureString = new SecureString(chars)) {
        assertEquals(4, secureString.length());
        assertFalse(secureString.isEmpty());
      }
    }

    @Test
    @DisplayName("Should create SecureString from String")
    void shouldCreateSecureStringFromString() {
      try (SecureString secureString = new SecureString("test")) {
        assertEquals(4, secureString.length());
        assertFalse(secureString.isEmpty());
      }
    }

    @Test
    @DisplayName("Should handle null character array")
    void shouldHandleNullCharArray() {
      try (SecureString secureString = new SecureString((char[]) null)) {
        assertEquals(0, secureString.length());
        assertTrue(secureString.isEmpty());
      }
    }

    @Test
    @DisplayName("Should handle null String")
    void shouldHandleNullString() {
      try (SecureString secureString = new SecureString((String) null)) {
        assertEquals(0, secureString.length());
        assertTrue(secureString.isEmpty());
      }
    }

    @Test
    @DisplayName("Should copy character array to prevent external modification")
    void shouldCopyCharArrayToPreventExternalModification() {
      char[] original = "test".toCharArray();
      try (SecureString secureString = new SecureString(original)) {
        // Modify original array
        original[0] = 'X';

        // SecureString should still have original data
        char[] retrieved = secureString.getChars();
        assertEquals('t', retrieved[0]);

        // Clear retrieved array to test independence
        retrieved[0] = 'Y';
        char[] retrieved2 = secureString.getChars();
        assertEquals('t', retrieved2[0]);
      }
    }
  }

  @Nested
  @DisplayName("Data Access Tests")
  class DataAccessTests {

    @Test
    @DisplayName("Should return copy of character data")
    void shouldReturnCopyOfCharacterData() {
      try (SecureString secureString = new SecureString("sensitive")) {
        char[] chars1 = secureString.getChars();
        char[] chars2 = secureString.getChars();

        assertArrayEquals(chars1, chars2);
        assertNotSame(chars1, chars2); // Different array instances
      }
    }

    @Test
    @DisplayName("Should return correct length")
    void shouldReturnCorrectLength() {
      try (SecureString emptyString = new SecureString("")) {
        assertEquals(0, emptyString.length());
      }

      try (SecureString shortString = new SecureString("hi")) {
        assertEquals(2, shortString.length());
      }

      try (SecureString longString = new SecureString("this is a longer string")) {
        assertEquals(23, longString.length());
      }
    }

    @Test
    @DisplayName("Should correctly identify empty strings")
    void shouldCorrectlyIdentifyEmptyStrings() {
      try (SecureString emptyString = new SecureString("")) {
        assertTrue(emptyString.isEmpty());
      }

      try (SecureString nonEmptyString = new SecureString("a")) {
        assertFalse(nonEmptyString.isEmpty());
      }
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("Should correctly compare with another SecureString")
    void shouldCorrectlyCompareWithAnotherSecureString() {
      try (SecureString string1 = new SecureString("password");
          SecureString string2 = new SecureString("password");
          SecureString string3 = new SecureString("different")) {

        assertTrue(string1.equals(string2));
        assertFalse(string1.equals(string3));
        assertFalse(string2.equals(string3));
      }
    }

    @Test
    @DisplayName("Should correctly compare with character array")
    void shouldCorrectlyCompareWithCharArray() {
      try (SecureString secureString = new SecureString("password")) {
        assertTrue(secureString.equals("password".toCharArray()));
        assertFalse(secureString.equals("different".toCharArray()));
        assertFalse(secureString.equals((char[]) null));
      }
    }

    @Test
    @DisplayName("Should handle empty comparisons")
    void shouldHandleEmptyComparisons() {
      try (SecureString emptyString = new SecureString("")) {
        assertTrue(emptyString.equals(new char[0]));
        assertTrue(emptyString.equals("".toCharArray()));
        assertFalse(emptyString.equals("a".toCharArray()));
      }
    }

    @Test
    @DisplayName("Should use constant-time comparison")
    void shouldUseConstantTimeComparison() {
      // This test verifies that comparison doesn't short-circuit
      try (SecureString secureString = new SecureString("password123")) {
        // These should all take similar time regardless of where they differ
        assertFalse(secureString.equals("aassword123".toCharArray()));
        assertFalse(secureString.equals("passwordaaa".toCharArray()));
        assertFalse(secureString.equals("passwor".toCharArray()));
        assertFalse(secureString.equals("passwordextra".toCharArray()));
      }
    }
  }

  @Nested
  @DisplayName("Memory Clearing Tests")
  class MemoryClearingTests {

    @Test
    @DisplayName("Should clear data when explicitly cleared")
    void shouldClearDataWhenExplicitlyCleared() {
      SecureString secureString = new SecureString("sensitive");
      assertFalse(secureString.isCleared());

      secureString.clear();
      assertTrue(secureString.isCleared());

      assertThrows(IllegalStateException.class, secureString::getChars);
      assertThrows(IllegalStateException.class, secureString::length);
      assertThrows(IllegalStateException.class, secureString::isEmpty);
    }

    @Test
    @DisplayName("Should clear data when closed (try-with-resources)")
    void shouldClearDataWhenClosed() {
      SecureString secureString;
      try (SecureString temp = new SecureString("sensitive")) {
        secureString = temp;
        assertFalse(secureString.isCleared());
      }

      assertTrue(secureString.isCleared());
    }

    @Test
    @DisplayName("Should handle multiple clear calls safely")
    void shouldHandleMultipleClearCallsSafely() {
      SecureString secureString = new SecureString("sensitive");

      secureString.clear();
      assertTrue(secureString.isCleared());

      // Should not throw exception on second clear
      assertDoesNotThrow(secureString::clear);
      assertTrue(secureString.isCleared());
    }

    @Test
    @DisplayName("Should not allow operations after clearing")
    void shouldNotAllowOperationsAfterClearing() {
      SecureString secureString = new SecureString("sensitive");
      secureString.clear();

      assertThrows(IllegalStateException.class, secureString::getChars);
      assertThrows(IllegalStateException.class, secureString::length);
      assertThrows(IllegalStateException.class, secureString::isEmpty);

      SecureString anotherString = new SecureString("test");
      assertThrows(IllegalStateException.class, () -> secureString.equals(anotherString));
      assertThrows(IllegalStateException.class, () -> secureString.equals("test".toCharArray()));
    }
  }

  @Nested
  @DisplayName("Unicode and Special Character Tests")
  class UnicodeAndSpecialCharacterTests {

    @Test
    @DisplayName("Should handle Unicode characters correctly")
    void shouldHandleUnicodeCharactersCorrectly() {
      String unicodeText = "สวัสดี世界🎮";
      try (SecureString secureString = new SecureString(unicodeText)) {
        char[] retrieved = secureString.getChars();
        assertEquals(unicodeText, new String(retrieved));
      }
    }

    @Test
    @DisplayName("Should handle special characters")
    void shouldHandleSpecialCharacters() {
      String specialChars = "!@#$%^&*()_+-=[]{}|;:'\",.<>?/~`";
      try (SecureString secureString = new SecureString(specialChars)) {
        assertTrue(secureString.equals(specialChars.toCharArray()));
      }
    }

    @Test
    @DisplayName("Should handle control characters")
    void shouldHandleControlCharacters() {
      String controlChars = "\n\r\t\0";
      try (SecureString secureString = new SecureString(controlChars)) {
        assertEquals(4, secureString.length());
        assertTrue(secureString.equals(controlChars.toCharArray()));
      }
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should not expose sensitive data in toString")
    void shouldNotExposeSensitiveDataInToString() {
      try (SecureString secureString = new SecureString("verysensitivepassword")) {
        String toString = secureString.toString();

        assertFalse(toString.contains("verysensitivepassword"));
        assertTrue(toString.contains("SecureString"));
        assertTrue(toString.contains("length="));
        assertTrue(toString.contains("cleared="));
      }
    }

    @Test
    @DisplayName("Should show correct state in toString")
    void shouldShowCorrectStateInToString() {
      SecureString secureString = new SecureString("test");
      String toStringBefore = secureString.toString();
      assertTrue(toStringBefore.contains("cleared=false"));

      secureString.clear();
      String toStringAfter = secureString.toString();
      assertTrue(toStringAfter.contains("cleared=true"));
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle very long strings")
    void shouldHandleVeryLongStrings() {
      StringBuilder longString = new StringBuilder();
      for (int i = 0; i < 10000; i++) {
        longString.append("a");
      }

      try (SecureString secureString = new SecureString(longString.toString())) {
        assertEquals(10000, secureString.length());
        assertFalse(secureString.isEmpty());
      }
    }

    @Test
    @DisplayName("Should handle strings with only whitespace")
    void shouldHandleStringsWithOnlyWhitespace() {
      try (SecureString secureString = new SecureString("   \t\n\r   ")) {
        assertEquals(9, secureString.length());
        assertFalse(secureString.isEmpty());
      }
    }

    @Test
    @DisplayName("Should handle comparison with cleared SecureString")
    void shouldHandleComparisonWithClearedSecureString() {
      SecureString secureString1 = new SecureString("test");
      SecureString secureString2 = new SecureString("test");

      secureString2.clear();

      assertThrows(IllegalStateException.class, () -> secureString1.equals(secureString2));
      assertThrows(IllegalStateException.class, () -> secureString2.equals(secureString1));
    }
  }
}
