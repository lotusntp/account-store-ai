package com.accountselling.platform.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for creating a new category. DTO สำหรับการสร้างหมวดหมู่ใหม่ */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a new category")
public class CategoryCreateRequestDto {

  @NotBlank(message = "Category name is required")
  @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
  @Schema(description = "Category name", example = "Gaming Accounts", required = true)
  private String name;

  @Size(max = 500, message = "Description cannot exceed 500 characters")
  @Schema(
      description = "Category description",
      example = "Digital gaming accounts for various platforms")
  private String description;

  @Schema(
      description = "Parent category ID (null for root category)",
      example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
  private UUID parentId;

  @Schema(description = "Sort order for category display", example = "1")
  private Integer sortOrder;
}
