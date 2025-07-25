package com.accountselling.platform.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating stock item information.
 * DTO สำหรับการอัปเดตข้อมูลสต็อก
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for updating stock item information")
public class StockUpdateRequestDto {

    @Size(max = 1000, message = "Additional info cannot exceed 1000 characters")
    @Schema(description = "Additional information about the account", 
            example = "Level 80 character with rare items and premium subscription")
    private String additionalInfo;
}