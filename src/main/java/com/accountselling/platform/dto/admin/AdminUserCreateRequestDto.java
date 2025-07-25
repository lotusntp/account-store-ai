package com.accountselling.platform.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for creating a new user by admin. DTO สำหรับการสร้างผู้ใช้ใหม่โดย admin */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a new user (admin only)")
public class AdminUserCreateRequestDto {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Schema(description = "Username", example = "johndoe", required = true)
  private String username;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
  @Schema(description = "Password", example = "password123", required = true)
  private String password;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  @Size(max = 100, message = "Email cannot exceed 100 characters")
  @Schema(description = "Email address", example = "john.doe@example.com", required = true)
  private String email;

  @Size(max = 50, message = "First name cannot exceed 50 characters")
  @Schema(description = "First name", example = "John")
  private String firstName;

  @Size(max = 50, message = "Last name cannot exceed 50 characters")
  @Schema(description = "Last name", example = "Doe")
  private String lastName;

  @Schema(description = "Whether the user account is enabled", example = "true")
  private Boolean enabled = true;
}
