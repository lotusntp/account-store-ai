package com.accountselling.platform.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating new products.
 * Contains all required and optional fields for product creation.
 * 
 * DTO สำหรับการสร้างสินค้าใหม่
 * ประกอบด้วยฟิลด์ที่จำเป็นและทางเลือกสำหรับการสร้างสินค้า
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequestDto {

    @NotBlank(message = "Product name cannot be blank")
    @Size(max = 200, message = "Product name cannot exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Product description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Product price cannot be null")
    @DecimalMin(value = "0.01", message = "Product price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Product price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Category ID cannot be null")
    private UUID categoryId;

    @Size(max = 100, message = "Server name cannot exceed 100 characters")
    private String server;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    @Min(value = 0, message = "Sort order cannot be negative")
    @Builder.Default
    private Integer sortOrder = 0;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @Builder.Default
    private Boolean active = true;
}