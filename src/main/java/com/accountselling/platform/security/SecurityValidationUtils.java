package com.accountselling.platform.security;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class for security validation and data sanitization.
 *
 * <p>คลาสยูทิลิตี้สำหรับการตรวจสอบความปลอดภัยและการทำความสะอาดข้อมูล
 */
@Component
@Slf4j
public class SecurityValidationUtils {

  // Common injection patterns
  private static final List<Pattern> INJECTION_PATTERNS =
      Arrays.asList(
          Pattern.compile(
              ".*(<script|</script|javascript:|vbscript:|onload=|onerror=).*",
              Pattern.CASE_INSENSITIVE),
          Pattern.compile(
              ".*(union|select|insert|update|delete|drop|create|alter)\\s+.*",
              Pattern.CASE_INSENSITIVE),
          Pattern.compile(".*('|(\\-\\-)|;|\\||\\*|%).*"),
          Pattern.compile(".*(<|>|&lt;|&gt;|&amp;|&quot;|&#).*"));

  // Sensitive data patterns
  private static final Pattern CREDIT_CARD_PATTERN =
      Pattern.compile("\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b");
  private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

  // Password strength patterns
  private static final Pattern WEAK_PASSWORD_PATTERNS =
      Pattern.compile(
          ".*(password|123456|qwerty|admin|root|user|guest|test|demo).*", Pattern.CASE_INSENSITIVE);

  /**
   * Validates input data for potential security threats.
   *
   * @param input The input data to validate
   * @param fieldName Name of the field being validated
   * @return ValidationResult containing validation status and details
   */
  public ValidationResult validateInput(String input, String fieldName) {
    if (input == null) {
      return ValidationResult.valid();
    }

    // Check for injection patterns
    for (Pattern pattern : INJECTION_PATTERNS) {
      if (pattern.matcher(input).matches()) {
        log.warn("Potential injection detected in field '{}': pattern matched", fieldName);
        return ValidationResult.invalid("Potentially unsafe characters detected");
      }
    }

    // Check for excessive length
    if (input.length() > 10000) {
      log.warn(
          "Excessive input length detected in field '{}': {} characters",
          fieldName,
          input.length());
      return ValidationResult.invalid("Input too long");
    }

    // Check for null bytes
    if (input.contains("\0")) {
      log.warn("Null byte detected in field '{}'", fieldName);
      return ValidationResult.invalid("Invalid characters detected");
    }

    return ValidationResult.valid();
  }

  /**
   * Sanitizes input data by removing or escaping potentially dangerous characters.
   *
   * @param input The input data to sanitize
   * @return Sanitized input data
   */
  public String sanitizeInput(String input) {
    if (input == null) {
      return null;
    }

    // Remove null bytes
    String sanitized = input.replace("\0", "");

    // Escape HTML characters
    sanitized =
        sanitized
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");

    // Remove control characters except common whitespace
    sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

    // Trim excessive whitespace but preserve line breaks and tabs
    sanitized = sanitized.trim().replaceAll("[ ]+", " ");

    return sanitized;
  }

  /**
   * Validates password strength and security.
   *
   * @param password The password to validate
   * @return ValidationResult containing validation status and feedback
   */
  public ValidationResult validatePasswordSecurity(String password) {
    if (password == null || password.isEmpty()) {
      return ValidationResult.invalid("Password cannot be empty");
    }

    // Check minimum length
    if (password.length() < 8) {
      return ValidationResult.invalid("Password must be at least 8 characters long");
    }

    // Check maximum length
    if (password.length() > 128) {
      return ValidationResult.invalid("Password cannot exceed 128 characters");
    }

    // Check for weak patterns
    if (WEAK_PASSWORD_PATTERNS.matcher(password).matches()) {
      return ValidationResult.invalid("Password contains common weak patterns");
    }

    // Check character variety
    boolean hasUpper = password.matches(".*[A-Z].*");
    boolean hasLower = password.matches(".*[a-z].*");
    boolean hasDigit = password.matches(".*\\d.*");
    boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>?].*");

    if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
      return ValidationResult.invalid(
          "Password must contain uppercase, lowercase, digit, and special characters");
    }

    return ValidationResult.valid();
  }

  /**
   * Checks if data contains sensitive information that should be encrypted.
   *
   * @param data The data to check
   * @return true if data appears to contain sensitive information
   */
  public boolean containsSensitiveData(String data) {
    if (data == null || data.isEmpty()) {
      return false;
    }

    String lowerData = data.toLowerCase();

    // Check for common sensitive data indicators
    if (lowerData.contains("password")
        || lowerData.contains("secret")
        || lowerData.contains("token")
        || lowerData.contains("key")
        || lowerData.contains("credential")
        || lowerData.contains("auth")) {
      return true;
    }

    // Check for structured sensitive data patterns
    if (CREDIT_CARD_PATTERN.matcher(data).find() || SSN_PATTERN.matcher(data).find()) {
      return true;
    }

    // Check for email addresses (might be sensitive in some contexts)
    if (EMAIL_PATTERN.matcher(data).find()) {
      return true;
    }

    return false;
  }

  /**
   * Masks sensitive data for logging purposes.
   *
   * @param data The data to mask
   * @param fieldName Name of the field (for context)
   * @return Masked data safe for logging
   */
  public String maskSensitiveData(String data, String fieldName) {
    if (data == null || data.isEmpty()) {
      return data;
    }

    String lowerFieldName = fieldName.toLowerCase();

    // Full masking for highly sensitive fields
    if (lowerFieldName.contains("password")
        || lowerFieldName.contains("secret")
        || lowerFieldName.contains("token")
        || lowerFieldName.contains("key")) {
      return "***MASKED***";
    }

    // Partial masking for other sensitive data
    if (containsSensitiveData(data)) {
      if (data.length() <= 4) {
        return "***";
      } else if (data.length() <= 8) {
        return data.substring(0, 2) + "***";
      } else {
        return data.substring(0, 3) + "***" + data.substring(data.length() - 2);
      }
    }

    return data;
  }

  /**
   * Validates that encrypted data has the expected format.
   *
   * @param encryptedData The encrypted data to validate
   * @return ValidationResult containing validation status
   */
  public ValidationResult validateEncryptedDataFormat(String encryptedData) {
    if (encryptedData == null || encryptedData.isEmpty()) {
      return ValidationResult.invalid("Encrypted data cannot be empty");
    }

    // Check if it looks like Base64 encoded data
    if (!encryptedData.matches("^[A-Za-z0-9+/]*={0,2}$")) {
      return ValidationResult.invalid("Encrypted data does not appear to be valid Base64");
    }

    // Check minimum length (IV + tag + some data)
    if (encryptedData.length() < 32) {
      return ValidationResult.invalid("Encrypted data appears too short");
    }

    return ValidationResult.valid();
  }

  /**
   * Generates a secure random string for testing purposes.
   *
   * @param length Length of the string to generate
   * @return Secure random string
   */
  public String generateSecureRandomString(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("Length must be positive");
    }

    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    java.security.SecureRandom random = new java.security.SecureRandom();
    StringBuilder sb = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }

    return sb.toString();
  }

  /** Validation result class. */
  public static class ValidationResult {
    private final boolean valid;
    private final String message;

    private ValidationResult(boolean valid, String message) {
      this.valid = valid;
      this.message = message;
    }

    public static ValidationResult valid() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String message) {
      return new ValidationResult(false, message);
    }

    public boolean isValid() {
      return valid;
    }

    public String getMessage() {
      return message;
    }

    @Override
    public String toString() {
      return "ValidationResult{valid=" + valid + ", message='" + message + "'}";
    }
  }
}
