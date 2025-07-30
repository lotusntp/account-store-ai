package com.accountselling.platform.exception.security;

import com.accountselling.platform.exception.BaseException;

/**
 * Exception thrown when encryption or decryption operations fail.
 *
 * <p>Exception ที่เกิดขึ้นเมื่อการเข้ารหัสหรือถอดรหัสข้อมูลล้มเหลว
 */
public class EncryptionException extends BaseException {

  public EncryptionException(String message) {
    super(message);
  }

  public EncryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
