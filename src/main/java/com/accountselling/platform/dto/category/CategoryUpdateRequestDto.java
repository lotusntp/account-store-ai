package com.accountselling.platform.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing category.
 * DTO สำหรับการอัปเดตหมวดหมู่ที่มีอยู่
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for updating an existing category")
public class CategoryUpdateRequestDto {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Schema(description = "Category name", example = "Gaming Accounts", required = true)
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Category description", example = "Digital gaming accounts for various platforms")
    private String description;

    @Schema(description = "Sort order for category display", example = "1")
    private Integer sortOrder;

    @Schema(description = "Whether the category is active", example = "true")
    private Boolean active;
}