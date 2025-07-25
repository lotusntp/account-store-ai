package com.accountselling.platform.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for token refresh request. Contains refresh token for renewing access token.
 *
 * <p>DTO สำหรับคำขอต่ออายุ token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDto {

  @NotBlank(message = "Refresh token cannot be blank")
  private String refreshToken;
}
