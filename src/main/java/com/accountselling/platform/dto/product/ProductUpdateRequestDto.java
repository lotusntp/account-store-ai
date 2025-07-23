package com.accountselling.platform.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for updating existing products.
 * Contains fields that can be updated for existing products.
 * 
 * DTO สำหรับการอัปเดตสินค้าที่มีอยู่
 * ประกอบด้วยฟิลด์ที่สามารถอัปเดตได้สำหรับสินค้าที่มีอยู่
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequestDto {

    @Size(max = 200, message = "Product name cannot exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Product description cannot exceed 2000 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Product price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Product price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal price;

    private UUID categoryId;

    @Size(max = 100, message = "Server name cannot exceed 100 characters")
    private String server;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;

    private Boolean active;

    /**
     * Check if any field is provided for update.
     * ตรวจสอบว่ามีฟิลด์ใดถูกระบุสำหรับการอัปเดตหรือไม่
     */
    public boolean hasUpdateFields() {
        return name != null || description != null || price != null || 
               categoryId != null || server != null || imageUrl != null || 
               sortOrder != null || lowStockThreshold != null || active != null;
    }
}