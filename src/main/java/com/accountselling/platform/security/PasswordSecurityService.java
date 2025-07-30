package com.accountselling.platform.security;

import java.security.SecureRandom;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for secure password handling including validation, hashing, and strength checking.
 *
 * <p>บริการสำหรับการจัดการรหัสผ่านอย่างปลอดภัย รวมถึงการตรวจสอบความแข็งแกร่ง การเข้ารหัสด้วย hash
 * algorithm และการตรวจสอบความถูกต้อง
 */
@Service
@Slf4j
public class PasswordSecurityService {

  // Password strength patterns
  private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
  private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
  private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
  private static final Pattern SPECIAL_CHAR_PATTERN =
      Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>?].*");
  private static final Pattern COMMON_PATTERNS =
      Pattern.compile(".*(password|123456|qwerty|admin|root|user).*", Pattern.CASE_INSENSITIVE);

  // Minimum password requirements
  private static final int MIN_PASSWORD_LENGTH = 8;
  private static final int MAX_PASSWORD_LENGTH = 128;
  private static final int RECOMMENDED_MIN_LENGTH = 12;

  private final PasswordEncoder bcryptEncoder;
  private final PasswordEncoder argon2Encoder;
  private final SecureRandom secureRandom;

  public PasswordSecurityService() {
    // Configure BCrypt with strength 12 (recommended for 2024)
    this.bcryptEncoder = new BCryptPasswordEncoder(12, new SecureRandom());

    // Configure Argon2 with recommended parameters
    this.argon2Encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);

    this.secureRandom = new SecureRandom();
    log.info("PasswordSecurityService initialized with BCrypt(12) and Argon2");
  }

  /**
   * Hashes a password using BCrypt with salt.
   *
   * @param rawPassword The plain text password
   * @return BCrypt hashed password
   * @throws IllegalArgumentException if password is invalid
   */
  public String hashPasswordWithBCrypt(String rawPassword) {
    validatePasswordForHashing(rawPassword);
    String hashed = bcryptEncoder.encode(rawPassword);
    log.debug("Password hashed successfully with BCrypt");
    return hashed;
  }

  /**
   * Hashes a password using Argon2 with salt.
   *
   * @param rawPassword The plain text password
   * @return Argon2 hashed password
   * @throws IllegalArgumentException if password is invalid
   */
  public String hashPasswordWithArgon2(String rawPassword) {
    validatePasswordForHashing(rawPassword);
    String hashed = argon2Encoder.encode(rawPassword);
    log.debug("Password hashed successfully with Argon2");
    return hashed;
  }

  /**
   * Verifies a password against its BCrypt hash.
   *
   * @param rawPassword The plain text password to verify
   * @param hashedPassword The BCrypt hashed password
   * @return true if password matches the hash
   */
  public boolean verifyBCryptPassword(String rawPassword, String hashedPassword) {
    if (rawPassword == null || hashedPassword == null) {
      return false;
    }

    try {
      boolean matches = bcryptEncoder.matches(rawPassword, hashedPassword);
      log.debug("BCrypt password verification: {}", matches ? "success" : "failed");
      return matches;
    } catch (Exception e) {
      log.warn("BCrypt password verification failed with exception", e);
      return false;
    }
  }

  /**
   * Verifies a password against its Argon2 hash.
   *
   * @param rawPassword The plain text password to verify
   * @param hashedPassword The Argon2 hashed password
   * @return true if password matches the hash
   */
  public boolean verifyArgon2Password(String rawPassword, String hashedPassword) {
    if (rawPassword == null || hashedPassword == null) {
      return false;
    }

    try {
      boolean matches = argon2Encoder.matches(rawPassword, hashedPassword);
      log.debug("Argon2 password verification: {}", matches ? "success" : "failed");
      return matches;
    } catch (Exception e) {
      log.warn("Argon2 password verification failed with exception", e);
      return false;
    }
  }

  /**
   * Checks the strength of a password and returns a detailed assessment.
   *
   * @param password The password to assess
   * @return PasswordStrength assessment result
   */
  public PasswordStrength assessPasswordStrength(String password) {
    if (password == null) {
      return new PasswordStrength(PasswordStrengthLevel.VERY_WEAK, "Password cannot be null");
    }

    PasswordStrength.PasswordStrengthBuilder builder = PasswordStrength.builder();
    int score = 0;
    StringBuilder feedback = new StringBuilder();

    // Length check
    if (password.length() < MIN_PASSWORD_LENGTH) {
      feedback
          .append("Password must be at least ")
          .append(MIN_PASSWORD_LENGTH)
          .append(" characters. ");
    } else if (password.length() >= RECOMMENDED_MIN_LENGTH) {
      score += 2;
    } else {
      score += 1;
    }

    if (password.length() > MAX_PASSWORD_LENGTH) {
      feedback
          .append("Password must not exceed ")
          .append(MAX_PASSWORD_LENGTH)
          .append(" characters. ");
    }

    // Character variety checks
    if (UPPERCASE_PATTERN.matcher(password).matches()) {
      score += 1;
    } else {
      feedback.append("Add uppercase letters. ");
    }

    if (LOWERCASE_PATTERN.matcher(password).matches()) {
      score += 1;
    } else {
      feedback.append("Add lowercase letters. ");
    }

    if (DIGIT_PATTERN.matcher(password).matches()) {
      score += 1;
    } else {
      feedback.append("Add numbers. ");
    }

    if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
      score += 2;
    } else {
      feedback.append("Add special characters (!@#$%^&*). ");
    }

    // Common pattern check
    if (COMMON_PATTERNS.matcher(password).matches()) {
      score -= 2;
      feedback.append("Avoid common passwords and patterns. ");
    }

    // Repetitive character check
    if (hasRepetitiveCharacters(password)) {
      score -= 1;
      feedback.append("Avoid repetitive character patterns. ");
    }

    // Sequential character check
    if (hasSequentialCharacters(password)) {
      score -= 1;
      feedback.append("Avoid sequential character patterns (abc, 123). ");
    }

    // Determine strength level
    PasswordStrengthLevel level;
    if (score >= 7) {
      level = PasswordStrengthLevel.VERY_STRONG;
    } else if (score >= 5) {
      level = PasswordStrengthLevel.STRONG;
    } else if (score >= 3) {
      level = PasswordStrengthLevel.MODERATE;
    } else if (score >= 1) {
      level = PasswordStrengthLevel.WEAK;
    } else {
      level = PasswordStrengthLevel.VERY_WEAK;
    }

    String feedbackMessage =
        feedback.length() > 0
            ? feedback.toString().trim()
            : "Password meets security requirements.";

    return builder
        .level(level)
        .score(score)
        .feedback(feedbackMessage)
        .meetsMinimumRequirements(score >= 3 && password.length() >= MIN_PASSWORD_LENGTH)
        .build();
  }

  /**
   * Generates a cryptographically secure random password.
   *
   * @param length The desired password length (minimum 8, maximum 128)
   * @param includeSpecialChars Whether to include special characters
   * @return A secure random password
   */
  public String generateSecurePassword(int length, boolean includeSpecialChars) {
    if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
      throw new IllegalArgumentException(
          "Password length must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH);
    }

    String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String lowercase = "abcdefghijklmnopqrstuvwxyz";
    String digits = "0123456789";
    String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    StringBuilder characterSet = new StringBuilder();
    characterSet.append(uppercase).append(lowercase).append(digits);

    if (includeSpecialChars) {
      characterSet.append(special);
    }

    StringBuilder password = new StringBuilder();

    // Ensure at least one character from each required category
    password.append(uppercase.charAt(secureRandom.nextInt(uppercase.length())));
    password.append(lowercase.charAt(secureRandom.nextInt(lowercase.length())));
    password.append(digits.charAt(secureRandom.nextInt(digits.length())));

    if (includeSpecialChars) {
      password.append(special.charAt(secureRandom.nextInt(special.length())));
    }

    // Fill the rest with random characters
    int remainingLength = length - password.length();
    for (int i = 0; i < remainingLength; i++) {
      password.append(characterSet.charAt(secureRandom.nextInt(characterSet.length())));
    }

    // Shuffle the password to avoid predictable patterns
    return shuffleString(password.toString());
  }

  /**
   * Safely clears a password character array from memory.
   *
   * @param password The password character array to clear
   */
  public void clearPassword(char[] password) {
    if (password != null) {
      for (int i = 0; i < password.length; i++) {
        password[i] = '\0';
      }
    }
  }

  private void validatePasswordForHashing(String password) {
    if (password == null) {
      throw new IllegalArgumentException("Password cannot be null");
    }
    if (password.length() > MAX_PASSWORD_LENGTH) {
      throw new IllegalArgumentException(
          "Password too long (max " + MAX_PASSWORD_LENGTH + " characters)");
    }
  }

  private boolean hasRepetitiveCharacters(String password) {
    int consecutiveCount = 1;
    for (int i = 1; i < password.length(); i++) {
      if (password.charAt(i) == password.charAt(i - 1)) {
        consecutiveCount++;
        if (consecutiveCount >= 3) {
          return true;
        }
      } else {
        consecutiveCount = 1;
      }
    }
    return false;
  }

  private boolean hasSequentialCharacters(String password) {
    String lower = password.toLowerCase();
    for (int i = 2; i < lower.length(); i++) {
      char c1 = lower.charAt(i - 2);
      char c2 = lower.charAt(i - 1);
      char c3 = lower.charAt(i);

      // Check for ascending sequence
      if (c1 + 1 == c2 && c2 + 1 == c3) {
        return true;
      }

      // Check for descending sequence
      if (c1 - 1 == c2 && c2 - 1 == c3) {
        return true;
      }
    }
    return false;
  }

  private String shuffleString(String str) {
    char[] chars = str.toCharArray();
    for (int i = chars.length - 1; i > 0; i--) {
      int j = secureRandom.nextInt(i + 1);
      char temp = chars[i];
      chars[i] = chars[j];
      chars[j] = temp;
    }
    return new String(chars);
  }

  /** Password strength assessment result. */
  public static class PasswordStrength {
    private final PasswordStrengthLevel level;
    private final int score;
    private final String feedback;
    private final boolean meetsMinimumRequirements;

    private PasswordStrength(
        PasswordStrengthLevel level, int score, String feedback, boolean meetsMinimumRequirements) {
      this.level = level;
      this.score = score;
      this.feedback = feedback;
      this.meetsMinimumRequirements = meetsMinimumRequirements;
    }

    public PasswordStrength(PasswordStrengthLevel level, String feedback) {
      this(level, 0, feedback, false);
    }

    public static PasswordStrengthBuilder builder() {
      return new PasswordStrengthBuilder();
    }

    public PasswordStrengthLevel getLevel() {
      return level;
    }

    public int getScore() {
      return score;
    }

    public String getFeedback() {
      return feedback;
    }

    public boolean meetsMinimumRequirements() {
      return meetsMinimumRequirements;
    }

    public static class PasswordStrengthBuilder {
      private PasswordStrengthLevel level;
      private int score;
      private String feedback;
      private boolean meetsMinimumRequirements;

      public PasswordStrengthBuilder level(PasswordStrengthLevel level) {
        this.level = level;
        return this;
      }

      public PasswordStrengthBuilder score(int score) {
        this.score = score;
        return this;
      }

      public PasswordStrengthBuilder feedback(String feedback) {
        this.feedback = feedback;
        return this;
      }

      public PasswordStrengthBuilder meetsMinimumRequirements(boolean meetsMinimumRequirements) {
        this.meetsMinimumRequirements = meetsMinimumRequirements;
        return this;
      }

      public PasswordStrength build() {
        return new PasswordStrength(level, score, feedback, meetsMinimumRequirements);
      }
    }
  }

  /** Password strength levels. */
  public enum PasswordStrengthLevel {
    VERY_WEAK,
    WEAK,
    MODERATE,
    STRONG,
    VERY_STRONG
  }
}
