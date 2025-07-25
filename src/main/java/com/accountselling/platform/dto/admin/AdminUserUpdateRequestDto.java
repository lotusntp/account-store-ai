package com.accountselling.platform.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for updating user information by admin. DTO สำหรับการอัปเดตข้อมูลผู้ใช้โดย admin */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for updating user information (admin only)")
public class AdminUserUpdateRequestDto {

  @Email(message = "Invalid email format")
  @Size(max = 100, message = "Email cannot exceed 100 characters")
  @Schema(description = "Email address", example = "john.doe@example.com")
  private String email;

  @Size(max = 50, message = "First name cannot exceed 50 characters")
  @Schema(description = "First name", example = "John")
  private String firstName;

  @Size(max = 50, message = "Last name cannot exceed 50 characters")
  @Schema(description = "Last name", example = "Doe")
  private String lastName;

  @Schema(description = "Whether the user account is enabled", example = "true")
  private Boolean enabled;

  /** Check if any field is provided for update. ตรวจสอบว่ามีฟิลด์ใดถูกระบุสำหรับการอัปเดตหรือไม่ */
  public boolean hasUpdateFields() {
    return email != null || firstName != null || lastName != null || enabled != null;
  }
}
