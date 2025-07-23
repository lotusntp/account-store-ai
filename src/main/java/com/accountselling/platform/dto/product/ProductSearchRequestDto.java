package com.accountselling.platform.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for product search and filtering requests.
 * Used in REST API endpoints for advanced product search functionality.
 * 
 * DTO สำหรับการค้นหาและกรองสินค้า
 * ใช้ใน REST API endpoints สำหรับฟังก์ชันค้นหาสินค้าแบบขั้นสูง
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequestDto {

    @Size(max = 200, message = "Search name cannot exceed 200 characters")
    private String name;

    private UUID categoryId;

    @Builder.Default
    private Boolean includeSubcategories = false;

    @Size(max = 100, message = "Server name cannot exceed 100 characters")
    private String server;

    @DecimalMin(value = "0.01", message = "Minimum price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.01", message = "Maximum price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal maxPrice;

    private Boolean inStock;

    @Builder.Default
    private Boolean activeOnly = true;

    // Pagination parameters
    @Min(value = 0, message = "Page number cannot be negative")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Builder.Default
    private Integer size = 20;

    @Size(max = 50, message = "Sort field cannot exceed 50 characters")
    @Builder.Default
    private String sortBy = "sortOrder";

    @Pattern(regexp = "asc|desc", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Sort direction must be 'asc' or 'desc'")
    @Builder.Default
    private String sortDirection = "asc";

    /**
     * Custom validation to ensure maxPrice is greater than or equal to minPrice.
     * การตรวจสอบที่กำหนดเองเพื่อให้แน่ใจว่า maxPrice มากกว่าหรือเท่ากับ minPrice
     */
    public boolean isValidPriceRange() {
        if (minPrice != null && maxPrice != null) {
            return maxPrice.compareTo(minPrice) >= 0;
        }
        return true;
    }

    /**
     * Check if any search criteria is provided.
     * ตรวจสอบว่ามีเกณฑ์การค้นหาใดๆ ถูกระบุหรือไม่
     */
    public boolean hasSearchCriteria() {
        return name != null || categoryId != null || server != null || 
               minPrice != null || maxPrice != null || inStock != null;
    }

    /**
     * Check if price filtering is enabled.
     * ตรวจสอบว่าเปิดใช้งานการกรองตามราคาหรือไม่
     */
    public boolean hasPriceFiltering() {
        return minPrice != null || maxPrice != null;
    }
}