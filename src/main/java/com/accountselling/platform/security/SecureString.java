package com.accountselling.platform.security;

import java.util.Arrays;

/**
 * Secure memory handling for sensitive character data.
 *
 * <p>คลาสสำหรับจัดการข้อมูลที่มีความสำคัญในหน่วยความจำอย่างปลอดภัย ช่วยป้องกัน memory dump และ
 * garbage collection exposure
 */
public class SecureString implements AutoCloseable {

  private char[] chars;
  private boolean cleared = false;

  /**
   * Creates a SecureString from a character array. The original array is copied to prevent external
   * modification.
   *
   * @param chars The character data to store securely
   */
  public SecureString(char[] chars) {
    if (chars == null) {
      this.chars = new char[0];
    } else {
      this.chars = Arrays.copyOf(chars, chars.length);
    }
  }

  /**
   * Creates a SecureString from a regular String. Note: This should be avoided when possible as
   * String objects remain in memory.
   *
   * @param str The string to store securely
   */
  public SecureString(String str) {
    if (str == null) {
      this.chars = new char[0];
    } else {
      this.chars = str.toCharArray();
    }
  }

  /**
   * Gets a copy of the character array. The caller is responsible for clearing the returned array.
   *
   * @return A copy of the stored character data
   * @throws IllegalStateException if the SecureString has been cleared
   */
  public char[] getChars() {
    ensureNotCleared();
    return Arrays.copyOf(chars, chars.length);
  }

  /**
   * Gets the length of the stored data.
   *
   * @return The length of the character array
   * @throws IllegalStateException if the SecureString has been cleared
   */
  public int length() {
    ensureNotCleared();
    return chars.length;
  }

  /**
   * Checks if the SecureString is empty.
   *
   * @return true if the stored data is empty
   * @throws IllegalStateException if the SecureString has been cleared
   */
  public boolean isEmpty() {
    ensureNotCleared();
    return chars.length == 0;
  }

  /**
   * Compares this SecureString with another SecureString for equality. Uses constant-time
   * comparison to prevent timing attacks.
   *
   * @param other The SecureString to compare with
   * @return true if both SecureStrings contain the same data
   */
  public boolean equals(SecureString other) {
    if (other == null) {
      return false;
    }

    ensureNotCleared();
    other.ensureNotCleared();

    if (this.chars.length != other.chars.length) {
      return false;
    }

    // Constant-time comparison to prevent timing attacks
    int result = 0;
    for (int i = 0; i < this.chars.length; i++) {
      result |= this.chars[i] ^ other.chars[i];
    }

    return result == 0;
  }

  /**
   * Compares this SecureString with a character array for equality. Uses constant-time comparison
   * to prevent timing attacks.
   *
   * @param chars The character array to compare with
   * @return true if the SecureString contains the same data as the array
   */
  public boolean equals(char[] chars) {
    if (chars == null) {
      return this.chars.length == 0;
    }

    ensureNotCleared();

    if (this.chars.length != chars.length) {
      return false;
    }

    // Constant-time comparison to prevent timing attacks
    int result = 0;
    for (int i = 0; i < this.chars.length; i++) {
      result |= this.chars[i] ^ chars[i];
    }

    return result == 0;
  }

  /**
   * Clears the stored data by overwriting it with zeros. After calling this method, the
   * SecureString cannot be used anymore.
   */
  public void clear() {
    if (!cleared && chars != null) {
      Arrays.fill(chars, '\0');
      cleared = true;
    }
  }

  /**
   * Checks if the SecureString has been cleared.
   *
   * @return true if the data has been cleared
   */
  public boolean isCleared() {
    return cleared;
  }

  /** AutoCloseable implementation that automatically clears the data. */
  @Override
  public void close() {
    clear();
  }

  /**
   * Finalizer that clears data if not already cleared. This is a safety net, but explicit clearing
   * is preferred.
   */
  @Override
  protected void finalize() throws Throwable {
    try {
      clear();
    } finally {
      super.finalize();
    }
  }

  private void ensureNotCleared() {
    if (cleared) {
      throw new IllegalStateException("SecureString has been cleared and cannot be used");
    }
  }

  @Override
  public String toString() {
    return "SecureString[length=" + (cleared ? 0 : chars.length) + ", cleared=" + cleared + "]";
  }
}
