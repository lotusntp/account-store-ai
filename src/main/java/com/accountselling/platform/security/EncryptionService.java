package com.accountselling.platform.security;

import com.accountselling.platform.exception.security.EncryptionException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for encrypting and decrypting sensitive data using AES-256-GCM.
 *
 * <p>บริการสำหรับการเข้ารหัสและถอดรหัสข้อมูลที่มีความสำคัญด้วย AES-256-GCM
 * รองรับการเข้ารหัสข้อมูลสำคัญ เช่น account credentials และ payment information
 */
@Service
@Slf4j
public class EncryptionService {

  private static final String ENCRYPTION_ALGORITHM = "AES";
  private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12; // 96 bits
  private static final int GCM_TAG_LENGTH = 16; // 128 bits
  private static final int KEY_LENGTH = 256; // AES-256

  private final SecretKey secretKey;
  private final SecureRandom secureRandom;

  public EncryptionService(@Value("${app.encryption.key:}") String encryptionKey) {
    this.secureRandom = new SecureRandom();
    this.secretKey = initializeSecretKey(encryptionKey);
    log.info("EncryptionService initialized with AES-256-GCM");
  }

  /**
   * Encrypts sensitive data using AES-256-GCM encryption.
   *
   * @param plaintext The data to encrypt
   * @return Base64-encoded encrypted data with IV prepended
   * @throws EncryptionException if encryption fails
   */
  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isEmpty()) {
      throw new IllegalArgumentException("Plaintext cannot be null or empty");
    }

    try {
      // Generate random IV for each encryption
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      // Initialize cipher for encryption
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

      // Encrypt the data
      byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Combine IV + encrypted data
      byte[] result = new byte[GCM_IV_LENGTH + encryptedData.length];
      System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
      System.arraycopy(encryptedData, 0, result, GCM_IV_LENGTH, encryptedData.length);

      // Return Base64 encoded result
      String encrypted = Base64.getEncoder().encodeToString(result);
      log.debug("Successfully encrypted data of length: {}", plaintext.length());
      return encrypted;

    } catch (Exception e) {
      log.error("Failed to encrypt data", e);
      throw new EncryptionException("Failed to encrypt sensitive data", e);
    }
  }

  /**
   * Decrypts data that was encrypted using the encrypt method.
   *
   * @param encryptedData Base64-encoded encrypted data with IV prepended
   * @return The decrypted plaintext
   * @throws EncryptionException if decryption fails
   */
  public String decrypt(String encryptedData) {
    if (encryptedData == null || encryptedData.isEmpty()) {
      throw new IllegalArgumentException("Encrypted data cannot be null or empty");
    }

    try {
      // Decode from Base64
      byte[] decodedData = Base64.getDecoder().decode(encryptedData);

      if (decodedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
        throw new EncryptionException("Invalid encrypted data format");
      }

      // Extract IV from the beginning
      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);

      // Extract encrypted content
      byte[] encryptedContent = new byte[decodedData.length - GCM_IV_LENGTH];
      System.arraycopy(decodedData, GCM_IV_LENGTH, encryptedContent, 0, encryptedContent.length);

      // Initialize cipher for decryption
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

      // Decrypt the data
      byte[] decryptedData = cipher.doFinal(encryptedContent);
      String result = new String(decryptedData, StandardCharsets.UTF_8);

      log.debug("Successfully decrypted data");
      return result;

    } catch (Exception e) {
      log.error("Failed to decrypt data", e);
      throw new EncryptionException("Failed to decrypt sensitive data", e);
    }
  }

  /**
   * Encrypts sensitive data and returns it as SecureString for safer memory handling.
   *
   * @param plaintext The data to encrypt
   * @return SecureString containing encrypted data
   * @throws EncryptionException if encryption fails
   */
  public SecureString encryptToSecureString(String plaintext) {
    String encrypted = encrypt(plaintext);
    return new SecureString(encrypted.toCharArray());
  }

  /**
   * Decrypts data from SecureString and returns plaintext.
   *
   * @param secureEncryptedData SecureString containing encrypted data
   * @return The decrypted plaintext
   * @throws EncryptionException if decryption fails
   */
  public String decryptFromSecureString(SecureString secureEncryptedData) {
    String encryptedData = new String(secureEncryptedData.getChars());
    return decrypt(encryptedData);
  }

  /**
   * Checks if the encryption service is properly initialized and functional.
   *
   * @return true if the service can encrypt and decrypt successfully
   */
  public boolean isHealthy() {
    try {
      String testData = "health_check_test_data";
      String encrypted = encrypt(testData);
      String decrypted = decrypt(encrypted);
      return testData.equals(decrypted);
    } catch (Exception e) {
      log.warn("Encryption service health check failed", e);
      return false;
    }
  }

  /**
   * Generates a new AES-256 key for encryption. Used for key rotation.
   *
   * @return Base64-encoded new encryption key
   * @throws EncryptionException if key generation fails
   */
  public String generateNewKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
      keyGenerator.init(KEY_LENGTH);
      SecretKey newKey = keyGenerator.generateKey();
      String encodedKey = Base64.getEncoder().encodeToString(newKey.getEncoded());
      log.info("Generated new encryption key");
      return encodedKey;
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to generate new encryption key", e);
      throw new EncryptionException("Failed to generate new encryption key", e);
    }
  }

  private SecretKey initializeSecretKey(String encryptionKey) {
    try {
      if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
        log.warn("No encryption key provided, generating new key");
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
        keyGenerator.init(KEY_LENGTH);
        return keyGenerator.generateKey();
      }

      // Decode the provided key
      byte[] decodedKey = Base64.getDecoder().decode(encryptionKey.trim());
      if (decodedKey.length != KEY_LENGTH / 8) {
        throw new IllegalArgumentException(
            "Invalid key length. Expected "
                + (KEY_LENGTH / 8)
                + " bytes, got "
                + decodedKey.length);
      }

      log.info("Using provided encryption key");
      return new SecretKeySpec(decodedKey, ENCRYPTION_ALGORITHM);

    } catch (Exception e) {
      log.error("Failed to initialize encryption key, generating new key", e);
      try {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
        keyGenerator.init(KEY_LENGTH);
        return keyGenerator.generateKey();
      } catch (NoSuchAlgorithmException ex) {
        throw new RuntimeException("Failed to initialize encryption service", ex);
      }
    }
  }
}
